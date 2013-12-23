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

import java.util.Set;

/** @author andrew00x */
public interface User {
    /** Get user name. */
    String getName();

    /** Get user roles. Collection is unmodifiable. */
    Set<String> getRoles();

    /**
     * Check is user in specified {@code role}.
     *
     * @param role
     *         role name to check
     * @return {@code true} if user in role and {@code false} otherwise
     */
    boolean isMemberOf(String role);
}
