/**
 * Özgün AY
 */
package org.thingsboard.server.dao.timeseries.vsensor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.BoundStatementBuilder;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.Row;
import com.google.common.base.Function;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.Aggregation;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.BooleanDataEntry;
import org.thingsboard.server.common.data.kv.DeleteTsKvQuery;
import org.thingsboard.server.common.data.kv.DoubleDataEntry;
import org.thingsboard.server.common.data.kv.JsonDataEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.ReadTsKvQuery;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.dao.model.vsensor.ReadingType;
import org.thingsboard.server.dao.model.vsensor.VModelConstants;
import org.thingsboard.server.dao.nosql.CassandraAbstractAsyncDao;
import org.thingsboard.server.dao.nosql.TbResultSet;
import org.thingsboard.server.dao.nosql.mongo.configurations.TransformationEntity;
import org.thingsboard.server.dao.nosql.mongo.configurations.TransformationSystem;
import org.thingsboard.server.dao.nosql.mongo.repository.readingtype.ReadingTypeService;
import org.thingsboard.server.dao.nosql.mongo.repository.transformation.TransformationService;
import org.thingsboard.server.dao.sqlts.AggregationTimeseriesDao;
import org.thingsboard.server.dao.timeseries.SimpleListenableFuture;
import org.thingsboard.server.dao.timeseries.TimeseriesDao;
import org.thingsboard.server.dao.util.NoSqlTsDao;

import lombok.extern.slf4j.Slf4j;

/**
 * @author Özgün Ay
 */
@Component
@Slf4j
@NoSqlTsDao
@Primary
@ConditionalOnExpression("${cassandra.vsensor.enabled}")
public class VCassandraBaseTimeseriesDao extends CassandraAbstractAsyncDao
        implements TimeseriesDao, AggregationTimeseriesDao {

    private static final String ASC_ORDER = "ASC";
    private static final String DESC_ORDER = "DESC";
    private static final String SELECT_PREFIX = "SELECT ";

    @Autowired
    private ReadingTypeService readingTypeService;

    @Autowired
    private TransformationSystem transformationSystem;

    @Autowired
    private TransformationEntity transformationEntity;

    @Autowired
    private TransformationService transformationService;

    @Autowired
    private Environment environment;

    @Value("${cassandra.vsensor.keyspace_name}")
    private String keyspaceName;

    private PreparedStatement[] fetchStmtsAsc;
    private PreparedStatement[] fetchStmtsDesc;

    private boolean isInstall() {
        return environment.acceptsProfiles(Profiles.of("install"));
    }

    @PostConstruct
    public void init() {
        super.startExecutor();
        if (!isInstall()) {
            getFetchStmt(Aggregation.NONE, DESC_ORDER);
        }
    }

    @PreDestroy
    public void stop() {
        super.stopExecutor();
    }

    @Override
    public ListenableFuture<List<TsKvEntry>> findAllAsync(TenantId tenantId, EntityId entityId, ReadTsKvQuery query) {
        final SimpleListenableFuture<List<TsKvEntry>> resultFuture = new SimpleListenableFuture<>();

        Optional<ReadingType> readingType = readingTypeService.findByCode(query.getKey());

        Optional<UUID> transformationEntityId = transformationService.getId(transformationSystem.getReadingType(), transformationEntity.getDataSource(), transformationSystem.getThingsboard(), transformationEntity.getDevice(), entityId.toString());

        Optional<UUID> transformationTenantId = transformationService.getId(transformationSystem.getReadingType(), transformationEntity.getTenant(), transformationSystem.getThingsboard(), transformationEntity.getTenant(), tenantId.toString());

        UUID key = null;

        if (!transformationEntityId.isPresent()) {
            log.warn("Failed to read DataSource from MongoDB.");
        }

        if (!transformationTenantId.isPresent()) {
            log.warn("Failed to read Tenant from MongoDB.");
        }

        if (readingType.isPresent()) {
            key = UUID.fromString(readingType.get().getId());
        } else {
            log.warn("Failed to read ReadingType from MongoDB.");
            key = EntityId.NULL_UUID;
        }

        PreparedStatement proto = getFetchStmt(Aggregation.NONE, query.getOrder());
        BoundStatementBuilder stmtBuilder = new BoundStatementBuilder(proto.bind());

        stmtBuilder.setUuid(0, transformationTenantId.get());
        stmtBuilder.setUuid(1, transformationEntityId.get());
        stmtBuilder.setUuid(2, key);
        stmtBuilder.setInstant(3, longToInstant(query.getStartTs()));
        stmtBuilder.setInstant(4, longToInstant(query.getEndTs()));
        stmtBuilder.setInt(5, query.getLimit());

        BoundStatement stmt = stmtBuilder.build();

        Futures.addCallback(executeAsyncRead(tenantId, stmt), new FutureCallback<TbResultSet>() {
            @Override
            public void onSuccess(@Nullable TbResultSet result) {
                if (result == null) {
                    resultFuture.set(convertResultToTsKvEntryList(Collections.emptyList()));
                    return;
                }

                Futures.addCallback(result.allRows(readResultsProcessingExecutor), new FutureCallback<List<Row>>() {

                    @Override
                    public void onSuccess(@Nullable List<Row> result) {
                        resultFuture
                                .set(convertResultToTsKvEntryList(result == null ? Collections.emptyList() : result));
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        log.error("[{}][{}] Failed to fetch data for query {}-{}", stmt, t);
                    }
                }, readResultsProcessingExecutor);
            }

            @Override
            public void onFailure(Throwable t) {
                log.error("[{}][{}] Failed to fetch data for query {}-{}", stmt, t);
            }
        }, readResultsProcessingExecutor);

        return resultFuture;
    }

    @Override
    public ListenableFuture<List<TsKvEntry>> findAllAsync(TenantId tenantId, EntityId entityId,
            List<ReadTsKvQuery> queries) {
        List<ListenableFuture<List<TsKvEntry>>> futures = queries.stream()
                .map(query -> findAllAsync(tenantId, entityId, query)).collect(Collectors.toList());
        return Futures.transform(Futures.allAsList(futures), new Function<List<List<TsKvEntry>>, List<TsKvEntry>>() {
            @Nullable
            @Override
            public List<TsKvEntry> apply(@Nullable List<List<TsKvEntry>> results) {
                if (results == null || results.isEmpty()) {
                    return null;
                }
                return results.stream().flatMap(List::stream).collect(Collectors.toList());
            }
        }, readResultsProcessingExecutor);
    }

    @Override
    public ListenableFuture<Integer> save(TenantId tenantId, EntityId entityId, TsKvEntry tsKvEntry, long ttl) {
        // NOTE: This is readonly service
        return Futures.immediateFuture(0);
    }

    @Override
    public ListenableFuture<Integer> savePartition(TenantId tenantId, EntityId entityId, long tsKvEntryTs, String key,
            long ttl) {
        // NOTE: This is readonly service
        return Futures.immediateFuture(0);
    }

    @Override
    public ListenableFuture<Void> remove(TenantId tenantId, EntityId entityId, DeleteTsKvQuery query) {
        // NOTE: This is readonly service
        return Futures.immediateFuture(null);
    }

    @Override
    public ListenableFuture<Void> removePartition(TenantId tenantId, EntityId entityId, DeleteTsKvQuery query) {
        // NOTE: This is readonly service
        return Futures.immediateFuture(null);
    }

    private static KvEntry toKvEntry(Row row, String key) {
        KvEntry kvEntry = null;
        String strV = row.get(VModelConstants.STRING_VALUE_COLUMN, String.class);
        if (strV != null) {
            kvEntry = new StringDataEntry(key, strV);
        } else {
            Long longV = row.get(VModelConstants.LONG_VALUE_COLUMN, Long.class);
            if (longV != null) {
                kvEntry = new LongDataEntry(key, longV);
            } else {
                BigDecimal decimalV = row.get(VModelConstants.DOUBLE_VALUE_COLUMN, BigDecimal.class);
                if (decimalV != null) {
                    kvEntry = new DoubleDataEntry(key, decimalV.doubleValue());
                } else {
                    Boolean boolV = row.get(VModelConstants.BOOLEAN_VALUE_COLUMN, Boolean.class);
                    if (boolV != null) {
                        kvEntry = new BooleanDataEntry(key, boolV);
                    } else {
                        String jsonV = row.get(VModelConstants.JSON_VALUE_COLUMN, String.class);
                        if (StringUtils.isNoneEmpty(jsonV)) {
                            kvEntry = new JsonDataEntry(key, jsonV);
                        } else {
                            log.warn("All values in key-value row are nullable ");
                        }
                    }
                }
            }
        }
        return kvEntry;
    }

    protected List<TsKvEntry> convertResultToTsKvEntryList(List<Row> rows) {
        List<TsKvEntry> entries = new ArrayList<>(rows.size());
        if (!rows.isEmpty()) {
            rows.forEach(row -> entries.add(convertResultToTsKvEntry(row)));
        }
        return entries;
    }

    private TsKvEntry convertResultToTsKvEntry(Row row) {
        Instant readAt = row.get(VModelConstants.READ_AT_COLUMN, Instant.class);
        long readAtTs = readAt.toEpochMilli();
        String key = row.get(VModelConstants.READING_TYPE_ID_COLUMN, UUID.class).toString();
        Optional<ReadingType> readingType = readingTypeService.findById(key);
        if (readingType.isPresent()) {
            String code = readingType.get().getCode();
            return new BasicTsKvEntry(readAtTs, toKvEntry(row, code));
        } else {
            log.warn("Failed to read ReadingType from MongoDB.");
            return new BasicTsKvEntry(readAtTs, toKvEntry(row, key));
        }
    }

    private PreparedStatement getFetchStmt(Aggregation aggType, String orderBy) {
        switch (orderBy) {
            case ASC_ORDER:
                if (fetchStmtsAsc == null) {
                    fetchStmtsAsc = initFetchStmt(orderBy);
                }
                return fetchStmtsAsc[aggType.ordinal()];
            case DESC_ORDER:
                if (fetchStmtsDesc == null) {
                    fetchStmtsDesc = initFetchStmt(orderBy);
                }
                return fetchStmtsDesc[aggType.ordinal()];
            default:
                throw new RuntimeException("Not supported" + orderBy + "order!");
        }
    }

    private PreparedStatement[] initFetchStmt(String orderBy) {
        PreparedStatement[] fetchStmts = new PreparedStatement[Aggregation.values().length];

        for (Aggregation type : Aggregation.values()) {
            if (type == Aggregation.SUM && fetchStmts[Aggregation.AVG.ordinal()] != null) {
                fetchStmts[type.ordinal()] = fetchStmts[Aggregation.AVG.ordinal()];
            } else if (type == Aggregation.AVG && fetchStmts[Aggregation.SUM.ordinal()] != null) {
                fetchStmts[type.ordinal()] = fetchStmts[Aggregation.SUM.ordinal()];
            } else {
                String query = SELECT_PREFIX + String.join(", ", VModelConstants.getFetchColumnNames(type)) + " FROM "
                        + keyspaceName + "." + VModelConstants.READINGS_TABLE + " WHERE "
                        + VModelConstants.TENANT_ID_READINGS_COLUMN + " = ?" + " AND "
                        + VModelConstants.DATA_SOURCE_ID_COLUMN + " = ?" + " AND "
                        + VModelConstants.READING_TYPE_ID_COLUMN + " = ?" + " AND " + VModelConstants.READ_AT_COLUMN
                        + " > ?" + " AND " + VModelConstants.READ_AT_COLUMN + " <= ?"
                        + (type == Aggregation.NONE
                                ? " ORDER BY " + VModelConstants.READ_AT_COLUMN + " " + orderBy + " LIMIT ?"
                                : "");

                fetchStmts[type.ordinal()] = prepare(query);
            }
        }

        return fetchStmts;
    }

    private Instant longToInstant(long ts) {
        return Instant.ofEpochMilli(ts);
    }
}