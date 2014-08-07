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
package com.codenvy.api.project.server;

import com.codenvy.api.core.rest.annotations.GenerateLink;
import com.codenvy.api.project.shared.dto.ProjectTypeDescriptor;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;

/**
 * ProjectDescriptionService
 *
 * @author gazarenkov
 * @deprecated use ProjectTypeService instead
 */
@Path("project-description")
public class ProjectTypeDescriptionService extends ProjectTypeService {
    @Inject
    public ProjectTypeDescriptionService(ProjectTypeDescriptionRegistry registry) {
        super(registry);
    }

    @GenerateLink(rel = Constants.LINK_REL_PROJECT_TYPES)
    @GET
    @Path("descriptions")
    @Produces(MediaType.APPLICATION_JSON)
    public List<ProjectTypeDescriptor> getProjectTypes() {
        return super.getProjectTypes();
    }
}