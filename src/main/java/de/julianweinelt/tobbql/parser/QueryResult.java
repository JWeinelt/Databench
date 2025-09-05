package de.julianweinelt.tobbql.parser;

import com.google.gson.Gson;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class QueryResult {
    public static final QueryResult EMPTY_RESULT = new QueryResult(QueryResultType.EMPTY, Type.UNKNOWN, 0, new ArrayList<>());

    @Getter
    private final QueryResultType type;
    @Getter
    private final Type queryType;
    @Getter
    private final int rowsChanged;
    @Getter
    private final List<HashMap<String, Object>> resultSet;

    public QueryResult(QueryResultType type, Type queryType, int rowsChanged) {
        this.type = type;
        this.queryType = queryType;
        this.rowsChanged = rowsChanged;
        resultSet = new ArrayList<>();
    }

    public QueryResult(QueryResultType type, Type queryType, int rowsChanged, List<HashMap<String, Object>> resultSet) {
        this.type = type;
        this.queryType = queryType;
        this.rowsChanged = rowsChanged;
        this.resultSet = resultSet;
    }

    public QueryResult(String jsonBody) {
        Gson gson = new Gson();
        QueryResult qr = gson.fromJson(jsonBody, QueryResult.class);
        this.type = qr.type;
        this.queryType = qr.queryType;
        this.rowsChanged = qr.rowsChanged;
        this.resultSet = qr.resultSet;
    }

    @Nullable
    public HashMap<String, Object> getRow(int index) {
        if (index < 0 || index >= resultSet.size()) return null;
        return resultSet.get(index);
    }

    public List<Object> getValuesOfColumn(String columnName) {
        List<Object> values = new ArrayList<>();
        for (HashMap<String, Object> row : resultSet) {
            if (row.containsKey(columnName)) {
                values.add(row.get(columnName));
            }
        }
        return values;
    }
}