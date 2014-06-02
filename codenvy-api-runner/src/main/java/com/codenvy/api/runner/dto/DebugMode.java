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
package com.codenvy.api.runner.dto;

import com.codenvy.dto.shared.DTO;

/**
 * Describes debug mode of an application.
 *
 * @author andrew00x
 * @see #getMode()
 */
@DTO
public interface DebugMode {
    /** Debugger mode. If {@code null} Runner uses default debug mode. Default mode depends to Runner implementation. */
    String getMode();

    void setMode(String mode);

    DebugMode withMode(String mode);
}
