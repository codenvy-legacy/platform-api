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

import com.codenvy.api.vfs.server.VirtualFile;
import com.codenvy.api.vfs.server.exceptions.VirtualFileSystemException;
import com.codenvy.api.vfs.server.search.QueryExpression;

public interface Searcher {
    /**
     * Return paths of matched items on virtual filesystem.
     *
     * @param query
     *         query expression
     * @return paths of matched items
     * @throws VirtualFileSystemException
     *         if an error occurs
     */
    String[] search(QueryExpression query) throws VirtualFileSystemException;

    /**
     * Add VirtualFile to index.
     *
     * @param virtualFile
     *         VirtualFile to add
     * @throws VirtualFileSystemException
     *         if an error occurs
     */
    void add(VirtualFile virtualFile) throws VirtualFileSystemException;

    /**
     * Delete VirtualFile to index.
     *
     * @param path
     *         path of VirtualFile
     * @throws VirtualFileSystemException
     *         if an error occurs
     */
    void delete(String path) throws VirtualFileSystemException;

    /**
     * Updated indexed VirtualFile.
     *
     * @param virtualFile
     *         VirtualFile to add
     * @throws VirtualFileSystemException
     *         if an error occurs
     */
    void update(VirtualFile virtualFile) throws VirtualFileSystemException;

    /** Close Searcher. */
    void close();
}