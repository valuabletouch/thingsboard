package org.thingsboard.server.vsensor.dao.timeseries;

import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.*;
import org.thingsboard.server.common.data.vsensor.Reading;
import org.thingsboard.server.common.data.vsensor.ReadingType;
import org.thingsboard.server.common.data.vsensor.ReadingTypeService;
import org.thingsboard.server.common.data.vsensor.TransformationService;
import org.thingsboard.server.dao.dictionary.KeyDictionaryDao;
import org.thingsboard.server.dao.exception.IncorrectParameterException;
import org.thingsboard.server.dao.model.sqlts.ts.TsKvEntity;
import org.thingsboard.server.dao.model.vsensor.ReadingAggregationDto;
import org.thingsboard.server.dao.model.vsensor.ReadingEntity;
import org.thingsboard.server.dao.model.vsensor.VModelConstants;
import org.thingsboard.server.dao.service.Validator;
import org.thingsboard.server.dao.sqlts.AbstractChunkedAggregationTimeseriesDao;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.dao.util.SqlTsDao;
import org.thingsboard.server.dao.util.TimeUtils;
import org.thingsboard.server.dao.vsensor.mongo.configurations.TransformationEntity;
import org.thingsboard.server.dao.vsensor.mongo.configurations.TransformationSystem;
import org.thingsboard.server.vsensor.dao.readings.ReadingRepository;
import org.thingsboard.server.vsensor.queue.rabbitmq.ReadingRabbitMqProducer;

import javax.annotation.Nullable;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

import static org.thingsboard.server.common.data.StringUtils.isBlank;

/**
 * @author Özgün Ay
 */
@Component
@Slf4j
@SqlTsDao
@Primary
@ConditionalOnExpression("${sql.vsensor.enabled}")
public class PsqlTimeseriesDao extends AbstractChunkedAggregationTimeseriesDao implements TimeseriesService {

    @Value("${database.ts_max_intervals}")
    private long maxTsIntervals;

    @Autowired
    private ReadingRepository readingRepository;

    @Autowired
    private ReadingTypeService readingTypeService;

    @Autowired
    private TransformationSystem transformationSystem;

    @Autowired
    private TransformationEntity transformationEntity;

    @Autowired
    private TransformationService transformationService;

    @Autowired
    private ReadingRabbitMqProducer readingRabbitMqProducer;

    @Autowired
    private KeyDictionaryDao keyDictionaryDao;

    @Override
    public ListenableFuture<ReadTsKvQueryResult> findAllAsync(TenantId tenantId, EntityId entityId,
            ReadTsKvQuery query) {
        var aggParams = query.getAggParameters();

        if (query.getAggregation() == Aggregation.NONE) {
            List<TsKvEntry> tsKvEntries = new ArrayList<>();

            Optional<UUID> transformationTenantId = transformationService.getFromKey(
                    transformationSystem.getThingsboard(),
                    transformationEntity.getTenant(), tenantId.toString(), transformationSystem.getReadingType(),
                    transformationEntity.getTenant());

            Optional<UUID> transformationDataSourceId = transformationService.getFromKey(
                    transformationSystem.getThingsboard(), transformationEntity.getDevice(), entityId.toString(),
                    transformationSystem.getReadingType(), transformationEntity.getDataSource());

            Optional<ReadingType> readingType = readingTypeService.findByCode(query.getKey());

            if (transformationTenantId.isEmpty()) {
                log.warn("TenantId not found for ThingsBoard TenantId: " + tenantId.toString());
            }

            if (transformationDataSourceId.isEmpty()) {
                log.warn("DataSourceId not found for ThingsBoard EntityId: " + entityId.toString());
            }

            if (readingType.isEmpty()) {
                log.warn("ReadingType not found for code: " + query.getKey());
            }

            if (transformationTenantId.isPresent() && transformationDataSourceId.isPresent()
                    && readingType.isPresent()) {
                List<ReadingEntity> readings = readingRepository
                        .findByTenantIdAndDataSourceIdAndReadingTypeIdAndReadAtBetween(
                                transformationTenantId.get(), transformationDataSourceId.get(),
                                UUID.fromString(readingType.get().getId()),
                                longToOffsetDateTime(query.getStartTs()), longToOffsetDateTime(query.getEndTs()));

                for (ReadingEntity reading : readings) {
                    TsKvEntry tsKvEntry = convertResultToTsKvEntry(reading);
                    tsKvEntries.add(tsKvEntry);
                }
            }

            long lastTs = tsKvEntries.stream().map(TsKvEntry::getTs).max(Long::compare).orElse(query.getStartTs());

            ReadTsKvQueryResult result = new ReadTsKvQueryResult(query.getId(), tsKvEntries, lastTs);

            return Futures.immediateFuture(result);
        } else {
            List<ListenableFuture<Optional<TsKvEntity>>> futures = new ArrayList<>();
            var intervalType = aggParams.getIntervalType();
            long startPeriod = query.getStartTs();
            long endPeriod = Math.max(query.getStartTs() + 1, query.getEndTs());
            while (startPeriod < endPeriod) {
                long startTs = startPeriod;
                long endTs;
                if (IntervalType.MILLISECONDS.equals(intervalType)) {
                    endTs = startPeriod + aggParams.getInterval();
                } else {
                    endTs = TimeUtils.calculateIntervalEnd(startTs, intervalType, aggParams.getTzId());
                }
                endTs = Math.min(endTs, endPeriod);
                long ts = startTs + (endTs - startTs) / 2;
                ListenableFuture<Optional<ReadingEntity>> aggregateReadingEntry = findAndAggregateAsync(entityId,
                        query.getKey(), startTs, endTs, ts, query.getAggregation());
                startPeriod = endTs;

                ListenableFuture<Optional<TsKvEntity>> aggregateTsKvEntry = convertReadingEntityAndAggregationTypeToTsKvEntry(
                        aggregateReadingEntry, query.getAggregation().toString());

                futures.add(aggregateTsKvEntry);
            }

            return getReadTsKvQueryResultFuture(query, Futures.allAsList(futures));
        }
    }

    @Override
    public ListenableFuture<Integer> save(TenantId tenantId, EntityId entityId, TsKvEntry tsKvEntry, long ttl) {
        if (entityId.getEntityType() == EntityType.DEVICE) {
            Optional<UUID> transformationTenantId = transformationService.getFromKey(
                    transformationSystem.getThingsboard(), transformationEntity.getTenant(), tenantId.toString(),
                    transformationSystem.getReadingType(), transformationEntity.getTenant());

            Optional<UUID> transformationDataSourceId = transformationService.getFromKey(
                    transformationSystem.getThingsboard(), transformationEntity.getDevice(), entityId.toString(),
                    transformationSystem.getReadingType(), transformationEntity.getDataSource());

            Optional<ReadingType> readingType = readingTypeService.findByCode(tsKvEntry.getKey());

            if (!transformationTenantId.isPresent()) {
                log.warn("TenantId not found for ThingsBoard TenantId: " + tenantId.toString());

                return Futures.immediateFuture(0);
            }

            if (!transformationDataSourceId.isPresent()) {
                log.warn("DataSourceId not found for ThingsBoard EntityId: " + entityId.toString());

                return Futures.immediateFuture(0);
            }

            if (!readingType.isPresent()) {
                log.warn("ReadingType not found for code: " + tsKvEntry.getKey());

                return Futures.immediateFuture(0);
            }

            Reading reading = new Reading();

            reading.setDataSourceId(transformationDataSourceId.get());
            reading.setReadingTypeId(UUID.fromString(readingType.get().getId()));

            reading.setReadAt(longToOffsetDateTime(tsKvEntry.getTs()));
            addValue(tsKvEntry, reading);
            addDataType(tsKvEntry, reading);

            try {
                readingRabbitMqProducer.sendToQueue(reading);
            } catch (IOException e) {
                e.printStackTrace();
            }

            return Futures.immediateFuture(0);
        } else {
            return Futures.immediateFuture(0);
        }
    }

    ListenableFuture<Optional<ReadingEntity>> findAndAggregateAsync(EntityId entityId, String key, long startTs, // sysadmin neden burada?
            long endTs, long ts, Aggregation aggregation) {
        return service.submit(() -> {
            ReadingEntity entity = switchReadingAggregation(entityId, key, startTs, endTs, aggregation);
            if (entity != null && entity.isNotEmpty()) {
                var dataSourceId = convertEntityIdToDataSourceId(entityId);
                var readingTypeId = convertEntityKeyToReadingTypeId(key);
                var readAt = longToOffsetDateTime(ts);
                entity.setDataSourceId(dataSourceId);
                entity.setReadingTypeId(readingTypeId);
                entity.setReadAt(readAt);
                return Optional.of(entity);
            } else {
                return Optional.empty();
            }
        });
    }

    @Override
    public ListenableFuture<List<ReadTsKvQueryResult>> findAllByQueries(TenantId tenantId, EntityId entityId,
            List<ReadTsKvQuery> queries) {
        validate(entityId);
        queries.forEach(this::validate);

        List<ListenableFuture<ReadTsKvQueryResult>> futures = queries
                .stream()
                .map(query -> findAllAsync(tenantId, entityId, query))
                .collect(Collectors.toList());
        return Futures.transform(Futures.allAsList(futures), new Function<>() {
            @Nullable
            @Override
            public List<ReadTsKvQueryResult> apply(@Nullable List<ReadTsKvQueryResult> results) {
                if (results == null || results.isEmpty()) {
                    return null;
                }
                return results.stream().filter(Objects::nonNull).collect(Collectors.toList());
            }
        }, service);
    }

    @Override
    public ListenableFuture<List<TsKvEntry>> findAll(TenantId tenantId, EntityId entityId,
            List<ReadTsKvQuery> queries) {
        return Futures.immediateFuture(null);
    }

    @Override
    public ListenableFuture<Optional<TsKvEntry>> findLatest(TenantId tenantId, EntityId entityId, String keys) {
        return Futures.immediateFuture(Optional.empty());
    }

    @Override
    public ListenableFuture<List<TsKvEntry>> findLatest(TenantId tenantId, EntityId entityId, Collection<String> keys) {
        return Futures.immediateFuture(new ArrayList<>());
    }

    @Override
    public List<TsKvEntry> findLatestSync(TenantId tenantId, EntityId entityId, Collection<String> keys) {
        return new ArrayList<>();
    }

    @Override
    public ListenableFuture<List<TsKvEntry>> findAllLatest(TenantId tenantId, EntityId entityId) {
        return Futures.immediateFuture(new ArrayList<>());
    }

    @Override
    public ListenableFuture<Integer> save(TenantId tenantId, EntityId entityId, TsKvEntry tsKvEntry) {
        return save(tenantId, entityId, tsKvEntry, 0);
    }

    @Override
    public ListenableFuture<Integer> save(TenantId tenantId, EntityId entityId, List<TsKvEntry> tsKvEntryList,
            long ttl) {
        for (TsKvEntry tsKvEntry : tsKvEntryList) {
            save(tenantId, entityId, tsKvEntry, ttl);
        }
        return Futures.immediateFuture(null);
    }

    @Override
    public ListenableFuture<Integer> saveWithoutLatest(TenantId tenantId, EntityId entityId, List<TsKvEntry> tsKvEntry,
            long ttl) {
        return Futures.immediateFuture(null);
    }

    @Override
    public ListenableFuture<List<Void>> saveLatest(TenantId tenantId, EntityId entityId, List<TsKvEntry> tsKvEntry) {
        return Futures.immediateFuture(null);
    }

    @Override
    public ListenableFuture<List<TsKvLatestRemovingResult>> remove(TenantId tenantId, EntityId entityId,
            List<DeleteTsKvQuery> queries) {
        return Futures.immediateFuture(null);
    }

    @Override
    public ListenableFuture<List<TsKvLatestRemovingResult>> removeLatest(TenantId tenantId, EntityId entityId,
            Collection<String> keys) {
        return Futures.immediateFuture(null);
    }

    @Override
    public ListenableFuture<Collection<String>> removeAllLatest(TenantId tenantId, EntityId entityId) {
        return Futures.immediateFuture(null);
    }

    @Override
    public List<String> findAllKeysByDeviceProfileId(TenantId tenantId, DeviceProfileId deviceProfileId) {
        return null;
    }

    @Override
    public List<String> findAllKeysByEntityIds(TenantId tenantId, List<EntityId> entityIds) {
        return null;
    }

    private ReadingEntity switchReadingAggregation(EntityId entityId, String key, long startTs, long endTs,
            Aggregation aggregation) {
        UUID dataSourceId = convertEntityIdToDataSourceId(entityId);
        UUID readingTypeId = convertEntityKeyToReadingTypeId(key);
        OffsetDateTime dateFrom = longToOffsetDateTime(startTs);
        OffsetDateTime dateTo = longToOffsetDateTime(endTs);
        switch (aggregation) {
            case AVG:
                ReadingAggregationDto avgDto = readingRepository.findNumericMax(dataSourceId, readingTypeId, dateFrom,
                        dateTo);
                if (!isAggregationNull(avgDto)) {
                    return new ReadingEntity(avgDto.getLongValue(), avgDto.getDoubleValue(), avgDto.getLongCountValue(),
                            avgDto.getDoubleCountValue(), avgDto.getAggType(),
                            OffsetDateTime.ofInstant(avgDto.getAggValuesLastTs(), ZoneId.of("UTC")));
                } else {
                    return new ReadingEntity();
                }
            case MAX:
                ReadingAggregationDto numericMaxDto = readingRepository.findNumericMax(dataSourceId, readingTypeId,
                        dateFrom, dateTo);
                if (!isAggregationNull(numericMaxDto)) {
                    return new ReadingEntity(numericMaxDto.getLongValue(), numericMaxDto.getDoubleValue(),
                            numericMaxDto.getLongCountValue(), numericMaxDto.getDoubleCountValue(),
                            numericMaxDto.getAggType(),
                            OffsetDateTime.ofInstant(numericMaxDto.getAggValuesLastTs(), ZoneId.of("UTC")));
                } else {
                    ReadingAggregationDto stringMaxDto = readingRepository.findStringMax(dataSourceId, readingTypeId,
                            dateFrom, dateTo);
                    return new ReadingEntity(
                            stringMaxDto.getStrValue(),
                            stringMaxDto.getAggValuesLastTs() != null
                                    ? OffsetDateTime.ofInstant(stringMaxDto.getAggValuesLastTs(),
                                            ZoneId.of("UTC"))
                                    : null);
                }
            case MIN:
                ReadingAggregationDto numericMinDto = readingRepository.findNumericMin(dataSourceId, readingTypeId,
                        dateFrom, dateTo);
                if (!isAggregationNull(numericMinDto)) {
                    return new ReadingEntity(numericMinDto.getLongValue(), numericMinDto.getDoubleValue(),
                            numericMinDto.getLongCountValue(), numericMinDto.getDoubleCountValue(),
                            numericMinDto.getAggType(),
                            OffsetDateTime.ofInstant(numericMinDto.getAggValuesLastTs(), ZoneId.of("UTC")));
                } else {
                    ReadingAggregationDto stringMinDto = readingRepository.findStringMin(dataSourceId, readingTypeId,
                            dateFrom, dateTo);
                    return new ReadingEntity(
                            stringMinDto.getStrValue(),
                            stringMinDto.getAggValuesLastTs() != null
                                    ? OffsetDateTime.ofInstant(stringMinDto.getAggValuesLastTs(),
                                            ZoneId.of("UTC"))
                                    : null);
                }
            case SUM:
                ReadingAggregationDto sumDto = readingRepository.findSum(dataSourceId, readingTypeId, dateFrom, dateTo);
                if (!isAggregationNull(sumDto)) {
                    return new ReadingEntity(
                        sumDto.getLongValue(),
                        sumDto.getDoubleValue(),
                        sumDto.getLongCountValue(),
                        sumDto.getDoubleCountValue(),
                        sumDto.getAggType(),
                        sumDto.getAggValuesLastTs() != null ?
                            OffsetDateTime.ofInstant(sumDto.getAggValuesLastTs(), ZoneId.of("UTC")) :
                            null);
                } else {
                    return new ReadingEntity();
                }
            case COUNT:
                ReadingAggregationDto countDto = readingRepository.findSum(dataSourceId, readingTypeId, dateFrom,
                        dateTo);
                return new ReadingEntity(
                        countDto.getBooleanCountValue(),
                        countDto.getStrCountValue(),
                        countDto.getLongCountValue(),
                        countDto.getDoubleCountValue(),
                        countDto.getJsonCountValue(),
                        countDto.getAggValuesLastTs() != null
                                ? OffsetDateTime.ofInstant(countDto.getAggValuesLastTs(), ZoneId.of("UTC"))
                                : null);
            default:
                throw new IllegalArgumentException("Not supported aggregation type: " + aggregation);
        }
    }

    private boolean isAggregationNull(ReadingAggregationDto aggregation) {
        return aggregation.getLongValue() == null && aggregation.getDoubleValue() == null
                && aggregation.getLongCountValue() == null && aggregation.getDoubleCountValue() == null;
    }

    private UUID convertEntityIdToDataSourceId(EntityId entityId) {
        return transformationService.getFromKey(
                transformationSystem.getThingsboard(), transformationEntity.getDevice(), entityId.toString(),
                transformationSystem.getReadingType(), transformationEntity.getDataSource()).get();
    }

    private UUID convertEntityKeyToReadingTypeId(String key) {
        return UUID.fromString(readingTypeService.findByCode(key).get().getId());
    }

    private TsKvEntry convertResultToTsKvEntry(ReadingEntity reading) {
        Instant readAt = reading.getReadAt().toInstant();
        long readAtTs = readAt.toEpochMilli();
        String key = reading.getReadingTypeId().toString();
        Optional<ReadingType> readingType = readingTypeService.findById(key);

        if (readingType.isPresent()) {
            String code = readingType.get().getCode();

            return new BasicTsKvEntry(readAtTs, toKvEntry(reading, code));
        } else {
            log.warn("ReadingType not found for id: " + key);

            return new BasicTsKvEntry(readAtTs, toKvEntry(reading, key));
        }
    }

    private ListenableFuture<Optional<TsKvEntity>> convertReadingEntityAndAggregationTypeToTsKvEntry(
            ListenableFuture<Optional<ReadingEntity>> readingEntity, String aggregation) {
        return Futures.transform(readingEntity, new Function<>() {
            @Nullable
            @Override
            public Optional<TsKvEntity> apply(@Nullable Optional<ReadingEntity> readingEntity) {
                if (readingEntity.isPresent()) {
                    ReadingEntity entity = readingEntity.get();
                    if (entity.isNotEmpty()) {
                        return Optional.of(convertToTsKvEntity(entity, aggregation));
                    } else {
                        return Optional.empty();
                    }
                } else {
                    return Optional.empty();
                }
            }
        }, service);
    }

    private TsKvEntity convertToTsKvEntity(ReadingEntity entity, String aggregation) {
        TsKvEntity tsKvEntity = new TsKvEntity();
        tsKvEntity.setEntityId(entity.getDataSourceId());
        tsKvEntity.setKey(
            keyDictionaryDao.getOrSaveKeyId(readingTypeService.findById(entity.getReadingTypeId().toString()).get().getCode()));
        tsKvEntity.setTs(entity.getReadAt().toInstant().toEpochMilli());
        tsKvEntity.setStrKey(entity.getReadingTypeId().toString());
        tsKvEntity.setAggValuesCount(entity.getAggValuesCount());
        tsKvEntity.setAggValuesLastTs(entity.getAggValuesLastTs());
        tsKvEntity.setBooleanValue(entity.getValueBoolean());
        tsKvEntity.setStrValue(entity.getValueString());
        tsKvEntity.setLongValue(entity.getValueLong());
        // tsKvEntity.setDoubleValue(entity.getValueDecimal().doubleValue());

        if (entity.getValueDecimal() != null) {
            if (entity.getValueDecimal().compareTo(BigDecimal.valueOf(Double.MAX_VALUE)) > 0) {
                tsKvEntity.setDoubleValue(Double.MAX_VALUE);
            } else if (entity.getValueDecimal().compareTo(BigDecimal.valueOf(Double.MIN_VALUE)) < 0) {
                tsKvEntity.setDoubleValue(Double.MIN_VALUE);
            } else {
                tsKvEntity.setDoubleValue(entity.getValueDecimal().doubleValue());
            }
        } else {
            tsKvEntity.setDoubleValue(0D);
        }

        tsKvEntity.setJsonValue(entity.getValueJson());
        return tsKvEntity;
    }

    private static KvEntry toKvEntry(ReadingEntity reading, String key) {
        KvEntry kvEntry = null;

        if (reading.getValueDecimal() != null) {
            BigDecimal decimalV = reading.getValueDecimal();
            kvEntry = new DoubleDataEntry(key, decimalV.doubleValue());
        } else if (reading.getValueLong() != null) {
            Long longV = reading.getValueLong();
            kvEntry = new LongDataEntry(key, longV);
        } else if (reading.getValueBoolean() != null) {
            Boolean booleanV = reading.getValueBoolean();
            kvEntry = new BooleanDataEntry(key, booleanV);
        } else if (reading.getValueString() != null) {
            String stringV = reading.getValueString();
            kvEntry = new StringDataEntry(key, stringV);
        } else if (reading.getValueJson() != null) {
            String jsonV = reading.getValueJson();
            kvEntry = new JsonDataEntry(key, jsonV);
        } else if (reading.getValueDateTime() != null) {
            Instant instantV = reading.getValueDateTime().toInstant();
            kvEntry = new LongDataEntry(key, instantV.toEpochMilli());
        } else {
            log.warn("Reading value is null for key: " + key);
        }

        return kvEntry;
    }

    private static void addValue(KvEntry kvEntry, Reading reading) {
        switch (kvEntry.getDataType()) {
            case BOOLEAN:
                Optional<Boolean> booleanValue = kvEntry.getBooleanValue();
                booleanValue.ifPresent(reading::setValueBoolean);
                break;
            case STRING:
                Optional<String> stringValue = kvEntry.getStrValue();
                stringValue.ifPresent(reading::setValueString);
                break;
            case LONG:
                Optional<Long> longValue = kvEntry.getLongValue();
                longValue.ifPresent(reading::setValueLong);
                break;
            case DOUBLE:
                Optional<Double> doubleValue = kvEntry.getDoubleValue();
                doubleValue.ifPresent(d -> reading.setValueDecimal(BigDecimal.valueOf(d)));
                break;
            case JSON:
                Optional<String> jsonValue = kvEntry.getJsonValue();
                jsonValue.ifPresent(reading::setValueJson);
                break;
        }
    }

    private static void addDataType(KvEntry kvEntry, Reading reading) {
        switch (kvEntry.getDataType()) {
            case BOOLEAN:
                reading.setDataType(String.valueOf(VModelConstants.DATA_TYPE_BOOLEAN));
                break;
            case STRING:
                reading.setDataType(String.valueOf(VModelConstants.DATA_TYPE_STRING));
                break;
            case LONG:
                reading.setDataType(String.valueOf(VModelConstants.DATA_TYPE_LONG));
                break;
            case DOUBLE:
                reading.setDataType(String.valueOf(VModelConstants.DATA_TYPE_DECIMAL));
                break;
            case JSON:
                reading.setDataType(String.valueOf(VModelConstants.DATA_TYPE_STRING));
                break;
        }
    }

    private OffsetDateTime longToOffsetDateTime(long ts) {
        return OffsetDateTime.ofInstant(Instant.ofEpochMilli(ts), TimeZone.getDefault().toZoneId());
    }

    private static void validate(EntityId entityId) {
        Validator.validateEntityId(entityId, "Incorrect entityId " + entityId);
    }

    private void validate(ReadTsKvQuery query) {
        if (query == null) {
            throw new IncorrectParameterException("ReadingQuery can't be null");
        } else if (isBlank(query.getKey())) {
            throw new IncorrectParameterException("Incorrect ReadingQuery. Key can't be empty");
        } else if (query.getAggregation() == null) {
            throw new IncorrectParameterException("Incorrect ReadingQuery. Aggregation can't be empty");
        }
        if (!Aggregation.NONE.equals(query.getAggregation())) {
            long step = Math.max(query.getInterval(), 1000);
            long intervalCounts = (query.getEndTs() - query.getStartTs()) / step;
            if (intervalCounts > maxTsIntervals || intervalCounts < 0) {
                throw new IncorrectParameterException("Incorrect ReadingQuery. Number of intervals is to high - "
                        + intervalCounts + ". " +
                        "Please increase 'interval' parameter for your query or reduce the time range of the query.");
            }
        }
    }
}
