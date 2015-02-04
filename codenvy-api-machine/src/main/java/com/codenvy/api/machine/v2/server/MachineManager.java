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
package com.codenvy.api.machine.v2.server;

import com.codenvy.api.core.NotFoundException;
import com.codenvy.api.machine.v2.server.spi.ImageProvider;
import com.codenvy.api.machine.v2.server.spi.Machine;
import com.codenvy.api.machine.v2.shared.ProjectBinding;
import com.codenvy.api.machine.v2.shared.Recipe;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Facade for Machine level operations.
 *
 * @author gazarenkov
 */
@Singleton
final class MachineManager {

    private final Map<String, ImageProvider> imageProviders = new HashMap<>();

    @Inject
    MachineManager(Set<ImageProvider> providers) {
        for (ImageProvider provider : providers) {
            this.imageProviders.put(provider.getType(), provider);
        }
    }

    Machine create(Recipe recipe) throws InvalidRecipeException, MachineException {
        return null;
    }

    Machine create(String snapshotId) throws NotFoundException, MachineException {
        return null;
    }

    List<Machine> getMachines(ProjectBinding project) {
        return null;
    }

    Machine getMachine(String machineId) throws NotFoundException {
        return null;
    }

    Snapshot save(Machine machine) throws MachineException {
        return null;
    }

    List<Snapshot> getSnapshots() {
        return null;
    }

    void removeSnapshot(String snapshotId) throws NotFoundException {
    }

    void destory(Machine machine) throws NotFoundException, MachineException {
    }

    Snapshot destory(Machine machine, boolean saveSnapshot) throws NotFoundException, MachineException {
        return null;
    }
}
