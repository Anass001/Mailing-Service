package tech.bluemail.platform.models.admin;

import tech.bluemail.platform.orm.*;
import java.io.*;
import tech.bluemail.platform.meta.annotations.*;
import java.sql.*;
import tech.bluemail.platform.exceptions.*;

public class OfferLink extends ActiveRecord implements Serializable
{
    @Column(name = "id", primary = true, autoincrement = true, type = "integer", nullable = false)
    public int id;
    @Column(name = "status_id", type = "integer", nullable = false)
    public int statusId;
    @Column(name = "creative_id", type = "integer", nullable = false)
    public int creativeId;
    @Column(name = "value", type = "text", nullable = false)
    public String value;
    @Column(name = "type", type = "text", nullable = false, length = 20)
    public String type;
    @Column(name = "created_by", type = "integer", nullable = false)
    public int createdBy;
    @Column(name = "last_updated_by", type = "integer", nullable = true)
    public int lastUpdatedBy;
    @Column(name = "created_at", type = "date", nullable = false)
    public Date createdAt;
    @Column(name = "last_updated_at", type = "date", nullable = true)
    public Date lastUpdatedAt;
    
    public OfferLink() throws DatabaseException {
        super();
        this.setDatabase("master");
        this.setSchema("admin");
        this.setTable("offer_links");
    }
    
    public OfferLink(final Object primaryValue) throws DatabaseException {
        super(primaryValue);
        this.setDatabase("master");
        this.setSchema("admin");
        this.setTable("offer_links");
        this.load();
    }
}
