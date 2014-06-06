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
package com.codenvy.dto.client;

import com.codenvy.dto.shared.ServerError;

/**
 * Notifies the client of an error on the frontend.
 *
 * @author <a href="mailto:evidolob@codenvy.com">Evgen Vidolob</a>
 */
public class ServerErrorImpl extends RoutableDtoClientImpl implements ServerError {

    protected ServerErrorImpl() {
    }

    @Override
    public final native String getDetails() /*-{
        return this["details"];
    }-*/;

    public final native ServerErrorImpl setDetails(String details) /*-{
        this["details"] = details;
        return this;
    }-*/;

    public final native boolean hasDetails() /*-{
        return this.hasOwnProperty("details");
    }-*/;
}