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
package com.codenvy.api.project.shared;

import com.codenvy.api.core.ConflictException;
import com.codenvy.api.core.rest.shared.dto.ServiceError;

/**
 *
 * Used when source of persisted value is invalid
 * For instance file not found or can not be read when expected
 *
 * @author gazarenkov
 */
public class ValueStorageException extends ConflictException {

    public ValueStorageException(String message) {
        super(message);
    }

    public ValueStorageException(ServiceError serviceError) {
        super(serviceError);
    }
}
