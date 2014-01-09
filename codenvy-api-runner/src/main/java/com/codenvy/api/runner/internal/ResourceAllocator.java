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
package com.codenvy.api.runner.internal;

/**
 * Abstraction for allocation and releasing resources.
 *
 * @author <a href="mailto:aparfonov@codenvy.com">Andrey Parfonov</a>
 */
public interface ResourceAllocator {
    /**
     * Allocate type resource managed by this instance.
     *
     * @return this instance
     * @throws AllocateResourceException
     *         if it is not possible to allocate resource
     */
    ResourceAllocator allocate() throws AllocateResourceException;

    /** Release type resource managed by this instance */
    void release();
}
