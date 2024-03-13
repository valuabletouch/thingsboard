package org.thingsboard.server.vsensor.dao.readings;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.dao.model.sqlts.ts.TsKvEntity;
import org.thingsboard.server.dao.model.vsensor.ReadingEntity;

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
                                          @Param("dateTo") OffsetDateTime dateTo,
                                          Pageable pageable);

        @Query(value = "SELECT * " +
                "FROM \"Readings\" " +
                "WHERE \"ValueString\" IS NOT NULL " +
                "AND \"DataSourceId\" = :dataSourceId " +
                "AND \"ReadingTypeId\" = :readingTypeId " +
                "AND \"ReadAt\" >= :dateFrom " +
                "AND \"ReadAt\" < :dateTo " +
                "ORDER BY \"ValueString\" DESC " +
                "LIMIT 1", nativeQuery = true)
        ReadingEntity findStringMax(@Param("dataSourceId") UUID dataSourceId,
                                 @Param("readingTypeId") UUID readingTypeId,
                                 @Param("dateFrom") OffsetDateTime dateFrom,
                                 @Param("dateTo") OffsetDateTime dateTo);

        @Query(value = "SELECT " +
                "MAX(COALESCE(\"ValueLong\", -9223372036854775807)) AS maxLongValue, " +
                "MAX(COALESCE(\"ValueDecimal\", -1.79769E+308)) AS maxDoubleValue, " +
                "SUM(CASE WHEN \"ValueLong\" IS NULL THEN 0 ELSE 1 END) AS countNotNullLongValue, " +
                "SUM(CASE WHEN \"ValueDecimal\" IS NULL THEN 0 ELSE 1 END) AS countNotNullDoubleValue, " +
                "'MAX' AS maxConstant, " +
                "MAX(\"ReadAt\") AS maxTs " +
                "FROM \"Readings\" " +
                "WHERE \"DataSourceId\" = :dataSourceId " +
                "AND \"ReadingTypeId\" = :readingTypeId " +
                "AND \"ReadAt\" >= :dateFrom " +
                "AND \"ReadAt\" < :dateTo", nativeQuery = true)
        ReadingEntity findNumericMax(@Param("dataSourceId") UUID dataSourceId,
                                  @Param("readingTypeId") UUID readingTypeId,
                                  @Param("dateFrom") OffsetDateTime dateFrom,
                                  @Param("dateTo") OffsetDateTime dateTo);

        @Query(value = "SELECT * " +
                "FROM \"Readings\" " +
                "WHERE \"ValueString\" IS NOT NULL " +
                "AND \"DataSourceId\" = :dataSourceId " +
                "AND \"ReadingTypeId\" = :readingTypeId " +
                "AND \"ReadAt\" >= :dateFrom " +
                "AND \"ReadAt\" < :dateTo " +
                "ORDER BY \"ValueString\" ASC " +
                "LIMIT 1", nativeQuery = true)
        ReadingEntity findStringMin(@Param("dataSourceId") UUID dataSourceId,
                                 @Param("readingTypeId") UUID readingTypeId,
                                 @Param("dateFrom") OffsetDateTime dateFrom,
                                 @Param("dateTo") OffsetDateTime dateTo);

        @Query(value = "SELECT " +
                "MAX(COALESCE(\"ValueLong\", -9223372036854775807)) AS maxLongValue, " +
                "MAX(COALESCE(\"ValueDecimal\", -1.79769E+308)) AS maxDoubleValue, " +
                "SUM(CASE WHEN \"ValueLong\" IS NULL THEN 0 ELSE 1 END) AS countNotNullLongValue, " +
                "SUM(CASE WHEN \"ValueDecimal\" IS NULL THEN 0 ELSE 1 END) AS countNotNullDoubleValue, " +
                "'MAX' AS maxConstant, " +
                "MAX(\"ReadAt\") AS maxTs " +
                "FROM \"Readings\" " +
                "WHERE \"DataSourceId\" = :dataSourceId " +
                "AND \"ReadingTypeId\" = :readingTypeId " +
                "AND \"ReadAt\" >= :dateFrom " +
                "AND \"ReadAt\" < :dateTo", nativeQuery = true)
        ReadingEntity findNumericMin(@Param("dataSourceId") UUID dataSourceId,
                                  @Param("readingTypeId") UUID readingTypeId,
                                  @Param("dateFrom") OffsetDateTime dateFrom,
                                  @Param("dateTo") OffsetDateTime dateTo);

        @Query(value = "SELECT " +
                "SUM(CASE WHEN \"ValueBoolean\" IS NULL THEN 0 ELSE 1 END) AS \"ValueBoolean\", " +
                "SUM(CASE WHEN \"ValueLong\" IS NULL THEN 0 ELSE 1 END) AS \"ValueLong\", " +
                "SUM(CASE WHEN \"ValueDecimal\" IS NULL THEN 0 ELSE 1 END) AS \"ValueDecimal\", " +
                "SUM(CASE WHEN \"ValueDateTime\" IS NULL THEN 0 ELSE 1 END) AS \"ValueDateTime\", " +
                "SUM(CASE WHEN \"ValueString\" IS NULL THEN 0 ELSE 1 END) AS \"ValueString\", " +
                "SUM(CASE WHEN \"ValueJson\" IS NULL THEN 0 ELSE 1 END) AS \"ValueJson\" " +
                "FROM \"Readings\" " +
                "WHERE \"DataSourceId\" = :dataSourceId " +
                "AND \"ReadingTypeId\" = :readingTypeId " +
                "AND \"ReadAt\" >= :dateFrom " +
                "AND \"ReadAt\" < :dateTo", nativeQuery = true)
        ReadingEntity findCount(@Param("dataSourceId") UUID dataSourceId,
                             @Param("readingTypeId") UUID readingTypeId,
                             @Param("dateFrom") OffsetDateTime dateFrom,
                             @Param("dateTo") OffsetDateTime dateTo);

        @Query(value = "SELECT " +
                "SUM(COALESCE(\"ValueLong\", 0)) AS sumLongValue,\n" +
                "SUM(COALESCE(\"ValueDecimal\", 0.0)) AS sumDoubleValue,\n" +
                "COUNT(CASE WHEN \"ValueLong\" IS NULL THEN 1 ELSE NULL END) AS countNullLongValue,\n" +
                "COUNT(CASE WHEN \"ValueDecimal\" IS NULL THEN 1 ELSE NULL END) AS countNullDoubleValue,\n" +
                "'AVG' AS avg,\n" +
                "MAX(\"ReadAt\") AS maxTs\n" +
                "FROM \"Readings\"\n" +
                "WHERE \"DataSourceId\" = :dataSourceId " +
                "AND \"ReadingTypeId\" = :readingTypeId " +
                "AND \"ReadAt\" >= :dateFrom " +
                "AND \"ReadAt\" < :dateTo", nativeQuery = true)
        ReadingEntity findAvg(@Param("dataSourceId") UUID dataSourceId,
                           @Param("readingTypeId") UUID readingTypeId,
                           @Param("dateFrom") OffsetDateTime dateFrom,
                           @Param("dateTo") OffsetDateTime dateTo);

        @Query(value = "SELECT " +
                "SUM(COALESCE(\"ValueLong\", 0)) AS sumLongValue,\n" +
                "SUM(COALESCE(\"ValueDecimal\", 0.0)) AS sumDoubleValue,\n" +
                "COUNT(CASE WHEN \"ValueLong\" IS NULL THEN 1 ELSE NULL END) AS countNullLongValue,\n" +
                "COUNT(CASE WHEN \"ValueDecimal\" IS NULL THEN 1 ELSE NULL END) AS countNullDoubleValue,\n" +
                "'SUM' AS avg,\n" +
                "MAX(\"ReadAt\") AS maxTs\n" +
                "FROM \"Readings\"\n" +
                "WHERE \"DataSourceId\" = :dataSourceId " +
                "AND \"ReadingTypeId\" = :readingTypeId " +
                "AND \"ReadAt\" >= :dateFrom " +
                "AND \"ReadAt\" < :dateTo", nativeQuery = true)
        ReadingEntity findSum(@Param("dataSourceId") UUID dataSourceId,
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