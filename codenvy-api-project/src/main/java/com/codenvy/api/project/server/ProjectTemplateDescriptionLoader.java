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

import com.codenvy.api.project.server.type.ProjectType2;
import com.codenvy.api.project.shared.dto.ProjectTemplateDescriptor;
import com.codenvy.dto.server.DtoFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Set;

/**
 * Reads project template descriptions that may be described in separate .json files for every project type. This file should be named as
 * &lt;project_type_id&gt;.json.
 *
 * @author Artem Zatsarynnyy
 */
@Singleton
public class ProjectTemplateDescriptionLoader {

    private final Set<ProjectType2>       projectTypes;
    private final ProjectTemplateRegistry templateRegistry;

    private static final Logger LOG = LoggerFactory.getLogger(ProjectTemplateRegistry.class);

    @Inject
    public ProjectTemplateDescriptionLoader(Set<ProjectType2> projectTypes,
                                            ProjectTemplateRegistry templateRegistry) {
        this.projectTypes = projectTypes;
        this.templateRegistry = templateRegistry;
    }

    @PostConstruct
    protected void load() {
        for (ProjectType2 projectType : projectTypes) {
            load(projectType.getId());
        }
    }

    /**
     * Load project template descriptions for the specified project type.
     *
     * @param projectTypeId
     *         id of the project type for which templates should be loaded
     * @throws IOException
     *         if i/o error occurs while reading file with templates
     */
    private void load(String projectTypeId) {
        try {
            final URL url = Thread.currentThread().getContextClassLoader().getResource(projectTypeId + ".json");
            if (url != null) {
                final List<ProjectTemplateDescriptor> templates;
                try (InputStream inputStream = url.openStream()) {
                    templates = DtoFactory.getInstance().createListDtoFromJson(inputStream, ProjectTemplateDescriptor.class);
                }
                templateRegistry.register(projectTypeId, templates);
            }
        } catch (IOException e) {
            LOG.debug(String.format("Can't load information about project templates for %s project type", projectTypeId), e);
        }
    }
}
