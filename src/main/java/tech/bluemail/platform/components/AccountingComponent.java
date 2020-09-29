package tech.bluemail.platform.components;

import java.io.*;

public class AccountingComponent implements Serializable
{
    public int dropId;
    public int ipId;
    public int delivered;
    public int bounced;
    
    public AccountingComponent(final int dropId, final int ipId, final int delivered, final int bounced) {
        super();
        this.dropId = dropId;
        this.ipId = ipId;
        this.delivered = delivered;
        this.bounced = bounced;
    }
}
