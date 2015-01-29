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

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author andrew00x
 */
@Singleton
public class MachineFactories {
    private final Map<String, MachineFactory> machineBuilderFactories;

    @Inject
    public MachineFactories(Set<MachineFactory> builderFactories) {
        machineBuilderFactories = new ConcurrentHashMap<>();
        for (MachineFactory builderFactory : builderFactories) {
            machineBuilderFactories.put(builderFactory.getMachineBuilderType(), builderFactory);
        }
    }

    public MachineFactory get(String type) {
        if (type == null) {
            return null;
        }
        return machineBuilderFactories.get(type);
    }

    public Set<MachineFactory> getAll() {
        return new LinkedHashSet<>(machineBuilderFactories.values());
    }
}
