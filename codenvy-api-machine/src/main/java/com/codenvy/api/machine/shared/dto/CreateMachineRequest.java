/*******************************************************************************
 * Copyright (c) 2012-2015 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package com.codenvy.api.machine.shared.dto;

import com.codenvy.dto.shared.DTO;

/**
 * @author Alexander Garagatyi
 */
@DTO
public interface CreateMachineRequest {
    String getType();

    void setType(String type);

    CreateMachineRequest withType(String type);

    String getRecipe();

    void setRecipe(String recipe);

    CreateMachineRequest withRecipe(String recipe);

    String getWorkspace();

    void setWorkspace(String workspace);

    CreateMachineRequest withWorkspace(String workspace);

    String getDisplayName();

    void setDisplayName(String displayName);

    CreateMachineRequest withDisplayName(String displayName);

    String getOutputChannel();

    void setOutputChannel(String outputChannel);

    CreateMachineRequest withOutputChannel(String outputChannel);
}
