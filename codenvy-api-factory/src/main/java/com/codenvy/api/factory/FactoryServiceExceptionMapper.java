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
package com.codenvy.api.factory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class FactoryServiceExceptionMapper implements ExceptionMapper<FactoryUrlException> {

    private static final Logger LOG = LoggerFactory.getLogger(FactoryServiceExceptionMapper.class);

    @Override
    public Response toResponse(FactoryUrlException exception) {
        LOG.warn(exception.getLocalizedMessage());

        int responseStatus = exception.getResponseStatus();

        String message = exception.getMessage();
        if (message != null) {
            return Response.status(responseStatus).entity(message).type(MediaType.TEXT_PLAIN).build();
        }
        return Response.status(responseStatus).build();
    }
}
