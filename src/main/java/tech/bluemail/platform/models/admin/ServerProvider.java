/*
 * Decompiled with CFR <Could not determine version>.
 */
package tech.bluemail.platform.models.admin;

import java.io.Serializable;
import java.sql.Date;
import tech.bluemail.platform.exceptions.DatabaseException;
import tech.bluemail.platform.meta.annotations.Column;
import tech.bluemail.platform.orm.ActiveRecord;

public class ServerProvider
extends ActiveRecord
implements Serializable {
    @Column(name="id", primary=true, autoincrement=true, type="integer", nullable=false)
    public int id;
    @Column(name="status_id", type="integer", nullable=false)
    public int statusId;
    @Column(name="name", type="text", nullable=false, length=100)
    public String name;
    @Column(name="website", type="text", nullable=false, length=100)
    public String website;
    @Column(name="username", type="text", nullable=false, length=100)
    public String username;
    @Column(name="password", type="text", nullable=false, length=100)
    public String password;
    @Column(name="created_by", type="integer", nullable=false)
    public int createdBy;
    @Column(name="last_updated_by", type="integer", nullable=true)
    public int lastUpdatedBy;
    @Column(name="created_at", type="date", nullable=false)
    public Date createdAt;
    @Column(name="last_updated_at", type="date", nullable=true)
    public Date lastUpdatedAt;

    public ServerProvider() throws DatabaseException {
        this.setDatabase("master");
        this.setSchema("admin");
        this.setTable("server_providers");
    }

    public ServerProvider(Object primaryValue) throws DatabaseException {
        super(primaryValue);
        this.setDatabase("master");
        this.setSchema("admin");
        this.setTable("server_providers");
        this.load();
    }
}

