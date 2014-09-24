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
package com.codenvy.api.user.shared.dto;

import com.codenvy.api.core.rest.shared.dto.Link;
import com.codenvy.dto.shared.DTO;
import com.wordnik.swagger.annotations.ApiModelProperty;

import java.util.List;

/**
 * @author andrew00x
 */
@DTO
public interface UserDescriptor {
    @ApiModelProperty(value = "User ID")
    String getId();

    void setId(String id);

    UserDescriptor withId(String id);

    List<String> getAliases();

    void setAliases(List<String> aliases);

    UserDescriptor withAliases(List<String> aliases);

    @ApiModelProperty(value = "User email")
    String getEmail();

    void setEmail(String email);

    UserDescriptor withEmail(String email);

    @ApiModelProperty(value = "User password")
    String getPassword();

    void setPassword(String password);

    UserDescriptor withPassword(String password);

    List<Link> getLinks();

    void setLinks(List<Link> links);

    UserDescriptor withLinks(List<Link> links);
}
