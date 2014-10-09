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
package com.codenvy.api.project.server;

/**
 * Describes builder configuration for project.
 *
 * @author andrew00x
 */
public class Builders {
    /** Default builder identifier. */
    private String _default;

    public Builders() {
    }

    public Builders(String _default) {
        this._default = _default;
    }

    /** Copy constructor. */
    public Builders(Builders other) {
        this._default = other._default;
    }

    /** Gets default builder identifier, e.g. "maven". */
    public String getDefault() {
        return _default;
    }

    /** Sets default builder identifier. e.g. "maven". */
    public void setDefault(String _default) {
        this._default = _default;
    }

    public Builders withDefault(String _default) {
        this._default = _default;
        return this;
    }
}
