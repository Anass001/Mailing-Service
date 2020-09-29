/*
 * Decompiled with CFR <Could not determine version>.
 */
package tech.bluemail.platform.models.admin;

import java.io.Serializable;
import tech.bluemail.platform.exceptions.DatabaseException;
import tech.bluemail.platform.meta.annotations.Column;
import tech.bluemail.platform.orm.ActiveRecord;

public class Negative
extends ActiveRecord
implements Serializable {
    @Column(name="id", primary=true, autoincrement=true, type="integer", nullable=false)
    public int id;
    @Column(name="user_id", type="integer", nullable=false)
    public int userId;
    @Column(name="name", type="text", nullable=false, length=100)
    public String name;
    @Column(name="value", type="text", nullable=true, length=200)
    public String value;

    public Negative() throws DatabaseException {
        this.setDatabase("master");
        this.setSchema("admin");
        this.setTable("negative");
    }

    public Negative(Object primaryValue) throws DatabaseException {
        super(primaryValue);
        this.setDatabase("master");
        this.setSchema("admin");
        this.setTable("negative");
        this.load();
    }
}

