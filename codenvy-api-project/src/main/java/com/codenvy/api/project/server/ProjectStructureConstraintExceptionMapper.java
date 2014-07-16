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

import com.codenvy.api.core.rest.shared.dto.ServiceError;
import com.codenvy.dto.server.DtoFactory;

import javax.inject.Singleton;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * @author andrew00x
 */
@Provider
@Singleton
public class ProjectStructureConstraintExceptionMapper implements ExceptionMapper<ProjectStructureConstraintException> {
    @Override
    public Response toResponse(ProjectStructureConstraintException exception) {
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                       .entity(DtoFactory.getInstance().createDto(ServiceError.class).withMessage(exception.getMessage()))
                       .type(MediaType.APPLICATION_JSON)
                       .build();
    }
}
