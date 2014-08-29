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
import com.codenvy.api.project.shared.RunnerEnvironmentConfiguration;
import com.codenvy.api.project.shared.dto.AttributeDescriptor;
import com.codenvy.api.project.shared.dto.ImportSourceDescriptor;
import com.codenvy.api.project.shared.dto.ProjectTemplateDescriptor;
import com.codenvy.api.project.shared.dto.ProjectTypeDescriptor;
import com.codenvy.api.project.shared.dto.RunnerEnvironmentConfigurationDescriptor;
import com.codenvy.dto.server.DtoFactory;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * ProjectDescriptionService
 *
 * @author gazarenkov
 */
@Path("project-type")
public class ProjectTypeService extends Service {

    private ProjectTypeDescriptionRegistry registry;

    @Inject
    public ProjectTypeService(ProjectTypeDescriptionRegistry registry) {
        this.registry = registry;
    }

    @GenerateLink(rel = Constants.LINK_REL_PROJECT_TYPES)
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<ProjectTypeDescriptor> getProjectTypes() {
        final DtoFactory factory = DtoFactory.getInstance();
        final List<ProjectTypeDescriptor> types = new LinkedList<>();
        for (ProjectTypeDescription typeDescription : registry.getDescriptions()) {
            final ProjectType projectType = typeDescription.getProjectType();
            final ProjectTypeDescriptor descriptor = factory.createDto(ProjectTypeDescriptor.class)
                                                            .withProjectTypeId(projectType.getId())
                                                            .withProjectTypeName(projectType.getName())
                                                            .withProjectTypeCategory(projectType.getCategory())
                                                            .withBuilder(projectType.getBuilder())
                                                            .withRunner(projectType.getRunner());
            final List<AttributeDescriptor> attributeDescriptors = new LinkedList<>();
            for (AttributeDescription attributeDescription : typeDescription.getAttributeDescriptions()) {
                attributeDescriptors.add(factory.createDto(AttributeDescriptor.class).withName(attributeDescription.getName()));
            }
            descriptor.setAttributeDescriptors(attributeDescriptors);
            final List<ProjectTemplateDescriptor> templateDescriptors = new LinkedList<>();
            for (ProjectTemplateDescription templateDescription : registry.getTemplates(projectType)) {
                ProjectTemplateDescriptor templateDescriptor = factory.createDto(ProjectTemplateDescriptor.class)
                                                                      .withDisplayName(templateDescription.getDisplayName())
                                                                      .withSource(factory.createDto(ImportSourceDescriptor.class)
                                                                                         .withType(templateDescription.getImporterType())
                                                                                         .withLocation(templateDescription.getLocation()))
                                                                      .withCategory(templateDescription.getCategory())
                                                                      .withDescription(templateDescription.getDescription())
                                                                      .withDefaultRunnerEnvironment(
                                                                              templateDescription.getDefaultRunnerEnvironment())
                                                                      .withDefaultBuilderEnvironment(
                                                                              templateDescription.getDefaultBuilderEnvironment())
                                                                      .withRunnerEnvironmentConfigurations(
                                                                              getReformatedRunnerEnvConfigs(templateDescription));
                templateDescriptors.add(templateDescriptor);
            }
            descriptor.setTemplates(templateDescriptors);
            descriptor.setIconRegistry(registry.getIconRegistry(projectType));
            types.add(descriptor);
        }
        return types;
    }

    private Map<String, RunnerEnvironmentConfigurationDescriptor> getReformatedRunnerEnvConfigs(
            ProjectTemplateDescription templateDescription) {
        String defaultRunnerEnvironment = templateDescription.getDefaultRunnerEnvironment();
        Map<String, RunnerEnvironmentConfiguration> runnerEnvironmentConfigurations =
                templateDescription.getRunnerEnvironmentConfigurations();
        Map<String, RunnerEnvironmentConfigurationDescriptor> runnerEnvConfigs = new LinkedHashMap<>();
        RunnerEnvironmentConfigurationDescriptor runnerEnvironmentConfigurationDescriptor =
                DtoFactory.getInstance().createDto(RunnerEnvironmentConfigurationDescriptor.class);
        if (templateDescription.getRunnerEnvironmentConfigurations() != null) {
            if (runnerEnvironmentConfigurations != null && defaultRunnerEnvironment != null) {
                RunnerEnvironmentConfiguration descriptor =
                        runnerEnvironmentConfigurations.get(defaultRunnerEnvironment);
                if (descriptor != null) {
                    Integer recommendedMemorySize = descriptor.getRecommendedMemorySize();
                    if (recommendedMemorySize != null) {
                        runnerEnvironmentConfigurationDescriptor.setRecommendedMemorySize(recommendedMemorySize);
                        runnerEnvConfigs.put(defaultRunnerEnvironment, runnerEnvironmentConfigurationDescriptor);
                    }
                }
            }
        }
        return runnerEnvConfigs;
    }
}