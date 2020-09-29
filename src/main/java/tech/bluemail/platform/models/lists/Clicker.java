/*
 * Decompiled with CFR <Could not determine version>.
 */
package tech.bluemail.platform.models.lists;

import java.io.Serializable;
import java.sql.Timestamp;
import tech.bluemail.platform.exceptions.DatabaseException;
import tech.bluemail.platform.meta.annotations.Column;
import tech.bluemail.platform.orm.ActiveRecord;

public class Clicker
extends ActiveRecord
implements Serializable {
    @Column(name="id", primary=true, autoincrement=true, type="integer", nullable=false)
    public int id;
    @Column(name="email", type="text", nullable=false, length=100)
    public String email;
    @Column(name="fname", type="text", nullable=true, length=100)
    public String fname;
    @Column(name="lname", type="text", nullable=true, length=100)
    public String lname;
    @Column(name="action_date", type="timestamp", nullable=true)
    public Timestamp actionDate;
    @Column(name="offers_excluded", type="text", nullable=true)
    public String offersExcluded;
    @Column(name="verticals", type="text", nullable=true)
    public String verticals;
    @Column(name="agent", type="text", nullable=true)
    public String agent;
    @Column(name="ip", type="text", nullable=true, length=100)
    public String ip;
    @Column(name="country", type="text", nullable=true)
    public String country;
    @Column(name="region", type="text", nullable=true)
    public String region;
    @Column(name="city", type="text", nullable=true)
    public String city;
    @Column(name="language", type="text", nullable=true, length=100)
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

    public Clicker() throws DatabaseException {
        this.setDatabase("lists");
        this.setSchema("");
        this.setTable("");
    }

    public Clicker(Object primaryValue) throws DatabaseException {
        super(primaryValue);
        this.setDatabase("lists");
        this.setSchema("");
        this.setTable("");
        this.load();
    }
}

