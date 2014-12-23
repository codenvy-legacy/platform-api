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

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * @author Vitaly Parfonov
 */
@Singleton
public class ProjectTemplateRegistry {

    private final Map<String, List<ProjectTemplateDescriptor>> templates = new ConcurrentHashMap<>();
    private final Set<ProjectType2> projectTypes;
    private final ProjectTemplateDescriptionLoader templateLoader;

    @Inject
    public ProjectTemplateRegistry(Set<ProjectType2> projectTypes, ProjectTemplateDescriptionLoader templateLoader) {
        this.projectTypes = projectTypes;
        this.templateLoader = templateLoader;
    }


    @PostConstruct
    private void registerTemplates() {
        for (ProjectType2 projectType : projectTypes) {
            try {
                for(ProjectTemplateDescriptor templateDescriptor : templateLoader.load(projectType.getId())) {
                    templateDescriptor.setProjectType(projectType.getId());
                    register(templateDescriptor);
                }
            } catch (IOException e) {
                e.printStackTrace();
//                LOG.info("MavenProjectType", "Templates not loaded for maven project type");
            }
        }
    }

    public void register(ProjectTemplateDescriptor template) {
        List<ProjectTemplateDescriptor> templateList = templates.get(template.getProjectType());
        if (templateList == null) {
            templates.put(template.getProjectType(), templateList = new CopyOnWriteArrayList<>());
        }
        templateList.add(template);
    }


    public List<ProjectTemplateDescriptor> getTemplates(String projectType) {
        return templates.get(projectType);
    }
}
