/*
 * CODENVY CONFIDENTIAL
 * __________________
 *
 *  [2012] - [2013] Codenvy, S.A.
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */
package com.codenvy.api.auth.sso.server;


import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.security.Principal;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Random alphanumeric sequence that provide access to codenvy services.
 * Can be saved in the cookie in browser or provides some other way (i.e. query, header).
 */
public final class AccessTicket implements Externalizable {
    private final Set<String> registeredClients;
    private       Principal   principal;
    /** Time of ticket creation in milliseconds. */
    private       long        creationTime;
    /** Value of access cookie associated with this access key. */
    private       String      accessToken;

    //used for externalization.
    public AccessTicket() {
        this.registeredClients = new HashSet<>();
    }

    public AccessTicket(String accessToken, Principal principal) {
        this(accessToken, principal, System.currentTimeMillis());
    }

    public AccessTicket(String accessToken, Principal principal, long creationTime) {

        if (accessToken == null) {
            throw new IllegalArgumentException("Invalid access token: " + accessToken);
        }
        if (principal == null) {
            throw new IllegalArgumentException("Invalid principal: " + principal);
        }
        if (creationTime < 0) {
            throw new IllegalArgumentException("Invalid creation time : " + creationTime);
        }
        this.accessToken = accessToken;
        this.principal = principal;
        this.creationTime = creationTime;
        this.registeredClients = new HashSet<>();
    }

    public String getAccessToken() {
        return accessToken;
    }

    public Principal getPrincipal() {
        return principal;
    }

    /** Get time of token creation. */
    public long getCreationTime() {
        return creationTime;
    }

    /** Get copy of the set of registered clients for this token. */
    public Set<String> getRegisteredClients() {
        return new LinkedHashSet<>(registeredClients);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AccessTicket)) return false;

        AccessTicket ticket = (AccessTicket)o;

        if (creationTime != ticket.creationTime) return false;
        if (accessToken != null ? !accessToken.equals(ticket.accessToken) : ticket.accessToken != null) return false;
        if (principal != null ? !principal.equals(ticket.principal) : ticket.principal != null) return false;
        if (registeredClients != null ? !registeredClients.equals(ticket.registeredClients) : ticket.registeredClients != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = registeredClients != null ? registeredClients.hashCode() : 0;
        result = 31 * result + (principal != null ? principal.hashCode() : 0);
        result = 31 * result + (int)(creationTime ^ (creationTime >>> 32));
        result = 31 * result + (accessToken != null ? accessToken.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "AccessTicket{" +
               "registeredClients=" + registeredClients +
               ", principal=" + principal +
               ", creationTime=" + creationTime +
               ", accessToken='" + accessToken + '\'' +
               '}';
    }

    @Override
    public void writeExternal(final ObjectOutput out) throws IOException {
        if (principal == null || accessToken == null) {
            throw new RuntimeException("Object can't be externalized because it is invalid.");
        }
        out.writeObject(principal);
        out.writeLong(creationTime);
        out.writeUTF(accessToken);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        principal = (Principal)in.readObject();
        creationTime = in.readLong();
        accessToken = in.readUTF();
    }
}
