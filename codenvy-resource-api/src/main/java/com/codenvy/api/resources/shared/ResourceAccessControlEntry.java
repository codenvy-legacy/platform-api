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
package com.codenvy.api.resources.shared;

import com.codenvy.api.vfs.shared.Principal;

import java.util.Collection;

/**
 * Item of {@link AccessControlList} which describes permission of single principal.
 *
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 */
public interface ResourceAccessControlEntry {
    /** Get Principal. */
    Principal getPrincipal();

    /**
     * Get set of permissions. Returned {@code Collection} is mutable and all changes SHOULD affect the internal state of this instance.
     * Implementation SHOULD track changes of returned collection of permissions and report about updates of it with method {@code
     * #isUpdated}. Implementation should never return {@code null} but empty {@code Collection} instead.
     */
    Collection<String> getPermissions();

    /**
     * Replace existed set of permissions with specified one. After call of this method {@link #isUpdated()} SHOULD return {@code true}
     *
     * @param permissions
     *         new set of permissions
     */
    void setPermissions(Collection<String> permissions);

    /** Check existing of specified permission. */
    boolean hasPermission(String permission);

    /**
     * Reports about updates of set of permissions. This method SHOULD return {@code true} if set of permission is updated or replaced.
     *
     * @see #getPermissions()
     * @see #setPermissions(java.util.Collection)
     */
    boolean isUpdated();
}
