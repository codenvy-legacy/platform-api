/*******************************************************************************
* Copyright (c) 2012-2014 Codenvy, S.A.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
* Codenvy, S.A. - initial API and implementation
*******************************************************************************/
package com.codenvy.api.core.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Helpers to manage system processes.
 *
 * @author andrew00x
 */
public final class ProcessUtil {
    private static final ProcessManager PROCESS_MANAGER = ProcessManager.newInstance();

    public static void process(Process p, LineConsumer stdout, LineConsumer stderr) throws IOException {
        BufferedReader inputReader = null;
        BufferedReader errorReader = null;
        try {
            final InputStream inputStream = p.getInputStream();
            final InputStream errorStream = p.getErrorStream();
            inputReader = new BufferedReader(new InputStreamReader(inputStream));
            errorReader = new BufferedReader(new InputStreamReader(errorStream));
            String line;
            try {
                while ((line = inputReader.readLine()) != null) {
                    stdout.writeLine(line);
                }
            } finally {
                stdout.close();
            }
            try {
                while ((line = errorReader.readLine()) != null) {
                    stderr.writeLine(line);
                }
            } finally {
                stderr.close();
            }
        } finally {
            if (inputReader != null) {
                try {
                    inputReader.close();
                } catch (IOException ignored) {
                }
            }
            if (errorReader != null) {
                try {
                    errorReader.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    public static boolean isAlive(Process process) {
        return PROCESS_MANAGER.isAlive(process);
    }

    public static boolean isAlive(int pid) {
        return PROCESS_MANAGER.isAlive(pid);
    }

    public static void kill(Process process) {
        PROCESS_MANAGER.kill(process);
    }

    public static void kill(int pid) {
        PROCESS_MANAGER.kill(pid);
    }

    public static int getPid(Process process) {
        return PROCESS_MANAGER.getPid(process);
    }

    public static int system(String command) {
        return PROCESS_MANAGER.system(command);
    }

    private ProcessUtil() {
    }
}

