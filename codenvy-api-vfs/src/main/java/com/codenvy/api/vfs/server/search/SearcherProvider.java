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
package com.codenvy.api.vfs.server.search;

import com.codenvy.api.vfs.server.MountPoint;
import com.codenvy.api.vfs.server.exceptions.VirtualFileSystemException;

/**
 * Manages instances of Searcher.
 *
 * @author andrew00x
 */
public interface SearcherProvider {
    /**
     * Get LuceneSearcher for specified MountPoint.
     *
     * @param mountPoint
     *         MountPoint
     * @param create
     *         <code>true</code> to create new Searcher if necessary; <code>false</code> to return <code>null</code> if Searcher is not
     *         initialized yet
     * @return <code>Searcher</code> or <code>null</code> if <code>create</code> is <code>false</code> and the Searcher is not initialized
     *         yet
     * @see com.codenvy.api.vfs.server.MountPoint
     */
    Searcher getSearcher(MountPoint mountPoint, boolean create) throws VirtualFileSystemException;
}
