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

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Storage for created machines
 *
 * @author Alexander Garagatyi
 */
@Singleton
public class MachineRegistry {
    private final ConcurrentMap<String, Machine>                   machines;

    @Inject
    public MachineRegistry() {
        this.machines = new ConcurrentHashMap<>();
    }

    /**
     * Get machine by id
     *
     * @param id
     *         machine id
     * @return machine with given id
     * @throws NotFoundException
     *         if machine with specified id is not found
     */
    public Machine getMachine(String id) throws NotFoundException {
        final Machine machine = machines.get(id);
        if (machine == null) {
            throw new NotFoundException(String.format("Machine not found %s.", id));
        }

        return machine;
    }

    public void putMachine(Machine machine) {
        machines.put(machine.getId(), machine);
    }
}
