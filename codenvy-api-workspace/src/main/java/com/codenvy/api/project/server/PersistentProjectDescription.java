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

import com.codenvy.api.project.shared.Attribute;
import com.codenvy.api.project.shared.ProjectDescription;
import com.codenvy.api.project.shared.ProjectType;
import com.codenvy.api.vfs.server.VirtualFileSystem;
import com.codenvy.api.vfs.shared.dto.Project;

import java.util.List;

/** @author <a href="mailto:aparfonov@codenvy.com">Andrey Parfonov</a> */
public class PersistentProjectDescription extends ProjectDescription {
    public PersistentProjectDescription(ProjectType projectType, Attribute... attributes) {
        super(projectType, attributes);
    }

    public PersistentProjectDescription(ProjectType projectType, List<Attribute> attributes) {
        super(projectType, attributes);
    }

    public void store(Project project, VirtualFileSystem vfs) {
        // TODO
    }
}
