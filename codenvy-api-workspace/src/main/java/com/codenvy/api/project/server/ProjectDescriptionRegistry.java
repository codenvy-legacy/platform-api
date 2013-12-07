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

import com.codenvy.api.project.shared.ProjectDescription;
import com.codenvy.api.project.shared.ProjectDescriptionExtension;
import com.codenvy.api.project.shared.ProjectType;
import com.codenvy.api.project.shared.ProjectTypeExtension;

import javax.inject.Singleton;
import java.util.*;

/**
 * ProjectDescriptionRegistry
 *
 * @author gazarenkov
 */
@Singleton
public class ProjectDescriptionRegistry {

    private final Map<String,ProjectDescription> descriptions = new HashMap<String, ProjectDescription>();

    public void registerType(ProjectTypeExtension ext) {

        descriptions.put(ext.getProjectType().getId(), new ProjectDescription(ext.getProjectType(),
                ext.getPredefinedAttributes()));
    }

    public void registerDescription(ProjectDescriptionExtension ext) {
        for(ProjectType type : ext.getProjectTypes()) {

            if(descriptions.containsKey(type.getId())) {
                // TODO type should be registered? how to do that?
                descriptions.get(type.getId()).addAttributes(ext.getAttributes());
            } else {
                descriptions.put(type.getId(), new ProjectDescription(type, ext.getAttributes()));
            }

        }
    }


    public Collection<ProjectDescription> getDescriptions() {

        return descriptions.values();
    }


}