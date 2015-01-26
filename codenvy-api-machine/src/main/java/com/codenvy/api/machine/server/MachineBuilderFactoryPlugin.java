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
import java.util.Set;

/**
 * @author andrew00x
 */
@Singleton
class MachineBuilderFactoryPlugin {
    @Inject
    MachineBuilderFactoryPlugin(MachineBuilderFactoryRegistry builderFactoryRegistry, Set<MachineBuilderFactory> builderFactories) {
        for (MachineBuilderFactory builderFactory : builderFactories) {
            builderFactoryRegistry.add(builderFactory);
        }
    }
}
