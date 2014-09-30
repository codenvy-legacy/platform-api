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
package com.codenvy.api.factory.converter;

import com.codenvy.api.core.ApiException;
import com.codenvy.api.core.NotFoundException;
import com.codenvy.api.factory.dto.Factory;
import com.codenvy.api.factory.dto.FactoryProject;
import com.codenvy.api.factory.dto.ProjectAttributes;
import com.codenvy.dto.server.DtoFactory;

/**
 * Move 'pname', 'projectattributes.pname' parameter into the 'project' object.
 *
 * @author Alexander Garagatyi
 */
public class ProjectNameConverter implements LegacyConverter {
    @Override
    public void convert(Factory factory) throws ApiException {
    }

    @Override
    public void convertToV1_2(Factory factory) throws ApiException {
        if (factory.getPname() != null) {
            ProjectAttributes attributes = factory.getProjectattributes();
            if (null == attributes || attributes.getPname() == null) {
                attributes =
                        attributes == null ? DtoFactory.getInstance().createDto(ProjectAttributes.class) : attributes;
                attributes.setPname(factory.getPname());
                factory.setPname(null);
                factory.setProjectattributes(attributes);
            } else {
                throw new ApiException("Parameters 'pname' and 'projectsttributes.pname' are mutually exclusive.");
            }
        }
    }
}
