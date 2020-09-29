package tech.bluemail.platform.models.stats;

import tech.bluemail.platform.orm.*;
import java.io.*;
import tech.bluemail.platform.meta.annotations.*;
import java.sql.*;
import tech.bluemail.platform.exceptions.*;

public class Open extends ActiveRecord implements Serializable
{
    @Column(name = "id", primary = true, autoincrement = true, type = "integer", nullable = false)
    public int id;
    @Column(name = "drop_id", type = "integer", nullable = false)
    public int dropId;
    @Column(name = "email", type = "text", nullable = false, length = 100)
    public String email;
    @Column(name = "action_date", type = "timestamp", nullable = false)
    public Timestamp actionDate;
    @Column(name = "list", type = "text", nullable = false, length = 100)
    public String list;
    @Column(name = "ip", type = "text", nullable = true, length = 20)
    public String ip;
    @Column(name = "country", type = "text", nullable = true)
    public String country;
    @Column(name = "region", type = "text", nullable = true)
    public String region;
    @Column(name = "city", type = "text", nullable = true)
    public String city;
    @Column(name = "language", type = "text", nullable = true, length = 2)
    public String language;
    @Column(name = "device_type", type = "text", nullable = true)
    public String deviceType;
    @Column(name = "device_name", type = "text", nullable = true, length = 100)
    public String deviceName;
    @Column(name = "os", type = "text", nullable = true)
    public String os;
    @Column(name = "browser_name", type = "text", nullable = true)
    public String browserName;
    @Column(name = "browser_version", type = "text", nullable = true, length = 100)
    public String browserVersion;
    @Column(name = "action_occurences", type = "integer", nullable = true)
    public int actionOccurences;
    
    public Open() throws DatabaseException {
        super();
        this.setDatabase("master");
        this.setSchema("stats");
        this.setTable("opens");
    }
    
    public Open(final Object primaryValue) throws DatabaseException {
        super(primaryValue);
        this.setDatabase("master");
        this.setSchema("stats");
        this.setTable("opens");
        this.load();
    }
}
