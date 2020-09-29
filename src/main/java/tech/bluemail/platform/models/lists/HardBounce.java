package tech.bluemail.platform.models.lists;

import tech.bluemail.platform.orm.*;
import java.io.*;
import tech.bluemail.platform.meta.annotations.*;
import tech.bluemail.platform.exceptions.*;

public class HardBounce extends ActiveRecord implements Serializable
{
    @Column(name = "id", primary = true, autoincrement = true, type = "integer", nullable = false)
    public int id;
    @Column(name = "email", type = "text", nullable = false, length = 100)
    public String email;
    
    public HardBounce() throws DatabaseException {
        super();
        this.setDatabase("lists");
        this.setSchema("");
        this.setTable("hard_bounce");
    }
    
    public HardBounce(final Object primaryValue) throws DatabaseException {
        super(primaryValue);
        this.setDatabase("lists");
        this.setSchema("");
        this.setTable("hard_bounce");
        this.load();
    }
}
