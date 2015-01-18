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
package com.codenvy.api.local;

import com.codenvy.api.core.ConflictException;
import com.codenvy.api.user.server.TokenValidator;

import javax.inject.Singleton;

/**
 * Dummy implementation of {@link com.codenvy.api.user.server.TokenValidator}.
 * 
 * @author Ann Shumilova
 */
@Singleton
public class DummyTokenValidator implements TokenValidator {
    /** {@inheritDoc} */
    @Override
    public String validateToken(String token) throws ConflictException {
        return "codenvy@codenvy.com";
    }
}
