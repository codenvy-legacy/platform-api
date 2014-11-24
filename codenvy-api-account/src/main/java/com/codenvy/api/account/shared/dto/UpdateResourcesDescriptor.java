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
package com.codenvy.api.account.shared.dto;

import com.codenvy.dto.shared.DTO;

import java.util.Map;

/**
 * //TODO
 *
 * @author Sergii Leschenko
 */
//TODO Mb rename this class
@DTO
public interface UpdateResourcesDescriptor {
    void setWorkspaceId(String workspaceId);

    String getWorkspaceId();

    UpdateResourcesDescriptor withWorkspaceId(String workspaceId);

    Map<String, String> getResources();

    void setResources(Map<String, String> resources);

    UpdateResourcesDescriptor withResources(Map<String, String> resources);
}