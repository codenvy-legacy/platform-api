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

/**
 * Factory for machines and its builders of different types.
 *
 * @author Alexander Garagatyi
 */
public abstract class MachineFactory {
    private final MachineIdGenerator machineIdGenerator;

    protected MachineFactory(MachineIdGenerator machineIdGenerator) {
        this.machineIdGenerator = machineIdGenerator;
    }

    /** Returns new machine builder. */
    public final MachineBuilder newMachineBuilder() {
        return newMachineBuilder(machineIdGenerator.generateId());
    }

    protected abstract MachineBuilder newMachineBuilder(String machineId);

    public abstract Machine getMachine(String machineId);

    /** Returns type of machine or its builder that this factory produces. */
    public abstract String getMachineBuilderType();
}
