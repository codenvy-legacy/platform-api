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
package com.codenvy.api.runner;

/**
 * @author andrew00x
 */
class ResourceQuotaController {
    final int maxMemSize;

    private int available;

    ResourceQuotaController(int maxMemSize) {
        this.maxMemSize = maxMemSize;
        this.available = maxMemSize;
    }

    synchronized void decrementMemory(int mem) throws RunnerException {
        if (available >= mem) {
            available -= mem;
        } else {
            throw new RunnerException(String.format("Not enough resources to start application. Available memory %dM but %dM required. ",
                                                    available, mem));
        }
    }

    synchronized void releaseMemory(int mem) {
        available += mem;
        if (mem > maxMemSize) {
            available = maxMemSize;
        }
    }

    synchronized int availableMemory() {
        return available;
    }
}
