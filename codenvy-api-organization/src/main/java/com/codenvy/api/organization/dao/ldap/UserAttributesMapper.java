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

import com.codenvy.api.user.shared.dto.User;
import com.codenvy.dto.server.DtoFactory;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Mapper is used for mapping LDAP Attributes to/from {@code User} POJO.
 *
 * @author andrew00x
 */
@Singleton
public class UserAttributesMapper {

    final String[] userObjectClasses;
    final String   userDn;
    final String   userIdAttr;
    final String   userPasswordAttr;
    final String   userEmailAttr;
    final String   userAliasesAttr;

    /**
     * Basically for representing user in LDAP 'person', 'organizationalPerson' or 'inetOrgPerson' are used. Some of attributes may be
     * required by LDAP schemas but we may not have some of them in {@code User} abstraction. This attributes may have pre-configured with
     * default values.
     */
    private final Map<String, String> requiredAttributes;

    /**
     * Creates new instance of UserAttributesMapper.
     *
     * @param userObjectClasses
     *         values for objectClass attribute. Typical value is 'inetOrgPerson'.
     * @param userDn
     *         name of attribute that contains name of User object. Typical value is 'CN'.
     *         <p/>
     *         Example:
     *         Imagine this attribute is set to typical name 'CN' and full name of parent object for user records is 'dc=codenvy,dc=com'
     *         then full name to user record is 'CN=my_user,dc=codenvy,dc=com'.
     * @param userIdAttr
     *         name of attribute that contains ID of User object. Typical value is 'uid'.
     * @param userPasswordAttr
     *         name of attribute that contains password. Typical value is 'userPassword'.
     * @param userEmailAttr
     *         name of attribute that contains email address. Typical value is 'mail'.
     * @param userAliasesAttr
     *         name of attribute that contains password. Typical value is 'initials'.
     */
    public UserAttributesMapper(@Named("user.ldap.object_classes") String[] userObjectClasses,
                                @Named("user.ldap.user_dn") String userDn,
                                @Named("user.ldap.attr.id") String userIdAttr,
                                @Named("user.ldap.attr.password") String userPasswordAttr,
                                @Named("user.ldap.attr.email") String userEmailAttr,
                                @Named("user.ldap.attr.aliases") String userAliasesAttr) {
        this.userObjectClasses = userObjectClasses;
        this.userDn = userDn;
        this.userIdAttr = userIdAttr;
        this.userPasswordAttr = userPasswordAttr;
        this.userEmailAttr = userEmailAttr;
        this.userAliasesAttr = userAliasesAttr;
        requiredAttributes = new LinkedHashMap<>();
        addRequiredAttributesTo(userObjectClasses, requiredAttributes);
    }

    public UserAttributesMapper() {
        this(new String[]{"inetOrgPerson"}, "cn", "uid", "userPassword", "mail", "initials");
    }

    /**
     * Add mapping for required attributes. Such attributes are required by LDAP schema but may not be obtained directly or indirectly from
     * {@code User} instance. Such attributes will be added as in newly created user record in LDAP server.
     */
    protected void addRequiredAttributesTo(String[] objectClasses, Map<String, String> requiredAttributes) {
        for (String objectClass : objectClasses) {
            if ("inetOrgPerson".equalsIgnoreCase(objectClass)
                || "organizationalPerson".equalsIgnoreCase(objectClass)
                || "person".equalsIgnoreCase(objectClass)) {
                requiredAttributes.put("sn", "<none>");
                break;
            }
        }
    }

    /** Restores instance of {@code User} from LDAP {@code Attributes}. */
    public User fromAttributes(Attributes attributes) throws NamingException {
        final User user = DtoFactory.getInstance().createDto(User.class);
        user.setId(attributes.get(userIdAttr).get().toString());
        final Object obj = attributes.get(userPasswordAttr).get();
        if (obj instanceof byte[]) {
            user.setPassword(new String((byte[])obj));
        }
        user.setEmail(attributes.get(userEmailAttr).get().toString());
        final NamingEnumeration enumeration = attributes.get(userAliasesAttr).getAll();
        final List<String> aliases = new LinkedList<>();
        try {
            while (enumeration.hasMoreElements()) {
                String value = (String)enumeration.nextElement();
                aliases.add(value);
            }
        } finally {
            enumeration.close();
        }
        user.setAliases(aliases);
        return user;
    }

    /** Converts {@code User} instance to LDAP {@code Attributes}. */
    public Attributes toAttributes(User user) throws NamingException {
        final Attributes attributes = new BasicAttributes();
        final Attribute objectClassAttr = new BasicAttribute("objectClass");
        for (String oc : userObjectClasses) {
            objectClassAttr.add(oc);
        }
        attributes.put(objectClassAttr);
        attributes.put(userDn, user.getId());
        attributes.put(userIdAttr, user.getId());
        attributes.put(userEmailAttr, user.getEmail());
        attributes.put(userPasswordAttr, user.getPassword());
        final Attribute aliasesAttr = new BasicAttribute(userAliasesAttr);
        final List<String> aliases = user.getAliases();
        if (!aliases.isEmpty()) {
            for (String alias : aliases) {
                aliasesAttr.add(alias);
            }
        } else {
            aliasesAttr.add(user.getEmail());
        }
        attributes.put(aliasesAttr);
        for (Map.Entry<String, String> e : requiredAttributes.entrySet()) {
            attributes.put(e.getKey(), e.getValue());
        }
        return attributes;
    }

    /** Compares two {@code User} objects and provides diff of {@code ModificationItem[]} form. */
    public ModificationItem[] createModifications(User user1, User user2) {
        final List<ModificationItem> mods = new ArrayList<>(3);
        if (!user1.getEmail().equals(user2.getEmail())) {
            mods.add(new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute(userEmailAttr, user2.getEmail())));
        }
        if (!user1.getAliases().equals(user2.getAliases())) {
            final Attribute aliasesAttr = new BasicAttribute(userAliasesAttr);
            final List<String> aliases = user2.getAliases();
            if (!aliases.isEmpty()) {
                for (String alias : aliases) {
                    aliasesAttr.add(alias);
                }
            } else {
                aliasesAttr.add(user2.getEmail());
            }
            mods.add(new ModificationItem(DirContext.REPLACE_ATTRIBUTE, aliasesAttr));
        }
        if (!user1.getPassword().equals(user2.getPassword())) {
            mods.add(new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute(userPasswordAttr, user2.getPassword())));
        }
        return mods.toArray(new ModificationItem[mods.size()]);
    }
}
