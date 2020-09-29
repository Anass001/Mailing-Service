package tech.bluemail.platform.models.admin;

import tech.bluemail.platform.orm.*;
import java.io.*;
import tech.bluemail.platform.meta.annotations.*;
import java.sql.*;
import tech.bluemail.platform.exceptions.*;

public class Offer extends ActiveRecord implements Serializable
{
    @Column(name = "id", primary = true, autoincrement = true, type = "integer", nullable = false)
    public int id;
    @Column(name = "status_id", type = "integer", nullable = false)
    public int statusId;
    @Column(name = "sponsor_id", type = "integer", nullable = false)
    public int sponsorId;
    @Column(name = "production_id", type = "integer", nullable = false)
    public int productionId;
    @Column(name = "drop_id", type = "integer", nullable = false)
    public int dropId;
    @Column(name = "vertical_id", type = "integer", nullable = false)
    public int verticalId;
    @Column(name = "name", type = "text", nullable = false)
    public String name;
    @Column(name = "flag", type = "text", nullable = false)
    public String flag;
    @Column(name = "description", type = "text", nullable = true)
    public String description;
    @Column(name = "rate", type = "text", nullable = true, length = 20)
    public String rate;
    @Column(name = "launch_date", type = "date", nullable = false)
    public Date launchDate;
    @Column(name = "expiring_date", type = "date", nullable = false)
    public Date expiringDate;
    @Column(name = "rules", type = "text", nullable = true)
    public String rules;
    @Column(name = "epc", type = "text", nullable = true, length = 20)
    public String epc;
    @Column(name = "suppression_list", type = "text", nullable = true)
    public String suppressionList;
    @Column(name = "created_by", type = "integer", nullable = false)
    public int createdBy;
    @Column(name = "last_updated_by", type = "integer", nullable = true)
    public int lastUpdatedBy;
    @Column(name = "created_at", type = "date", nullable = false)
    public Date createdAt;
    @Column(name = "last_updated_at", type = "date", nullable = true)
    public Date lastUpdatedAt;
    @Column(name = "authorized_users", type = "text", nullable = true)
    public String authorizedUsers;
    @Column(name = "key", type = "text", nullable = true, length = 10)
    public String key;
    
    public Offer() throws DatabaseException {
        super();
        this.setDatabase("master");
        this.setSchema("admin");
        this.setTable("offers");
    }
    
    public Offer(final Object primaryValue) throws DatabaseException {
        super(primaryValue);
        this.setDatabase("master");
        this.setSchema("admin");
        this.setTable("offers");
        this.load();
    }
}
