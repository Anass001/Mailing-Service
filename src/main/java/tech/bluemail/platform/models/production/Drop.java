package tech.bluemail.platform.models.production;

import tech.bluemail.platform.orm.*;
import java.io.*;
import tech.bluemail.platform.meta.annotations.*;
import java.sql.*;
import tech.bluemail.platform.exceptions.*;

public class Drop extends ActiveRecord implements Serializable
{
    @Column(name = "id", primary = true, autoincrement = true, type = "integer", nullable = false)
    public int id;
    @Column(name = "user_id", type = "integer", nullable = false)
    public int userId;
    @Column(name = "server_id", type = "integer", nullable = false)
    public int serverId;
    @Column(name = "isp_id", type = "integer", nullable = true)
    public int ispId;
    @Column(name = "status", type = "text", nullable = false, length = 20)
    public String status;
    @Column(name = "start_time", type = "timestamp", nullable = false)
    public Timestamp startTime;
    @Column(name = "finish_time", type = "timestamp", nullable = true)
    public Timestamp finishTime;
    @Column(name = "total_emails", type = "integer", nullable = false)
    public int totalEmails;
    @Column(name = "sent_progress", type = "integer", nullable = true)
    public int sentProgress;
    @Column(name = "offer_id", type = "integer", nullable = false)
    public int offerId;
    @Column(name = "offer_from_name_id", type = "integer", nullable = false)
    public int offerFromNameId;
    @Column(name = "offer_subject_id", type = "integer", nullable = false)
    public int offerSubjectId;
    @Column(name = "recipients_emails", type = "text", nullable = true)
    public String recipientsEmails;
    @Column(name = "pids", type = "text", nullable = true)
    public String pids;
    @Column(name = "header", type = "text", nullable = true)
    public String header;
    @Column(name = "creative_id", type = "integer", nullable = false)
    public int creativeId;
    @Column(name = "lists", type = "text", nullable = true)
    public String lists;
    @Column(name = "post_data", type = "text", nullable = false)
    public String postData;
    
    public Drop() throws DatabaseException {
        super();
        this.setDatabase("master");
        this.setSchema("production");
        this.setTable("drops");
    }
    
    public Drop(final Object primaryValue) throws DatabaseException {
        super(primaryValue);
        this.setDatabase("master");
        this.setSchema("production");
        this.setTable("drops");
        this.load();
    }
}
