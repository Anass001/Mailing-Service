/*
 * Decompiled with CFR <Could not determine version>.
 */
package tech.bluemail.platform.components;

import java.io.Serializable;

public class AccountingComponent
implements Serializable {
    public int dropId;
    public int ipId;
    public int delivered;
    public int bounced;

    public AccountingComponent(int dropId, int ipId, int delivered, int bounced) {
        this.dropId = dropId;
        this.ipId = ipId;
        this.delivered = delivered;
        this.bounced = bounced;
    }
}

