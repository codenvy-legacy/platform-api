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
package com.codenvy.api.project.server;

import com.codenvy.api.core.rest.Service;
import com.codenvy.api.project.shared.dto.ProjectImporterDescriptor;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.List;

/**
 * Provide information about registered ProjectImporter's via REST.
 *
 * @author Vitaly Parfonov
 */
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
        final List<ProjectImporter> importers = importersRegistry.getImporters();
        final List<ProjectImporterDescriptor> descriptors = new ArrayList<>(importers.size());
        for (ProjectImporter importer : importers) {
            descriptors.add(DtoConverter.toImporterDescriptor(importer));
        }
        return descriptors;
    }
}
