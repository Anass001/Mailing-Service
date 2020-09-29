package tech.bluemail.platform.models.lists;

import tech.bluemail.platform.orm.*;
import java.io.*;
import tech.bluemail.platform.meta.annotations.*;
import tech.bluemail.platform.exceptions.*;

public class Clean extends ActiveRecord implements Serializable
{
    @Column(name = "id", primary = true, autoincrement = true, type = "integer", nullable = false)
    public int id;
    @Column(name = "email", type = "text", nullable = false, length = 100)
    public String email;
    @Column(name = "fname", type = "text", nullable = true, length = 100)
    public String fname;
    @Column(name = "lname", type = "text", nullable = true, length = 100)
    public String lname;
    @Column(name = "offers_excluded", type = "text", nullable = true)
    public String offersExcluded;
    
    public Clean() throws DatabaseException {
        super();
        this.setDatabase("lists");
        this.setSchema("");
        this.setTable("");
    }
    
    public Clean(final Object primaryValue) throws DatabaseException {
        super(primaryValue);
        this.setDatabase("lists");
        this.setSchema("");
        this.setTable("");
        this.load();
    }
}
