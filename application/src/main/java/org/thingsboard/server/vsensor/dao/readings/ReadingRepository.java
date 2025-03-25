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
/*
* Ahmet Ertuğrul KAYA
*/
package org.thingsboard.server.vsensor.dao.readings;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.dao.vsensor.models.ReadingAggregationDto;
import org.thingsboard.server.dao.vsensor.models.ReadingEntity;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface ReadingRepository extends JpaRepository<ReadingEntity, UUID> {

        @Query(value = "SELECT * " +
                "FROM \"Readings\" " +
                "WHERE \"DataSourceId\" = :datasourceId " +
                "AND \"ReadingTypeId\" = :readingTypeId " +
                "AND \"ReadAt\" >= :dateFrom " +
                "AND \"ReadAt\" < :dateTo ", nativeQuery = true)
        List<ReadingEntity> findAllWithLimit(@Param("datasourceId") UUID datasourceId,
                                          @Param("readingTypeId") UUID readingTypeId,
                                          @Param("dateFrom") OffsetDateTime dateFrom,
                                          @Param("dateTo") OffsetDateTime dateTo);

        @Query(value = "SELECT " +
                "MAX(\"ValueString\") as strValue, " +
                "MAX(\"ReadAt\") as aggValuesLastTs " +
                "FROM \"Readings\" " +
                "WHERE \"ValueString\" IS NOT NULL " +
                "AND \"DataSourceId\" = :dataSourceId " +
                "AND \"ReadingTypeId\" = :readingTypeId " +
                "AND \"ReadAt\" >= :dateFrom " +
                "AND \"ReadAt\" < :dateTo", nativeQuery = true)
        ReadingAggregationDto findStringMax(@Param("dataSourceId") UUID dataSourceId,
                                 @Param("readingTypeId") UUID readingTypeId,
                                 @Param("dateFrom") OffsetDateTime dateFrom,
                                 @Param("dateTo") OffsetDateTime dateTo);

        @Query(value = "SELECT " +
                "MAX(\"ValueLong\") AS longValue, " +
                "MAX(\"ValueDecimal\") AS doubleValue, " +
                "SUM(CASE WHEN \"ValueLong\" IS NULL THEN 0 ELSE 1 END) AS longCountValue, " +
                "SUM(CASE WHEN \"ValueDecimal\" IS NULL THEN 0 ELSE 1 END) AS doubleCountValue, " +
                "'MAX' AS aggType, " +
                "MAX(\"ReadAt\") AS aggValuesLastTs " +
                "FROM \"Readings\" " +
                "WHERE \"DataSourceId\" = :dataSourceId " +
                "AND \"ReadingTypeId\" = :readingTypeId " +
                "AND \"ReadAt\" >= :dateFrom " +
                "AND \"ReadAt\" < :dateTo", nativeQuery = true)
        ReadingAggregationDto findNumericMax(@Param("dataSourceId") UUID dataSourceId,
                                @Param("readingTypeId") UUID readingTypeId,
                                @Param("dateFrom") OffsetDateTime dateFrom,
                                @Param("dateTo") OffsetDateTime dateTo);

        @Query(value = "SELECT " +
                "MIN(\"ValueString\") as strValue, " +
                "MIN(\"ReadAt\") as aggValuesLastTs " +
                "FROM \"Readings\" " +
                "WHERE \"ValueString\" IS NOT NULL " +
                "AND \"DataSourceId\" = :dataSourceId " +
                "AND \"ReadingTypeId\" = :readingTypeId " +
                "AND \"ReadAt\" >= :dateFrom " +
                "AND \"ReadAt\" < :dateTo", nativeQuery = true)
        ReadingAggregationDto findStringMin(@Param("dataSourceId") UUID dataSourceId,
                                 @Param("readingTypeId") UUID readingTypeId,
                                 @Param("dateFrom") OffsetDateTime dateFrom,
                                 @Param("dateTo") OffsetDateTime dateTo);

        @Query(value = "SELECT " +
                "MIN(\"ValueLong\") AS longValue, " +
                "MIN(\"ValueDecimal\") AS doubleValue, " +
                "SUM(CASE WHEN \"ValueLong\" IS NULL THEN 0 ELSE 1 END) AS longCountValue, " +
                "SUM(CASE WHEN \"ValueDecimal\" IS NULL THEN 0 ELSE 1 END) AS doubleCountValue, " +
                "'MIN' AS aggType, " +
                "MAX(\"ReadAt\") AS aggValuesLastTs " +
                "FROM \"Readings\" " +
                "WHERE \"DataSourceId\" = :dataSourceId " +
                "AND \"ReadingTypeId\" = :readingTypeId " +
                "AND \"ReadAt\" >= :dateFrom " +
                "AND \"ReadAt\" < :dateTo", nativeQuery = true)
        ReadingAggregationDto findNumericMin(@Param("dataSourceId") UUID dataSourceId,
                                  @Param("readingTypeId") UUID readingTypeId,
                                  @Param("dateFrom") OffsetDateTime dateFrom,
                                  @Param("dateTo") OffsetDateTime dateTo);

        @Query(value = "SELECT " +
                "SUM(CASE WHEN \"ValueBoolean\" IS NULL THEN 0 ELSE 1 END) AS booleanCountValue, " +
                "SUM(CASE WHEN \"ValueString\" IS NULL THEN 0 ELSE 1 END) AS strCountValue, " +
                "SUM(CASE WHEN \"ValueLong\" IS NULL THEN 0 ELSE 1 END) AS longCountValue, " +
                "SUM(CASE WHEN \"ValueDecimal\" IS NULL THEN 0 ELSE 1 END) AS doubleCountValue, " +
                "SUM(CASE WHEN \"ValueJson\" IS NULL THEN 0 ELSE 1 END) AS jsonCountValue, " +
                "MAX(\"ReadAt\") AS aggValuesLastTs " +
                "FROM \"Readings\" " +
                "WHERE \"DataSourceId\" = :dataSourceId " +
                "AND \"ReadingTypeId\" = :readingTypeId " +
                "AND \"ReadAt\" >= :dateFrom " +
                "AND \"ReadAt\" < :dateTo", nativeQuery = true)
        ReadingAggregationDto findCount(@Param("dataSourceId") UUID dataSourceId,
                             @Param("readingTypeId") UUID readingTypeId,
                             @Param("dateFrom") OffsetDateTime dateFrom,
                             @Param("dateTo") OffsetDateTime dateTo);

        @Query(value = "SELECT " +
                "SUM(COALESCE(\"ValueLong\", 0)) AS longValue, " +
                "SUM(COALESCE(\"ValueDecimal\", 0.0)) AS doubleValue, " +
                "COUNT(CASE WHEN \"ValueLong\" IS NULL THEN 1 ELSE NULL END) AS longCountValue, " +
                "COUNT(CASE WHEN \"ValueDecimal\" IS NULL THEN 1 ELSE NULL END) AS doubleCountValue, " +
                "'AVG' AS aggType, " +
                "MAX(\"ReadAt\") AS aggValuesLastTs " +
                "FROM \"Readings\" " +
                "WHERE \"DataSourceId\" = :dataSourceId " +
                "AND \"ReadingTypeId\" = :readingTypeId " +
                "AND \"ReadAt\" >= :dateFrom " +
                "AND \"ReadAt\" < :dateTo", nativeQuery = true)
        ReadingAggregationDto findAvg(@Param("dataSourceId") UUID dataSourceId,
                           @Param("readingTypeId") UUID readingTypeId,
                           @Param("dateFrom") OffsetDateTime dateFrom,
                           @Param("dateTo") OffsetDateTime dateTo);

        @Query(value = "SELECT " +
                "SUM(COALESCE(\"ValueLong\", 0)) AS longValue, " +
                "SUM(COALESCE(\"ValueDecimal\", 0.0)) AS doubleValue, " +
                "COUNT(CASE WHEN \"ValueLong\" IS NULL THEN 1 ELSE NULL END) AS longCountValue, " +
                "COUNT(CASE WHEN \"ValueDecimal\" IS NULL THEN 1 ELSE NULL END) AS doubleCountValue, " +
                "'SUM' AS aggType, " +
                "MAX(\"ReadAt\") AS aggValuesLastTs " +
                "FROM \"Readings\" " +
                "WHERE \"DataSourceId\" = :dataSourceId " +
                "AND \"ReadingTypeId\" = :readingTypeId " +
                "AND \"ReadAt\" >= :dateFrom " +
                "AND \"ReadAt\" < :dateTo", nativeQuery = true)
        ReadingAggregationDto findSum(@Param("dataSourceId") UUID dataSourceId,
                           @Param("readingTypeId") UUID readingTypeId,
                           @Param("dateFrom") OffsetDateTime dateFrom,
                           @Param("dateTo") OffsetDateTime dateTo);

        List<ReadingEntity> findByTenantIdAndDataSourceIdAndReadingTypeIdAndReadAtBetween(
                UUID tenantId,
                UUID dataSourceId,
                UUID readingTypeId,
                OffsetDateTime dateFrom,
                OffsetDateTime dateTo);
}