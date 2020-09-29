/*
 * Decompiled with CFR <Could not determine version>.
 */
package tech.bluemail.platform.models.production;

import java.io.Serializable;
import java.sql.Timestamp;
import tech.bluemail.platform.exceptions.DatabaseException;
import tech.bluemail.platform.meta.annotations.Column;
import tech.bluemail.platform.orm.ActiveRecord;

public class DropIp
extends ActiveRecord
implements Serializable {
    @Column(name="id", primary=true, autoincrement=true, type="integer", nullable=false)
    public int id;
    @Column(name="server_id", type="integer", nullable=false)
    public int serverId;
    @Column(name="isp_id", type="integer", nullable=true)
    public int ispId;
    @Column(name="drop_id", type="integer", nullable=false)
    public int dropId;
    @Column(name="ip_id", type="integer", nullable=false)
    public int ipId;
    @Column(name="drop_date", type="timestamp", nullable=false)
    public Timestamp dropDate;
    @Column(name="total_sent", type="integer", nullable=true)
    public int totalSent;
    @Column(name="delivered", type="integer", nullable=true)
    public int delivered;
    @Column(name="bounced", type="integer", nullable=true)
    public int bounced;

    public DropIp() throws DatabaseException {
        this.setDatabase("master");
        this.setSchema("production");
        this.setTable("drop_ips");
    }

    public DropIp(Object primaryValue) throws DatabaseException {
        super(primaryValue);
        this.setDatabase("master");
        this.setSchema("production");
        this.setTable("drop_ips");
        this.load();
    }
}

