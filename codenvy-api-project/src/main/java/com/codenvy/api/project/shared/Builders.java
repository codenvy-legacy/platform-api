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
package com.codenvy.api.project.shared;

/**
 * @author andrew00x
 */
public class Builders {
    private String _default;

    public Builders(String _default) {
        this._default = _default;
    }

    public Builders(Builders other) {
        this._default = other._default;
    }

    public String getDefault() {
        return _default;
    }

    public void setDefault(String _default) {
        this._default = _default;
    }

    public Builders withDefault(String _default) {
        this._default = _default;
        return this;
    }
}
