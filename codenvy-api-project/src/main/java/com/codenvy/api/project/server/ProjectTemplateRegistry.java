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

import com.codenvy.api.project.shared.dto.ProjectTemplateDescriptor;

import javax.inject.Singleton;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Vitaly Parfonov
 */
@Singleton
public class ProjectTemplateRegistry {

    private final Map<String, List<ProjectTemplateDescriptor>> templates = new ConcurrentHashMap<>();

    public void register(ProjectTemplateDescriptor template) {
        List<ProjectTemplateDescriptor> tmpls = this.templates.get(template.getProjectType());
        if (tmpls == null) {
            tmpls = new LinkedList<>();
        }
        tmpls.add(template);
        templates.put(template.getProjectType(), tmpls);
    }


    public List<ProjectTemplateDescriptor> getTemplates(String projectType) {
       return templates.get(projectType);
    }
}
