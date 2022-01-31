/**
 * Özgün AY
 */
package org.thingsboard.server.dao.model.vsensor;

import java.util.UUID;

import org.apache.commons.lang3.ArrayUtils;
import org.thingsboard.server.common.data.kv.Aggregation;

public class VModelConstants {

    private VModelConstants() {
    }

    /**
     * Cassandra VSensor attributes and timeseries constants.
     */
    public static final String READINGS_TABLE = "readings";

    public static final String TENANT_ID_READINGS_COLUMN = "tenantid";
    public static final String DATA_SOURCE_ID_COLUMN = "datasourceid";
    public static final String READING_TYPE_ID_COLUMN = "readingtypeid";
    public static final String READ_AT_COLUMN = "readat";
    public static final String CREATED_AT_COLUMN = "createdat";
    public static final String CREATED_BY_ID_COLUMN = "createdbyid";
    public static final String DATA_TYPE_COLUMN = "datatype";

    /**
     * Main names of cassandra key-value columns storage.
     */
    public static final String BOOLEAN_VALUE_COLUMN = "valueboolean";
    public static final String LONG_VALUE_COLUMN = "valuelong";
    public static final String DOUBLE_VALUE_COLUMN = "valuedecimal";
    public static final String DATE_TIME_VALUE_COLUMN = "valuedatetime";
    public static final String STRING_VALUE_COLUMN = "valuestring";
    public static final String JSON_VALUE_COLUMN = "valuejson";

    protected static final String[] NONE_AGGREGATION_COLUMNS =
        new String[] {
            LONG_VALUE_COLUMN,
            DOUBLE_VALUE_COLUMN,
            BOOLEAN_VALUE_COLUMN,
            STRING_VALUE_COLUMN,
            JSON_VALUE_COLUMN,
            DATE_TIME_VALUE_COLUMN,
            READING_TYPE_ID_COLUMN,
            READ_AT_COLUMN
        };

    protected static final String[] COUNT_AGGREGATION_COLUMNS =
        new String[] {
            count(LONG_VALUE_COLUMN),
            count(DOUBLE_VALUE_COLUMN),
            count(BOOLEAN_VALUE_COLUMN),
            count(STRING_VALUE_COLUMN),
            count(JSON_VALUE_COLUMN),
            count(DATE_TIME_VALUE_COLUMN),
            count(READING_TYPE_ID_COLUMN),
            count(READ_AT_COLUMN)
        };

    protected static final String[] MIN_AGGREGATION_COLUMNS =
        ArrayUtils.addAll(
            COUNT_AGGREGATION_COLUMNS,
            min(LONG_VALUE_COLUMN),
            min(DOUBLE_VALUE_COLUMN),
            min(BOOLEAN_VALUE_COLUMN),
            min(STRING_VALUE_COLUMN),
            min(JSON_VALUE_COLUMN),
            min(DATE_TIME_VALUE_COLUMN));

    protected static final String[] MAX_AGGREGATION_COLUMNS =
        ArrayUtils.addAll(
            COUNT_AGGREGATION_COLUMNS,
            max(LONG_VALUE_COLUMN),
            max(DOUBLE_VALUE_COLUMN),
            max(BOOLEAN_VALUE_COLUMN),
            max(STRING_VALUE_COLUMN),
            max(DATE_TIME_VALUE_COLUMN),
            max(JSON_VALUE_COLUMN));

    protected static final String[] SUM_AGGREGATION_COLUMNS =
        ArrayUtils.addAll(
            COUNT_AGGREGATION_COLUMNS,
            sum(LONG_VALUE_COLUMN),
            sum(DOUBLE_VALUE_COLUMN));

    protected static final String[] AVG_AGGREGATION_COLUMNS = SUM_AGGREGATION_COLUMNS;

    public static final int DATA_TYPE_BOOLEAN = 3;
    public static final int DATA_TYPE_STRING = 18;
    public static final int DATA_TYPE_LONG = 11;
    public static final int DATA_TYPE_DECIMAL = 15;
    public static final int DATA_TYPE_OBJECT = 1;

    public static final UUID SYSTEM_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    public static String min(String s) {
        return "min(" + s + ")";
    }

    public static String max(String s) {
        return "max(" + s + ")";
    }

    public static String sum(String s) {
        return "sum(" + s + ")";
    }

    public static String count(String s) {
        return "count(" + s + ")";
    }

    public static String[] getFetchColumnNames(Aggregation aggregation) {
        switch (aggregation) {
            case NONE:
                return NONE_AGGREGATION_COLUMNS;
            case MIN:
                return MIN_AGGREGATION_COLUMNS;
            case MAX:
                return MAX_AGGREGATION_COLUMNS;
            case SUM:
                return SUM_AGGREGATION_COLUMNS;
            case COUNT:
                return COUNT_AGGREGATION_COLUMNS;
            case AVG:
                return AVG_AGGREGATION_COLUMNS;
            default:
                throw new RuntimeException("Aggregation type: " + aggregation + " is not supported!");
        }
    }
}
