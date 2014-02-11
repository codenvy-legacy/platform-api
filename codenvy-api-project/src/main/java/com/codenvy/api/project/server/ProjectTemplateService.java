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
import com.codenvy.api.project.server.exceptions.SourceImporterNotFoundException;
import com.codenvy.api.project.shared.ProjectTemplateDescription;
import com.codenvy.api.project.shared.ProjectType;
import com.codenvy.api.project.shared.dto.ImportSourceDescriptor;
import com.codenvy.api.project.shared.dto.ProjectDescriptor;
import com.codenvy.api.project.shared.dto.ProjectTemplateDescriptor;
import com.codenvy.api.vfs.server.exceptions.VirtualFileSystemException;
import com.codenvy.dto.server.DtoFactory;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** @author Vitaly Parfonov */
@Path("project-template/{ws-id}")
public class ProjectTemplateService extends Service {

    @Inject
    private ProjectService          projectService;
    @Inject
    private ProjectTemplateRegistry templateRegistry;
    @Inject
    private ProjectTypeRegistry     projectTypeRegistry;

    @GenerateLink(rel = Constants.LINK_REL_CREATE_PROJECT_FROM_TEMPLATE)
    @POST
    @Path("create")
    @Produces(MediaType.APPLICATION_JSON)
    public ProjectDescriptor create(@PathParam("ws-id") String workspace,
                                    @Required @Description("project name") @QueryParam("name") String name,
                                    @Required @Description("project type id") @QueryParam("projectTypeId") String projectTypeId,
                                    @Required @Description("template description id") @QueryParam("templateId") String templateId)
            throws VirtualFileSystemException, IOException, SourceImporterNotFoundException {
        List<ProjectTemplateDescription> descriptions = templateRegistry.getTemplateDescriptions(projectTypeId);
        for (ProjectTemplateDescription description : descriptions) {
            if (description.getId().equals(templateId)) {
                ImportSourceDescriptor importSourceDescriptor = DtoFactory.getInstance().createDto(ImportSourceDescriptor.class)
                                                                          .withType("zip")
                                                                          .withLocation(description.getLocation());
                return projectService.importSource(workspace, name, importSourceDescriptor);
            }
        }
        return null;
    }

    @GenerateLink(rel = Constants.LINK_REL_GET_PROJECT_TEMPLATES)
    @GET
    @Path("get")
    @Produces(MediaType.APPLICATION_JSON)
    public List<ProjectTemplateDescriptor> getTemplates(@Description("project type id") @QueryParam("projectTypeId") String projectTypeId) {
        Map<String, List<ProjectTemplateDescription>> templatesMap = new HashMap<>();
        if (projectTypeId != null) {
            templatesMap.put(projectTypeId, templateRegistry.getTemplateDescriptions(projectTypeId));
        } else {
            for (ProjectType type : projectTypeRegistry.getRegisteredTypes()) {
                templatesMap.put(type.getId(), templateRegistry.getTemplateDescriptions(type.getId()));
            }
        }

        List<ProjectTemplateDescriptor> result = new ArrayList<>();
        for (Map.Entry<String, List<ProjectTemplateDescription>> entry : templatesMap.entrySet()) {
            for (ProjectTemplateDescription description : entry.getValue()) {
                result.add(DtoFactory.getInstance().createDto(ProjectTemplateDescriptor.class)
                                     .withProjectTypeId(entry.getKey())
                                     .withTemplateId(description.getId())
                                     .withTemplateTitle(description.getTitle())
                                     .withTemplateDescription(description.getDescription())
                                     .withTemplateLocation(description.getLocation()));
            }
        }
        return result;
    }

}
