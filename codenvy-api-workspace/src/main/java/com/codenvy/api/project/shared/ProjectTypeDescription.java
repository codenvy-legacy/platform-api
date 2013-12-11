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
package com.codenvy.api.project.shared;

import java.util.List;

/**
 * Description of type of project.
 *
 * @author andrew00x
 */
public class ProjectTypeDescription extends ProjectDescription {
    public ProjectTypeDescription(ProjectType projectType, Attribute... attributes) {
        super(projectType, attributes);
        addCommonAttributes();
    }

    public ProjectTypeDescription(ProjectType projectType, List<Attribute> attributes) {
        super(projectType, attributes);
        addCommonAttributes();
    }

    public ProjectTypeDescription(ProjectType projectType) {
        super(projectType);
        addCommonAttributes();
    }

    // Probably temporary solution for adding common attributes that are applicable for any type of project.
    protected void addCommonAttributes() {
        setAttribute(new Attribute("zipball_sources_url", "<url for downloading zip bundle of folder or project>"));
    }
}