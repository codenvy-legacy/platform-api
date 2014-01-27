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
package com.codenvy.api.organization.dao.ldap;

import com.codenvy.api.organization.dao.UserDao;
import com.codenvy.api.organization.exception.OrganizationServiceException;
import com.codenvy.api.organization.shared.dto.User;
import com.codenvy.dto.server.DtoFactory;

import javax.inject.Inject;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import java.util.Arrays;
import java.util.List;

/**
 *
 */
public class UserDaoImpl implements UserDao {

    /** LDAP objectClass attribute name */
    protected static final String OBJECT_CLASS = "objectClass";

    /** Cloud IDE organization service user LDAP objectClass representation. */
    protected static final String USER = "cloudIdeUser";

    /** User props */

    /** Cloud IDE organization service aliases list LDAP attribute name. */
    protected static final String ALIASES = "cloudIdeAliases";

    /** Cloud IDE organization service user password LDAP attribute name. */
    protected static final String PASSWORD = "cloudIdeUserPassword";

    /** Cloud IDE organization service user password LDAP attribute name. */
    protected static final String EMAIL = "cloudIdeUserEmail";

    /** Cloud IDE organization service user password LDAP attribute name. */
    protected static final String PROFILE = "cloudIdeUserProfile";


    protected LdapService ldapService;

    @Inject
    public UserDaoImpl(LdapService ldapService) {
        this.ldapService = ldapService;
    }

    @Override
    public void create(User user) throws OrganizationServiceException {
        try {
            String userDN =
                    LdapService.OU + "=" + user.getId() + "," + LdapService.OU + "=" + LdapService.USERS_SUBTREE_NAME;
            BasicAttributes attributes = userPropertiesToLdapAttributes(user);
            ldapService.createLdapEntry(userDN, attributes);
        } catch (NamingException e) {
            throw new OrganizationServiceException("Error trying to create " + User.class.getSimpleName() + ":"
                                                   + user.getId(), e);
        }

    }

    @Override
    public void update(User user) throws OrganizationServiceException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void removeById(String id) throws OrganizationServiceException {
        try {
            ldapService.removeEntryByName(
                    LdapService.OU + "=" + id + "," + LdapService.OU + "=" + LdapService.USERS_SUBTREE_NAME);
        } catch (NamingException e) {
            throw new OrganizationServiceException("Error trying to remove " + User.class.getSimpleName() + ":" + id,
                                                   e);
        }
    }

    @Override
    public User getByAlias(String alias) throws OrganizationServiceException {
        try {
            User user = null;
            Attributes userLdapAttributes =
                    getLdapEntryAttributes(ALIASES + "=*," + alias + ",*", USER,
                                           LdapService.OU + "=" + LdapService.USERS_SUBTREE_NAME);

            if (userLdapAttributes != null) {
                user = ldapAttributesToUserProperties(userLdapAttributes);
            }
            return user;
        } catch (NamingException e) {
            throw new OrganizationServiceException("Error trying to obtain " + User.class.getSimpleName() + ":" + alias,
                                                   e);
        }
    }

    @Override
    public User getById(String id) throws OrganizationServiceException {
        try {
            User user = null;
            Attributes userLdapAttributes = getLdapEntryAttributes(LdapService.OU + "=" + id, USER,
                                                                   LdapService.OU + "=" +
                                                                   LdapService.USERS_SUBTREE_NAME);

            if (userLdapAttributes != null) {
                user = ldapAttributesToUserProperties(userLdapAttributes);
            }
            return user;
        } catch (NamingException e) {
            throw new OrganizationServiceException("Error trying to obtain " + User.class.getSimpleName() + ":" + id,
                                                   e);
        }
    }


    // Helpers

    public static String join(List<String> names) {
        StringBuilder sb = new StringBuilder(",");
        for (String name : names) {
            sb.append(name).append(",");
        }
        return sb.toString();
    }

    private BasicAttributes userPropertiesToLdapAttributes(User user) {
        BasicAttributes attributes = new BasicAttributes();
        attributes.put(OBJECT_CLASS, USER);
        attributes.put(LdapService.OU, user.getId());

        attributes.put(PASSWORD, user.getPassword());
        attributes.put(ALIASES, join(user.getAliases()));
        attributes.put(PROFILE, user.getProfileId());
        attributes.put(EMAIL, user.getEmail());
        return attributes;
    }

    private User ldapAttributesToUserProperties(Attributes attributes) throws NamingException {
        User user = DtoFactory.getInstance().createDto(User.class);
        user.setId(attributes.get(LdapService.OU).get().toString());
        user.setPassword(attributes.get(PASSWORD).get().toString());
        user.setEmail(attributes.get(EMAIL).get().toString());
        user.setProfileId(attributes.get(PROFILE).get().toString());
        user.setAliases(Arrays.asList(attributes.get(ALIASES).get().toString().split(",")));
        return user;
    }

    protected Attributes getLdapEntryAttributes(String relativeDN, String objectClass, String baseDN)
            throws NamingException {
        Attributes attributes = null;
        NamingEnumeration<SearchResult> ldapNamingEnumeration = null;

        try {
            ldapNamingEnumeration =
                    ldapService.searchEntryByNameAndObjectClass(relativeDN, objectClass, baseDN,
                                                                SearchControls.ONELEVEL_SCOPE);
            if (ldapNamingEnumeration.hasMoreElements()) {
                attributes = ldapNamingEnumeration.nextElement().getAttributes();
            }

            return attributes;
        } finally {
            ldapService.close(ldapNamingEnumeration);
        }
    }
}
