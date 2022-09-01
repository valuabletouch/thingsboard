package org.thingsboard.server.dao.vsensor.mongo.configurations;

import org.springframework.boot.autoconfigure.mongo.MongoProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;

@Configuration
public class MongoConfig {
    @Primary
    @Bean(name = "readingTypeProperties")
    @ConfigurationProperties(prefix = "spring.data.mongodb.readingtype")
    public MongoProperties getReadingTypeProperties() throws Exception {
        return new MongoProperties();
    }

    @Bean(name = "transformationMongoProperties")
    @ConfigurationProperties(prefix = "spring.data.mongodb.transformation")
    public MongoProperties getTransformationMongoProperties() throws Exception {
        return new MongoProperties();
    }

    @Primary
    @Bean(name = "readingTypeMongoTemplate")
    public MongoTemplate getReadingTypeMongoTemplate() throws Exception {
        return new MongoTemplate(readingTypeMongoDatabaseFactory(getReadingTypeProperties()));
    }

    @Bean(name = "transformationMongoTemplate")
    public MongoTemplate getTransformationMongoTemplate() throws Exception {
        return new MongoTemplate(transformationMongoDatabaseFactory(getTransformationMongoProperties()));
    }

    @Primary
    @Bean
    public MongoDatabaseFactory readingTypeMongoDatabaseFactory(MongoProperties mongo) throws Exception {
        return new SimpleMongoClientDatabaseFactory(mongo.getUri());
    }

    @Bean
    public MongoDatabaseFactory transformationMongoDatabaseFactory(MongoProperties mongo) throws Exception {
        return new SimpleMongoClientDatabaseFactory(mongo.getUri());
    }
}
