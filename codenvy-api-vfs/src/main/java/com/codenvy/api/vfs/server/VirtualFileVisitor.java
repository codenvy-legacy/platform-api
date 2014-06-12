/*******************************************************************************
 * Copyright (c) 2012-2014 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
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
