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
package org.eclipse.che.api.auth.shared.dto;

import org.eclipse.che.dto.shared.DTO;

import java.util.Map;

/**
 * @author Sergii Kabashniuk
 */
@DTO
public interface AuthorizationTokenRequest {
    String getRequestedRole();

    void setRequestedRole(String requestedRole);

    AuthorizationTokenRequest withRequestedRole(String requestedRole);

    String getInvokedMethod();

    void setInvokedMethod(String invokedMethod);

    AuthorizationTokenRequest withInvokedMethod(String invokedMethod);


    String getInvokedResource();

    void setInvokedResource(String invokedResource);

    AuthorizationTokenRequest withInvokedResource(String invokedResource);


    Map<String, String> getAttributes();

    void setAttributes(Map<String, String> attributes);

    AuthorizationTokenRequest withAttributes(Map<String, String> attributes);

}
