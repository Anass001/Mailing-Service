package tech.bluemail.platform.models.admin;

import tech.bluemail.platform.orm.*;
import java.io.*;
import tech.bluemail.platform.meta.annotations.*;
import tech.bluemail.platform.exceptions.*;

public class Header extends ActiveRecord implements Serializable
{
    @Column(name = "id", primary = true, autoincrement = true, type = "integer", nullable = false)
    public int id;
    @Column(name = "user_id", type = "integer", nullable = false)
    public int userId;
    @Column(name = "name", type = "text", nullable = false)
    public String name;
    @Column(name = "type", type = "text", nullable = true, length = 100)
    public String type;
    @Column(name = "value", type = "text", nullable = true)
    public String value;
    
    public Header() throws DatabaseException {
        super();
        this.setDatabase("master");
        this.setSchema("admin");
        this.setTable("headers");
    }
    
    public Header(final Object primaryValue) throws DatabaseException {
        super(primaryValue);
        this.setDatabase("master");
        this.setSchema("admin");
        this.setTable("headers");
        this.load();
    }
}
