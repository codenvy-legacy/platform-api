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
package org.eclipse.che.api.auth;

import org.eclipse.che.api.auth.shared.dto.AuthorizationTokenRequest;
import org.eclipse.che.api.auth.shared.dto.Token;
import org.eclipse.che.commons.env.EnvironmentContext;
import org.eclipse.che.dto.server.DtoFactory;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * @author Sergii Kabashniuk
 */
@Path("/bearertoken")
public class BearerTokenAuthorizationService {

    private final AuthorizationManager authorizationManager;

    @Inject
    public BearerTokenAuthorizationService(AuthorizationManager authorizationManager) {
        this.authorizationManager = authorizationManager;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("user")
    public Token generateToken(AuthorizationTokenRequest authorizationData) {

        return DtoFactory.getInstance().createDto(Token.class)
                         .withValue(authorizationManager
                                            .generateAuthorizationToken(EnvironmentContext.getCurrent().getUser().getId(),
                                                                        authorizationData));
    }
}
