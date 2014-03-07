/*
 * CODENVY CONFIDENTIAL
 * __________________
 *
 *  [2012] - [2013] Codenvy, S.A.
 *  All Rights Reserved.
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