/*
 * Decompiled with CFR <Could not determine version>.
 */
package tech.bluemail.platform.models.stats;

import java.io.Serializable;
import java.sql.Timestamp;
import tech.bluemail.platform.exceptions.DatabaseException;
import tech.bluemail.platform.meta.annotations.Column;
import tech.bluemail.platform.orm.ActiveRecord;

public class Click
extends ActiveRecord
implements Serializable {
    @Column(name="id", primary=true, autoincrement=true, type="integer", nullable=false)
    public int id;
    @Column(name="drop_id", type="integer", nullable=false)
    public int dropId;
    @Column(name="email", type="text", nullable=false, length=100)
    public String email;
    @Column(name="action_date", type="timestamp", nullable=false)
    public Timestamp actionDate;
    @Column(name="list", type="text", nullable=false, length=100)
    public String list;
    @Column(name="ip", type="text", nullable=true, length=20)
    public String ip;
    @Column(name="country", type="text", nullable=true)
    public String country;
    @Column(name="region", type="text", nullable=true)
    public String region;
    @Column(name="city", type="text", nullable=true)
    public String city;
    @Column(name="language", type="text", nullable=true, length=2)
    public String language;
    @Column(name="device_type", type="text", nullable=true)
    public String deviceType;
    @Column(name="device_name", type="text", nullable=true, length=100)
    public String deviceName;
    @Column(name="os", type="text", nullable=true)
    public String os;
    @Column(name="browser_name", type="text", nullable=true)
    public String browserName;
    @Column(name="browser_version", type="text", nullable=true, length=100)
    public String browserVersion;
    @Column(name="action_occurences", type="integer", nullable=true)
    public int actionOccurences;

    public Click() throws DatabaseException {
        this.setDatabase("master");
        this.setSchema("stats");
        this.setTable("clicks");
    }

    public Click(Object primaryValue) throws DatabaseException {
        super(primaryValue);
        this.setDatabase("master");
        this.setSchema("stats");
        this.setTable("clicks");
        this.load();
    }
}

