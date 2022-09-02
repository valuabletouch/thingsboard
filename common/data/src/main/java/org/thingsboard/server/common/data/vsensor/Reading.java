package org.thingsboard.server.common.data.vsensor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;

import lombok.Data;

@Data
public class Reading implements Serializable {

    private UUID tenantId;

    private UUID dataSourceId;

    private UUID readingTypeId;

    private String readAt;

    private String dataType;

    private Boolean valueBoolean;

    private Long valueLong;

    private BigDecimal valueDecimal;

    private String valueString;

    private String valueDatetime;

    private String valueJson;
}
