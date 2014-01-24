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
package com.codenvy.api.organization.exception;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

/**
 *
 */
public class ItemNamingExceptionMapper  implements ExceptionMapper<ItemNamingException> {
    /** @see javax.ws.rs.ext.ExceptionMapper#toResponse(Throwable) */
    @Override
    public Response toResponse(ItemNamingException exception) {
        return Response.status(Response.Status.BAD_REQUEST).entity(exception.getMessage()).type(MediaType.TEXT_PLAIN)
                       .build();
    }
}
