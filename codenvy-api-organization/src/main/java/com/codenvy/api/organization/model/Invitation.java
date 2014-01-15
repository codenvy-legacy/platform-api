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

package com.codenvy.api.organization.model;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * POJO representation of invitation entity used in organization service.
 * Invitation is supposed to give a user the ability to invite another user (not
 * necessarily present in user database) to defined workspace. Stores all
 * invitation related data in convenient format.
 */
public class Invitation extends AbstractOrganizationUnit {
    /** Subject of invitation. A person who is the initiator of invitation */
    private ItemReference sender;

    /** Object of invitation. A person who is to be invited. */
    private String recipient;

    /** Workspace where an recipient of invitation is to be invited to. */
    private ItemReference workspace;

    /** Invitation status */
    private InvitationStatus status = InvitationStatus.ACTIVE;

    /** Map of attributes to store metadata of invitation */
    private Map<String, String> attributes = new LinkedHashMap<>();


    public Invitation() {

    }

    public Invitation(Invitation source) {
        this.id = source.getId();
        Set<Link> newLinks = new HashSet<>();
        for (Link one : source.getLinks())
            newLinks.add(new Link(one.getType(), one.getHref(), one.getRel()));
        this.links = newLinks;

        this.recipient = source.getRecipient();
        this.sender = new ItemReference(source.getSender());
        this.workspace = new ItemReference(source.getWorkspace());
        this.status = source.getStatus();

        for (Map.Entry<String, String> one : source.getAttributes().entrySet())
            this.attributes.put(one.getKey(), one.getValue());
        this.temporary = source.isTemporary();
    }


    public ItemReference getSender() {
        return sender;
    }

    public void setSender(ItemReference sender) {
        this.sender = sender;
    }

    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    public ItemReference getWorkspace() {
        return workspace;
    }

    public void setWorkspace(ItemReference workspace) {
        this.workspace = workspace;
    }

    public InvitationStatus getStatus() {
        return status;
    }

    public void setStatus(InvitationStatus status) {
        this.status = status;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

    //===================================================

    public void setAttribute(String name, String value) {
        attributes.put(name, value);
    }

    public void removeAttribute(String name) {
        attributes.remove(name);
    }

    public String getAttribute(String name) {
        return attributes.get(name);
    }

    public String getAttribute(String name, String defaultValue) {
        String val = getAttribute(name);
        return (val == null) ? defaultValue : val;
    }

    //===================================================


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Invitation that = (Invitation)o;

        if (temporary != that.temporary) return false;
        if (attributes != null ? !attributes.equals(that.attributes) : that.attributes != null) return false;
        if (recipient != null ? !recipient.equals(that.recipient) : that.recipient != null) return false;
        if (sender != null ? !sender.equals(that.sender) : that.sender != null) return false;
        if (status != that.status) return false;
        if (workspace != null ? !workspace.equals(that.workspace) : that.workspace != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = sender != null ? sender.hashCode() : 0;
        result = 31 * result + (recipient != null ? recipient.hashCode() : 0);
        result = 31 * result + (workspace != null ? workspace.hashCode() : 0);
        result = 31 * result + (temporary ? 1 : 0);
        result = 31 * result + (status != null ? status.hashCode() : 0);
        result = 31 * result + (attributes != null ? attributes.hashCode() : 0);
        return result;
    }


    @Override
    public String toString() {
        return "Invitation{" +
               "sender=" + sender +
               ", recipient='" + recipient + '\'' +
               ", workspace=" + workspace +
               ", temporary=" + temporary +
               ", status=" + status +
               ", attributes=" + attributes +
               '}';
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);
        ItemReference sender = new ItemReference();
        sender.readExternal(in);
        this.sender = sender;
        recipient = in.readUTF();
        ItemReference ws = new ItemReference();
        ws.readExternal(in);
        this.workspace = ws;
        this.status = (InvitationStatus)in.readObject();
        int attributesNumber = in.readInt();
        for (int i = 0; i < attributesNumber; ++i) {
            attributes.put(in.readUTF(), in.readUTF());
        }
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        super.writeExternal(out);
        sender.writeExternal(out);
        out.writeUTF(recipient);
        workspace.writeExternal(out);
        out.writeObject(status);
        out.writeInt(attributes.size());
        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            out.writeUTF(entry.getKey());
            out.writeUTF(entry.getValue());
        }
    }
}
