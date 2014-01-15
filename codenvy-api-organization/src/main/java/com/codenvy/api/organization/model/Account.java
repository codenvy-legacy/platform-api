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
import java.util.*;

/**
 * POJO representation of account entity used in organization service. It is considered that account contains
 * information associated with workspaces it owns and their members. It may also determine workspace members'
 * behaviour.
 * This is how account interacts with the other organization service entities: <ul> <li>Account must have single owner
 * represented by {@link User_old}.</li> <li>Account can have zero or more {@link Workspace}.</li> </ul>
 */
public class Account extends AbstractOrganizationUnit {
    /**
     * Account owner identifier. In terms of organization service an owner of an account is a user. To getById more
     * information about user identifiers check {@link  User_old#id}.
     */
    private ItemReference owner;

    /** Account attributes. */
    private Map<String, String> attributes = new HashMap<>();

    /**
     * Basically any account can be an owner of arbitrary set of workspaces. The set of their identifiers (names) is
     * defined by this variable. To getById more information about workspace names check {@link Workspace#id}.
     */
    private Map<String, ItemReference> workspaces = new HashMap<>();

    private String name;

    public Account() {
    }

    public Account(ItemReference owner) {
        this.owner = owner;
    }

    public Account(String ownerId) {
        this.owner = new ItemReference(ownerId);
    }

    public ItemReference getOwner() {
        return owner;
    }

    public Account(String name, String ownerId) {
        this.name = name;
        this.owner = new ItemReference(ownerId);
    }

    public Account(String name, ItemReference owner) {
        this.name = name;
        this.owner = owner;
    }

    public Account(Account source) {
        this.name = source.getName();
        this.owner = new ItemReference(source.getOwner());
        this.id = source.getId();
        Set<Link> newLinks = new HashSet<>();
        for (Link one : source.getLinks())
            newLinks.add(new Link(one.getType(), one.getHref(), one.getRel()));
        this.links = newLinks;

        Map<String, ItemReference> workspaces = new HashMap<>();
        for (ItemReference workspace : source.getWorkspaces())
            workspaces.put(workspace.getId(), new ItemReference(workspace));
        this.workspaces = workspaces;

        for (Map.Entry<String, String> one : source.getAttributes().entrySet())
            this.attributes.put(one.getKey(), one.getValue());

        this.temporary = source.isTemporary();
    }

    public void setOwner(ItemReference owner) {
        this.owner = owner;
    }

    public Map<String, String> getAttributes() {
        return Collections.unmodifiableMap(attributes);
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

    public Set<ItemReference> getWorkspaces() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(workspaces.values()));
    }

    public boolean containsWorkspace(String workspaceId) {
        return workspaces.containsKey(workspaceId);
    }

    public void setWorkspaces(Set<ItemReference> workspaces) {
        Map<String, ItemReference> workspacesMap = new HashMap<>();
        for (ItemReference workspace : workspaces) {
            workspacesMap.put(workspace.getId(), workspace);
        }
        this.workspaces = workspacesMap;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = toLowerCase(name);
    }

    //===================================================

    public void addWorkspace(String workspaceId) {
        workspaces.put(workspaceId, new ItemReference(workspaceId));
    }

    public void removeWorkspace(String workspaceId) {
        workspaces.remove(workspaceId);
    }

    public void addWorkspace(ItemReference workspaceReference) {
        workspaces.put(workspaceReference.getId(), workspaceReference);
    }

    public void removeWorkspace(ItemReference workspaceReference) {
        workspaces.remove(workspaceReference.getId());
    }

    public void setAttribute(String name, String value) {
        attributes.put(name, value);
    }

    public void removeAttribute(String name) {
        attributes.remove(name);
    }

    public String getAttribute(String name) {
        return attributes.get(name);
    }

    //===================================================


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Account account = (Account)o;
        if (id != null ? !id.equals(account.id) : account.id != null) return false;
        if (temporary != account.temporary) return false;
        if (attributes != null ? !attributes.equals(account.attributes) : account.attributes != null) return false;
        if (name != null ? !name.equals(account.name) : account.name != null) return false;
        if (owner != null ? !owner.equals(account.owner) : account.owner != null) return false;
        if (workspaces != null ? !workspaces.equals(account.workspaces) : account.workspaces != null) return false;
        if (links != null ? !links.equals(account.links) : account.links != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = owner != null ? owner.hashCode() : 0;
        result = 31 * result + (id != null ? id.hashCode() : 0);
        result = 31 * result + (links != null ? links.hashCode() : 0);
        result = 31 * result + (temporary ? 1 : 0);
        result = 31 * result + (attributes != null ? attributes.hashCode() : 0);
        result = 31 * result + (workspaces != null ? workspaces.hashCode() : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Account{" +
               "owner=" + owner +
               ", temporary=" + temporary +
               ", attributes=" + attributes +
               ", workspaces=" + workspaces +
               ", name='" + name + '\'' +
               '}';
    }


    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);
        ItemReference owner = new ItemReference();
        owner.readExternal(in);
        this.owner = owner;
        name = in.readUTF();
        int attributesNumber = in.readInt();
        for (int i = 0; i < attributesNumber; ++i) {
            attributes.put(in.readUTF(), in.readUTF());
        }

        int wksNumber = in.readInt();
        for (int i = 0; i < wksNumber; ++i) {
            ItemReference wks = new ItemReference();
            wks.readExternal(in);
            workspaces.put(wks.getId(), wks);
        }
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        super.writeExternal(out);
        owner.writeExternal(out);
        out.writeUTF(name);

        out.writeInt(attributes.size());
        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            out.writeUTF(entry.getKey());
            out.writeUTF(entry.getValue());
        }

        out.writeInt(workspaces.size());
        for (ItemReference ref : workspaces.values()) {
            ref.writeExternal(out);
        }
    }
}
