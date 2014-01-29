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

import com.codenvy.api.user.server.dao.UserDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.*;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

/**
 * LDAP service implementation. Interlayer service is communicating with  ({@link UserDao},
 * from one side and interacting with LDAP server from another. LDAP server is used for persistent storage of user
 * database data.
 */
public class LdapService {
    /** Logger */
    private static final Logger LOG = LoggerFactory.getLogger(LdapService.class);

    /** LDAP organizationalUnit objectClass name */
    protected static final String ORGANIZATIONAL_UNIT = "organizationalUnit";

    /** LDAP organization unit attribute */
    protected static final String OU = "ou";

    /** Users subtree name */
    public static final String USERS_SUBTREE_NAME = "users";


    private Map<String, String> env = new HashMap<>();

    /**
     * Create LdapService instance using provided configuration parameters.
     *
     * @param environmentProperties
     *         environment properties
     * @throws NamingException
     */
    public LdapService(Map<String, String> environmentProperties) throws NamingException {
        this.env = environmentProperties;

        initUserSubTree();
        LOG.debug("LDAP service initialized.");

    }

    /**
     * Create (if not already created) LDAP hierarchy subtree root for each user DB entity group.
     *
     *
     * @throws NamingException
     *         if issue occurred during LdapService and LDAP server interaction
     */
    public void initUserSubTree() throws NamingException {
        LdapContext ctx = null;
        NamingEnumeration<SearchResult> searchCtx = null;
        DirContext createCtx = null;
        try {
            ctx = getLdapContext();

            searchCtx = ctx.search(getUrl(), "(" + OU + "=" + USERS_SUBTREE_NAME + ")", null);
            if (!searchCtx.hasMore()) {
                BasicAttribute oc = new BasicAttribute("objectClass");
                oc.add(ORGANIZATIONAL_UNIT);

                BasicAttributes attributes = new BasicAttributes();
                attributes.put(oc);
                attributes.put(OU, USERS_SUBTREE_NAME);

                createCtx = ctx.createSubcontext(OU + "=" + USERS_SUBTREE_NAME, attributes);

                if (LOG.isDebugEnabled()) {
                    LOG.debug("LDAP server {} entries sub tree initialized", USERS_SUBTREE_NAME);
                }
            }
        } finally {
            close(searchCtx);
            close(createCtx);
            close(ctx);
        }
    }

    /**
     * Gets URL which LDAP service uses to communicate with LDAP server
     *
     * @return URL value
     */
    public String getUrl() {
        return env.get(Context.PROVIDER_URL);
    }

    /**
     * Gets LDAP context using environmental properties defined during LdapService construction.
     *
     * @return LDAP context
     * @throws NamingException
     *         if issue occurred during LdapService and LDAP server interaction if any error occurred during initial LDAP
     *         context looking up
     */
    public LdapContext getLdapContext() throws NamingException {
        return new InitialLdapContext(new Hashtable<>(env), null);
    }

    /**
     * Close LDAP context
     *
     * @param ctx
     *         LDAP context to be closed
     */
    public void close(Context ctx) {
        if (ctx != null) {
            try {
                ctx.close();
            } catch (NamingException e) {
                LOG.error(e.getLocalizedMessage(), e);
            }
        }
    }

    public void close(NamingEnumeration<SearchResult> namingEnumeration) {
        if (namingEnumeration != null) {
            try {
                namingEnumeration.close();
            } catch (NamingException e) {
                LOG.error(e.getLocalizedMessage(), e);
            }
        }
    }

    /**
     * Creates LDAP entry with given absolute distinguished name (taking into account that URL in {@link Context#PROVIDER_URL} property is
     * the base, you can getById this value using {@link LdapService#getUrl()}) and attributes.
     *
     * @param distinguishedName
     *         distinguished name
     * @param attributes
     *         attributes
     * @throws NamingException
     *         if issue occurred during LdapService and LDAP server interaction
     */
    public void createLdapEntry(String distinguishedName, BasicAttributes attributes) throws NamingException {
        LdapContext ctx = null;
        DirContext newCtx = null;
        try {
            ctx = getLdapContext();
            newCtx = ctx.createSubcontext(distinguishedName, attributes);
        } finally {
            close(newCtx);
            close(ctx);
        }
    }

    /**
     * Searches entries in LDAP hierarchy starting from base distinguished name with the given objectClass attribute value and scope.
     *
     * @param objectClass
     *         objectClass attribute
     * @param baseDN
     *         base distinguished name
     * @param scope
     *         scope
     * @return entries satisfying objectClass
     * @throws NamingException
     *         if issue occurred during LdapService and LDAP server interaction
     */
    public NamingEnumeration<SearchResult> searchEntriesByObjectClass(String objectClass, String baseDN, int scope)
            throws NamingException {
        LdapContext ctx = null;
        try {
            ctx = getLdapContext();
            SearchControls constraints = new SearchControls();
            constraints.setSearchScope(scope);

            String filter = "(" + "objectClass=" + objectClass + ")";

            return ctx.search(baseDN, filter, constraints);
        } finally {
            close(ctx);
        }
    }

    /**
     * Searches entries in LDAP hierarchy starting from base distinguished name with the given objectClass attribute value, distinguished
     * name and scope.
     *
     * @param name
     *         distinguished name
     * @param objectClass
     *         objectClass attribute
     * @param baseDN
     *         base distinguished name
     * @param scope
     *         scope
     * @return entries satisfying objectClass
     * @throws NamingException
     *         if issue occurred during LdapService and LDAP server interaction
     */
    public NamingEnumeration<SearchResult> searchEntryByNameAndObjectClass(String name, String objectClass,
                                                                           String baseDN,
                                                                           int scope) throws NamingException {
        LdapContext ctx = null;
        try {
            ctx = getLdapContext();
            SearchControls constraints = new SearchControls();
            constraints.setSearchScope(scope);

            String filter = "(&(" + name + ")" + "(" + "objectClass=" + objectClass + "))";

            return ctx.search(baseDN, filter, constraints);
        } finally {
            close(ctx);
        }

    }

    /**
     * Searches entries in LDAP hierarchy starting from base distinguished name with the given objectClass attribute value, distinguished
     * name and scope.
     *
     * @param attributes
     *         attributes to be searched by
     * @param baseDN
     *         base distinguished name
     * @param scope
     *         scope
     * @return entries satisfying objectClass
     * @throws NamingException
     *         if issue occurred during LdapService and LDAP server interaction
     */
    public NamingEnumeration<SearchResult> searchEntryByAttributes(Map<String, String> attributes,
                                                                   String baseDN,
                                                                   int scope) throws NamingException {
        LdapContext ctx = null;

        try {
            ctx = getLdapContext();
            SearchControls constraints = new SearchControls();
            constraints.setSearchScope(scope);

            StringBuilder criteria = new StringBuilder();
            for (Map.Entry<String, String> attribute : attributes.entrySet()) {
                criteria.append('(');
                criteria.append(attribute.getKey());
                criteria.append('=');
                criteria.append(attribute.getValue());
                criteria.append(')');
            }
            // this should be probably generalized to be able to use different logical operators
            String filter = "(&" + criteria.toString() + ")";

            return ctx.search(baseDN, filter, constraints);
        } finally {
            close(ctx);
        }

    }

    /**
     * Removes LDAP entry by absolute distinguished name
     *
     * @param name
     *         absolute distinguished name
     * @throws NamingException
     *         if issue occurred during LdapService and LDAP server interaction
     */
    public void removeEntryByName(String name) throws NamingException {
        LdapContext ctx = null;
        try {
            ctx = getLdapContext();
            ctx.destroySubcontext(name);
        } finally {
            close(ctx);
        }
    }
}
