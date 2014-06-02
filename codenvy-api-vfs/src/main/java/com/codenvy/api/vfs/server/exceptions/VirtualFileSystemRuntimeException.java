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
 * Should be thrown for any errors that are not expressible by another VFS (Virtual File System) exception. Used as base class for
 * any VFS unchecked exceptions.
 *
 * @author <a href="mailto:andrey.parfonov@exoplatform.com">Andrey Parfonov</a>
 */
@SuppressWarnings("serial")
public class VirtualFileSystemRuntimeException extends RuntimeException {
    /**
     * @param message
     *         the detail message
     * @param cause
     *         the cause
     */
    public VirtualFileSystemRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * @param message
     *         the detail message
     */
    public VirtualFileSystemRuntimeException(String message) {
        super(message);
    }

    /**
     * @param cause
     *         the cause
     */
    public VirtualFileSystemRuntimeException(Throwable cause) {
        super(cause);
    }
}
