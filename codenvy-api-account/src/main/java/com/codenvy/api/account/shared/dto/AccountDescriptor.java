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
import java.util.Map;

/**
 * @author andrew00x
 */
@DTO
public interface AccountDescriptor {

    Map<String, String> getAttributes();

    void setAttributes(Map<String, String> attributes);

    AccountDescriptor withAttributes(Map<String, String> attributes);

    String getName();

    void setName(String name);

    AccountDescriptor withName(String name);

    String getId();

    void setId(String id);

    AccountDescriptor withId(String id);

    List<Link> getLinks();

    void setLinks(List<Link> links);

    AccountDescriptor withLinks(List<Link> links);
}
