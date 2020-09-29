/*
 * Decompiled with CFR <Could not determine version>.
 * 
 * Could not load the following classes:
 *  org.apache.commons.lang.ArrayUtils
 */
package tech.bluemail.platform.orm;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import org.apache.commons.lang.ArrayUtils;
import tech.bluemail.platform.exceptions.DatabaseException;
import tech.bluemail.platform.logging.Logger;
import tech.bluemail.platform.orm.Database;

public class Query {
    private String database = Database.getDefault().getKey();
    private String from = "";
    private String[] fields = new String[0];
    private int offset = 0;
    private int limit = 0;
    private String[] order = new String[0];
    private String direction = "ASC";
    private String[] group = new String[0];
    private String[] join = new String[0];
    private String[] where = new String[0];
    private Object[] whereParameters = new Object[0];
    private Object[] parameters = new Object[0];
    private String query = "";
    public static final int SELECT = 0;
    public static final int INSERT = 1;
    public static final int UPDATE = 2;
    public static final int DELETE = 3;
    public static final int ONLY_BUILD_QUERY = 0;
    public static final int EXECUTE_QUERY = 1;
    public static final String ASC = "ASC";
    public static final String DESC = "DESC";
    public static final String LEFT_JOIN = "LEFT JOIN";
    public static final String RIGHT_JOIN = "RIGHT JOIN";
    public static final String INNER_JOIN = "INNER JOIN";
    public static final String FULL_OUTER_JOIN = "FULL OUTER JOIN";

    public List<LinkedHashMap<String, Object>> all() {
        List<LinkedHashMap<String, Object>> results = new ArrayList<LinkedHashMap<String, Object>>();
        try {
            if (this.whereParameters != null && this.whereParameters.length > 0) {
                this.parameters = ArrayUtils.addAll((Object[])this.parameters, (Object[])this.whereParameters);
            }
            if ("".equalsIgnoreCase(this.query)) {
                this.build(0);
            }
            results = Database.get(this.database).executeQuery(this.query, this.parameters, 1);
            this.reset();
            return results;
        }
        catch (Exception e) {
            Logger.error(new DatabaseException(e), Query.class);
        }
        return results;
    }

    public LinkedHashMap<String, Object> first() {
        LinkedHashMap<String, Object> row = new LinkedHashMap<String, Object>();
        try {
            List<LinkedHashMap<String, Object>> results;
            if (this.whereParameters != null && this.whereParameters.length > 0) {
                this.parameters = ArrayUtils.addAll((Object[])this.parameters, (Object[])this.whereParameters);
            }
            if ("".equalsIgnoreCase(this.query)) {
                this.build(0);
            }
            row = !(results = Database.get(this.database).executeQuery(this.query, this.parameters, 0)).isEmpty() ? results.get(0) : row;
            this.reset();
            return row;
        }
        catch (Exception e) {
            Logger.error(new DatabaseException(e), Query.class);
        }
        return row;
    }

    public int count() {
        int count = 0;
        try {
            if (this.whereParameters != null && this.whereParameters.length > 0) {
                this.parameters = ArrayUtils.addAll((Object[])this.parameters, (Object[])this.whereParameters);
            }
            if ("".equalsIgnoreCase(this.query)) {
                this.build(0);
            }
            count = Database.get(this.database).executeQuery(this.query, this.parameters, 1).size();
            this.reset();
            return count;
        }
        catch (Exception e) {
            Logger.error(new DatabaseException(e), Query.class);
        }
        return count;
    }

    public int insert(Object[] parameters) {
        int result = 0;
        try {
            this.parameters = parameters;
            if ("".equalsIgnoreCase(this.query)) {
                this.build(1);
            }
            result = Database.get(this.database).executeUpdate(this.query, this.parameters, 1);
            this.reset();
            return result;
        }
        catch (Exception e) {
            Logger.error(new DatabaseException(e), Query.class);
        }
        return result;
    }

    public int update(Object[] parameters) {
        int result = 0;
        try {
            this.parameters = parameters;
            if (this.whereParameters != null && this.whereParameters.length > 0) {
                this.parameters = ArrayUtils.addAll((Object[])this.parameters, (Object[])this.whereParameters);
            }
            if ("".equalsIgnoreCase(this.query)) {
                this.build(2);
            }
            result = Database.get(this.database).executeUpdate(this.query, this.parameters, 0);
            this.reset();
            return result;
        }
        catch (Exception e) {
            Logger.error(new DatabaseException(e), Query.class);
        }
        return result;
    }

    public int delete() {
        int result = 0;
        try {
            if (this.whereParameters != null && this.whereParameters.length > 0) {
                this.parameters = this.whereParameters;
            }
            if ("".equalsIgnoreCase(this.query)) {
                this.build(3);
            }
            result = Database.get(this.database).executeUpdate(this.query, this.parameters, 0);
            this.reset();
            return result;
        }
        catch (Exception e) {
            Logger.error(new DatabaseException(e), Query.class);
        }
        return result;
    }

    public Query from(String from, String[] fields) {
        this.from = from;
        if (fields == null || 0 == fields.length) {
            fields = new String[]{"*"};
        }
        this.fields = (String[])ArrayUtils.addAll((Object[])this.fields, (Object[])fields);
        return this;
    }

    public Query where(String condition, Object[] parameters, String concat) {
        concat = "and".equalsIgnoreCase(concat) || "or".equalsIgnoreCase(concat) || "nand".equalsIgnoreCase(concat) || "nor".equalsIgnoreCase(concat) ? concat + " " : "";
        this.where = (String[])ArrayUtils.add((Object[])this.where, (Object)(concat + condition));
        this.whereParameters = ArrayUtils.addAll((Object[])this.whereParameters, (Object[])parameters);
        return this;
    }

    public Query order(String[] columns, String direction) {
        this.order = (String[])ArrayUtils.addAll((Object[])this.order, (Object[])columns);
        this.direction = direction;
        return this;
    }

    public Query limit(int offset, int limit) {
        this.offset = offset;
        this.limit = limit;
        return this;
    }

    public Query group(String[] columns) {
        this.group = (String[])ArrayUtils.addAll((Object[])this.group, (Object[])columns);
        return this;
    }

    public Query join(String join, String on, String[] fields, String type) {
        String string = type = type == null || "".equalsIgnoreCase(type) ? LEFT_JOIN : type;
        if (fields == null) {
            fields = new String[]{};
        }
        if (0 == fields.length) {
            fields[0] = "*";
        }
        this.fields = (String[])ArrayUtils.addAll((Object[])this.fields, (Object[])fields);
        this.join = (String[])ArrayUtils.add((Object[])this.join, (Object)(type + " " + join + " ON " + on));
        return this;
    }

    public Query build(int type) throws DatabaseException {
        switch (type) {
            case 0: {
                int i;
                String template = "SELECT %s FROM %s %s %s %s %s %s";
                String fields = "";
                String wheres = "";
                String orders = "";
                String limit = "";
                String joins = "";
                String groups = "";
                for (i = 0; i < this.fields.length; ++i) {
                    fields = fields + this.fields[i];
                    if (i == this.fields.length - 1) continue;
                    fields = fields + ",";
                }
                if (this.join != null && this.join.length > 0) {
                    for (i = 0; i < this.join.length; ++i) {
                        joins = joins + this.join[i];
                        if (i == this.join.length - 1) continue;
                        joins = joins + this.join[i] + " ";
                    }
                }
                if (this.where != null && this.where.length > 0) {
                    wheres = "WHERE ";
                    for (i = 0; i < this.where.length; ++i) {
                        wheres = wheres + this.where[i];
                        if (i == this.where.length - 1) continue;
                        wheres = wheres + " ";
                    }
                }
                if (this.group != null && this.group.length > 0) {
                    groups = "GROUP BY ";
                    for (i = 0; i < this.group.length; ++i) {
                        groups = groups + this.group[i];
                        if (i == this.group.length - 1) continue;
                        groups = groups + ",";
                    }
                }
                if (this.order != null && this.order.length > 0) {
                    orders = "ORDER BY ";
                    for (i = 0; i < this.order.length; ++i) {
                        orders = orders + this.order[i];
                        if (i == this.order.length - 1) continue;
                        orders = orders + ",";
                    }
                    orders = orders + " " + this.direction;
                }
                if (this.limit > 0) {
                    limit = this.offset > 0 ? ("mysql".equalsIgnoreCase(Database.get(this.database).getDriver()) ? "LIMIT " + this.offset + "," + this.limit : "OFFSET " + this.offset + " LIMIT " + this.limit) : "LIMIT " + this.limit;
                }
                this.query = String.format(template, fields, this.from, joins, wheres, groups, orders, limit);
                return this;
            }
            case 1: {
                int i;
                String template = "INSERT INTO %s (%s) VALUES (%s)";
                String fields = "";
                String values = "";
                int[] removeIndexes = new int[]{};
                for (i = 0; i < this.fields.length; ++i) {
                    fields = fields + this.fields[i];
                    if (i == this.fields.length - 1) continue;
                    fields = fields + ",";
                }
                for (i = 0; i < this.fields.length; ++i) {
                    if (this.parameters[i] == null) {
                        values = values + "NULL";
                        removeIndexes = ArrayUtils.add((int[])removeIndexes, (int)i);
                    } else {
                        values = values + "?";
                    }
                    if (i == this.fields.length - 1) continue;
                    values = values + ",";
                }
                int[] i2 = removeIndexes;
                int joins = i2.length;
                int groups = 0;
                do {
                    if (groups >= joins) {
                        this.query = String.format(template, this.from, fields, values);
                        return this;
                    }
                    int removeIndex = i2[groups];
                    this.parameters = ArrayUtils.remove((Object[])this.parameters, (int)removeIndex);
                    ++groups;
                } while (true);
            }
            case 2: {
                int i;
                String template = "UPDATE %s SET %s %s";
                String fields = "";
                String wheres = "";
                int[] removeIndexes = new int[]{};
                for (i = 0; i < this.fields.length; ++i) {
                    if (this.parameters[i] == null) {
                        fields = fields + this.fields[i] + " = NULL";
                        removeIndexes = ArrayUtils.add((int[])removeIndexes, (int)i);
                    } else {
                        fields = fields + this.fields[i] + " = ?";
                    }
                    if (i == this.fields.length - 1) continue;
                    fields = fields + ",";
                }
                if (this.where != null && this.where.length > 0) {
                    wheres = "WHERE ";
                    for (i = 0; i < this.where.length; ++i) {
                        wheres = wheres + this.where[i];
                        if (i == this.where.length - 1) continue;
                        wheres = wheres + " ";
                    }
                }
                int[] i3 = removeIndexes;
                int joins = i3.length;
                int groups = 0;
                do {
                    if (groups >= joins) {
                        this.query = String.format(template, this.from, fields, wheres);
                        return this;
                    }
                    int removeIndex = i3[groups];
                    this.parameters = ArrayUtils.remove((Object[])this.parameters, (int)removeIndex);
                    ++groups;
                } while (true);
            }
            case 3: {
                String template = "DELETE FROM %s %s";
                String wheres = "";
                if (this.where != null && this.where.length > 0) {
                    wheres = "WHERE ";
                    for (int i = 0; i < this.where.length; ++i) {
                        wheres = wheres + this.where[i];
                        if (i == this.where.length - 1) continue;
                        wheres = wheres + " ";
                    }
                }
                this.query = String.format(template, this.from, wheres);
                return this;
            }
        }
        throw new DatabaseException("Unsupported query type !");
    }

    private void reset() {
        this.from = "";
        this.fields = new String[0];
        this.offset = 0;
        this.limit = 0;
        this.order = new String[0];
        this.direction = ASC;
        this.group = new String[0];
        this.join = new String[0];
        this.where = new String[0];
        this.parameters = new Object[0];
        this.query = "";
    }

    public Query(String database) {
        this.database = database;
    }

    public String getDatabase() {
        return this.database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public Object[] getWhereParameters() {
        return this.whereParameters;
    }

    public void setWhereParameters(Object[] whereParameters) {
        this.whereParameters = whereParameters;
    }

    public String getFrom() {
        return this.from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String[] getFields() {
        return this.fields;
    }

    public void setFields(String[] fields) {
        this.fields = fields;
    }

    public int getOffset() {
        return this.offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public int getLimit() {
        return this.limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public String[] getOrder() {
        return this.order;
    }

    public void setOrder(String[] order) {
        this.order = order;
    }

    public String getDirection() {
        return this.direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public String[] getGroup() {
        return this.group;
    }

    public void setGroup(String[] group) {
        this.group = group;
    }

    public String[] getJoin() {
        return this.join;
    }

    public void setJoin(String[] join) {
        this.join = join;
    }

    public String[] getWhere() {
        return this.where;
    }

    public void setWhere(String[] where) {
        this.where = where;
    }

    public Object[] getParameters() {
        return this.parameters;
    }

    public void setParameters(Object[] parameters) {
        this.parameters = parameters;
    }

    public String getQuery() {
        return this.query;
    }

    public void setQuery(String query) {
        this.query = query;
    }
}

