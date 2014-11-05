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

import com.codenvy.api.core.ApiException;
import com.codenvy.api.core.ServerException;
import com.codenvy.api.core.rest.shared.Links;
import com.codenvy.api.core.rest.shared.dto.Link;
import com.codenvy.api.project.shared.dto.AttributeDescriptor;
import com.codenvy.api.project.shared.dto.BuildersDescriptor;
import com.codenvy.api.project.shared.dto.ImportSourceDescriptor;
import com.codenvy.api.project.shared.dto.ItemReference;
import com.codenvy.api.project.shared.dto.ProjectDescriptor;
import com.codenvy.api.project.shared.dto.ProjectImporterDescriptor;
import com.codenvy.api.project.shared.dto.ProjectProblem;
import com.codenvy.api.project.shared.dto.ProjectReference;
import com.codenvy.api.project.shared.dto.ProjectTemplateDescriptor;
import com.codenvy.api.project.shared.dto.ProjectTypeDescriptor;
import com.codenvy.api.project.shared.dto.ProjectUpdate;
import com.codenvy.api.project.shared.dto.RunnerConfiguration;
import com.codenvy.api.project.shared.dto.RunnersDescriptor;
import com.codenvy.api.vfs.shared.dto.AccessControlEntry;
import com.codenvy.api.vfs.shared.dto.Principal;
import com.codenvy.commons.env.EnvironmentContext;
import com.codenvy.commons.user.User;
import com.codenvy.dto.server.DtoFactory;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Helper methods for convert server essentials to DTO and back.
 *
 * @author andrew00x
 */
public class DtoConverter {

    /*================================ Method for conversion from DTO. ===============================*/

    private DtoConverter() { //converter
    }

    public static ProjectTemplateDescription fromDto(ProjectTemplateDescriptor dto) {
        final String category = dto.getCategory();
        final ImportSourceDescriptor importSource = dto.getSource();
        final BuildersDescriptor builders = dto.getBuilders();
        final RunnersDescriptor runners = dto.getRunners();
        return new ProjectTemplateDescription(
                category == null ? com.codenvy.api.project.shared.Constants.DEFAULT_TEMPLATE_CATEGORY : category,
                importSource == null ? null : importSource.getType(),
                dto.getDisplayName(),
                dto.getDescription(),
                importSource == null ? null : importSource.getLocation(),
                importSource == null ? null : importSource.getParameters(),
                builders == null ? null : fromDto(builders),
                runners == null ? null : fromDto(runners));
    }

    public static ProjectDescription fromDto(ProjectUpdate dto, ProjectTypeDescriptionRegistry typeRegistry) throws ServerException {
        final String typeId = dto.getType();
        ProjectType projectType;
        if (typeId == null) {
            // Treat type as blank type if type is not set in .codenvy/project.json
            projectType = ProjectType.BLANK;
        } else {
            projectType = typeRegistry.getProjectType(typeId);
            if (projectType == null) {
                // Type is unknown but set in codenvy/.project.json
                projectType = new ProjectType(typeId);
            }
        }
        final ProjectDescription projectDescription = new ProjectDescription(projectType);
        projectDescription.setDescription(dto.getDescription());
        final Map<String, List<String>> updateAttributes = dto.getAttributes();
        if (!updateAttributes.isEmpty()) {
            final List<Attribute> attributes = new ArrayList<>(updateAttributes.size());
            for (Map.Entry<String, List<String>> e : updateAttributes.entrySet()) {
                attributes.add(new Attribute(e.getKey(), e.getValue()));
            }
            projectDescription.setAttributes(attributes);
        }
        final BuildersDescriptor buildersDescriptor = dto.getBuilders();
        if (buildersDescriptor != null) {
            projectDescription.setBuilders(fromDto(buildersDescriptor));
        }
        final RunnersDescriptor runnersDescriptor = dto.getRunners();
        if (runnersDescriptor != null) {
            projectDescription.setRunners(fromDto(runnersDescriptor));
        }
        return projectDescription;
    }

    public static Builders fromDto(BuildersDescriptor dto) {
        return new Builders(dto.getDefault());
    }

    /*================================ Methods for conversion to DTO. ===============================*/

    public static Runners fromDto(RunnersDescriptor dto) {
        final Runners runners = new Runners(dto.getDefault());
        for (Map.Entry<String, RunnerConfiguration> e : dto.getConfigs().entrySet()) {
            final RunnerConfiguration config = e.getValue();
            if (config != null) {
                runners.getConfigs().put(e.getKey(), new Runners.Config(config.getRam(), config.getOptions(), config.getVariables()));
            }
        }
        return runners;
    }

    public static ProjectTypeDescriptor toTypeDescriptor(ProjectType projectType,
                                                         ProjectTypeDescriptionRegistry typeRegistry) {
        final DtoFactory dtoFactory = DtoFactory.getInstance();
        final ProjectTypeDescriptor descriptor = dtoFactory.createDto(ProjectTypeDescriptor.class)
                                                           .withType(projectType.getId())
                                                           .withTypeName(projectType.getName())
                                                           .withTypeCategory(projectType.getCategory());
        final List<AttributeDescription> typeAttributes = typeRegistry.getAttributeDescriptions(projectType);
        if (!typeAttributes.isEmpty()) {
            final List<AttributeDescriptor> typeAttribute = new ArrayList<>(typeAttributes.size());
            for (AttributeDescription attributeDescription : typeAttributes) {
                typeAttribute.add(dtoFactory.createDto(AttributeDescriptor.class).withName(attributeDescription.getName()));
            }
            descriptor.setAttributeDescriptors(typeAttribute);
        }
        final List<ProjectTemplateDescription> typeTemplates = typeRegistry.getTemplates(projectType);
        if (!typeTemplates.isEmpty()) {
            final List<ProjectTemplateDescriptor> templateDescriptors = new ArrayList<>(typeTemplates.size());
            for (ProjectTemplateDescription template : typeTemplates) {
                templateDescriptors.add(toTemplateDescriptor(dtoFactory, template));
            }
            descriptor.setTemplates(templateDescriptors);
        }
        descriptor.setIconRegistry(typeRegistry.getIconRegistry(projectType));
        Builders builders = typeRegistry.getBuilders(projectType);
        if (builders != null) {
            descriptor.setBuilders(toDto(builders));
        }
        Runners runners = typeRegistry.getRunners(projectType);
        if (runners != null) {
            descriptor.setRunners(toDto(runners));
        }
        return descriptor;
    }

    public static ProjectTemplateDescriptor toTemplateDescriptor(ProjectTemplateDescription projectTemplate) {
        return toTemplateDescriptor(DtoFactory.getInstance(), projectTemplate);
    }

    private static ProjectTemplateDescriptor toTemplateDescriptor(DtoFactory dtoFactory, ProjectTemplateDescription projectTemplate) {
        final ImportSourceDescriptor importSource = dtoFactory.createDto(ImportSourceDescriptor.class)
                                                              .withType(projectTemplate.getImporterType())
                                                              .withLocation(projectTemplate.getLocation())
                                                              .withParameters(projectTemplate.getParameters());
        final Builders builders = projectTemplate.getBuilders();
        final Runners runners = projectTemplate.getRunners();
        final ProjectTemplateDescriptor dto = dtoFactory.createDto(ProjectTemplateDescriptor.class)
                                                        .withDisplayName(projectTemplate.getDisplayName())
                                                        .withSource(importSource)
                                                        .withCategory(projectTemplate.getCategory())
                                                        .withDescription(projectTemplate.getDescription());
        if (builders != null) {
            dto.withBuilders(toDto(dtoFactory, builders));
        }
        if (runners != null) {
            dto.withRunners(toDto(dtoFactory, runners));
        }
        return dto;
    }

    public static ProjectImporterDescriptor toImporterDescriptor(ProjectImporter importer) {
        return DtoFactory.getInstance().createDto(ProjectImporterDescriptor.class)
                         .withId(importer.getId())
                         .withInternal(importer.isInternal())
                         .withDescription(importer.getDescription() != null ? importer.getDescription() : "description not found")
                         .withCategory(importer.getCategory().getValue());
    }

    public static ItemReference toItemReferenceDto(FileEntry file, UriBuilder uriBuilder) throws ServerException {
        return DtoFactory.getInstance().createDto(ItemReference.class)
                         .withName(file.getName())
                         .withPath(file.getPath())
                         .withType("file")
                         .withMediaType(file.getMediaType())
                         .withLinks(generateFileLinks(file, uriBuilder));
    }

    public static ItemReference toItemReferenceDto(FolderEntry folder, UriBuilder uriBuilder) throws ServerException {
        return DtoFactory.getInstance().createDto(ItemReference.class)
                         .withName(folder.getName())
                         .withPath(folder.getPath())
                         .withType(folder.isProjectFolder() ? "project" : "folder")
                         .withMediaType("text/directory")
                         .withLinks(generateFolderLinks(folder, uriBuilder));
    }

    public static ProjectDescriptor toDescriptorDto(Project project, UriBuilder uriBuilder) {
        final EnvironmentContext environmentContext = EnvironmentContext.getCurrent();
        final DtoFactory dtoFactory = DtoFactory.getInstance();
        final ProjectDescriptor dto = dtoFactory.createDto(ProjectDescriptor.class);
        // Try to provide as much as possible information about project.
        // If get error then save information about error with 'problems' field in ProjectDescriptor.
        final String wsId = project.getWorkspace();
        final String wsName = environmentContext.getWorkspaceName();
        final String name = project.getName();
        final String path = project.getPath();
        dto.withWorkspaceId(wsId).withWorkspaceName(wsName).withName(name).withPath(path);
        ProjectDescription projectDescription = null;
        try {
            projectDescription = project.getDescription();
        } catch (ServerException | ValueStorageException e) {
            dto.getProblems().add(createProjectProblem(dtoFactory, e));
            dto.withType(ProjectType.BLANK.getId()).withTypeName(ProjectType.BLANK.getName());
        }
        if (projectDescription != null) {
            dto.withDescription(projectDescription.getDescription());
            final ProjectType projectType = projectDescription.getProjectType();
            dto.withType(projectType.getId()).withTypeName(projectType.getName());
            final List<Attribute> attributes = projectDescription.getAttributes();
            final Map<String, List<String>> attributesMap = new LinkedHashMap<>(attributes.size());
            if (!attributes.isEmpty()) {
                for (Attribute attribute : attributes) {
                    try {
                        attributesMap.put(attribute.getName(), attribute.getValues());
                    } catch (ValueStorageException e) {
                        dto.getProblems().add(createProjectProblem(dtoFactory, e));
                    }
                }
            }
            dto.withAttributes(attributesMap);

            final Builders builders = projectDescription.getBuilders();
            if (builders != null) {
                dto.withBuilders(toDto(dtoFactory, builders));
            }
            final Runners runners = projectDescription.getRunners();
            if (runners != null) {
                dto.withRunners(toDto(dtoFactory, runners));
            }
        }

        final User currentUser = environmentContext.getUser();
        List<AccessControlEntry> acl = null;
        try {
            acl = project.getPermissions();
        } catch (ServerException e) {
            dto.getProblems().add(createProjectProblem(dtoFactory, e));
        }
        if (acl != null) {
            final List<String> permissions = new LinkedList<>();
            if (acl.isEmpty()) {
                // there is no any restriction at all
                permissions.add("all");
            } else {
                for (AccessControlEntry accessControlEntry : acl) {
                    final Principal principal = accessControlEntry.getPrincipal();
                    if ((Principal.Type.USER == principal.getType() && currentUser.getName().equals(principal.getName()))
                        || (Principal.Type.USER == principal.getType() && "any".equals(principal.getName()))
                        || (Principal.Type.GROUP == principal.getType() && currentUser.isMemberOf(principal.getName()))) {

                        permissions.addAll(accessControlEntry.getPermissions());
                    }
                }
            }
            dto.withPermissions(permissions);
        }

        try {
            dto.withCreationDate(project.getCreationDate());
        } catch (ServerException e) {
            dto.getProblems().add(createProjectProblem(dtoFactory, e));
        }

        try {
            dto.withModificationDate(project.getModificationDate());
        } catch (ServerException e) {
            dto.getProblems().add(createProjectProblem(dtoFactory, e));
        }

        try {
            dto.withVisibility(project.getVisibility());
        } catch (ServerException e) {
            dto.getProblems().add(createProjectProblem(dtoFactory, e));
        }

        dto.withBaseUrl(uriBuilder.clone().path(ProjectService.class, "getProject").build(wsId, path.substring(1)).toString())
           .withLinks(generateProjectLinks(project, uriBuilder));
        if (wsName != null) {
            dto.withIdeUrl(uriBuilder.clone().replacePath("ws").path(wsName).path(path).build().toString());
        }
        return dto;
    }

    public static BuildersDescriptor toDto(Builders builders) {
        return toDto(DtoFactory.getInstance(), builders);
    }

    private static BuildersDescriptor toDto(DtoFactory dtoFactory, Builders builders) {
        return dtoFactory.createDto(BuildersDescriptor.class).withDefault(builders.getDefault());
    }

    public static RunnersDescriptor toDto(Runners runners) {
        return toDto(DtoFactory.getInstance(), runners);
    }

    private static RunnersDescriptor toDto(DtoFactory dtoFactory, Runners runners) {
        final RunnersDescriptor dto = dtoFactory.createDto(RunnersDescriptor.class).withDefault(runners.getDefault());
        final Map<String, Runners.Config> configs = runners.getConfigs();
        Map<String, RunnerConfiguration> configsDto = new LinkedHashMap<>(configs.size());
        for (Map.Entry<String, Runners.Config> e : configs.entrySet()) {
            final Runners.Config config = e.getValue();
            if (config != null) {
                configsDto.put(e.getKey(), dtoFactory.createDto(RunnerConfiguration.class)
                                                     .withRam(config.getRam())
                                                     .withOptions(config.getOptions())
                                                     .withVariables(config.getVariables())
                              );
            }
        }
        dto.withConfigs(configsDto);
        return dto;
    }

    private static List<Link> generateProjectLinks(Project project, UriBuilder uriBuilder) {
        final List<Link> links = generateFolderLinks(project.getBaseFolder(), uriBuilder);
        final String relPath = project.getPath().substring(1);
        final String workspace = project.getWorkspace();
        links.add(
                Links.createLink("PUT", uriBuilder.clone().path(ProjectService.class, "updateProject").build(workspace, relPath).toString(),
                                 MediaType.APPLICATION_JSON, MediaType.APPLICATION_JSON, Constants.LINK_REL_UPDATE_PROJECT));
        links.add(
                Links.createLink("GET",
                                 uriBuilder.clone().path(ProjectService.class, "getRunnerEnvironments").build(workspace, relPath)
                                           .toString(),
                                 MediaType.APPLICATION_JSON, MediaType.APPLICATION_JSON, Constants.LINK_REL_GET_RUNNER_ENVIRONMENTS));
        return links;
    }

    private static List<Link> generateFolderLinks(FolderEntry folder, UriBuilder uriBuilder) {
        final List<Link> links = new LinkedList<>();
        final String workspace = folder.getWorkspace();
        final String relPath = folder.getPath().substring(1);
        //String method, String href, String produces, String rel
        links.add(Links.createLink("GET", uriBuilder.clone().path(ProjectService.class, "exportZip").build(workspace, relPath).toString(),
                                   "application/zip", Constants.LINK_REL_EXPORT_ZIP));
        links.add(Links.createLink("GET", uriBuilder.clone().path(ProjectService.class, "getChildren").build(workspace, relPath).toString(),
                                   MediaType.APPLICATION_JSON, Constants.LINK_REL_CHILDREN));
        links.add(Links.createLink("GET", uriBuilder.clone().path(ProjectService.class, "getTree").build(workspace, relPath).toString(),
                                   null, MediaType.APPLICATION_JSON, Constants.LINK_REL_TREE));
        links.add(Links.createLink("GET", uriBuilder.clone().path(ProjectService.class, "getModules").build(workspace, relPath).toString(),
                                   MediaType.APPLICATION_JSON, Constants.LINK_REL_MODULES));
        links.add(Links.createLink("DELETE", uriBuilder.clone().path(ProjectService.class, "delete").build(workspace, relPath).toString(),
                                   Constants.LINK_REL_DELETE));
        return links;
    }

    private static List<Link> generateFileLinks(FileEntry file, UriBuilder uriBuilder) throws ServerException {
        final List<Link> links = new LinkedList<>();
        final String workspace = file.getWorkspace();
        final String relPath = file.getPath().substring(1);
        links.add(Links.createLink("GET", uriBuilder.clone().path(ProjectService.class, "getFile").build(workspace, relPath).toString(),
                                   null, file.getMediaType(), Constants.LINK_REL_GET_CONTENT));
        links.add(Links.createLink("PUT", uriBuilder.clone().path(ProjectService.class, "updateFile").build(workspace, relPath).toString(),
                                   MediaType.WILDCARD, null, Constants.LINK_REL_UPDATE_CONTENT));
        links.add(Links.createLink("DELETE", uriBuilder.clone().path(ProjectService.class, "delete").build(workspace, relPath).toString(),
                                   Constants.LINK_REL_DELETE));
        return links;
    }

    public static ProjectReference toReferenceDto(Project project, UriBuilder uriBuilder) {
        final EnvironmentContext environmentContext = EnvironmentContext.getCurrent();
        final DtoFactory dtoFactory = DtoFactory.getInstance();
        final ProjectReference dto = dtoFactory.createDto(ProjectReference.class);
        final String wsId = project.getWorkspace();
        final String wsName = environmentContext.getWorkspaceName();
        final String name = project.getName();
        final String path = project.getPath();
        dto.withName(name).withPath(path).withWorkspaceId(wsId).withWorkspaceName(wsName);
        dto.withWorkspaceId(wsId).withWorkspaceName(wsName).withName(name).withPath(path);

        try {
            final ProjectDescription projectDescription = project.getDescription();
            dto.withDescription(projectDescription.getDescription());
            final ProjectType projectType = projectDescription.getProjectType();
            dto.withType(projectType.getId()).withTypeName(projectType.getName());
        } catch (ServerException | ValueStorageException e) {
            dto.withType(ProjectType.BLANK.getId()).withTypeName(ProjectType.BLANK.getName());
            dto.getProblems().add(createProjectProblem(dtoFactory, e));
        }

        try {
            dto.withCreationDate(project.getCreationDate());
        } catch (ServerException e) {
            dto.getProblems().add(createProjectProblem(dtoFactory, e));
        }

        try {
            dto.withModificationDate(project.getModificationDate());
        } catch (ServerException e) {
            dto.getProblems().add(createProjectProblem(dtoFactory, e));
        }

        try {
            dto.withVisibility(project.getVisibility());
        } catch (ServerException e) {
            dto.getProblems().add(createProjectProblem(dtoFactory, e));
        }

        dto.withUrl(uriBuilder.clone().path(ProjectService.class, "getProject").build(wsId, name).toString());
        if (wsName != null) {
            dto.withIdeUrl(uriBuilder.clone().replacePath("ws").path(wsName).path(path).build().toString());
        }
        return dto;
    }

    private static ProjectProblem createProjectProblem(DtoFactory dtoFactory, ApiException error) {
        // TODO: setup error code
        return dtoFactory.createDto(ProjectProblem.class).withCode(1).withMessage(error.getMessage());
    }
}
