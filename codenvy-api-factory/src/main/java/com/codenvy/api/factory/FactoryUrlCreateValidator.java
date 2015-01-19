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
package com.codenvy.api.factory;

import com.codenvy.api.core.ApiException;
import com.codenvy.api.factory.dto.Factory;

import javax.servlet.http.HttpServletRequest;
import java.net.URI;

/**
 * Interface for validations of factory urls on creation stage.
 *
 * @author Alexander Garagatyi
 */
public interface FactoryUrlCreateValidator {

    /**
     * Validates factory url object on creation stage. Implementation should throw
     * {@link com.codenvy.api.core.ApiException} if factory url object is invalid.
     *
     * @param factory
     *         factory object to validate
     * @throws com.codenvy.api.core.ApiException
     *         - in case if factory is not valid
     */
    void validateOnCreate(Factory factory) throws ApiException;
}
