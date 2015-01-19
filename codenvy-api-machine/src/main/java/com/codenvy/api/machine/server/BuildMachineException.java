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
package com.codenvy.api.machine.server;

import com.codenvy.api.core.ServerException;
import com.codenvy.api.core.rest.shared.dto.ServiceError;

/**
 * @author andrew00x
 */
@SuppressWarnings("serial")
public class BuildMachineException extends ServerException {
    public BuildMachineException(String message) {
        super(message);
    }

    public BuildMachineException(ServiceError serviceError) {
        super(serviceError);
    }

    public BuildMachineException(Throwable cause) {
        super(cause);
    }

    public BuildMachineException(String message, Throwable cause) {
        super(message, cause);
    }
}
