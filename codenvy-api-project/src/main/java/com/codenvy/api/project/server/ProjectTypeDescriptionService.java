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
import com.codenvy.api.core.rest.annotations.GenerateLink;
import com.codenvy.api.project.shared.AttributeDescription;
import com.codenvy.api.project.shared.ProjectTemplateDescription;
import com.codenvy.api.project.shared.ProjectType;
import com.codenvy.api.project.shared.ProjectTypeDescription;
import com.codenvy.api.project.shared.dto.AttributeDescriptor;
import com.codenvy.api.project.shared.dto.ImportSourceDescriptor;
import com.codenvy.api.project.shared.dto.ProjectTemplateDescriptor;
import com.codenvy.api.project.shared.dto.ProjectTypeDescriptor;
import com.codenvy.dto.server.DtoFactory;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.List;

/**
 * ProjectDescriptionService
 *
 * @author gazarenkov
 */
@Path("project-description")
public class ProjectTypeDescriptionService extends Service {
    @Inject
    private ProjectTypeDescriptionRegistry registry;

    @GenerateLink(rel = Constants.LINK_REL_PROJECT_TYPES)
    @GET
    @Path("descriptions")
    @Produces(MediaType.APPLICATION_JSON)
    public List<ProjectTypeDescriptor> getProjectTypes() {
        final List<ProjectTypeDescriptor> types = new ArrayList<>();
        for (ProjectTypeDescription typeDescription : registry.getDescriptions()) {
            final ProjectType projectType = typeDescription.getProjectType();
            final ProjectTypeDescriptor descriptor = DtoFactory.getInstance().createDto(ProjectTypeDescriptor.class);
            descriptor.setProjectTypeId(projectType.getId());
            descriptor.setProjectTypeName(projectType.getName());
            descriptor.setProjectTypeCategory(projectType.getCategory());
            final List<AttributeDescriptor> attributeDescriptors = new ArrayList<>();
            for (AttributeDescription attributeDescription : typeDescription.getAttributeDescriptions()) {
                attributeDescriptors.add(DtoFactory.getInstance().createDto(AttributeDescriptor.class)
                                                   .withName(attributeDescription.getName()));
            }
            descriptor.setAttributeDescriptors(attributeDescriptors);
            final List<ProjectTemplateDescriptor> templateDescriptors = new ArrayList<>();
            for (ProjectTemplateDescription templateDescription : registry.getTemplates(projectType)) {
                ProjectTemplateDescriptor templateDescriptor = DtoFactory.getInstance().createDto(ProjectTemplateDescriptor.class)
                        .withDisplayName(templateDescription.getDisplayName())
                        .withSources(DtoFactory.getInstance().createDto(ImportSourceDescriptor.class)
                                               .withType(templateDescription.getImporterType())
                                               .withLocation(templateDescription.getLocation()))
                        .withDescription(templateDescription.getDescription());
                templateDescriptors.add(templateDescriptor);
            }
            descriptor.setTemplates(templateDescriptors);
            types.add(descriptor);
        }
        return types;
    }
}