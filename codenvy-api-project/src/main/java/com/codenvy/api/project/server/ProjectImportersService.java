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

import com.codenvy.api.core.rest.Service;
import com.codenvy.api.project.shared.dto.ProjectImporterDescriptor;
import com.codenvy.dto.server.DtoFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.List;

/**
 * Provide information about registered ProjectImporter's via REST
 *
 * @author Vitaly Parfonov
 */
@Singleton
@Path("project-importers")
public class ProjectImportersService extends Service {

    private final ProjectImporterRegistry importersRegistry;

    @Inject
    public ProjectImportersService(ProjectImporterRegistry importersRegistry) {
        this.importersRegistry = importersRegistry;
    }


    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<ProjectImporterDescriptor> getImporters() {
        List<ProjectImporterDescriptor> descriptors = new ArrayList<>();
        List<ProjectImporter> importers = importersRegistry.getImporters();
        for (ProjectImporter importer : importers) {
            ProjectImporterDescriptor descriptor = DtoFactory.getInstance().createDto(ProjectImporterDescriptor.class)
                                                             .withId(importer.getId())
                                                             .withInternal(importer.isInternal())
                                                             .withDescription(importer.getDescription() != null ? importer
                                                                     .getDescription() : "description not found");
            descriptors.add(descriptor);
        }
        return descriptors;
    }
}
