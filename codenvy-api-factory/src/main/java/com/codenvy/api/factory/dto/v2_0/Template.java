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
package com.codenvy.api.factory.dto.v2_0;

import com.codenvy.api.project.shared.dto.ImportSourceDescriptor;
import com.codenvy.api.project.shared.dto.NewProject;
import com.codenvy.api.workspace.shared.dto.NewWorkspace;
import com.codenvy.dto.shared.DTO;

/**
 * @author andrew00x
 */
@DTO
public interface Template {
    String getV();

    void setV(String v);

    Template withV(String v);

    ImportSourceDescriptor getSource();

    void setSource(ImportSourceDescriptor source);

    Template withSource(ImportSourceDescriptor source);

    NewWorkspace getWorkspace();

    void setWorkspace(NewWorkspace workspace);

    Template withWorkspace(NewWorkspace workspace);

    Policies getPolicies();

    void setPolicies(Policies policies);

    Template withPolicies(Policies policies);

    NewProject getProject();

    void setProject(NewProject project);

    Template withProject(NewProject project);

    Actions getAction();

    void setAction(Actions action);

    Template withAction(Actions action);
}
