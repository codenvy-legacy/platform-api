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
public interface Account {

    List<Attribute> getAttributes();

    void setAttributes(List<Attribute> attributes);

    Account withAttributes(List<Attribute> attributes);

    String getName();

    void setName(String name);

    Account withName(String name);

    String getId();

    void setId(String id);

    Account withId(String id);

    List<Link> getLinks();

    void setLinks(List<Link> links);

    Account withLinks(List<Link> links);
}
