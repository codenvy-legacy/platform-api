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
package com.codenvy.api.core.util;

/**
 * Typically may be in use for windows systems only. For *nix like system UnixProcessManager is in use.
 *
 * @author andrew00x
 */
class DefaultProcessManager extends ProcessManager {
    /*
    NOTE: some methods are not implemented for other system than unix like system.
     */

    @Override
    public void kill(Process process) {
        if (isAlive(process)) {
            process.destroy();
            try {
                process.waitFor(); // wait for process death
            } catch (InterruptedException e) {
                Thread.interrupted();
            }
        }
    }

    @Override
    public void kill(int pid) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isAlive(Process process) {
        try {
            process.exitValue();
            return false;
        } catch (IllegalThreadStateException e) {
            return true;
        }
    }

    @Override
    public boolean isAlive(int pid) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getPid(Process process) {
        throw new UnsupportedOperationException();
    }

    @Override
    int system(String command) {
        throw new UnsupportedOperationException();
    }
}
