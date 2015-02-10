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
public interface CreateMachineFromRecipe {
    String getOutputChannel();

    void setOutputChannel(String outputChannel);

    CreateMachineFromRecipe withOutputChannel(String outputChannel);

    String getRecipe();

    void setRecipe(String recipe);

    CreateMachineFromRecipe withRecipe(String recipe);
}
