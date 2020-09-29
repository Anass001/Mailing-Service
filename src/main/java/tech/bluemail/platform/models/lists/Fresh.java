package tech.bluemail.platform.models.lists;

import tech.bluemail.platform.orm.*;
import java.io.*;
import tech.bluemail.platform.meta.annotations.*;
import tech.bluemail.platform.exceptions.*;

public class Fresh extends ActiveRecord implements Serializable
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
    public int listId;
    
    public Fresh() throws DatabaseException {
        super();
        this.listId = 0;
        this.setDatabase("lists");
        this.setSchema("");
        this.setTable("");
    }
    
    public Fresh(final Object primaryValue) throws DatabaseException {
        super(primaryValue);
        this.listId = 0;
        this.setDatabase("lists");
        this.setSchema("");
        this.setTable("");
        this.load();
    }
}
