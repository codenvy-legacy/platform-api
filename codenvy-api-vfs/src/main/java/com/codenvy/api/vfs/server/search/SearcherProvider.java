/*******************************************************************************
* Copyright (c) 2012-2014 Codenvy, S.A.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
* Codenvy, S.A. - initial API and implementation
*******************************************************************************/
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
