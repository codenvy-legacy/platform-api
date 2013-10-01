/*
 * CODENVY CONFIDENTIAL
 * __________________
 *
 *  [2012] - [2013] Codenvy, S.A.
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */
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
