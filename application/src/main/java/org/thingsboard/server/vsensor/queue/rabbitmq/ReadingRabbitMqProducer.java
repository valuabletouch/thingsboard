package org.thingsboard.server.vsensor.queue.rabbitmq;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.vsensor.Reading;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;

import com.google.gson.Gson;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.AMQP.BasicProperties;

@Service
@ConditionalOnExpression("'${queue.type:null}'=='rabbitmq'")
public class ReadingRabbitMqProducer {

    @Value("${queue.rabbitmq.host:}")
    private String host;
    @Value("${queue.rabbitmq.port:}")
    private int port;
    @Value("${queue.rabbitmq.virtual_host:}")
    private String virtualHost;
    @Value("${queue.rabbitmq.username:}")
    private String username;
    @Value("${queue.rabbitmq.password:}")
    private String password;
    @Value("${queue.rabbitmq.automatic_recovery_enabled:}")
    private boolean automaticRecoveryEnabled;
    @Value("${queue.rabbitmq.connection_timeout:}")
    private int connectionTimeout;
    @Value("${queue.rabbitmq.handshake_timeout:}")
    private int handshakeTimeout;

    private static final Gson gson = new Gson();

    @Value("${queue.rabbitmq.vsensor.exchange_name:}")
    private String exchangeName;

    private String routingKey = "add_reading";

    private Connection connection;

    private Channel channel;

    @PostConstruct
    public void init() throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();

        factory.setUsername(username);
        factory.setPassword(password);
        factory.setVirtualHost(virtualHost);
        factory.setHost(host);
        factory.setPort(port);
        factory.setAutomaticRecoveryEnabled(automaticRecoveryEnabled);
        factory.setConnectionTimeout(connectionTimeout);
        factory.setHandshakeTimeout(handshakeTimeout);

        try {
            connection = factory.newConnection();
            channel = connection.createChannel();
        } catch (IOException | TimeoutException e) {
            throw new IOException("Failed to create RabbitMQ channel.", e);
        }
    }

    public void sendToQueue(Reading reading) throws IOException {
        AMQP.BasicProperties properties = getProperties();

        channel.basicPublish(exchangeName, routingKey, true, properties, gson.toJson(reading).getBytes());
    }

    public BasicProperties getProperties() {
        String correlationId = UUID.randomUUID().toString().replace("-", "");

        Map<String, Object> headers = getHeaders(correlationId);

        return new AMQP.BasicProperties.Builder()
                .timestamp(new Date(System.currentTimeMillis()))
                .messageId(UUID.randomUUID().toString().replace("-", ""))
                .correlationId(correlationId)
                .deliveryMode(1)
                .headers(headers)
                .build();
    }

    private Map<String, Object> getHeaders(String correlationId) {
        TimeZone timeZone = TimeZone.getTimeZone("UTC");
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        dateFormat.setTimeZone(timeZone);

        Map<String, Object> user = new HashMap<>();
        user.put("id", null);
        user.put("isAuthenticated", false);
        user.put("isAdmin", false);
        user.put("isSystem", true);
        user.put("roles", new String[] {});
        user.put("scopes", new String[] { "thingsboard" });
        user.put("claims", new HashMap<String, String[]>());

        Map<String, Object> messageContext = new HashMap<>();
        messageContext.put("correlationId", correlationId);
        messageContext.put("spanContext", null);
        messageContext.put("connectionId", UUID.randomUUID().toString().replace("-", ""));
        messageContext.put("traceId", UUID.randomUUID().toString().replace("-", ""));
        messageContext.put("resourceId", null);
        messageContext.put("user", user);
        messageContext.put("createdAt", dateFormat.format(new Date(System.currentTimeMillis())));

        Map<String, Object> headers = new HashMap<>();
        headers.put("message_context", gson.toJson(messageContext));

        return headers;
    }
}
