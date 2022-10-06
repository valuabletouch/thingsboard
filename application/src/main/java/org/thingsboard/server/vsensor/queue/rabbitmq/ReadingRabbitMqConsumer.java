package org.thingsboard.server.vsensor.queue.rabbitmq;

import java.io.IOException;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import javax.annotation.PostConstruct;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

import org.javatuples.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.BooleanDataEntry;
import org.thingsboard.server.common.data.kv.DoubleDataEntry;
import org.thingsboard.server.common.data.kv.JsonDataEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.vsensor.Reading;
import org.thingsboard.server.common.data.vsensor.ReadingType;
import org.thingsboard.server.common.data.vsensor.ReadingTypeService;
import org.thingsboard.server.common.data.vsensor.TransformationService;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.queue.ServiceQueue;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.common.msg.session.SessionMsgType;
import org.thingsboard.server.common.stats.MessagesStats;
import org.thingsboard.server.common.stats.StatsFactory;
import org.thingsboard.server.common.stats.StatsType;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.vsensor.mongo.configurations.TransformationEntity;
import org.thingsboard.server.dao.vsensor.mongo.configurations.TransformationSystem;
import org.thingsboard.server.gen.transport.TransportProtos.ToRuleEngineMsg;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.provider.TbQueueProducerProvider;
import org.thingsboard.server.queue.rabbitmq.TbRabbitMqSettings;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@ConditionalOnExpression("'${queue.type:null}'=='rabbitmq'")
public class ReadingRabbitMqConsumer {

    private static final String DEVICE_CACHE_NAME = "devices";
    private static final String DEVICE_PROFILE_CACHE_NAME = "deviceProfiles";
    private static final long CACHE_TTL = 15 * 60l;
    private static final long CACHE_EVICT_PERIOD = 60 * 1000l;

    private static final List<Pair<DeviceId, LocalDateTime>> deviceCacheExpireList = new ArrayList<>();
    private static final List<Pair<DeviceProfileId, LocalDateTime>> deviceProfileCacheExpireList = new ArrayList<>();

    private static final Gson gson = new Gson();

    private static final JsonParser jsonParser = new JsonParser();

    private static final List<SimpleDateFormat> simpleDateFormats = new ArrayList<>();

    @Value("${queue.rabbitmq.vsensor.exchange_name:}")
    private String exchangeName;

    @Value("${queue.rabbitmq.vsensor.routing_key:}")
    private String routingKey;

    @Value("${queue.rabbitmq.vsensor.queue_name:}")
    private String queueName;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private TbRabbitMqSettings rabbitMqSettings;

    @Autowired
    private ReadingTypeService readingTypeService;

    @Autowired
    protected DeviceService deviceService;

    @Autowired
    protected DeviceProfileService deviceProfileService;

    @Autowired
    private TbQueueProducerProvider producerProvider;

    @Autowired
    private PartitionService partitionService;

    @Autowired
    private StatsFactory statsFactory;

    @Autowired
    private TransformationService transformationService;

    @Autowired
    private TransformationSystem transformationSystem;

    @Autowired
    private TransformationEntity transformationEntity;

    private TbQueueProducer<TbProtoQueueMsg<ToRuleEngineMsg>> ruleEngineMsgProducer;

    private MessagesStats ruleEngineProducerStats;

    @PostConstruct
    private void init() {
        try {
            if (isNullOrEmpty(exchangeName) || isNullOrEmpty(routingKey) || isNullOrEmpty(queueName)) {
                log.error("RabbitMQ queue configuration is not set properly. Skipping queue initialization.");
                return;
            }

            simpleDateFormats.add(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX"));
            simpleDateFormats.add(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SXXX"));
            simpleDateFormats.add(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSXXX"));
            simpleDateFormats.add(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX"));
            simpleDateFormats.add(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSXXX"));
            simpleDateFormats.add(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSXXX"));
            simpleDateFormats.add(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX"));

            Connection connection = rabbitMqSettings.getConnectionFactory().newConnection();

            Channel channel = connection.createChannel();

            channel.queueDeclare(queueName, true, false, false, null);

            channel.queueBind(queueName, exchangeName, routingKey);

            DefaultConsumer defaultConsumer = new DefaultConsumer(channel) {

                @Override
                public void handleDelivery(String tag, Envelope env, AMQP.BasicProperties props, byte[] body) {
                    try {
                        MessageContext messageContext = gson.fromJson(
                                props.getHeaders().get("message_context").toString(),
                                MessageContext.class);

                        User user = gson.fromJson(messageContext.getUser().toString(), User.class);

                        if (!Arrays.asList(user.getScopes()).contains("thingsboard")) {
                            String message = new String(body, StandardCharsets.UTF_8);
                            Reading reading = gson.fromJson(message, Reading.class);
                            TsKvEntry tsKvEntry = convertResultToTsKvEntry(reading);
                            TenantId tenantId = getTenantId(reading.getTenantId());
                            DeviceId deviceId = getDeviceId(reading.getDataSourceId());

                            sendToRuleEngine(tenantId, deviceId, tsKvEntry);
                        }
                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                    }
                }
            };

            channel.basicConsume(queueName, true, defaultConsumer);

            ruleEngineMsgProducer = producerProvider.getRuleEngineMsgProducer();

            ruleEngineProducerStats = statsFactory.createMessagesStats(StatsType.RULE_ENGINE.getName() + ".producer");
        } catch (IOException | TimeoutException e) {
            log.error("Failed to create connection.", e);
        }
    }

    private void sendToRuleEngine(TenantId tenantId, DeviceId deviceId, TsKvEntry tsKvEntry)
            throws InterruptedException, ExecutionException {
        try {
            Device device = findDeviceById(tenantId, deviceId);
            DeviceProfile deviceProfile = findDeviceProfileById(tenantId, device.getDeviceProfileId());

            TbMsgMetaData metaData = new TbMsgMetaData();
            metaData.putValue("deviceName", device.getName());
            metaData.putValue("deviceType", device.getType());
            metaData.putValue("ts", tsKvEntry.getTs() + "");

            JsonObject valuesJson = getValuesJsonElement(tsKvEntry);
            valuesJson.addProperty("$is_already_saved", "1");

            JsonObject messageJson = new JsonObject();
            messageJson.add("values", valuesJson);
            messageJson.addProperty("ts", tsKvEntry.getTs());

            RuleChainId ruleChainId;
            String tbQueueName;

            if (deviceProfile == null) {
                log.warn("[{}] Device profile is null!", device.getDeviceProfileId());
                ruleChainId = null;
                tbQueueName = ServiceQueue.MAIN;
            } else {
                ruleChainId = deviceProfile.getDefaultRuleChainId();
                String defaultQueueName = deviceProfile.getDefaultQueueName();
                tbQueueName = defaultQueueName != null ? defaultQueueName : ServiceQueue.MAIN;
            }

            TbMsg tbMsg = TbMsg.newMsg(tbQueueName, SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, metaData,
                    gson.toJson(messageJson), ruleChainId,
                    null);

            TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_RULE_ENGINE, tenantId,
                    tbMsg.getOriginator());

            ToRuleEngineMsg msg = ToRuleEngineMsg.newBuilder().setTbMsg(TbMsg.toByteString(tbMsg))
                    .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                    .setTenantIdLSB(tenantId.getId().getLeastSignificantBits()).build();
            ruleEngineProducerStats.incrementTotal();
            ruleEngineMsgProducer.send(tpi, new TbProtoQueueMsg<>(tbMsg.getId(), msg), null);
        } catch (Exception e) {
            log.error("[{}] Failed to send message to queue.", deviceId, e);
        }
    }

    private static KvEntry toKvEntry(Reading reading, String key) {
        KvEntry kvEntry = null;

        if (reading.getValueDecimal() != null) {
            BigDecimal decimalV = reading.getValueDecimal();
            kvEntry = new DoubleDataEntry(key, decimalV.doubleValue());
        } else if (reading.getValueLong() != null) {
            Long longV = reading.getValueLong();
            kvEntry = new LongDataEntry(key, longV);
        } else if (reading.getValueString() != null) {
            String strV = reading.getValueString();
            kvEntry = new StringDataEntry(key, strV);
        } else if (reading.getValueBoolean() != null) {
            Boolean boolV = reading.getValueBoolean();
            kvEntry = new BooleanDataEntry(key, boolV);
        } else if (reading.getValueJson() != null) {
            String jsonV = reading.getValueJson();
            kvEntry = new JsonDataEntry(key, jsonV);
        } else {
            log.warn("All values in key-value row are nullable");
        }

        return kvEntry;
    }

    private TsKvEntry convertResultToTsKvEntry(Reading reading) throws ParseException {
        long readAtTs = getEpochTime(reading.getReadAt());
        String key = reading.getReadingTypeId().toString();
        Optional<ReadingType> readingType = readingTypeService.findById(key);
        if (readingType.isPresent()) {
            String code = readingType.get().getCode();
            return new BasicTsKvEntry(readAtTs, toKvEntry(reading, code));
        } else {
            return new BasicTsKvEntry(readAtTs, toKvEntry(reading, key));
        }
    }

    private TenantId getTenantId(UUID tenantId) {
        Optional<UUID> result = transformationService.getToKey(transformationSystem.getReadingType(),
                transformationEntity.getTenant(), tenantId.toString(), transformationSystem.getThingsboard(),
                transformationEntity.getTenant());

        if (result.isPresent()) {
            return new TenantId(result.get());
        }

        log.warn("Failed to read Tenant from MongoDB.");

        return null;
    }

    private DeviceId getDeviceId(UUID dataSourceId) {
        Optional<UUID> result = transformationService.getToKey(transformationSystem.getReadingType(),
                transformationEntity.getDataSource(), dataSourceId.toString(), transformationSystem.getThingsboard(),
                transformationEntity.getDevice());

        if (result.isPresent()) {
            return new DeviceId(result.get());
        }

        log.warn("Failed to read DeviceId from MongoDB.");

        return null;
    }

    private static JsonObject getValuesJsonElement(TsKvEntry kvEntry) {
        JsonObject json = new JsonObject();

        switch (kvEntry.getDataType()) {
            case BOOLEAN:
                json.addProperty(kvEntry.getKey(), kvEntry.getBooleanValue().get());
                break;
            case LONG:
                json.addProperty(kvEntry.getKey(), kvEntry.getLongValue().get());
                break;
            case DOUBLE:
                json.addProperty(kvEntry.getKey(), kvEntry.getDoubleValue().get());
                break;
            case STRING:
                json.addProperty(kvEntry.getKey(), kvEntry.getValueAsString());
                break;
            case JSON:
                json.add(kvEntry.getKey(), jsonParser.parse(kvEntry.getJsonValue().get()));
                break;
        }

        return json;
    }

    @Cacheable(value = DEVICE_CACHE_NAME)
    private Device findDeviceById(TenantId tenantId, DeviceId deviceId) {
        deviceCacheExpireList.add(new Pair<>(deviceId, LocalDateTime.now().plusSeconds(CACHE_TTL)));

        return deviceService.findDeviceById(tenantId, deviceId);
    }

    @Cacheable(value = DEVICE_PROFILE_CACHE_NAME)
    private DeviceProfile findDeviceProfileById(TenantId tenantId, DeviceProfileId deviceProfileId) {
        deviceProfileCacheExpireList.add(new Pair<>(deviceProfileId, LocalDateTime.now().plusSeconds(CACHE_TTL)));

        return deviceProfileService.findDeviceProfileById(tenantId, deviceProfileId);
    }

    @Scheduled(fixedRate = CACHE_EVICT_PERIOD)
    private void evictExpired() {
        for (Pair<DeviceId, LocalDateTime> pair : deviceCacheExpireList) {
            if (pair.getValue1().isBefore(LocalDateTime.now())) {
                cacheManager.getCache(DEVICE_CACHE_NAME).evict(pair.getValue0());

                deviceCacheExpireList.remove(pair);
            }
        }

        for (Pair<DeviceProfileId, LocalDateTime> pair : deviceProfileCacheExpireList) {
            if (pair.getValue1().isBefore(LocalDateTime.now())) {
                cacheManager.getCache(DEVICE_PROFILE_CACHE_NAME).evict(pair.getValue0());

                deviceProfileCacheExpireList.remove(pair);
            }
        }
    }

    private static boolean isNullOrEmpty(String name) {
        return name == null || name.isEmpty();
    }

    private long getEpochTime(String value) throws ParseException {
        ParseException lastException = null;

        for (SimpleDateFormat simpleDateFormat : simpleDateFormats) {
            try {
                return simpleDateFormat.parse(value).getTime();
            } catch (ParseException pe) {
                lastException = pe;
            }
        }

        throw lastException;
    }

    @Getter
    private static class MessageContext {
        private String correlationId;
        private String spanContext;
        private String connectionId;
        private String traceId;
        private String resourceId;
        private Object user;
        private String createdAt;
    }

    @Getter
    private static class User {
        private String id;
        private String isAuthenticated;
        private String isAdmin;
        private String isSystem;
        private String[] roles;
        private String[] scopes;
        private Array claims;
    }
}
