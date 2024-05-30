/**
 * Copyright © 2016-2024 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.dao.vsensor.models;

import lombok.Data;
import lombok.Getter;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "\"LastReadings\"")
@Getter
public class LastReadingEntity {

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

    public LastReadingEntity() { super(); }

    public LastReadingEntity(
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

    public boolean isNotEmpty() {
        return valueString != null || valueLong != null || valueDecimal != null || valueBoolean != null;
    }

    protected static boolean isAllNull(Object... args) {
        for (Object arg : args) {
            if (arg != null) {
                return false;
            }
        }
        return true;
    }
}
