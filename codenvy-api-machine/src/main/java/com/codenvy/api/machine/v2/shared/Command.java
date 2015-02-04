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
package com.codenvy.api.machine.v2.shared;

/**
 * @author gazarenkov
 */
public class Command {
    private final String name;
    private final String commandLine;

    public Command(String name, String commandLine) {
        this.name = name;
        this.commandLine = commandLine;
    }

    public String getName() {
        return name;
    }

    public String getCommandLine() {
        return commandLine;
    }

    @Override
    public String toString() {
        return "Command{" +
               "name='" + name + '\'' +
               ", commandLine='" + commandLine + '\'' +
               '}';
    }
}
