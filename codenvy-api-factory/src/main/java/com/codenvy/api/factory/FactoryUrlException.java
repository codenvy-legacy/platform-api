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
package com.codenvy.api.factory;

/** Common factory url exception */
public class FactoryUrlException extends Exception {
    /**
     * Response status if any exception occurs,
     * <br>
     * Default value: 400
     */
    int responseStatus;

    public FactoryUrlException() {
        this(400);
    }

    public FactoryUrlException(String message, Throwable cause) {
        this(400, message, cause);
    }

    public FactoryUrlException(String message) {
        this(400, message);
    }

    public FactoryUrlException(Throwable cause) {
        this(400, cause);
    }

    public FactoryUrlException(int responseStatus) {
        super();
        this.responseStatus = responseStatus;
    }

    public FactoryUrlException(int responseStatus, String message, Throwable cause) {
        super(message, cause);
        this.responseStatus = responseStatus;
    }

    public FactoryUrlException(int responseStatus, String message) {
        super(message);
        this.responseStatus = responseStatus;
    }

    public FactoryUrlException(int responseStatus, Throwable cause) {
        super(cause);
        this.responseStatus = responseStatus;
    }

    public int getResponseStatus() {
        return responseStatus;
    }

    public void setResponseStatus(int responseStatus) {
        this.responseStatus = responseStatus;
    }
}
