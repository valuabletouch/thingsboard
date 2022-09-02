package org.thingsboard.server.dao.vsensor.mongo.configurations;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;

@Configuration
public class TransformationEntity {

    @Getter
    @Value("${transformation.entity.tenant}")
    private String tenant;

    @Getter
    @Value("${transformation.entity.device}")
    private String device;

    @Getter
    @Value("${transformation.entity.datasource}")
    private String dataSource;

}
