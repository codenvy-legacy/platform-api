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
package com.codenvy.api.core.rest;

import com.codenvy.api.core.rest.dto.JsonDto;
import com.codenvy.api.core.util.ReflectionUtil;

import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

/**
 * Base class for all API services.
 *
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 */
public abstract class Service {
    @Context
    protected UriInfo uriInfo;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public final Response getServiceDescriptor() {
        return Response.status(Response.Status.OK).entity(JsonDto.toJson(ReflectionUtil.generateServiceDescriptor(uriInfo, getClass())))
                       .build();
    }

    public ServiceContext getServiceContext() {
        final Class serviceClass = getClass();
        return new ServiceContext() {
            @Override
            public UriBuilder getServicePathBuilder() {
                return uriInfo.getBaseUriBuilder().path(serviceClass);
            }
        };
    }
}
