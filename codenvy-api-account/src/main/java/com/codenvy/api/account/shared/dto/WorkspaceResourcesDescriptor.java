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
package com.codenvy.api.account.shared.dto;

import com.codenvy.dto.shared.DTO;
import com.wordnik.swagger.annotations.ApiModelProperty;

import java.util.Map;

/**
 * @author Sergii Leschenko
 */
@DTO
public interface WorkspaceResourcesDescriptor {
    @ApiModelProperty(value = "Workspace ID")
    String getWorkspaceId();

    void setWorkspaceId(String workspaceId);

    WorkspaceResourcesDescriptor withWorkspaceId(String workspaceId);

    Map<String, String> getResources();

    void setResources(Map<String, String> resources);

    WorkspaceResourcesDescriptor withResources(Map<String, String> resources);
}
