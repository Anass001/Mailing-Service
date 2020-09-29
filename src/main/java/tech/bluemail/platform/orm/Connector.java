/*
 * Decompiled with CFR <Could not determine version>.
 */
package tech.bluemail.platform.orm;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Consumer;
import tech.bluemail.platform.exceptions.DatabaseException;
import tech.bluemail.platform.logging.Logger;
import tech.bluemail.platform.orm.DataSource;
import tech.bluemail.platform.orm.Query;

public class Connector {
    private DataSource dataSource;
    private String key;
    private String databaseName;
    private String host;
    private int port;
    private String username;
    private String password;
    private String driver = "mysql";
    private String charset = "utf8";
    private String engine = "InnoDB";
    private String[] supportedDrivers = new String[]{"mysql", "pgsql"};
    private String lastErrorMessage = "";
    private int lastInsertedId = 0;
    private int affectedRowsCount = 0;
    public static final int FETCH_FIRST = 0;
    public static final int FETCH_ALL = 1;
    public static final int FETCH_LAST = 3;
    public static final int AFFECTED_ROWS = 0;
    public static final int LAST_INSERTED_ID = 1;
    public static final int BEGIN_TRANSACTION = 0;
    public static final int COMMIT_TRANSACTION = 1;
    public static final int ROLLBACK_TRANSACTION = 2;

    public synchronized void iniDataSource() throws Exception {
        switch (this.getDriver()) {
            case "mysql": {
                this.dataSource = new DataSource("com.mysql.jdbc.Driver", "jdbc:mysql://" + this.getHost() + ":" + this.getPort() + "/" + this.getName(), this.getUsername(), this.getPassword());
                return;
            }
            case "pgsql": {
                this.dataSource = new DataSource("org.postgresql.Driver", "jdbc:postgresql://" + this.getHost() + ":" + this.getPort() + "/" + this.getName(), this.getUsername(), this.getPassword());
                return;
            }
        }
        throw new DatabaseException("Database Not Supported !");
    }

    public synchronized List<LinkedHashMap<String, Object>> executeQuery(String query, Object[] data, int returnType) throws DatabaseException {
        ArrayList<LinkedHashMap<String, Object>> results = new ArrayList<LinkedHashMap<String, Object>>();
        try {
            try (Connection connection = this.dataSource.getConnection();
                 PreparedStatement pr = connection.prepareStatement(query, 1005, 1008);){
                if (data != null && data.length > 0) {
                    int index = 1;
                    int type = 0;
                    for (Object object : data) {
                        if (object == null) continue;
                        switch (object.getClass().getName()) {
                            case "java.lang.String": {
                                type = 12;
                                break;
                            }
                            case "java.lang.Double": {
                                type = 3;
                                break;
                            }
                            case "java.lang.Integer": {
                                type = 4;
                                break;
                            }
                            case "java.sql.Date": {
                                type = 91;
                                break;
                            }
                            case "java.sql.Timestamp": {
                                type = 93;
                                break;
                            }
                            case "java.lang.Boolean": {
                                type = 16;
                            }
                        }
                        pr.setObject(index, object, type);
                        ++index;
                    }
                }
                try (ResultSet result = pr.executeQuery();){
                    int count;
                    block45 : {
                        LinkedHashMap<String, Object> row;
                        int i;
                        if (!result.isBeforeFirst()) return results;
                        ResultSetMetaData meta = result.getMetaData();
                        count = 0;
                        switch (returnType) {
                            case 1: {
                                break;
                            }
                            case 0: {
                                result.first();
                                row = new LinkedHashMap();
                                for (i = 1; i <= meta.getColumnCount(); ++i) {
                                    row.put(meta.getColumnName(i), result.getObject(i));
                                }
                                results.add(row);
                                ++count;
                            }
                            default: {
                                break block45;
                            }
                        }
                        while (result.next()) {
                            row = new LinkedHashMap<String, Object>();
                            for (i = 1; i <= meta.getColumnCount(); ++i) {
                                row.put(meta.getColumnName(i), result.getObject(i));
                            }
                            results.add(row);
                            ++count;
                        }
                    }
                    this.affectedRowsCount = count;
                    return results;
                }
            }
        }
        catch (Exception e) {
            this.lastErrorMessage = e.getMessage();
            throw new DatabaseException(e);
        }
    }

    public synchronized int executeUpdate(String query, Object[] data, int returnType) throws DatabaseException {
        int result = 0;
        try {
            try (Connection connection = this.dataSource.getConnection();
                 PreparedStatement pr = returnType == 1 ? connection.prepareStatement(query, 1) : connection.prepareStatement(query);){
                if (data != null && data.length > 0) {
                    int index = 1;
                    int type = 0;
                    Object[] arrobject = data;
                    int n = arrobject.length;
                    for (int i = 0; i < n; ++index, ++i) {
                        Object object = arrobject[i];
                        if (object != null) {
                            switch (object.getClass().getName()) {
                                case "java.lang.String": {
                                    type = 12;
                                    break;
                                }
                                case "java.lang.Double": {
                                    type = 3;
                                    break;
                                }
                                case "java.lang.Integer": {
                                    type = 4;
                                    break;
                                }
                                case "java.sql.Date": {
                                    type = 91;
                                    break;
                                }
                                case "java.sql.Timestamp": {
                                    type = 93;
                                    break;
                                }
                                case "java.lang.Boolean": {
                                    type = 16;
                                }
                            }
                        }
                        pr.setObject(index, object, type);
                    }
                }
                result = pr.executeUpdate();
                if (returnType == 1) {
                    ResultSet rs = pr.getGeneratedKeys();
                    if (rs.next()) {
                        result = this.lastInsertedId = rs.getInt(1);
                    }
                    this.closeResultset(rs);
                    return result;
                }
                this.affectedRowsCount = result;
                return result;
            }
        }
        catch (Exception e) {
            this.lastErrorMessage = e.getMessage();
            throw new DatabaseException(e);
        }
    }

    public synchronized List<String> availableTables(String schema) throws Exception {
        String columns;
        ArrayList<String> tables = new ArrayList<String>();
        String sql = "";
        String condition = schema != null && !"".equals(schema) ? "WHERE schemaname = '" + schema + "'" : "";
        String string = columns = schema != null && !"".equals(schema) ? "relname" : "schemaname || '.' || relname";
        if ("pgsql".equalsIgnoreCase(this.driver)) {
            sql = "SELECT " + columns + " AS name FROM pg_stat_user_tables " + condition + " ORDER BY name ASC";
        } else if ("mysql".equalsIgnoreCase(this.driver)) {
            sql = "SHOW tables FROM " + this.databaseName;
        }
        List<LinkedHashMap<String, Object>> result = this.executeQuery(sql, null, 1);
        result.forEach(row -> tables.add(String.valueOf(row.get("name"))));
        return tables;
    }

    public synchronized void transaction(int type) {
        this.lastErrorMessage = "";
        try {
            switch (type) {
                case 0: {
                    this.dataSource.getConnection().setSavepoint();
                    return;
                }
                case 1: {
                    this.dataSource.getConnection().commit();
                    return;
                }
                case 2: {
                    this.dataSource.getConnection().rollback();
                    return;
                }
            }
            this.lastErrorMessage = "The passed transaction type is wrong!";
            throw new DatabaseException(this.lastErrorMessage);
        }
        catch (Exception e) {
            this.lastErrorMessage = e.getMessage();
            Logger.error(e, Connector.class);
        }
    }

    public synchronized void closePreparedStatement(PreparedStatement pr) {
        try {
            if (pr == null) return;
            pr.close();
            return;
        }
        catch (SQLException e) {
            this.lastErrorMessage = e.getMessage();
            Logger.error(e, Connector.class);
        }
    }

    public synchronized void closeResultset(ResultSet result) {
        try {
            if (result == null) return;
            result.close();
            return;
        }
        catch (SQLException e) {
            this.lastErrorMessage = e.getMessage();
            Logger.error(e, Connector.class);
        }
    }

    public Query query() {
        return new Query(this.key);
    }

    public String getKey() {
        return this.key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getName() {
        return this.databaseName;
    }

    public void setName(String name) {
        this.databaseName = name;
    }

    public String getHost() {
        return this.host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return this.port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getUsername() {
        return this.username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return this.password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDriver() {
        return this.driver;
    }

    public void setDriver(String driver) {
        this.driver = driver;
    }

    public String getCharset() {
        return this.charset;
    }

    public void setCharset(String charset) {
        this.charset = charset;
    }

    public String getEngine() {
        return this.engine;
    }

    public void setEngine(String engine) {
        this.engine = engine;
    }

    public String[] getSupportedDrivers() {
        return this.supportedDrivers;
    }

    public void setSupportedDrivers(String[] supportedDrivers) {
        this.supportedDrivers = supportedDrivers;
    }

    public String getLastErrorMessage() {
        return this.lastErrorMessage;
    }

    public void setLastErrorMessage(String lastErrorMessage) {
        this.lastErrorMessage = lastErrorMessage;
    }

    public int getLastInsertedId() {
        return this.lastInsertedId;
    }

    public void setLastInsertedId(int lastInsertedId) {
        this.lastInsertedId = lastInsertedId;
    }

    public int getAffectedRowsCount() {
        return this.affectedRowsCount;
    }

    public void setAffectedRowsCount(int affectedRowsCount) {
        this.affectedRowsCount = affectedRowsCount;
    }

    public DataSource getDataSource() {
        return this.dataSource;
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public String getDatabaseName() {
        return this.databaseName;
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }
}

