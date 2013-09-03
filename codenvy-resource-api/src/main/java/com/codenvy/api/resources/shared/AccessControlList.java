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

import java.util.List;

/**
 * Access Control List (ACL) of any Resource.
 *
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 */
public interface AccessControlList {
    /**
     * Get {@code Resource} to which this ACL belong to.
     *
     * @return {@code Resource} to which this ACL belong to
     */
    Resource getResource();

    /**
     * Get all Access Control Entries (ACE) of this ACL. Modifications to the returned {@code List} should not affect the internal state of
     * this object.
     *
     * @return all Access Control Entries (ACE) of this ACL
     */
    List<ResourceAccessControlEntry> getAll();

    /**
     * Get Access Control Entries (ACE) for specified {@code principal}.
     *
     * @param principal
     *         principal
     * @return Access Control Entries (ACE) for specified {@code principal}
     */
    ResourceAccessControlEntry getAccessControlEntry(Principal principal);

    /**
     * Save all changes of current ACL.
     *
     * @see com.codenvy.api.resources.shared.ResourceAccessControlEntry#isUpdated()
     */
    void save();
}
