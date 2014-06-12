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
package com.codenvy.api.vfs.server.exceptions;

/**
 * Thrown if object on which the action performed is locked.
 *
 * @author <a href="mailto:andrey.parfonov@exoplatform.com">Andrey Parfonov</a>
 */
@SuppressWarnings("serial")
public class LockException extends VirtualFileSystemException {
    /**
     * @param message
     *         the detail message
     */
    public LockException(String message) {
        super(message);
    }
}
