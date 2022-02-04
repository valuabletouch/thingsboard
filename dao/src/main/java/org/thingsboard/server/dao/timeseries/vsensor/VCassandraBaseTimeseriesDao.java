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
import com.google.common.util.concurrent.MoreExecutors;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTimeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.Aggregation;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.BooleanDataEntry;
import org.thingsboard.server.common.data.kv.DataType;
import org.thingsboard.server.common.data.kv.DeleteTsKvQuery;
import org.thingsboard.server.common.data.kv.DoubleDataEntry;
import org.thingsboard.server.common.data.kv.JsonDataEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.ReadTsKvQuery;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.vsensor.ReadingType;
import org.thingsboard.server.common.data.vsensor.ReadingTypeService;
import org.thingsboard.server.common.data.vsensor.TransformationService;
import org.thingsboard.server.dao.model.vsensor.VModelConstants;
import org.thingsboard.server.dao.nosql.CassandraAbstractAsyncDao;
import org.thingsboard.server.dao.nosql.TbResultSet;
import org.thingsboard.server.dao.nosql.mongo.configurations.TransformationEntity;
import org.thingsboard.server.dao.nosql.mongo.configurations.TransformationSystem;
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
    private PreparedStatement[] saveStmts;

    private boolean isInstall() {
        return environment.acceptsProfiles(Profiles.of("install"));
    }

    @PostConstruct
    public void init() {
        super.startExecutor();
        if (!isInstall()) {
            getFetchStmt(Aggregation.NONE, "DESC");
        }
    }

    @PreDestroy
    public void stop() {
        super.stopExecutor();
    }

    @Override
    public ListenableFuture<List<TsKvEntry>> findAllAsync(TenantId tenantId, EntityId entityId, ReadTsKvQuery query) {
        final SimpleListenableFuture<List<TsKvEntry>> resultFuture = new SimpleListenableFuture<>();

        Optional<UUID> transformationTenantId = transformationService.getFromKey(transformationSystem.getThingsboard(), transformationEntity.getTenant(), tenantId.toString(), transformationSystem.getReadingType(), transformationEntity.getTenant());

        Optional<UUID> transformationDataSourceId = transformationService.getFromKey(transformationSystem.getThingsboard(), transformationEntity.getDevice(), entityId.toString(), transformationSystem.getReadingType(), transformationEntity.getDataSource());

        Optional<ReadingType> readingType = readingTypeService.findByCode(query.getKey());

        if (!transformationTenantId.isPresent()) {
            log.warn("Failed to read Tenant from MongoDB.");
        }

        if (!transformationDataSourceId.isPresent()) {
            log.warn("Failed to read DataSource from MongoDB.");
        }

        if (!readingType.isPresent()) {
            log.warn("Failed to read ReadingType from MongoDB.");
        }

        PreparedStatement proto = getFetchStmt(Aggregation.NONE, query.getOrder());
        BoundStatementBuilder stmtBuilder = new BoundStatementBuilder(proto.bind());

        stmtBuilder.setUuid(0, transformationTenantId.isPresent() ? transformationTenantId.get() : VModelConstants.EMPTY_UUID);
        stmtBuilder.setUuid(1, transformationDataSourceId.isPresent() ? transformationDataSourceId.get() : VModelConstants.EMPTY_UUID);
        stmtBuilder.setUuid(2, readingType.isPresent() ? UUID.fromString(readingType.get().getId()) : VModelConstants.EMPTY_UUID);
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
        if (entityId.getEntityType() == EntityType.DEVICE) {
            Optional<UUID> transformationTenantId = transformationService.getFromKey(transformationSystem.getThingsboard(), transformationEntity.getTenant(), tenantId.toString(), transformationSystem.getReadingType(), transformationEntity.getTenant());

            Optional<UUID> transformationDataSourceId = transformationService.getFromKey(transformationSystem.getThingsboard(), transformationEntity.getDevice(), entityId.toString(), transformationSystem.getReadingType(), transformationEntity.getDataSource());

            Optional<ReadingType> readingType = readingTypeService.findByCode(tsKvEntry.getKey());

            if (!transformationTenantId.isPresent()) {
                log.warn("Failed to read Tenant from MongoDB.");
                return Futures.immediateFuture(0);
            }

            if (!transformationDataSourceId.isPresent()) {
                log.warn("Failed to read DataSource from MongoDB.");
                return Futures.immediateFuture(0);
            }

            if (!readingType.isPresent()) {
                log.warn("Failed to read ReadingType from MongoDB.");
                return Futures.immediateFuture(0);
            }

            BoundStatementBuilder stmtBuilder = new BoundStatementBuilder(getSaveStmt(tsKvEntry.getDataType()).bind());

            stmtBuilder
                .setUuid(0, transformationTenantId.get())
                .setUuid(1, transformationDataSourceId.get())
                .setUuid(2, UUID.fromString(readingType.get().getId()))
                .setInstant(3, longToInstant(tsKvEntry.getTs()));

            addValue(tsKvEntry, stmtBuilder, 4);
            addDataType(tsKvEntry, stmtBuilder, 5);

            stmtBuilder
                .setInstant(6, longToInstant(DateTimeUtils.currentTimeMillis()))
                .setUuid(7, VModelConstants.SYSTEM_USER_ID);

            BoundStatement stmt = stmtBuilder.build();

            return Futures.transform(getFuture(executeAsyncWrite(tenantId, stmt), rs -> null), result -> tsKvEntry.getDataPoints(), MoreExecutors.directExecutor());
        } else {
            return Futures.immediateFuture(0);
        }
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

        if (row.get(VModelConstants.DOUBLE_VALUE_COLUMN, BigDecimal.class) != null) {
            BigDecimal decimalV = row.get(VModelConstants.DOUBLE_VALUE_COLUMN, BigDecimal.class);
            kvEntry = new DoubleDataEntry(key, decimalV.doubleValue());
        } else if (row.get(VModelConstants.LONG_VALUE_COLUMN, Long.class) != null) {
            Long longV = row.get(VModelConstants.LONG_VALUE_COLUMN, Long.class);
            kvEntry = new LongDataEntry(key, longV);
        } else if (row.get(VModelConstants.STRING_VALUE_COLUMN, String.class) != null) {
            String strV = row.get(VModelConstants.STRING_VALUE_COLUMN, String.class);
            kvEntry = new StringDataEntry(key, strV);
        } else if (row.get(VModelConstants.BOOLEAN_VALUE_COLUMN, Boolean.class) != null) {
            Boolean boolV = row.get(VModelConstants.BOOLEAN_VALUE_COLUMN, Boolean.class);
            kvEntry = new BooleanDataEntry(key, boolV);
        } else if (StringUtils.isNoneEmpty(row.get(VModelConstants.JSON_VALUE_COLUMN, String.class))) {
            String jsonV = row.get(VModelConstants.JSON_VALUE_COLUMN, String.class);
            kvEntry = new JsonDataEntry(key, jsonV);
        } else {
            log.warn("All values in key-value row are nullable ");
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
        switch (orderBy.toUpperCase()) {
            case "ASC":
                if (fetchStmtsAsc == null) {
                    fetchStmtsAsc = initFetchStmt(orderBy);
                }
                return fetchStmtsAsc[aggType.ordinal()];
            case "DESC":
                if (fetchStmtsDesc == null) {
                    fetchStmtsDesc = initFetchStmt(orderBy);
                }
                return fetchStmtsDesc[aggType.ordinal()];
            default:
                throw new RuntimeException("Not supported" + orderBy + "order!");
        }
    }

    private PreparedStatement getSaveStmt(DataType dataType) {
        if (saveStmts == null) {
            saveStmts = new PreparedStatement[DataType.values().length];

            for (DataType type : DataType.values()) {
                saveStmts[type.ordinal()] =
                    prepare(
                        "INSERT INTO " + keyspaceName + "." + VModelConstants.READINGS_TABLE +
                            "(" +
                                VModelConstants.TENANT_ID_READINGS_COLUMN +
                                "," + VModelConstants.DATA_SOURCE_ID_COLUMN +
                                "," + VModelConstants.READING_TYPE_ID_COLUMN +
                                "," + VModelConstants.READ_AT_COLUMN +
                                "," + getValueColumnName(type) + 
                                "," + VModelConstants.DATA_TYPE_COLUMN +
                                "," + VModelConstants.CREATED_AT_COLUMN +
                                "," + VModelConstants.CREATED_BY_ID_COLUMN +
                            ")" +
                            " VALUES(?, ?, ?, ?, ?, ?, ?, ?)");
            }
        }

        return saveStmts[dataType.ordinal()];
    }

    private static void addDataType(KvEntry kvEntry, BoundStatementBuilder stmt, int column) {
        switch (kvEntry.getDataType()) {
            case BOOLEAN:
                stmt.setInt(column, VModelConstants.DATA_TYPE_BOOLEAN);
                break;
            case STRING:
                stmt.setInt(column, VModelConstants.DATA_TYPE_STRING);
                break;
            case LONG:
                stmt.setInt(column, VModelConstants.DATA_TYPE_LONG);
                break;
            case DOUBLE:
                stmt.setInt(column, VModelConstants.DATA_TYPE_DECIMAL);
                break;
            case JSON:
                stmt.setInt(column, VModelConstants.DATA_TYPE_OBJECT);
                break;
        }
    }

    private static void addValue(KvEntry kvEntry, BoundStatementBuilder stmt, int column) {
        switch (kvEntry.getDataType()) {
            case BOOLEAN:
                Optional<Boolean> booleanValue = kvEntry.getBooleanValue();
                booleanValue.ifPresent(b -> stmt.setBoolean(column, b));
                break;
            case STRING:
                Optional<String> stringValue = kvEntry.getStrValue();
                stringValue.ifPresent(s -> stmt.setString(column, s));
                break;
            case LONG:
                Optional<Long> longValue = kvEntry.getLongValue();
                longValue.ifPresent(l -> stmt.setLong(column, l));
                break;
            case DOUBLE:
                Optional<Double> doubleValue = kvEntry.getDoubleValue();
                doubleValue.ifPresent(d -> stmt.setBigDecimal(column, BigDecimal.valueOf(d)));
                break;
            case JSON:
                Optional<String> jsonValue = kvEntry.getJsonValue();
                jsonValue.ifPresent(jsonObject -> stmt.setString(column, jsonObject));
                break;
        }
    }

    private static String getValueColumnName(DataType type) {
        switch (type) {
            case BOOLEAN:
                return VModelConstants.BOOLEAN_VALUE_COLUMN;
            case STRING:
                return VModelConstants.STRING_VALUE_COLUMN;
            case LONG:
                return VModelConstants.LONG_VALUE_COLUMN;
            case DOUBLE:
                return VModelConstants.DOUBLE_VALUE_COLUMN;
            case JSON:
                return VModelConstants.JSON_VALUE_COLUMN;
            default:
                throw new RuntimeException("Not implemented!");
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
                String query =
                    "SELECT " + String.join(", ", VModelConstants.getFetchColumnNames(type)) +
                    " FROM " + keyspaceName + "." + VModelConstants.READINGS_TABLE +
                    " WHERE " + VModelConstants.TENANT_ID_READINGS_COLUMN + " = ?" +
                    " AND " + VModelConstants.DATA_SOURCE_ID_COLUMN + " = ?" +
                    " AND " + VModelConstants.READING_TYPE_ID_COLUMN + " = ?" +
                    " AND " + VModelConstants.READ_AT_COLUMN + " > ?" +
                    " AND " + VModelConstants.READ_AT_COLUMN + " <= ?" +
                    (type == Aggregation.NONE
                        ? " ORDER BY " + VModelConstants.READ_AT_COLUMN + " " + orderBy.toUpperCase() + " LIMIT ?"
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