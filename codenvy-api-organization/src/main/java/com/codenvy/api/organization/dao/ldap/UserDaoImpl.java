/*
 * CODENVY CONFIDENTIAL
 * __________________
 *
 *  [2012] - [2014] Codenvy, S.A.
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
import com.codenvy.api.user.server.exception.UserException;
import com.codenvy.api.user.server.exception.UserNotFoundException;
import com.codenvy.api.user.shared.dto.User;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.naming.Context;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;
import java.util.Hashtable;

/**
 * LDAP based implementation of {@code UserDao}.
 *
 * @author andrew00x
 */
@Singleton
public class UserDaoImpl implements UserDao {
    private static final Logger LOG = LoggerFactory.getLogger(UserDaoImpl.class);

    private final String providerUrl;
    private final String systemDn;
    private final String systemPassword;
    private final String authType;
    private final String usePool;
    private final String initPoolSize;
    private final String maxPoolSize;
    private final String prefPoolSize;
    private final String poolTimeout;
    private final String userContainerDn;

    private final UserAttributesMapper userAttributesMapper;
    private final String               userObjectclassFilter;

    /**
     * Creates new instance of {@code UserDaoImpl}.
     *
     * @param providerUrl
     *         URL of LDAP service provider, e.g. {@code ldap://localhost:389}.
     * @param systemDn
     *         principal used to open LDAP connection, e.g. {@code cn=Admin,ou=system,dc=codenvy,dc=com}. May be omitted if authentication
     *         is not needed, e.g. in tests. See {@link javax.naming.Context#SECURITY_PRINCIPAL}.
     * @param systemPassword
     *         password of principal to open LDAP connection.  May be omitted if authentication is not needed, e.g. in tests. See {@link
     *         javax.naming.Context#SECURITY_CREDENTIALS} .
     * @param authType
     *         authentication type, see {@link javax.naming.Context#SECURITY_AUTHENTICATION}
     * @param usePool
     *         setup policy for connection pooling. Allowed value of this parameter is "true" or "false". See <a
     *         href="http://docs.oracle.com/javase/jndi/tutorial/ldap/connect/config.html">details</a> about connection pooling.
     * @param initPoolSize
     *         initial size of connection pool. Parameter MUST be string representation of an integer. Make sense ONLY if parameter {@code
     *         usePool} is equals to "true".
     * @param maxPoolSize
     *         max size for connection poll. Parameter MUST be string representation of an integer. Make sense ONLY if parameter {@code
     *         usePool} is equals to "true".
     * @param prefPoolSize
     *         preferred size for connection poll. Parameter MUST be string representation of an integer. Make sense ONLY if parameter
     *         {@code usePool} is equals to "true". Often this parameter may be omitted.
     * @param poolTimeout
     *         time (in milliseconds) that an idle connection may remain in the pool. Parameter MUST be string representation of an
     *         integer.
     *         Make sense ONLY if parameter {@code usePool} is equals to "true".
     * @param userContainerDn
     *         full name of root object for user records, e.g. {@code ou=People,dc=codenvy,dc=com}
     * @param userAttributesMapper
     *         UserAttributesMapper
     */
    @Inject
    public UserDaoImpl(@Named(Context.PROVIDER_URL) String providerUrl,
                       @Nullable @Named(Context.SECURITY_PRINCIPAL) String systemDn,
                       @Nullable @Named(Context.SECURITY_CREDENTIALS) String systemPassword,
                       @Nullable @Named(Context.SECURITY_AUTHENTICATION) String authType,
                       @Nullable @Named("com.sun.jndi.ldap.connect.pool") String usePool,
                       @Nullable @Named("com.sun.jndi.ldap.connect.pool.initsize") String initPoolSize,
                       @Nullable @Named("com.sun.jndi.ldap.connect.pool.maxsize") String maxPoolSize,
                       @Nullable @Named("com.sun.jndi.ldap.connect.pool.prefsize") String prefPoolSize,
                       @Nullable @Named("com.sun.jndi.ldap.connect.pool.timeout") String poolTimeout,
                       @Named("user.ldap.user_container_dn") String userContainerDn,
                       UserAttributesMapper userAttributesMapper) {
        this.providerUrl = providerUrl;
        this.systemDn = systemDn;
        this.systemPassword = systemPassword;
        this.authType = authType;
        this.usePool = usePool;
        this.initPoolSize = initPoolSize;
        this.maxPoolSize = maxPoolSize;
        this.prefPoolSize = prefPoolSize;
        this.poolTimeout = poolTimeout;
        this.userContainerDn = userContainerDn;
        this.userAttributesMapper = userAttributesMapper;
        StringBuilder sb = new StringBuilder();
        for (String objectClass : userAttributesMapper.userObjectClasses) {
            sb.append("(objectClass=");
            sb.append(objectClass);
            sb.append(')');
        }
        this.userObjectclassFilter = sb.toString();
    }

    UserDaoImpl(@Named(Context.PROVIDER_URL) String providerUrl,
                @Nullable @Named(Context.SECURITY_PRINCIPAL) String systemDn,
                @Nullable @Named(Context.SECURITY_CREDENTIALS) String systemPassword,
                @Nullable @Named(Context.SECURITY_AUTHENTICATION) String authType,
                @Named("user.ldap.user_container_dn") String userContainerDn,
                UserAttributesMapper userAttributesMapper) {
        this(providerUrl, systemDn, systemPassword, authType, null, null, null, null, null, userContainerDn, userAttributesMapper);
    }

    UserDaoImpl(@Named(Context.PROVIDER_URL) String providerUrl,
                @Named("user.ldap.user_container_dn") String userContainerDn,
                UserAttributesMapper userAttributesMapper) {
        this(providerUrl, null, null, null, null, null, null, null, null, userContainerDn, userAttributesMapper);
    }

    protected InitialLdapContext getLdapContext() throws NamingException {
        Hashtable<String, String> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, providerUrl);
        if (authType != null) {
            env.put(Context.SECURITY_AUTHENTICATION, authType);
        }
        if (systemDn != null) {
            env.put(Context.SECURITY_PRINCIPAL, systemDn);
        }
        if (systemPassword != null) {
            env.put(Context.SECURITY_CREDENTIALS, systemPassword);
        }
        if ("true".equalsIgnoreCase(usePool)) {
            env.put("com.sun.jndi.ldap.connect.pool", "true");
            if (initPoolSize != null) {
                env.put("com.sun.jndi.ldap.connect.pool.initsize", initPoolSize);
            }
            if (maxPoolSize != null) {
                env.put("com.sun.jndi.ldap.connect.pool.maxsize", maxPoolSize);
            }
            if (prefPoolSize != null) {
                env.put("com.sun.jndi.ldap.connect.pool.prefsize", prefPoolSize);
            }
            if (poolTimeout != null) {
                env.put("com.sun.jndi.ldap.connect.pool.timeout", poolTimeout);
            }
        }
        return new InitialLdapContext(env, null);
    }

    @Override
    public void create(User user) throws UserException {
        InitialLdapContext context = null;
        try {
            context = getLdapContext();
            for (String alias : user.getAliases()) {
                if (getUserAttributesByAlias(context, alias) != null) {
                    throw new UserException(
                            String.format("Unable create new user '%s'. User alias %s is already in use.", user.getEmail(), alias));
                }
            }
            DirContext newContext = null;
            try {
                newContext = context.createSubcontext(getUserDn(user.getId()), userAttributesMapper.toAttributes(user));
            } finally {
                close(newContext);
            }
        } catch (javax.naming.NameAlreadyBoundException e) {
            throw new UserException(String.format("Unable create new user '%s'. User already exists.", user.getId()), e);
        } catch (NamingException e) {
            e.printStackTrace();
            throw new UserException(String.format("Unable create new user '%s'", user.getEmail()), e);
        } finally {
            close(context);
        }
    }

    @Override
    public void update(User user) throws UserException {
        final String userId = user.getId();
        InitialLdapContext context = null;
        try {
            context = getLdapContext();
            final Attributes existingUserAttributes = getUserAttributesById(context, userId);
            if (existingUserAttributes == null) {
                throw new UserNotFoundException(userId);
            }
            for (String alias : user.getAliases()) {
                if (getUserAttributesByAlias(context, alias) != null) {
                    throw new UserException(
                            String.format("Unable update user '%s'. User alias %s is already in use.", user.getId(), alias));
                }
            }
            final ModificationItem[] mods =
                    userAttributesMapper.createModifications(userAttributesMapper.fromAttributes(existingUserAttributes), user);
            if (mods.length > 0) {
                context.modifyAttributes(getUserDn(userId), mods);
            }
        } catch (NamingException e) {
            throw new UserException(String.format("Unable create new user '%s'", user.getEmail()), e);
        } finally {
            close(context);
        }
    }

    @Override
    public void remove(String id) throws UserException {
        InitialLdapContext context = null;
        try {
            context = getLdapContext();
            context.destroySubcontext(getUserDn(id));
        } catch (NameNotFoundException e) {
            throw new UserNotFoundException(id);
        } catch (NamingException e) {
            throw new UserException(String.format("Unable remove user '%s'", id), e);
        } finally {
            close(context);
        }
    }

    @Override
    public User getByAlias(String alias) throws UserException {
        InitialLdapContext context = null;
        try {
            context = getLdapContext();
            final Attributes attributes = getUserAttributesByAlias(context, alias);
            if (attributes == null) {
                return null;
            }
            return userAttributesMapper.fromAttributes(attributes);
        } catch (NamingException e) {
            throw new UserException(String.format("Unable get user '%s'", alias), e);
        } finally {
            close(context);
        }
    }

    @Override
    public User getById(String id) throws UserException {
        InitialLdapContext context = null;
        try {
            context = getLdapContext();
            final Attributes attributes = getUserAttributesById(context, id);
            if (attributes == null) {
                return null;
            }
            return userAttributesMapper.fromAttributes(attributes);
        } catch (NamingException e) {
            throw new UserException(String.format("Unable get user '%s'", id), e);
        } finally {
            close(context);
        }
    }

    protected String getUserDn(String userId) {
        return userAttributesMapper.userDn + '=' + userId + ',' + userContainerDn;
    }

    protected Attributes getUserAttributesById(InitialLdapContext context, String userId) throws NamingException {
        try {
            return context.getAttributes(getUserDn(userId));
        } catch (NameNotFoundException e) {
            return null;
        }
    }

    protected Attributes getUserAttributesByAlias(InitialLdapContext context, String alias) throws NamingException {
        final SearchControls constraints = new SearchControls();
        constraints.setSearchScope(SearchControls.SUBTREE_SCOPE);
        final String filter = "(&(" + userAttributesMapper.userAliasesAttr + '=' + alias + ")(" + userObjectclassFilter + "))";
        NamingEnumeration<SearchResult> enumeration = null;
        try {
            enumeration = context.search(userContainerDn, filter, constraints);
            if (enumeration.hasMore()) {
                return enumeration.nextElement().getAttributes();
            }
            return null;
        } finally {
            close(enumeration);
        }
    }

    private void close(Context ctx) {
        if (ctx != null) {
            try {
                ctx.close();
            } catch (NamingException e) {
                LOG.error(e.getMessage(), e);
            }
        }
    }

    private void close(NamingEnumeration<SearchResult> enumeration) {
        if (enumeration != null) {
            try {
                enumeration.close();
            } catch (NamingException e) {
                LOG.error(e.getMessage(), e);
            }
        }
    }
}
