/*
 * Decompiled with CFR <Could not determine version>.
 */
package tech.bluemail.platform.models.lists;

import java.io.Serializable;
import tech.bluemail.platform.exceptions.DatabaseException;
import tech.bluemail.platform.meta.annotations.Column;
import tech.bluemail.platform.orm.ActiveRecord;

public class Fresh
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
    @Column(name="offers_excluded", type="text", nullable=true)
    public String offersExcluded;
    public int listId = 0;

    public Fresh() throws DatabaseException {
        this.setDatabase("lists");
        this.setSchema("");
        this.setTable("");
    }

    public Fresh(Object primaryValue) throws DatabaseException {
        super(primaryValue);
        this.setDatabase("lists");
        this.setSchema("");
        this.setTable("");
        this.load();
    }
}

