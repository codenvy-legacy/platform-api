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
package org.eclipse.che.api.machine.server;

import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Alexander Garagatyi
 */
@Singleton
public class MachinesInMemoryImpl implements Machines {
    private final Map<String, MachineImpl> machines;

    public MachinesInMemoryImpl() {
        machines = new ConcurrentHashMap<>();
    }

    @Override
    public List<MachineImpl> getAll() {
        return new ArrayList<>(machines.values());
    }

    @Override
    public void put(MachineImpl machine) {
        machines.put(machine.getId(), machine);
    }

    @Override
    public void remove(String machineId) {
        machines.remove(machineId);
    }

    @Override
    public MachineImpl get(String id) {
        return machines.get(id);
    }
}
