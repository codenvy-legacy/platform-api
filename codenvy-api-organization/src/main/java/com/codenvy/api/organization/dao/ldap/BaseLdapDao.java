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

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import java.util.*;

public abstract class BaseLdapDao {
    /** Users subtree name */
    public static final String USERS_SUBTREE_NAME = "users";

    /** Accounts subtree name */
    public static final String ACCOUNTS_SUBTREE_NAME = "accounts";

    /** Workspaces subtree name */
    public static final String WORKSPACES_SUBTREE_NAME = "workspaces";

    /** Workspaces subtree name */
    public static final String INVITATIONS_SUBTREE_NAME = "invitations";

    /**
     * Is used to separate key from value in attributes and role name from role description in roles in LDAP records. Escaped with
     * "/&lt;/&gt;" if met in any key or value. Hence '/' character is escaped to with "//".
     */
    public static final String SEPARATOR = "<>";

    /** Cloud IDE organization service user LDAP objectClass representation. */
    protected static final String USER = "cloudIdeUser";

    /** Cloud IDE organization service account LDAP objectClass representation. */
    protected static final String ACCOUNT = "cloudIdeAccount";

    /** Cloud IDE organization service workspace LDAP objectClass representation. */
    protected static final String WORKSPACE = "cloudIdeWorkspace";

    /** Cloud IDE organization service invitation LDAP objectClass representation. */
    protected static final String INVITATION = "cloudIdeInvitation";

    /** Cloud IDE organization service invitation status LDAP attribute representation. */
    protected static final String INVITATION_STATUS = "cloudIdeInvitationStatus";

    /** Cloud IDE organization service user memberships LDAP attribute representation. */
    protected static final String MEMBERSHIPS = "cloudIdeMemberships";

    /** Cloud IDE organization service workspace members LDAP attribute representation. */
    protected static final String MEMBERS = "cloudIdeMembers";

    /** Cloud IDE organization service entities attributes LDAP representation. There are user profile attributes and account attributes. */
    protected static final String ATTRIBUTES = "cloudIdeAttributes";

    /** Cloud IDE organization service entity owner property LDAP attribute name. Can be account owner or workspace owner. */
    protected static final String OWNER = "cloudIdeEntityOwner";

    /** Cloud IDE organization service workspaces list LDAP attribute name. */
    protected static final String WORKSPACES = "cloudIdeWorkspaces";

    /** Cloud IDE organization service entity name LDAP attribute name. */
    protected static final String NAME = "cloudIdeName";

    /** Cloud IDE organization service sender LDAP attribute name. */
    protected static final String SENDER = "cloudIdeSender";

    /** Cloud IDE organization service recipient LDAP attribute name. */
    protected static final String RECIPIENT = "cloudIdeRecipient";

    /** Cloud IDE organization service accounts list LDAP attribute name. */
    protected static final String ACCOUNTS = "cloudIdeAccounts";

    /** Cloud IDE organization service aliases list LDAP attribute name. */
    protected static final String ALIASES = "cloudIdeAliases";

    /** Cloud IDE organization service user password LDAP attribute name. */
    protected static final String PASSWORD = "cloudIdeUserPassword";

    /** LDAP organizationalUnit objectClass name */
    protected static final String ORGANIZATIONAL_UNIT = "organizationalUnit";

    /** LDAP objectClass attribute name */
    protected static final String OBJECT_CLASS = "objectClass";

    /** LDAP organization unit attribute */
    protected static final String OU = "ou";

    /** LDAP description attribute */
    protected static final String DESCRIPTION = "description";

    /** LDAP service instance */
    protected LdapService ldapService;

    /**
     * Base class containing generic for all DAO interfaces implementations constants and LdapService instance.
     *
     * @param ldapService
     *         LDAP service
     */
    public BaseLdapDao(LdapService ldapService) {
        this.ldapService = ldapService;
    }

    public static String transform(Set<String> names) {
        StringBuilder sb = new StringBuilder(",");

        for (String name : names) {
            sb.append(name);
            sb.append(",");
        }

        return sb.toString();
    }

    public static String[] split(String set) {
        return set.split(",");
    }

    public static String escape(String string) {
        if (string == null) {
            return "";
        }

        return string.replaceAll("/", "//")
                     .replaceAll(SEPARATOR, "/" + SEPARATOR.charAt(0) + "/" + SEPARATOR.charAt(1));
    }

    public static String unescape(String string) {
        if (string == null) {
            return "";
        }

        return string.replaceAll("/" + SEPARATOR.charAt(0) + "/" + SEPARATOR.charAt(1), SEPARATOR)
                     .replaceAll("//", "/");
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

    protected List<Attributes> getLdapEntriesAttributes(Map<String, String> searchCriteria, String baseDN)
            throws NamingException {
        List<Attributes> attributes = new LinkedList<>();
        NamingEnumeration<SearchResult> ldapNamingEnumeration = null;
        try {
            ldapNamingEnumeration =
                    ldapService.searchEntryByAttributes(searchCriteria, baseDN, SearchControls.ONELEVEL_SCOPE);

            while (ldapNamingEnumeration.hasMoreElements()) {
                attributes.add(ldapNamingEnumeration.nextElement().getAttributes());
            }

            return attributes;
        } finally {
            ldapService.close(ldapNamingEnumeration);
        }
    }

    protected Map<String, String> getLdapEntriesAttributes(String objectClass, String baseDN, String attrName,
                                                           String attrValueName) throws NamingException {
        Map<String, String> attributesMap = new LinkedHashMap<>();

        NamingEnumeration<SearchResult> ldapNamingEnumeration = null;

        try {
            ldapNamingEnumeration =
                    ldapService.searchEntriesByObjectClass(objectClass, baseDN, SearchControls.ONELEVEL_SCOPE);

            while (ldapNamingEnumeration.hasMoreElements()) {
                Attributes attributes = ldapNamingEnumeration.nextElement().getAttributes();

                attributesMap
                        .put(attributes.get(attrName).get().toString(), attributes.get(attrValueName).get().toString());
            }

            return attributesMap;
        } finally {
            ldapService.close(ldapNamingEnumeration);
        }
    }
}
