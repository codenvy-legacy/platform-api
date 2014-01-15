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
 * POJO representation of user entity used in organization service.  User is the main unit of {@link }UserDBService).
 * This is how a user interacts with the other entities: <ul> <li>User can be a member of zero or more {@link
 * Workspace}.</li> <li>User can own zero or more {@link Account}.</li> <li>User can have zero or more {@link
 * Role}.</li> <li>User always has a {@link Profile}.</li> <li>User always has at least one alias (which equals to its
 * identifier), but can have more.</li> </ul>
 */
public class User extends AbstractOrganizationUnit {
    /**
     * Set of user aliases. User is allowed to have several aliases to simplify the logic of data access and
     * authentication operations. Aliases can be used instead of identifiers during GET, REMOVE and AUTHENTICATION
     * operations. As opposed to identifiers aliases are determined by organization service clients and can be
     * represented by any string constant.Obviously each alias must be unique throughout the organization service. In
     * context of organization service user aliases are case insensitive. Note that this very entity is not supposed to
     * be used for User-Workspace and User-Account relations.
     */
    private Set<String> aliases = new LinkedHashSet<>();

    /** User password */
    private String password;

    /** User profile */
    private Profile profile = new Profile();


    /**
     * Any user can be a member of arbitrary set of workspaces. The set of identifiers (names) of those workspaces is
     * represented by this variable. In context of organization service workspace identifiers (names) are case
     * insensitive. <p> To getById more information about workspaces check {@link Workspace} </p> As a user can be a
     * member
     * of arbitrary set of workspaces one can assign the user a set of role associated with specific workspace. The
     * variable contains user role associations.
     */
    private Map<String, Membership> memberships = new HashMap<>();

    /**
     * Any user can be an owner of arbitrary set of accounts. The set of identifiers (names) of those accounts is
     * represented by this variable. In context of organization service workspace identifiers (names) are case
     * insensitive. <p> To getById more information about accounts check {@link Account} </p>
     */
    private Map<String, ItemReference> accounts = new HashMap<>();

    public User() {
    }

    public User(String alias) {
        addAlias(alias);
    }

    public User(String alias, String password) {
        addAlias(alias);
        this.password = password;
    }

    public User(User source) {
        this.id = source.getId();
        this.password = source.getPassword();
        Set<Link> newLinks = new HashSet<>();
        for (Link one : source.getLinks())
            newLinks.add(new Link(one.getType(), one.getHref(), one.getRel()));
        this.links = newLinks;

        for (ItemReference account : source.getAccounts()) {
            accounts.put(account.getId(), new ItemReference(account));
        }

        Set<String> aliases = new HashSet<>();
        for (String alias : source.getAliases())
            aliases.add(alias);
        this.aliases = aliases;

        for (Membership membership : source.getMemberships()) {
            Set<Role> newRoles = new LinkedHashSet<>();
            for (Role role : membership.getRoles()) {
                newRoles.add(new Role(role.getName(), role.getDescription()));
            }

            Membership newMembership = new Membership();
            newMembership.setRoles(newRoles);
            newMembership.setWorkspace(new ItemReference(membership.getWorkspace()));

            this.memberships.put(newMembership.getWorkspace().getId(), newMembership);
        }

        Map<String, String> attributes = new HashMap<>(source.getProfile().getAttributes());

        Profile newProfile = new Profile();
        newProfile.setAttributes(attributes);
        this.profile = newProfile;
        this.temporary = source.isTemporary();
    }


    public Set<String> getAliases() {
        return Collections.unmodifiableSet(aliases);
    }

    public void setAliases(Set<String> aliases) {
        this.aliases = aliases;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Profile getProfile() {
        return profile;
    }

    public void setProfile(Profile profile) {
        this.profile = profile;
    }

    public Set<Membership> getMemberships() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(memberships.values()));
    }

    public boolean containsMembership(String workspaceId) {
        return memberships.containsKey(workspaceId);
    }

    public void setMemberships(Set<Membership> memberships) {
        HashMap<String, Membership> membershipMap = new HashMap<>();
        for (Membership membership : memberships) {
            membershipMap.put(membership.getWorkspace().getId(), membership);
        }
        this.memberships = membershipMap;
    }

    public Set<ItemReference> getAccounts() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(accounts.values()));
    }

    public boolean containsAccount(String accountId) {
        return accounts.containsKey(accountId);
    }

    public void setAccounts(Set<ItemReference> accounts) {
        HashMap<String, ItemReference> accountsMap = new HashMap<>();
        for (ItemReference accountReference : accounts) {
            accountsMap.put(accountReference.getId(), accountReference);
        }
        this.accounts = accountsMap;
    }

    //==============================================================

    public Membership getMembership(String workspaceId) {
        return memberships.get(workspaceId);
    }

    public void addMembership(Membership membership) {
        memberships.put(membership.getWorkspace().getId(), membership);
    }

    public void removeMembership(Membership membership) {
        memberships.remove(membership.getWorkspace().getId());
    }

    public void addMembership(String workspaceId) {
        addMembership(new Membership(workspaceId));
    }

    public void removeMembership(String workspaceId) {
        memberships.remove(workspaceId);
    }

    public void addMembershipRole(String roleName, String workspaceId) {
        getMembership(workspaceId).addRole(roleName);
    }

    public void removeMembershipRole(String roleName, String workspaceId) {
        getMembership(workspaceId).removeRole(roleName);
    }

    public void addMembershipRole(Role role, String workspaceId) {
        getMembership(workspaceId).addRole(role);
    }

    public void removeMembershipRole(Role role, String workspaceId) {
        getMembership(workspaceId).removeRole(role);
    }

    public void addAlias(String alias) {
        aliases.add(toLowerCase(alias));
    }

    public void removeAlias(String alias) {
        if (aliases.size() == 1) {
            throw new IllegalStateException("Can't remove the last alias");
        }
        aliases.remove(toLowerCase(alias));
    }

    @Deprecated
    public void addAccount(String accountId) {
        accounts.put(accountId, new ItemReference(accountId));
    }

    public void removeAccount(String accountId) {
        accounts.remove(accountId);
    }

    public void addAccount(ItemReference accountReference) {
        accounts.put(accountReference.getId(), accountReference);
    }

    public void removeAccount(ItemReference accountReference) {
        accounts.remove(accountReference.getId());
    }

    //==============================================================


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        User user = (User)o;

        if (temporary != user.temporary) return false;
        if (accounts != null ? !accounts.equals(user.accounts) : user.accounts != null) return false;
        if (aliases != null ? !aliases.equals(user.aliases) : user.aliases != null) return false;
        if (memberships != null ? !memberships.equals(user.memberships) : user.memberships != null) return false;
        if (password != null ? !password.equals(user.password) : user.password != null) return false;
        if (profile != null ? !profile.equals(user.profile) : user.profile != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = aliases != null ? aliases.hashCode() : 0;
        result = 31 * result + (password != null ? password.hashCode() : 0);
        result = 31 * result + (profile != null ? profile.hashCode() : 0);
        result = 31 * result + (temporary ? 1 : 0);
        result = 31 * result + (memberships != null ? memberships.hashCode() : 0);
        result = 31 * result + (accounts != null ? accounts.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "User{" +
               "aliases=" + aliases +
               ", password='" + password + '\'' +
               ", profile=" + profile +
               ", temporary=" + temporary +
               ", memberships=" + memberships +
               ", accounts=" + accounts +
               '}';
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);
        int aliasesSize = in.readInt();
        for (int i = 0; i < aliasesSize; ++i) {
            aliases.add(in.readUTF());
        }
        password = (String)in.readObject();
        Profile profile = new Profile();
        profile.readExternal(in);
        this.profile = profile;
        int membershipsSize = in.readInt();
        for (int i = 0; i < membershipsSize; ++i) {
            Membership membership = new Membership();
            membership.readExternal(in);
            memberships.put(membership.getWorkspace().getId(), membership);
        }

        int accountsSize = in.readInt();
        for (int i = 0; i < accountsSize; ++i) {
            ItemReference acc = new ItemReference();
            acc.readExternal(in);
            accounts.put(acc.getId(), acc);
        }
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        super.writeExternal(out);
        out.writeInt(aliases.size());
        for (String one : aliases) {
            out.writeUTF(one);
        }
        out.writeObject(password);
        profile.writeExternal(out);
        out.writeInt(memberships.size());
        for (Membership one : memberships.values()) {
            one.writeExternal(out);
        }
        out.writeInt(accounts.size());
        for (ItemReference one : accounts.values()) {
            one.writeExternal(out);
        }

    }
}
