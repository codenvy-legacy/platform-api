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
 * POJO representation of workspace entity used in organization service. Basically used to group all users in one
 * place.
 * This is how a account interacts with the organization service entities: <ul> <li>Workspace must have an owner
 * represented by {@link Account}.</li> <li>Workspace can have zero or more members represented by {@link User_old}.</li>
 * </ul>
 */
public class Workspace extends AbstractOrganizationUnit {
    /**
     * In terms of organization service any workspace must have an owner. An owner of a workspace is an account. This
     * variable contain the identifier (id) of the account owning current workspace.
     * <p/>
     * To getById more information about account identifiers check {@link Account#id}
     */
    private ItemReference owner;

    /**
     * Any workspace can have arbitrary set of members. In terms of organization service a member of workspace is a
     * user.
     * This variable is a set of identifiers of users who are the members of current workspace. To getById more
     * information
     * about user identifiers check {@link User_old#id}
     */
    private Map<String, Member> members = new HashMap<>();

    /** Workspace attributes. */
    private Map<String, String> attributes = new HashMap<>();

    private String name;

    public Workspace() {
    }

    public Workspace(ItemReference owner) {
        this.owner = owner;
    }

    public Workspace(String name, String ownerId) {
        this.name = name;
        this.owner = new ItemReference(ownerId);
    }

    public Workspace(String name, ItemReference owner) {
        this.name = name;
        this.owner = owner;
    }

    public Workspace(String ownerId) {
        this.owner = new ItemReference(ownerId);
    }

    public Workspace(Workspace source) {
        this.name = source.getName();
        this.owner = new ItemReference(source.getOwner());
        this.id = source.getId();
        Set<Link> newLinks = new HashSet<>();
        for (Link one : source.getLinks())
            newLinks.add(new Link(one.getType(), one.getHref(), one.getRel()));
        this.links = newLinks;

        for (Member member : source.getMembers()) {
            Member newMember = new Member(member.getUser().getId());
            for (Role role : member.getRoles()) {
                Role newRole = new Role(role.getName(), role.getDescription());
                newMember.addRole(newRole);
            }
            this.members.put(newMember.getUser().getId(), newMember);
        }

        for (Map.Entry<String, String> one : source.getAttributes().entrySet())
            this.attributes.put(one.getKey(), one.getValue());

        this.temporary = source.isTemporary();
    }


    public ItemReference getOwner() {
        return owner;
    }

    public void setOwner(ItemReference owner) {
        this.owner = owner;
    }

    public Set<Member> getMembers() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(members.values()));
    }

    public boolean containMember(String userId) {
        return members.containsKey(userId);
    }

    public void setMembers(Set<Member> members) {
        HashMap<String, Member> membersMap = new HashMap<>();
        for (Member member : members) {
            membersMap.put(member.getUser().getId(), member);
        }
        this.members = membersMap;
    }

    public Map<String, String> getAttributes() {
        return Collections.unmodifiableMap(attributes);
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = toLowerCase(name);
    }

    //=================================================

    public void addMember(String userId) {
        members.put(userId, new Member(userId));
    }

    public void removeMember(String userId) {
        members.remove(userId);
    }

    public void addMember(Member member) {
        members.put(member.getUser().getId(), member);
    }

    public void removeMember(Member member) {
        members.remove(member.getUser().getId());
    }

    public void addMemberRole(Role role, String userId) {
        members.get(userId).addRole(role);
    }

    public void removeMemberRole(Role role, String userId) {
        members.get(userId).removeRole(role);
    }

    public void addMemberRole(String roleName, String userId) {
        members.get(userId).addRole(roleName);
    }

    public void removeMemberRole(String roleName, String userId) {
        members.get(userId).removeRole(roleName);
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

    public String getAttribute(String name, String defaultValue) {
        String val = getAttribute(name);
        return (val == null) ? defaultValue : val;
    }

    //=================================================

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Workspace workspace = (Workspace)o;

        if (temporary != workspace.temporary) return false;
        if (attributes != null ? !attributes.equals(workspace.attributes) : workspace.attributes != null) return false;
        if (members != null ? !members.equals(workspace.members) : workspace.members != null) return false;
        if (name != null ? !name.equals(workspace.name) : workspace.name != null) return false;
        if (owner != null ? !owner.equals(workspace.owner) : workspace.owner != null) return false;
        if (id != null ? !id.equals(workspace.id) : workspace.id != null) return false;
        if (links != null ? !links.equals(workspace.links) : workspace.links != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = owner != null ? owner.hashCode() : 0;
        result = 31 * result + (id != null ? id.hashCode() : 0);
        result = 31 * result + (links != null ? links.hashCode() : 0);
        result = 31 * result + (members != null ? members.hashCode() : 0);
        result = 31 * result + (temporary ? 1 : 0);
        result = 31 * result + (attributes != null ? attributes.hashCode() : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        return result;
    }


    @Override
    public String toString() {
        return "Workspace{" +
               "owner=" + owner +
               ", members=" + members +
               ", temporary=" + temporary +
               ", attributes=" + attributes +
               ", name='" + name + '\'' +
               '}';
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);
        this.name = in.readUTF();
        ItemReference owner = new ItemReference();
        owner.readExternal(in);
        this.owner = owner;
        int membersNumber = in.readInt();
        for (int i = 0; i < membersNumber; ++i) {
            Member member = new Member();
            member.readExternal(in);
            members.put(member.getUser().getId(), member);
        }
        int attributesNumber = in.readInt();
        for (int i = 0; i < attributesNumber; ++i) {
            attributes.put(in.readUTF(), in.readUTF());
        }
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        super.writeExternal(out);
        out.writeUTF(name);
        owner.writeExternal(out);
        out.writeInt(members.size());
        for (Member one : members.values()) {
            one.writeExternal(out);
        }
        out.writeInt(attributes.size());
        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            out.writeUTF(entry.getKey());
            out.writeUTF(entry.getValue());
        }
    }
}
