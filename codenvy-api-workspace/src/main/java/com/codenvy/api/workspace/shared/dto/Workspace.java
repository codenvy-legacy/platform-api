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
 * @author andrew00x
 */
@DTO
public interface Workspace {
    String getId();

    void setId(String id);

    Workspace withId(String id);

    String getName();

    void setName(String name);

    Workspace withName(String name);

    void setTemporary(boolean temporary);

    boolean isTemporary();

    Workspace withTemporary(boolean temporary);

    String getAccountId();

    void setAccountId(String accountId);

    Workspace withAccountId(String accountId);

    List<Attribute> getAttributes();

    void setAttributes(List<Attribute> attributes);

    Workspace withAttributes(List<Attribute> attributes);

    List<Link> getLinks();

    void setLinks(List<Link> links);

    Workspace withLinks(List<Link> links);
}
