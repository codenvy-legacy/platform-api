/*
 * CODENVY CONFIDENTIAL
 * __________________
 * 
 *  [2012] - [2014] Codenvy, S.A. 
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
package com.codenvy.api.project.server;

import com.codenvy.api.core.rest.shared.dto.ServiceError;
import com.codenvy.dto.server.DtoFactory;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * @author andrew00x
 */
@Provider
public class FileSystemLevelExceptionMapper implements ExceptionMapper<FileSystemLevelException> {
    @Override
    public Response toResponse(FileSystemLevelException exception) {
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                       .entity(DtoFactory.getInstance().createDto(ServiceError.class).withMessage(exception.getMessage()))
                       .type(MediaType.APPLICATION_JSON)
                       .build();
    }
}
