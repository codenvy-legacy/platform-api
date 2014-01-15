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

public class Member implements Externalizable {
    private Map<String, Role> roles = new HashMap<>();

    private ItemReference user;

    public Member() {
    }

    public Member(Set<Role> roles, ItemReference user) {
        setRoles(roles);
        this.user = user;
    }

    public Member(Role role, ItemReference user) {
        addRole(role);
        this.user = user;
    }

    public Member(String roleName, String userId) {
        addRole(roleName);
        this.user = new ItemReference(userId);
    }

    public Member(String userId) {
        this.user = new ItemReference(userId);
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

    public ItemReference getUser() {
        return user;
    }

    public void setUser(ItemReference user) {
        this.user = user;
    }

    // ==============================================================

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

    // ==============================================================

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Member member = (Member)o;

        if (roles != null ? !roles.equals(member.roles) : member.roles != null) {
            return false;
        }
        if (user != null ? !user.equals(member.user) : member.user != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = roles != null ? roles.hashCode() : 0;
        result = 31 * result + (user != null ? user.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Membership{" + "roles=" + roles + ", user=" + user + '}';
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeInt(roles.size());
        for (Role role : roles.values()) {
            role.writeExternal(out);
        }
        user.writeExternal(out);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        int rolesNumber = in.readInt();
        for (int i = 0; i < rolesNumber; ++i) {
            Role role = new Role();
            role.readExternal(in);
            roles.put(role.getName(), role);
        }
        ItemReference user = new ItemReference();
        user.readExternal(in);
        this.user = user;
    }
}
