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

import java.util.List;
import java.util.Map;

/**
 * @author Sergii Leschenko
 */
@DTO
public interface UsedResources {
    Map<String, String> getByAccount();

    void setByAccount(Map<String, String> byAccount);

    UsedResources withByAccount(Map<String, String> byAccount);

    List<WorkspaceResourcesDescriptor> getByWorkspaces();

    void setByWorkspaces(List<WorkspaceResourcesDescriptor> byWorkspaces);

    UsedResources withByWorkspaces(List<WorkspaceResourcesDescriptor> byWorkspaces);
}
