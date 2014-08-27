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
import com.wordnik.swagger.annotations.ApiModelProperty;

import java.util.List;

/**
 * @author Eugene Voevodin
 */
@DTO
public interface AccountReference {

    @ApiModelProperty(value = "Account name")
    String getName();

    void setName(String name);

    AccountReference withName(String name);

    @ApiModelProperty(value = "Account ID")
    String getId();

    void setId(String id);

    AccountReference withId(String id);

    List<Link> getLinks();

    void setLinks(List<Link> links);

    AccountReference withLinks(List<Link> links);
}
