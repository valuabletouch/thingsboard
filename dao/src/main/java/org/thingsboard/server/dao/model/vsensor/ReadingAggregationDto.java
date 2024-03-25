package org.thingsboard.server.dao.model.vsensor;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.OffsetDateTime;

import org.springframework.beans.factory.annotation.Value;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

// @Projections(name="reading",types={ReadingEntity.class})
public interface ReadingAggregationDto {

    Long getLongValue();

    BigDecimal getDoubleValue();

    Long getBooleanCountValue();

    Long getStrCountValue();

    Long getLongCountValue();

    Long getDoubleCountValue();

    Long getJsonCountValue();

    String getStrValue();

    String getAggType();

    Timestamp getAggValuesLastTs();
}
