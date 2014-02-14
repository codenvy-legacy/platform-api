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

import com.codenvy.api.project.shared.ProjectTemplateDescription;
import com.codenvy.api.project.shared.ProjectTemplateExtension;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** @author Vitaly Parfonov */
@Singleton
public class ProjectTemplateRegistry {

    private final Map<String, List<ProjectTemplateDescription>> descriptions;

    @Inject
    public ProjectTemplateRegistry() {
        this.descriptions = new ConcurrentHashMap<>();
    }

    public void register(ProjectTemplateExtension extension) {
        if (descriptions.get(extension.getProjectType().getId()) != null) {
            descriptions.get(extension.getProjectType().getId()).addAll(extension.getTemplateDescriptions());
        } else {
            descriptions.put(extension.getProjectType().getId(), extension.getTemplateDescriptions());
        }
    }

    public List<ProjectTemplateDescription> getTemplateDescriptions(String projectTypeId) {
        return descriptions.get(projectTypeId);
    }
}
