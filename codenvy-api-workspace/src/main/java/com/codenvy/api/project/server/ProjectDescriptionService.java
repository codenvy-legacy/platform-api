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
import com.codenvy.api.project.shared.ProjectDescription;
import com.codenvy.api.project.shared.dto.AttributeDTO;
import com.codenvy.api.project.shared.dto.ProjectDescriptor;
import com.codenvy.dto.server.DtoFactory;

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
@Path("projectdescription")
public class ProjectDescriptionService extends Service {

    private ProjectDescriptionRegistry registry;

    public ProjectDescriptionService(ProjectDescriptionRegistry registry) {
        this.registry = registry;
    }

    @GET
    @Path("descriptions")
    @Produces(MediaType.APPLICATION_JSON)
    public List<ProjectDescriptor> getDescriptions()  {

        List<ProjectDescriptor> descriptors = new ArrayList<ProjectDescriptor> ();
        for(ProjectDescription description : this.registry.getDescriptions()) {

            List<AttributeDTO> attributes = new ArrayList<AttributeDTO>();
            for(Attribute attr : description.getAttributes()) {
                attributes.add(DtoFactory.getInstance().createDto(AttributeDTO.class)
                        .withName(attr.getName())
                        .withValue(attr.getValues()));
            }



            descriptors.add(DtoFactory.getInstance().createDto(ProjectDescriptor.class)
                .withAttributes(attributes)
                .withProjectTypeId(description.getProjectType().getId())
                .withProjectTypeName(description.getProjectType().getName()));
        }

        return descriptors;

    }

}