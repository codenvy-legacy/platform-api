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
package com.codenvy.api.vfs.server.exceptions;

/**
 * Should be used as base class for any VFS (Virtual File System) checked exception.
 *
 * @author <a href="mailto:andrey.parfonov@exoplatform.com">Andrey Parfonov</a>
 */
@SuppressWarnings("serial")
public class VirtualFileSystemException extends Exception {
    /**
     * @param message
     *         the detail message
     * @param cause
     *         the cause
     */
    public VirtualFileSystemException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * @param message
     *         the detail message
     */
    public VirtualFileSystemException(String message) {
        super(message);
    }

    /**
     * @param cause
     *         the cause
     */
    public VirtualFileSystemException(Throwable cause) {
        super(cause);
    }
}
