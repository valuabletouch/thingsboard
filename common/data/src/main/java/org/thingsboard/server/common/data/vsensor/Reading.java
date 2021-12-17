package org.thingsboard.server.common.data.vsensor;

import java.util.UUID;

import lombok.Data;

@Data
public class Reading {

    private UUID dataSourceId;

    private UUID readingTypeId;

    private String readAt;

    private String dataType;

    private String valueBoolean;

    private String valueLong;

    private String valueDecimal;

    private String valueString;

    private String valueDatetime;

    private String valueJson;
}
