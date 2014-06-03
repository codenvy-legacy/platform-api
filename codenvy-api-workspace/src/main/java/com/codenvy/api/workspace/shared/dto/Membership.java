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
package com.codenvy.api.workspace.shared.dto;

import com.codenvy.api.core.rest.shared.dto.Link;
import com.codenvy.dto.shared.DTO;

import java.util.List;

/**
 * Describes workspace membership
 *
 * @author Eugene Voevodin
 * @see com.codenvy.api.workspace.shared.dto.Workspace
 */
@DTO
public interface Membership {

    WorkspaceRef getWorkspaceRef();

    void setWorkspaceRef(WorkspaceRef ref);

    Membership withWorkspaceRef(WorkspaceRef ref);

    List<String> getRoles();

    void setRoles(List<String> roles);

    Membership withRoles(List<String> roles);

    Link getUserLink();

    void setUserLink(Link link);

    Membership withUserLink(Link link);
}
