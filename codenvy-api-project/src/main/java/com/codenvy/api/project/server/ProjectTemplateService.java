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
import com.codenvy.api.core.rest.annotations.Description;
import com.codenvy.api.core.rest.annotations.GenerateLink;
import com.codenvy.api.core.rest.annotations.Required;
import com.codenvy.api.project.shared.ProjectTemplateDescription;
import com.codenvy.api.project.shared.dto.ProjectDescriptor;
import com.codenvy.api.project.shared.dto.ProjectTemplateDescriptor;
import com.codenvy.api.vfs.server.VirtualFileSystemRegistry;
import com.codenvy.api.vfs.server.exceptions.VirtualFileSystemException;
import com.codenvy.dto.server.DtoFactory;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** @author Vitaly Parfonov */
@Path("project-template/{ws-id}")
public class ProjectTemplateService extends Service {

    @Inject
    private ProjectService            projectService;
    @Inject
    private VirtualFileSystemRegistry vfsRegistry;
    @Inject
    private ProjectTemplateRegistry   templateRegistry;

    @GenerateLink(rel = Constants.LINK_REL_CREATE_PROJECT_FROM_TEMPLATE)
    @POST
    @Path("create")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ProjectDescriptor create(@PathParam("ws-id") String workspace,
                                    @Required @Description("project name") @QueryParam("name") String name,
                                    @Required @Description("project type id") @QueryParam("projectTypeId") String projectTypeId,
                                    @Required @Description("template description id") @QueryParam("templateId") String templateId)
            throws VirtualFileSystemException, IOException {
        List<ProjectTemplateDescription> descriptions = templateRegistry.getTemplateDescriptions(projectTypeId);
        for (ProjectTemplateDescription description : descriptions) {
            if (description.getId().equals(templateId)) ;
            return projectService.importSource(workspace, name, "zip", description.getLocation());
        }
        return null;
    }

    @GenerateLink(rel = Constants.LINK_REL_PROJECT_TEMPLATES)
    @GET
    @Path("get")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public List<ProjectTemplateDescriptor> getTemplates(@Required @Description("project type id") @QueryParam("projectTypeId") String id) {
        List<ProjectTemplateDescription> descriptions = templateRegistry.getTemplateDescriptions(id);
        List<ProjectTemplateDescriptor> result = new ArrayList<>();
        for (ProjectTemplateDescription description : descriptions) {
            result.add(DtoFactory.getInstance().createDto(ProjectTemplateDescriptor.class)
                                 .withProjectTypeId(id)
                                 .withTemplateId(description.getId())
                                 .withTemplateTitle(description.getTitle())
                                 .withTemplateDescription(description.getDescription())
                                 .withTemplateLocation(description.getLocation()));
        }
        return result;
    }

}
