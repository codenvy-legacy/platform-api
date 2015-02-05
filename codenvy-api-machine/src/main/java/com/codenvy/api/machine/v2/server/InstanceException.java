/*******************************************************************************
 * Copyright (c) 2012-2015 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package com.codenvy.api.machine.v2.server;

import com.codenvy.api.core.ServerException;
import com.codenvy.api.core.rest.shared.dto.ServiceError;

/**
 * @author gazarenkov
 */
@SuppressWarnings("serial")
public class InstanceException extends ServerException {
    public InstanceException(String message) {
        super(message);
    }

    public InstanceException(ServiceError serviceError) {
        super(serviceError);
    }

    public InstanceException(Throwable cause) {
        super(cause);
    }

    public InstanceException(String message, Throwable cause) {
        super(message, cause);
    }
}
