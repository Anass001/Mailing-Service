/*
 * Decompiled with CFR <Could not determine version>.
 */
package tech.bluemail.platform.models.admin;

import java.io.Serializable;
import java.sql.Date;
import tech.bluemail.platform.exceptions.DatabaseException;
import tech.bluemail.platform.meta.annotations.Column;
import tech.bluemail.platform.orm.ActiveRecord;

public class OfferCreative
extends ActiveRecord
implements Serializable {
    @Column(name="id", primary=true, autoincrement=true, type="integer", nullable=false)
    public int id;
    @Column(name="status_id", type="integer", nullable=false)
    public int statusId;
    @Column(name="offer_id", type="integer", nullable=false)
    public int offerId;
    @Column(name="value", type="text", nullable=false)
    public String value;
    @Column(name="created_by", type="integer", nullable=false)
    public int createdBy;
    @Column(name="last_updated_by", type="integer", nullable=true)
    public int lastUpdatedBy;
    @Column(name="created_at", type="date", nullable=false)
    public Date createdAt;
    @Column(name="last_updated_at", type="date", nullable=true)
    public Date lastUpdatedAt;

    public OfferCreative() throws DatabaseException {
        this.setDatabase("master");
        this.setSchema("admin");
        this.setTable("offer_creatives");
    }

    public OfferCreative(Object primaryValue) throws DatabaseException {
        super(primaryValue);
        this.setDatabase("master");
        this.setSchema("admin");
        this.setTable("offer_creatives");
        this.load();
    }
}

