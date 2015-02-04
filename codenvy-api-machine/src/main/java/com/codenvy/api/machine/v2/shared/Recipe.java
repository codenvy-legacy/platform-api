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
 * Script to create new {@link com.codenvy.api.machine.v2.server.spi.Image}.
 *
 * @author gazarenkov
 */
public class Recipe {
    private final String type;
    private final String script;

    public Recipe(String type, String script) {
        this.type = type;
        this.script = script;
    }

    public String getType() {
        return type;
    }

    public String getScript() {
        return script;
    }
}
