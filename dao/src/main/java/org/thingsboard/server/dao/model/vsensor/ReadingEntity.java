package org.thingsboard.server.dao.model.vsensor;

import lombok.Data;
import lombok.Getter;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.vsensor.Reading;
import org.thingsboard.server.dao.model.ToData;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "\"Readings\"")
@Getter
@IdClass(ReadingCompositeKey.class)
public class ReadingEntity implements ToData<Reading> {

    protected static final String SUM = "SUM";
    protected static final String AVG = "AVG";
    protected static final String MIN = "MIN";
    protected static final String MAX = "MAX";

    @Transient
    protected Long aggValuesLastTs;
    @Transient
    protected Long aggValuesCount;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "\"Id\"", columnDefinition = "uuid")
    private UUID id;

    @Column(name = "\"TenantId\"", nullable = false)
    private UUID tenantId;

    @Column(name = "\"DataSourceId\"", nullable = false)
    private UUID dataSourceId;

    @Column(name = "\"ReadingTypeId\"", nullable = false)
    private UUID readingTypeId;

    @Column(name = "\"ReadAt\"", nullable = false)
    private OffsetDateTime readAt;

    @Column(name = "\"ValueBoolean\"")
    private Boolean valueBoolean;

    @Column(name = "\"ValueLong\"")
    private Long valueLong;

    @Column(name = "\"ValueDecimal\"")
    private BigDecimal valueDecimal;

    @Column(name = "\"ValueDateTime\"")
    private OffsetDateTime valueDateTime;

    @Column(name = "\"ValueString\"")
    private String valueString;

    @Column(name = "\"ValueJson\"")
    private String valueJson;

    @Column(name = "\"DataType\"", nullable = false)
    private String dataType;

    @Column(name = "\"CreatedById\"")
    private UUID createdById;

    @Column(name = "\"CreatedAt\"", nullable = false)
    private OffsetDateTime createdAt;

    public ReadingEntity() { super(); }

    public ReadingEntity(
            boolean valueBoolean,
            long valueLong,
            BigDecimal valueDecimal,
            OffsetDateTime valueDateTime,
            String valueString,
            String valueJson) {
        this.valueBoolean = valueBoolean;
        this.valueLong = valueLong;
        this.valueDecimal = valueDecimal;
        this.valueDateTime = valueDateTime;
        this.valueString = valueString;
        this.valueJson = valueJson;
    }

    public ReadingEntity(Reading reading) {
        tenantId = reading.getTenantId();
        dataSourceId = reading.getDataSourceId();
        readingTypeId = reading.getReadingTypeId();
        readAt = reading.getReadAt();
        valueBoolean = reading.getValueBoolean();
        valueLong = reading.getValueLong();
        valueDecimal = reading.getValueDecimal();
        valueDateTime = reading.getValueDatetime();
        valueString = reading.getValueString();
        valueJson = reading.getValueJson();
        dataType = reading.getDataType();
    }

    public ReadingEntity(String strValue, OffsetDateTime aggValuesLastTs) {
        if (!isAllNull(strValue, aggValuesLastTs)) {
            this.valueString = strValue;
            this.aggValuesLastTs = aggValuesLastTs != null ? aggValuesLastTs.toInstant().toEpochMilli() : 0;
        }
    }

    public ReadingEntity(Long longValue, BigDecimal doubleValue, Long longCountValue, Long doubleCountValue, String aggType, OffsetDateTime aggValuesLastTs) {
        if (!isAllNull(longValue, doubleValue, longCountValue, doubleCountValue)) {
            this.aggValuesLastTs = aggValuesLastTs != null ? aggValuesLastTs.toInstant().toEpochMilli() : 0;
            switch (aggType) {
                case AVG:
                    BigDecimal sum = new BigDecimal(0);
                    if (longValue != null) {
                        sum = sum.add(BigDecimal.valueOf(longValue));
                    }
                    if (doubleValue != null) {
                        sum = sum.add(doubleValue);
                    }
                    long totalCount = longCountValue + doubleCountValue;
                    if (totalCount > 0) {
                        this.valueDecimal = sum.divide(BigDecimal.valueOf(totalCount));
                    } else {
                        this.valueDecimal = new BigDecimal(0);
                    }
                    this.aggValuesCount = totalCount;
                    break;
                case SUM:
                    if (doubleCountValue > 0) {
                        this.valueDecimal = doubleValue.add((longValue != null ? BigDecimal.valueOf(longValue) : new BigDecimal(0)));
                    } else {
                        this.valueLong = longValue;
                    }
                    break;
                case MIN:
                case MAX:
                    if (longCountValue > 0 && doubleCountValue > 0) {
                        this.valueDecimal = MAX.equals(aggType) ? maxBigDecimal(doubleValue, BigDecimal.valueOf(longValue)) : minBigDecimal(doubleValue, BigDecimal.valueOf(longValue));
                    } else if (doubleCountValue > 0) {
                        this.valueDecimal = doubleValue;
                    } else if (longCountValue > 0) {
                        this.valueLong = longValue;
                    }
                    break;
            }
        }
    }

    public ReadingEntity(Long booleanValueCount, Long strValueCount, Long longValueCount, Long doubleValueCount, Long jsonValueCount, OffsetDateTime aggValuesLastTs) {
        if (!isAllNull(booleanValueCount, strValueCount, longValueCount, doubleValueCount)) {
            this.aggValuesLastTs = aggValuesLastTs != null ? aggValuesLastTs.toInstant().toEpochMilli() : 0;
            if (booleanValueCount != null) {
                this.valueLong = booleanValueCount;
            } else if (strValueCount != null) {
                this.valueLong = strValueCount;
            } else if (jsonValueCount != null) {
                this.valueLong = jsonValueCount;
            } else if (longValueCount != null) {
                this.valueLong = longValueCount;
                if (doubleValueCount != null) {
                    this.valueLong += doubleValueCount;
                }
            } else {
                this.valueLong = 0L;
            }
        }
    }

    public boolean isNotEmpty() {
        return valueString != null || valueLong != null || valueDecimal != null || valueBoolean != null;
    }

    @Override
    public Reading toData() {
        Reading result = new Reading();
        result.setTenantId(TenantId.fromUUID(tenantId).getId());
        result.setDataSourceId(dataSourceId);
        result.setReadingTypeId(readingTypeId);
        result.setReadAt(readAt);
        result.setValueBoolean(valueBoolean);
        result.setValueLong(valueLong);
        result.setValueDecimal(valueDecimal);
        result.setValueDatetime(valueDateTime);
        result.setValueString(valueString);
        result.setValueJson(valueJson);
        result.setDataType(dataType);
        return result;
    }

    protected static boolean isAllNull(Object... args) {
        for (Object arg : args) {
            if (arg != null) {
                return false;
            }
        }
        return true;
    }

    private BigDecimal maxBigDecimal(BigDecimal bigDecimal1, BigDecimal bigDecimal2) {
        return (bigDecimal1.compareTo(bigDecimal2) >= 0) ? bigDecimal1 : bigDecimal2;
    }

    private BigDecimal minBigDecimal(BigDecimal bigDecimal1, BigDecimal bigDecimal2) {
        return (bigDecimal1.compareTo(bigDecimal2) <= 0) ? bigDecimal1 : bigDecimal2;
    }
}