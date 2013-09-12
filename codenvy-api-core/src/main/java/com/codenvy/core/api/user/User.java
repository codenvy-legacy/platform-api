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
package com.codenvy.core.api.user;

import java.util.HashSet;
import java.util.Set;

/** @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a> */
public class User {
    private final String      name;
    private final Set<String> roles;

    public User(String name, Set<String> roles) {
        this.name = name;
        if (roles != null) {
            this.roles = new HashSet<>(roles);
        } else {
            this.roles = new HashSet<>(2);
        }
    }

    public String getName() {
        return name;
    }

    /** Get roles of user. Modification of returned {@code Set} must not effect internal {@code Set}. */
    public Set<String> getRoles() {
        return new HashSet<>(roles);
    }

    /**
     * Check is user in specified {@code role}.
     *
     * @param role
     *         role name to check
     * @return {@code true} if user in role and {@code false} otherwise
     */
    public boolean isMemberOf(String role) {
        return roles.contains(role);
    }
}
