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
package com.codenvy.api.factory.dto;

import com.codenvy.api.project.shared.dto.ImportSourceDescriptor;
import com.codenvy.api.project.shared.dto.NewProject;
import com.codenvy.api.workspace.shared.dto.NewWorkspace;
import com.codenvy.dto.shared.DTO;

/**
 * @author andrew00x
 */
@DTO
public interface FactoryJson {
    String getV();

    void setV(String v);

    FactoryJson withV(String v);

    ImportSourceDescriptor getSource();

    void setSource(ImportSourceDescriptor source);

    FactoryJson withSource(ImportSourceDescriptor source);

    NewWorkspace getWorkspace();

    void setWorkspace(NewWorkspace workspace);

    FactoryJson withWorkspace(NewWorkspace workspace);

    Policies getPolicies();

    void setPolicies(Policies policies);

    FactoryJson withPolicies(Policies policies);

    NewProject getProject();

    void setProject(NewProject project);

    FactoryJson withProject(NewProject project);

    Actions getAction();

    void setAction(Actions action);

    FactoryJson withAction(Actions action);
}
