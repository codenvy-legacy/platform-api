/*
 * CODENVY CONFIDENTIAL
 * __________________
 * 
 * [2012] - [$today.year] Codenvy, S.A. 
 * All Rights Reserved.
 * 
 * NOTICE:  All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */
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
                                                             .withDescription(importer.getDescription() != null ? importer
                                                                     .getDescription() : "description not found");
            descriptors.add(descriptor);
        }
        return descriptors;
    }
}
