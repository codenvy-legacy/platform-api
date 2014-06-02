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
package com.codenvy.api.auth;

import java.security.Principal;

/**
 * Represent principal that goes throw authentication, and can
 * be unique identified by its id.
 *
 * @author Sergii Kabashniuk
 */
public class UniquePrincipal implements Principal {
    private final String name;
    private final String id;

    public UniquePrincipal(String name, String id) {
        this.name = name;
        this.id = id;
    }

    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }
}
