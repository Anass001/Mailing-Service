package tech.bluemail.platform.models.admin;

import tech.bluemail.platform.orm.*;
import java.io.*;
import tech.bluemail.platform.meta.annotations.*;
import tech.bluemail.platform.exceptions.*;

public class Negative extends ActiveRecord implements Serializable
{
    @Column(name = "id", primary = true, autoincrement = true, type = "integer", nullable = false)
    public int id;
    @Column(name = "user_id", type = "integer", nullable = false)
    public int userId;
    @Column(name = "name", type = "text", nullable = false, length = 100)
    public String name;
    @Column(name = "value", type = "text", nullable = true, length = 200)
    public String value;
    
    public Negative() throws DatabaseException {
        super();
        this.setDatabase("master");
        this.setSchema("admin");
        this.setTable("negative");
    }
    
    public Negative(final Object primaryValue) throws DatabaseException {
        super(primaryValue);
        this.setDatabase("master");
        this.setSchema("admin");
        this.setTable("negative");
        this.load();
    }
}
