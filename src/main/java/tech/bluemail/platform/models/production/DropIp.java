package tech.bluemail.platform.models.production;

import tech.bluemail.platform.orm.*;
import java.io.*;
import tech.bluemail.platform.meta.annotations.*;
import java.sql.*;
import tech.bluemail.platform.exceptions.*;

public class DropIp extends ActiveRecord implements Serializable
{
    @Column(name = "id", primary = true, autoincrement = true, type = "integer", nullable = false)
    public int id;
    @Column(name = "server_id", type = "integer", nullable = false)
    public int serverId;
    @Column(name = "isp_id", type = "integer", nullable = true)
    public int ispId;
    @Column(name = "drop_id", type = "integer", nullable = false)
    public int dropId;
    @Column(name = "ip_id", type = "integer", nullable = false)
    public int ipId;
    @Column(name = "drop_date", type = "timestamp", nullable = false)
    public Timestamp dropDate;
    @Column(name = "total_sent", type = "integer", nullable = true)
    public int totalSent;
    @Column(name = "delivered", type = "integer", nullable = true)
    public int delivered;
    @Column(name = "bounced", type = "integer", nullable = true)
    public int bounced;
    
    public DropIp() throws DatabaseException {
        super();
        this.setDatabase("master");
        this.setSchema("production");
        this.setTable("drop_ips");
    }
    
    public DropIp(final Object primaryValue) throws DatabaseException {
        super(primaryValue);
        this.setDatabase("master");
        this.setSchema("production");
        this.setTable("drop_ips");
        this.load();
    }
}
