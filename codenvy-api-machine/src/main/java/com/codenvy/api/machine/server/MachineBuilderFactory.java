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
package com.codenvy.api.machine.server;

import com.codenvy.api.core.NotFoundException;

import javax.annotation.Nullable;

/**
 * Factory for builders of different types of machines
 *
 * @author Alexander Garagatyi
 */
public interface MachineBuilderFactory {
    /**
     * Returns machine builder of specified machine type.
     * Default machine type will be used if specified type is null
     *
     * @param machineType machine type of builder
     * @return builder for creating machine
     * @throws NotFoundException if machine type is not supported
     */
    MachineBuilder newMachineBuilder(@Nullable String machineType) throws NotFoundException;
}
