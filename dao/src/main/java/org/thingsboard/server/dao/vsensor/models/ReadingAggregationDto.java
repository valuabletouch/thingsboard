/**
* Ahmet Ertuğrul KAYA
*/
package org.thingsboard.server.dao.vsensor.models;

import java.math.BigDecimal;
import java.time.Instant;

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

    Instant getAggValuesLastTs();
}
