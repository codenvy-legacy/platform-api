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

    /** User email */
    private String email;

    /** User password */
    private String password;

    /** User profile ID */
    private String profileId;


    public User() {
    }

    public User(String email) {
        this.email = email;
    }

    public User(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public User(User source) {
        this.id = source.getId();
        this.email = source.getEmail();
        this.password = source.getPassword();
        Set<Link> newLinks = new HashSet<>();
        for (Link one : source.getLinks())
            newLinks.add(new Link(one.getType(), one.getHref(), one.getRel()));
        this.links = newLinks;


        Set<String> aliases = new HashSet<>();
        for (String alias : source.getAliases())
            aliases.add(alias);
        this.aliases = aliases;
        this.profileId = source.getProfileId();
        this.temporary = source.isTemporary();
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getProfileId() {
        return profileId;
    }

    public void setProfileId(String profileId) {
        this.profileId = profileId;
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

    public void addAlias(String alias) {
        aliases.add(toLowerCase(alias));
    }

    public void removeAlias(String alias) {
        if (aliases.size() == 1) {
            throw new IllegalStateException("Can't remove the last alias");
        }
        aliases.remove(toLowerCase(alias));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        User user = (User)o;

        if (temporary != user.temporary) return false;
        if (aliases != null ? !aliases.equals(user.aliases) : user.aliases != null) return false;
        if (password != null ? !password.equals(user.password) : user.password != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = aliases != null ? aliases.hashCode() : 0;
        result = 31 * result + (password != null ? password.hashCode() : 0);
        result = 31 * result + (temporary ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        return "User{" +
               "aliases=" + aliases +
               ", password='" + password + '\'' +
               ", temporary=" + temporary +
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
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        super.writeExternal(out);
        out.writeInt(aliases.size());
        for (String one : aliases) {
            out.writeUTF(one);
        }
        out.writeObject(password);
    }
}
