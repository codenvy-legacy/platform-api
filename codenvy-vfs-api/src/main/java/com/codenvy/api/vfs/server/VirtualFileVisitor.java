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
package com.codenvy.api.vfs.server;

import com.codenvy.api.vfs.server.exceptions.VirtualFileSystemException;

/**
 * This interface defines the visit method. When an implementation of this interface is passed to {@link
 * VirtualFile#accept(VirtualFileVisitor)} the <code>visit</code> method is called.
 *
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 */
public interface VirtualFileVisitor {
    /**
     * This method is called when the VirtualFileVisitor is passed to the {@link VirtualFile#accept(VirtualFileVisitor) accept} method of a
     * {@link VirtualFile}.
     *
     * @param virtualFile
     *         VirtualFile which is accepting this visitor
     * @throws VirtualFileSystemException
     *         if an error occurs
     */
    void visit(VirtualFile virtualFile) throws VirtualFileSystemException;
}
