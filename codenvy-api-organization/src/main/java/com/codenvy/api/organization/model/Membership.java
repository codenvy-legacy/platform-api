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

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.*;


/**
 * POJO representation of membership entity contained in {@link User_old} . Each user may have
 * several membreships.
 */
public class Membership implements Externalizable {
    private Map<String, Role> roles = new HashMap<>();

    private ItemReference workspace;

    public Membership() {
    }

    public Membership(Set<Role> roles, ItemReference workspace) {
        setRoles(roles);
        this.workspace = workspace;
    }

    public Membership(Role role, ItemReference workspace) {
        addRole(role);
        this.workspace = workspace;
    }

    public Membership(String roleName, String workspaceId) {
        addRole(roleName);
        this.workspace = new ItemReference(workspaceId);
    }

    public Membership(String roleName, String workspaceId, boolean temporary) {
        addRole(roleName);
        this.workspace = new ItemReference(workspaceId);
        this.workspace.setTemporary(temporary);
    }

    public Membership(String workspaceId) {
        this.workspace = new ItemReference(workspaceId);
    }

    public Set<Role> getRoles() {
        return Collections.unmodifiableSet(new HashSet<>(roles.values()));
    }

    public void setRoles(Set<Role> roles) {
        Map<String, Role> newRoles = new HashMap<>();
        for (Role role : roles) {
            newRoles.put(role.getName(), role);
        }
        this.roles = newRoles;
    }

    public ItemReference getWorkspace() {
        return workspace;
    }

    public void setWorkspace(ItemReference workspace) {
        this.workspace = workspace;
    }

    //==============================================================

    public void addRole(Role role) {
        roles.put(role.getName(), role);
    }

    public void removeRole(Role role) {
        roles.remove(role.getName());
    }

    public void addRole(String name, String description) {
        roles.put(name, new Role(name, description));
    }

    public void addRole(String name) {
        roles.put(name, new Role(name));
    }

    public void removeRole(String name) {
        roles.remove(name);
    }

    //==============================================================

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Membership that = (Membership)o;

        return !(roles != null ? !roles.equals(that.roles) : that.roles != null) &&
               !(workspace != null ? !workspace.equals(that.workspace) : that.workspace != null);

    }

    @Override
    public int hashCode() {
        int result = roles != null ? roles.hashCode() : 0;
        result = 31 * result + (workspace != null ? workspace.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Membership{" + "roles=" + roles + ", workspace=" + workspace + '}';
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeInt(roles.size());
        for (Role role : roles.values()) {
            role.writeExternal(out);
        }
        workspace.writeExternal(out);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        int rolesNumber = in.readInt();
        for (int i = 0; i < rolesNumber; ++i) {
            Role role = new Role();
            role.readExternal(in);
            roles.put(role.getName(), role);
        }
        ItemReference workspace = new ItemReference();
        workspace.readExternal(in);
        this.workspace = workspace;
    }
}
