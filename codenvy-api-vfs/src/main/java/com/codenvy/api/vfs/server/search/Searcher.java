/*
 * CODENVY CONFIDENTIAL
 * __________________
 *
 * [2012] - [2013] Codenvy, S.A.
 * All Rights Reserved.
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