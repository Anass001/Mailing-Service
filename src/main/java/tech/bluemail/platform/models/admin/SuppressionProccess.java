package tech.bluemail.platform.models.admin;

import tech.bluemail.platform.orm.*;
import java.io.*;
import tech.bluemail.platform.meta.annotations.*;
import java.sql.*;
import tech.bluemail.platform.exceptions.*;

public class SuppressionProccess extends ActiveRecord implements Serializable
{
    @Column(name = "id", primary = true, autoincrement = true, type = "integer", nullable = false)
    public int id;
    @Column(name = "user_id", type = "integer", nullable = false)
    public int userId;
    @Column(name = "sponsor_id", type = "integer", nullable = false)
    public int sponsorId;
    @Column(name = "offer_id", type = "integer", nullable = false)
    public int offerId;
    @Column(name = "status", type = "text", nullable = false, length = 20)
    public String status;
    @Column(name = "progress", type = "text", nullable = false)
    public String progress;
    @Column(name = "emails_found", type = "integer", nullable = false)
    public int emailsFound;
    @Column(name = "start_time", type = "timestamp", nullable = false)
    public Timestamp startTime;
    @Column(name = "finish_time", type = "timestamp", nullable = true)
    public Timestamp finishTime;
    
    public SuppressionProccess() throws DatabaseException {
        super();
        this.setDatabase("master");
        this.setSchema("admin");
        this.setTable("suppression_proccesses");
    }
    
    public SuppressionProccess(final Object primaryValue) throws DatabaseException {
        super(primaryValue);
        this.setDatabase("master");
        this.setSchema("admin");
        this.setTable("suppression_proccesses");
        this.load();
    }
}
