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

import com.codenvy.api.project.shared.dto.ProjectTemplateDescriptor;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Vitaly Parfonov
 */
@Singleton
public class ProjectTemplateRegistry {

    private final Map<String, List<ProjectTemplateDescriptor>> descriptors;

    @Inject
    public ProjectTemplateRegistry() {
        this.descriptors = new ConcurrentHashMap<>();
    }

    public void register(ProjectTemplateDescriptor descriptor) {
        if (descriptors.get(descriptor.getProjectTypeId()) != null)
            descriptors.get(descriptor.getProjectTypeId()).add(descriptor);
        else {
            List<ProjectTemplateDescriptor> list = new ArrayList<>();
            list.add(descriptor);
            descriptors.put(descriptor.getProjectTypeId(), list);
        }
    }

    public List<ProjectTemplateDescriptor> getTemplateDescriptors(String projectTypeId) {
        return descriptors.get(projectTypeId);
    }
}
