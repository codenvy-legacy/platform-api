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
import com.codenvy.api.project.shared.Attribute;
import com.codenvy.api.project.shared.ProjectType;
import com.codenvy.api.project.shared.ProjectTypeDescription;
import com.codenvy.api.project.shared.dto.ProjectTypeDescriptor;
import com.codenvy.dto.server.DtoFactory;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ProjectDescriptionService
 *
 * @author gazarenkov
 */
@Path("projectdescription") // TODO: nice name in url pattern
public class ProjectTypeDescriptionService extends Service {
    @Inject
    private ProjectTypeDescriptionRegistry registry;

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
            final Map<String, List<String>> attributeValues = new HashMap<>();
            for (Attribute attribute : typeDescription.getAttributes()) {
                attributeValues.put(attribute.getName(), attribute.getValues());
            }
            descriptor.setAttributes(attributeValues);
            types.add(descriptor);
        }
        return types;
    }
}