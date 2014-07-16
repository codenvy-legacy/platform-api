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

import com.codenvy.api.core.rest.shared.dto.Link;
import com.codenvy.dto.shared.DTO;

import java.util.List;

/**
 * @author andrew00x
 */
@DTO
public interface MemberDescriptor {
    List<String> getRoles();

    void setRoles(List<String> roles);

    MemberDescriptor withRoles(List<String> roles);

    String getUserId();

    void setUserId(String id);

    MemberDescriptor withUserId(String id);

    String getAccountId();

    void setAccountId(String id);

    MemberDescriptor withAccountId(String id);

    List<Link> getLinks();

    void setLinks(List<Link> links);

    MemberDescriptor withLinks(List<Link> links);
}
