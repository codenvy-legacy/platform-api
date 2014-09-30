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
package com.codenvy.api.factory.dto;

import com.codenvy.api.core.rest.shared.dto.Link;
import com.codenvy.dto.shared.DTO;

import java.util.List;

/**
 * Latest version of factory implementation.
 *
 * @author Alexander Garagatyi
 */
@DTO
public interface Factory extends FactoryV2_0 {
    List<Link> getLinks();

    void setLinks(List<Link> links);

    Factory withLinks(List<Link> links);
}
