package org.thingsboard.server.dao.vsensor.mongo.configurations;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;

@Configuration
public class TransformationSystem {

    @Getter
    @Value("${transformation.system.thingsboard}")
    private String thingsboard;

    @Getter
    @Value("${transformation.system.readingtype}")
    private String readingType;
}
