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
package com.codenvy.api.machine.shared.dto;

import com.codenvy.api.core.rest.shared.dto.Link;

import java.util.List;

/**
 * Describe application process inside of machine
 *
 * @author Alexander Garagatyi
 */
public interface ApplicationProcessDescriptor {
    int getId();

    void setId(String id);

    ApplicationProcessDescriptor withId(int id);

    List<Link> getLinks();

    void setLinks(List<Link> links);

    ApplicationProcessDescriptor withLinks(List<Link> links);
}
