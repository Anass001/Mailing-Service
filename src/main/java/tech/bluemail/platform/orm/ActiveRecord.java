package tech.bluemail.platform.orm;

import tech.bluemail.platform.exceptions.*;
import tech.bluemail.platform.logging.*;
import java.util.*;
import java.io.*;
import org.apache.commons.io.*;
import org.apache.commons.lang.*;
import tech.bluemail.platform.utils.*;
import tech.bluemail.platform.meta.annotations.*;

public class ActiveRecord
{
    private String database;
    private String schema;
    private String table;
    private LinkedHashMap<String, LinkedHashMap<String, Object>> columns;
    private LinkedHashMap<String, Object> primary;
    public static final String INT = "integer";
    public static final String DECIMAL = "decimal";
    public static final String TEXT = "text";
    public static final String DATE = "date";
    public static final String TIME_STAMP = "timestamp";
    public static final String BOOL = "boolean";
    public static final int CREATE_TABLE = 0;
    public static final int CREATE_CLASS = 1;
    public static final int FETCH_ARRAY = 2;
    public static final int FETCH_OBJECT = 3;
    public static final int OBJECTS_ROWS = 4;
    public static final int ARRAYS_ROWS = 5;
    
    public ActiveRecord() throws DatabaseException {
        super();
        this.database = "";
        this.schema = "";
        this.table = "";
        this.columns = new LinkedHashMap<String, LinkedHashMap<String, Object>>();
        this.primary = new LinkedHashMap<String, Object>();
        this.init();
    }
    
    public ActiveRecord(final Object primaryValue) throws DatabaseException {
        super();
        this.database = "";
        this.schema = "";
        this.table = "";
        this.columns = new LinkedHashMap<String, LinkedHashMap<String, Object>>();
        this.primary = new LinkedHashMap<String, Object>();
        this.init();
        this.setFieldValue(this.primary.get("field"), primaryValue);
    }
    
    public void load(final String column, final Object value) {
        LinkedHashMap<String, Object> row = new LinkedHashMap<String, Object>();
        final String schema = (this.schema == null || "".equals(this.schema)) ? "" : (this.schema + ".");
        try {
            if (column != null && !"".equals(column) && value != null && !"".equals(value)) {
                row = Database.get(this.database).query().from(schema + this.table, new String[] { "*" }).where(column + " = ?", new Object[] { value }, "").first();
            }
            if (!row.isEmpty()) {
                String field;
                row.forEach((c, v) -> {
                    if (this.getColumns().containsKey(c)) {
                        field = (String)this.getColumns().get(c).get("field");
                        this.setFieldValue(field, v);
                    }
                });
            }
        }
        catch (Exception e) {
            Logger.error(new DatabaseException(e), ActiveRecord.class);
        }
    }
    
    public void load() {
        if (!this.primary.isEmpty()) {
            final Object primaryValue = this.getFieldValue(String.valueOf(this.primary.get("field")));
            if (primaryValue != null) {
                this.load(String.valueOf(this.primary.get("name")), primaryValue);
            }
        }
    }
    
    public void unLoad() {
        Object o2 = null;
        Object value;
        this.columns.forEach((columnName, column) -> {
            if (columnName != null && column != null && !column.isEmpty()) {
                if ("integer".equalsIgnoreCase(String.valueOf(column.get("type")))) {
                    // new(java.lang.Integer.class)
                    new Integer(0);
                }
                else {
                    o2 = null;
                }
                value = o2;
                this.setFieldValue(String.valueOf(column.get("field")), value);
            }
        });
    }
    
    public void map(final LinkedHashMap<String, Object> row) {
        if (!row.isEmpty()) {
            String field;
            row.forEach((c, v) -> {
                if (this.getColumns().containsKey(c)) {
                    field = (String)this.getColumns().get(c).get("field");
                    this.setFieldValue(field, v);
                }
            });
        }
    }
    
    public int insert() {
        int id = 0;
        try {
            boolean customPrimary = false;
            int index = 0;
            final String schema = (this.schema == null || "".equals(this.schema)) ? "" : (this.schema + ".");
            final String[] columns = new String[this.columns.size()];
            final Object[] data = new Object[this.columns.size()];
            if (!this.primary.isEmpty()) {
                columns[index] = this.primary.get("name");
                Object value = this.getFieldValue(this.primary.get("field"));
                if (value == null || Integer.parseInt(String.valueOf(value)) == 0) {
                    if ("pgsql".equalsIgnoreCase(Database.get(this.database).getDriver())) {
                        value = 0;
                        final List<LinkedHashMap<String, Object>> nextVal = Database.get(this.database).executeQuery("SELECT nextval('" + schema + "seq_" + String.valueOf(this.primary.get("name")) + "_" + this.table + "')", null, 0);
                        if (!nextVal.isEmpty() && nextVal.get(0).containsKey("nextval")) {
                            value = Integer.parseInt(String.valueOf(nextVal.get(0).get("nextval")));
                        }
                    }
                    else {
                        value = null;
                    }
                    data[index] = value;
                }
                else {
                    if ("pgsql".equalsIgnoreCase(Database.get(this.database).getDriver())) {
                        customPrimary = true;
                    }
                    data[index] = value;
                }
                ++index;
            }
            final Iterator it = this.columns.entrySet().iterator();
            while (it.hasNext()) {
                final Map.Entry pair = it.next();
                final String columnName = pair.getKey();
                final LinkedHashMap<String, Object> column = pair.getValue();
                if (!column.get("primary")) {
                    columns[index] = columnName;
                    data[index] = this.getFieldValue(column.get("field"));
                    ++index;
                }
                it.remove();
            }
            id = Database.get(this.database).query().from(schema + this.table, columns).insert(data);
            if (customPrimary) {
                Database.get(this.database).executeUpdate("ALTER SEQUENCE IF EXISTS " + schema + "seq_" + String.valueOf(this.primary.get("name")) + "_" + this.table + " RESTART WITH " + (id + 1), null, 0);
            }
        }
        catch (Exception e) {
            Logger.error(new DatabaseException(e), ActiveRecord.class);
        }
        return id;
    }
    
    public int update() {
        int affectedRows = 0;
        int index = 0;
        final String schema = (this.schema == null || "".equals(this.schema)) ? "" : (this.schema + ".");
        final String[] columns = new String[this.columns.size() - 1];
        final Object[] data = new Object[this.columns.size() - 1];
        try {
            final String primaryColumn = this.primary.get("name");
            final Object primaryValue = this.getFieldValue(this.primary.get("field"));
            if (primaryValue == null || Integer.parseInt(String.valueOf(primaryValue)) == 0) {
                throw new DatabaseException("Primary Key Must Not Be Null !");
            }
            final Iterator it = this.columns.entrySet().iterator();
            while (it.hasNext()) {
                final Map.Entry pair = it.next();
                final String columnName = pair.getKey();
                final LinkedHashMap<String, Object> column = pair.getValue();
                if (!column.get("primary")) {
                    columns[index] = columnName;
                    data[index] = this.getFieldValue(column.get("field"));
                    ++index;
                }
                it.remove();
            }
            affectedRows = Database.get(this.database).query().from(schema + this.table, columns).where(primaryColumn + " = ?", new Object[] { primaryValue }, "").update(data);
        }
        catch (Exception e) {
            Logger.error(new DatabaseException(e), ActiveRecord.class);
        }
        return affectedRows;
    }
    
    public int delete() {
        int affectedRows = 0;
        try {
            final String schema = (this.schema == null || "".equals(this.schema)) ? "" : (this.schema + ".");
            final String primaryColumn = this.primary.get("name");
            final Object primaryValue = this.getFieldValue(this.primary.get("field"));
            if (primaryValue == null || Integer.parseInt(String.valueOf(primaryValue)) == 0) {
                throw new DatabaseException("Primary Key Must Not Be Null !");
            }
            affectedRows = Database.get(this.database).query().from(schema + this.table, new String[0]).where(primaryColumn + " = ?", new Object[] { primaryValue }, "").delete();
        }
        catch (Exception e) {
            Logger.error(new DatabaseException(e), ActiveRecord.class);
        }
        return affectedRows;
    }
    
    public boolean tableExists() {
        final boolean res = false;
        try {
            final List<LinkedHashMap<String, Object>> result = Database.get(this.database).executeQuery("SELECT EXISTS (SELECT 1 FROM pg_catalog.pg_class c JOIN pg_catalog.pg_namespace n ON n.oid = c.relnamespace WHERE  n.nspname = ? AND c.relname = ? AND c.relkind = 'r')", new Object[] { this.schema, this.table }, 0);
            if (result != null && !result.isEmpty()) {
                return result.get(0).get("exists");
            }
        }
        catch (Exception e) {
            Logger.error(new DatabaseException(e), ActiveRecord.class);
        }
        return res;
    }
    
    public int sync() {
        final int result = 0;
        try {
            final String schema = (this.schema == null || "".equals(this.schema)) ? "" : (this.schema + ".");
            final Iterator it = this.columns.entrySet().iterator();
            String lines = "";
            String indice = "";
            String sequence = "";
            String line = "";
            while (it.hasNext()) {
                final Map.Entry pair = it.next();
                final LinkedHashMap<String, Object> column = pair.getValue();
                final String name = pair.getKey();
                final boolean primary = column.get("primary");
                final boolean autoIncrement = column.get("autoincrement");
                final String type = column.get("type");
                final String nullable = column.get("nullable") ? " DEFAULT NULL " : " NOT NULL ";
                final int length = column.get("length");
                final String s = type;
                switch (s) {
                    case "integer": {
                        line = name + " integer " + nullable;
                        if ("pgsql".equalsIgnoreCase(Database.get(this.database).getDriver())) {
                            if (primary) {
                                indice = "CONSTRAINT c_pk_" + name + "_" + this.table + " PRIMARY KEY(" + name + ") \n";
                            }
                            if (autoIncrement) {
                                sequence = "seq_" + name + "_" + this.table;
                            }
                        }
                        else {
                            if (primary) {
                                line += " PRIMARY KEY ";
                            }
                            if (autoIncrement) {
                                line += " AUTO_INCREMENT ";
                            }
                        }
                        lines = lines + line + ",\n";
                        break;
                    }
                    case "decimal": {
                        lines = lines + name + " decimal " + nullable + ",\n";
                        break;
                    }
                    case "text": {
                        if (length > 0 && length <= 255) {
                            lines = lines + name + " varchar(" + length + ") " + nullable + ",\n";
                            break;
                        }
                        lines = lines + name + " text " + nullable + ",\n";
                        break;
                    }
                    case "boolean": {
                        lines = lines + name + " boolean " + nullable + ",\n";
                        break;
                    }
                    case "timestamp": {
                        lines = lines + name + " timestamp " + nullable + ",\n";
                        break;
                    }
                    case "date": {
                        lines = lines + name + " timestamp " + nullable + ",\n";
                        break;
                    }
                }
                it.remove();
            }
            final String query = String.format("CREATE TABLE IF NOT EXISTS %s (\n%s,\n%s);", schema + this.table, lines.substring(0, lines.length() - 2), indice);
            Database.get(this.database).executeUpdate(query, null, 0);
            if (!"".equalsIgnoreCase(sequence)) {
                final String sequenceQuery = "CREATE SEQUENCE IF NOT EXISTS " + schema + sequence + " START 1";
                Database.get(this.database).executeUpdate(sequenceQuery, null, 0);
            }
        }
        catch (Exception e) {
            Logger.error(new DatabaseException(e), ActiveRecord.class);
        }
        return result;
    }
    
    public static ActiveRecord first(final Class subClass) {
        return first(subClass, "", new Object[0]);
    }
    
    public static ActiveRecord first(final Class subClass, final String condition, final Object[] values) {
        ActiveRecord record = null;
        try {
            final ActiveRecord outerObject = subClass.getConstructor((Class<?>[])new Class[0]).newInstance(new Object[0]);
            final String schema = (outerObject.getSchema() == null || "".equals(outerObject.getSchema())) ? "" : (outerObject.getSchema() + ".");
            final Query queryBuilder = Database.get(outerObject.getDatabase()).query().from(schema + outerObject.getTable(), new String[] { "*" }).order(new String[] { String.valueOf(outerObject.getPrimary().get("name")) }, "ASC");
            if (condition != null && !"".equalsIgnoreCase(condition)) {
                queryBuilder.where(condition, values, "");
            }
            final LinkedHashMap<String, Object> row = queryBuilder.first();
            if (row != null && !row.isEmpty()) {
                record = subClass.getConstructor((Class<?>[])new Class[0]).newInstance(new Object[0]);
                final Iterator it = row.entrySet().iterator();
                while (it.hasNext()) {
                    final Map.Entry pair = it.next();
                    final String columnName = pair.getKey();
                    final Object columnValue = pair.getValue();
                    if (record.getColumns().containsKey(columnName)) {
                        record.setFieldValue(record.getColumns().get(columnName).get("field"), columnValue);
                    }
                    it.remove();
                }
            }
        }
        catch (Exception e) {
            Logger.error(new DatabaseException(e), ActiveRecord.class);
        }
        return record;
    }
    
    public static ActiveRecord last(final Class subClass) {
        return last(subClass, "", new Object[0]);
    }
    
    public static ActiveRecord last(final Class subClass, final String condition, final Object[] values) {
        ActiveRecord record = null;
        try {
            final ActiveRecord outerObject = subClass.getConstructor((Class<?>[])new Class[0]).newInstance(new Object[0]);
            final String schema = (outerObject.getSchema() == null || "".equals(outerObject.getSchema())) ? "" : (outerObject.getSchema() + ".");
            final Query queryBuilder = Database.get(outerObject.getDatabase()).query().from(schema + outerObject.getTable(), new String[] { "*" }).order(new String[] { String.valueOf(outerObject.getPrimary().get("name")) }, "DESC");
            if (condition != null && !"".equalsIgnoreCase(condition)) {
                queryBuilder.where(condition, values, "");
            }
            final LinkedHashMap<String, Object> row = queryBuilder.first();
            if (row != null && !row.isEmpty()) {
                record = subClass.getConstructor((Class<?>[])new Class[0]).newInstance(new Object[0]);
                final Iterator it = row.entrySet().iterator();
                while (it.hasNext()) {
                    final Map.Entry pair = it.next();
                    final String columnName = pair.getKey();
                    final Object columnValue = pair.getValue();
                    if (record.getColumns().containsKey(columnName)) {
                        record.setFieldValue(record.getColumns().get(columnName).get("field"), columnValue);
                    }
                    it.remove();
                }
            }
        }
        catch (Exception e) {
            Logger.error(new DatabaseException(e), ActiveRecord.class);
        }
        return record;
    }
    
    public static List all(final Class subClass) {
        return all(subClass, "", new Object[0]);
    }
    
    public static List all(final Class subClass, final String condition, final Object[] values) {
        final List records = new ArrayList();
        try {
            ActiveRecord record = null;
            final ActiveRecord outerObject = subClass.getConstructor((Class<?>[])new Class[0]).newInstance(new Object[0]);
            final String schema = (outerObject.getSchema() == null || "".equals(outerObject.getSchema())) ? "" : (outerObject.getSchema() + ".");
            final Query queryBuilder = Database.get(outerObject.getDatabase()).query().from(schema + outerObject.getTable(), new String[] { "*" });
            if (condition != null && !"".equalsIgnoreCase(condition)) {
                queryBuilder.where(condition, values, "");
            }
            final List<LinkedHashMap<String, Object>> rows = queryBuilder.order(new String[] { String.valueOf(outerObject.getPrimary().get("name")) }, "ASC").all();
            if (rows != null && !rows.isEmpty()) {
                for (final LinkedHashMap<String, Object> row : rows) {
                    record = subClass.getConstructor((Class<?>[])new Class[0]).newInstance(new Object[0]);
                    final Iterator it = row.entrySet().iterator();
                    while (it.hasNext()) {
                        final Map.Entry pair = it.next();
                        final String columnName = pair.getKey();
                        final Object columnValue = pair.getValue();
                        if (record.getColumns().containsKey(columnName)) {
                            record.setFieldValue(record.getColumns().get(columnName).get("field"), columnValue);
                        }
                        it.remove();
                    }
                    records.add(subClass.cast(record));
                }
            }
        }
        catch (Exception e) {
            Logger.error(new DatabaseException(e), ActiveRecord.class);
        }
        return records;
    }
    
    public static int[] insert(final List records) {
        int[] results = new int[0];
        if (records != null && records.size() > 0) {
            for (final Object record : records) {
                if (record != null) {
                    results = ArrayUtils.add(results, ((ActiveRecord)record).insert());
                }
            }
        }
        return results;
    }
    
    public static int update(final List records) {
        int affectedRows = 0;
        if (records != null && records.size() > 0) {
            for (final Object record : records) {
                if (record != null) {
                    affectedRows = ((ActiveRecord)record).insert();
                }
            }
        }
        return affectedRows;
    }
    
    public static int delete(final List records) {
        int affectedRows = 0;
        if (records != null && records.size() > 0) {
            for (final Object record : records) {
                if (record != null) {
                    affectedRows = ((ActiveRecord)record).delete();
                }
            }
        }
        return affectedRows;
    }
    
    public static int delete(final Class subClass) {
        return delete(subClass, "", new Object[0]);
    }
    
    public static int delete(final Class subClass, final String condition, final Object[] values) {
        int affectedRows = 0;
        try {
            final ActiveRecord outerObject = subClass.getConstructor((Class<?>[])new Class[0]).newInstance(new Object[0]);
            final String schema = (outerObject.getSchema() == null || "".equals(outerObject.getSchema())) ? "" : (outerObject.getSchema() + ".");
            final Query queryBuilder = Database.get(outerObject.getDatabase()).query().from(schema + outerObject.getTable(), new String[0]);
            if (condition != null && !"".equalsIgnoreCase(condition)) {
                queryBuilder.where(condition, values, "");
            }
            affectedRows = queryBuilder.delete();
        }
        catch (Exception e) {
            Logger.error(new DatabaseException(e), ActiveRecord.class);
        }
        return affectedRows;
    }
    
    public static boolean sync(final String model, final String database, final String schema, final String table) {
        boolean created = false;
        try {
            String template = FileUtils.readFileToString(new File(ActiveRecord.class.getResource("/tech/bluemail/platform/templates/model.tpl").getPath()));
            template = StringUtils.replace(template, "$P{MODEL}", model);
            template = StringUtils.replace(template, "$P{SCHEMA}", schema);
            template = StringUtils.replace(template, "$P{TABLE}", table);
            template = StringUtils.replace(template, "$P{DATABASE}", database);
            String columns = "";
            final String tab = "    ";
            final List<LinkedHashMap<String, Object>> rows = Database.get(database).executeQuery("SELECT * FROM information_schema.columns WHERE table_schema = ? AND table_name = ? ORDER BY ordinal_position ASC", new Object[] { schema, table }, 1);
            if (rows != null && !rows.isEmpty()) {
                for (final LinkedHashMap<String, Object> row : rows) {
                    if (row != null && !row.isEmpty()) {
                        final String name = String.valueOf(row.get("column_name")).toLowerCase();
                        final boolean primary = "id".equals(name);
                        String type = "character varying".equalsIgnoreCase(String.valueOf(row.get("data_type"))) ? "text" : String.valueOf(row.get("data_type")).toLowerCase();
                        type = (type.contains("timestamp") ? "timestamp" : type);
                        final int length = (!row.containsKey("character_maximum_length") || row.get("character_maximum_length") == null) ? 0 : Integer.parseInt(String.valueOf(row.get("character_maximum_length")));
                        final boolean nullable = "YES".equalsIgnoreCase(String.valueOf(row.get("is_nullable")));
                        String javaType = "String";
                        final char[] field = WordUtils.capitalize(name.replaceAll("_", " ").toLowerCase()).replaceAll(" ", "").toCharArray();
                        field[0] = Character.toLowerCase(field[0]);
                        final String s = type;
                        switch (s) {
                            case "integer": {
                                javaType = "int";
                                break;
                            }
                            case "text": {
                                javaType = "String";
                                break;
                            }
                            case "decimal": {
                                javaType = "double";
                                break;
                            }
                            case "boolean": {
                                javaType = "boolean";
                                break;
                            }
                            case "date": {
                                javaType = "java.sql.Date";
                                break;
                            }
                            case "timestamp": {
                                javaType = "java.sql.Timestamp";
                                break;
                            }
                        }
                        columns += "\n";
                        columns = columns + tab + "@Column\n";
                        columns = columns + tab + "(\n";
                        columns = columns + tab + tab + "name = \"" + name + "\",\n";
                        if (primary) {
                            columns = columns + tab + tab + "primary = true,\n";
                            columns = columns + tab + tab + "autoincrement = true,\n";
                        }
                        columns = columns + tab + tab + "type = \"" + type + "\",\n";
                        if (length > 0) {
                            columns = columns + tab + tab + "nullable = " + String.valueOf(nullable) + ",\n";
                            columns = columns + tab + tab + "length = " + length + "\n";
                        }
                        else {
                            columns = columns + tab + tab + "nullable = " + String.valueOf(nullable) + "\n";
                        }
                        columns = columns + tab + ")\n";
                        columns = columns + tab + "public " + javaType + " " + new String(field) + ";\n";
                    }
                }
            }
            template = StringUtils.replace(template, "$P{COLUMNS}", columns);
            FileUtils.writeStringToFile(new File(new File(System.getProperty("base.path")).getParent() + File.separator + "src" + File.separator + "tech" + File.separator + "bluemail" + File.separator + "platform" + File.separator + "models" + File.separator + model + ".java"), template);
            created = true;
        }
        catch (Exception e) {
            Logger.error(new DatabaseException(e), ActiveRecord.class);
        }
        return created;
    }
    
    private void init() throws DatabaseException {
        if (this.columns.isEmpty()) {
            final String[] fields = Inspector.classFields(this);
            if (fields != null && fields.length > 0) {
                for (final String field : fields) {
                    final Column meta = Inspector.columnMeta(this, field);
                    if (meta != null) {
                        if (!this.isTypeSupported(meta.type())) {
                            throw new DatabaseException(meta.type() + " Is Not A Valid Type !");
                        }
                        final LinkedHashMap column = new LinkedHashMap();
                        column.put("field", field);
                        column.put("name", meta.name());
                        column.put("autoincrement", meta.autoincrement());
                        column.put("primary", meta.primary());
                        column.put("type", meta.type());
                        column.put("nullable", meta.nullable());
                        column.put("length", meta.length());
                        if (meta.primary()) {
                            this.primary = (LinkedHashMap<String, Object>)column;
                        }
                        this.columns.put(String.valueOf(column.get("name")), column);
                    }
                }
            }
        }
        if (this.primary.isEmpty()) {
            throw new DatabaseException(this.table + this.primary + "This Active Record Class Must Have One Primary Column !");
        }
    }
    
    private Object getFieldValue(final String field) {
        Object value = null;
        try {
            value = this.getClass().getDeclaredField(field).get(this);
        }
        catch (Exception e) {
            Logger.error(new DatabaseException(e), ActiveRecord.class);
        }
        return value;
    }
    
    private void setFieldValue(final String field, final Object value) {
        try {
            this.getClass().getDeclaredField(field).set(this, value);
        }
        catch (Exception e) {
            Logger.error(new DatabaseException(e), ActiveRecord.class);
        }
    }
    
    private boolean isTypeSupported(final String type) {
        return "integer".equalsIgnoreCase(type) || "decimal".equalsIgnoreCase(type) || "text".equalsIgnoreCase(type) || "date".equalsIgnoreCase(type) || "timestamp".equalsIgnoreCase(type) || "boolean".equalsIgnoreCase(type);
    }
    
    public String getDatabase() {
        return this.database;
    }
    
    public void setDatabase(final String database) {
        this.database = database;
    }
    
    public String getSchema() {
        return this.schema;
    }
    
    public void setSchema(final String schema) {
        this.schema = schema;
    }
    
    public String getTable() {
        return this.table;
    }
    
    public void setTable(final String table) {
        this.table = table;
    }
    
    public LinkedHashMap<String, LinkedHashMap<String, Object>> getColumns() {
        return this.columns;
    }
    
    public void setColumns(final LinkedHashMap<String, LinkedHashMap<String, Object>> columns) {
        this.columns = columns;
    }
    
    public LinkedHashMap<String, Object> getPrimary() {
        return this.primary;
    }
    
    public void setPrimary(final LinkedHashMap<String, Object> primary) {
        this.primary = primary;
    }
    
    private /* synthetic */ void lambda$map$2(final String c, final Object v) {
        if (this.getColumns().containsKey(c)) {
            final String field = this.getColumns().get(c).get("field");
            this.setFieldValue(field, v);
        }
    }
    
    private /* synthetic */ void lambda$unLoad$1(final String columnName, final LinkedHashMap column) {
        if (columnName != null && column != null && !column.isEmpty()) {
            final Object value = "integer".equalsIgnoreCase(String.valueOf(column.get("type"))) ? new Integer(0) : null;
            this.setFieldValue(String.valueOf(column.get("field")), value);
        }
    }
    
    private /* synthetic */ void lambda$load$0(final String c, final Object v) {
        if (this.getColumns().containsKey(c)) {
            final String field = this.getColumns().get(c).get("field");
            this.setFieldValue(field, v);
        }
    }
}
