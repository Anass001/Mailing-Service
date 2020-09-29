/*
 * Decompiled with CFR <Could not determine version>.
 */
package tech.bluemail.platform.models.admin;

import java.io.Serializable;
import tech.bluemail.platform.exceptions.DatabaseException;
import tech.bluemail.platform.meta.annotations.Column;
import tech.bluemail.platform.orm.ActiveRecord;

public class Header
extends ActiveRecord
implements Serializable {
    @Column(name="id", primary=true, autoincrement=true, type="integer", nullable=false)
    public int id;
    @Column(name="user_id", type="integer", nullable=false)
    public int userId;
    @Column(name="name", type="text", nullable=false)
    public String name;
    @Column(name="type", type="text", nullable=true, length=100)
    public String type;
    @Column(name="value", type="text", nullable=true)
    public String value;

    public Header() throws DatabaseException {
        this.setDatabase("master");
        this.setSchema("admin");
        this.setTable("headers");
    }

    public Header(Object primaryValue) throws DatabaseException {
        super(primaryValue);
        this.setDatabase("master");
        this.setSchema("admin");
        this.setTable("headers");
        this.load();
    }
}

