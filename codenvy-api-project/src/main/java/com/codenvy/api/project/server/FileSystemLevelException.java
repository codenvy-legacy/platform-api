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
package com.codenvy.api.project.server;

import com.codenvy.api.vfs.server.exceptions.VirtualFileSystemException;

/**
 * Wrapper for all virtual filesystem exceptions. Basically caller should not catch such exceptions.
 *
 * @author andrew00x
 */
public final class FileSystemLevelException extends RuntimeException {
    public FileSystemLevelException(String message, VirtualFileSystemException cause) {
        super(message, cause);
    }
}
