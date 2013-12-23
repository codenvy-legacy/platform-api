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
package com.codenvy.api.core.user;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Base implementation of User interface.
 *
 * @author andrew00x
 */
public class UserImpl implements User {
    private final String      name;
    private final Set<String> roles;

    public UserImpl(String name, Collection<String> roles) {
        this.name = name;
        this.roles = roles == null ? Collections.<String>emptySet() : Collections.unmodifiableSet(new LinkedHashSet<>(roles));
    }

    public UserImpl(String name) {
        this(name, null);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Set<String> getRoles() {
        return roles;
    }

    @Override
    public boolean isMemberOf(String role) {
        return roles.contains(role);
    }

    @Override
    public String toString() {
        return "UserImpl{" +
               "name='" + name + '\'' +
               ", roles=" + roles +
               '}';
    }
}
