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
package com.codenvy.api.auth;

import com.codenvy.api.auth.shared.dto.Credentials;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.*;

/**
 * Authenticate user by username and password.
 * <p/>
 * In response user receive "token". This token user can use
 * to identify him in all other request to API, to do that he should pass it as query parameter.
 *
 * @author Sergii Kabashniuk
 * @author Alexander Garagatyi
 */
@Path("auth")
public class AuthenticationService {

    private final AuthenticationDao dao;

    @Inject
    public AuthenticationService(AuthenticationDao dao) {
        this.dao = dao;
    }

    /**
     * Get token to be able to call secure api methods.
     *
     * @param tokenAccessCookie
     *         - old session-based cookie with token
     * @param credentials
     *         - username and password
     * @return - auth token in JSON, session-based and persistent cookies
     * @throws AuthenticationException
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("login")
    public Response authenticate(Credentials credentials,
                                 @CookieParam("session-access-key") Cookie tokenAccessCookie,
                                 @Context UriInfo uriInfo)
            throws AuthenticationException {

        return dao.login(credentials, tokenAccessCookie, uriInfo);

    }

    /**
     * Perform logout for the given token.
     *
     * @param token
     *         - authentication token
     * @param tokenAccessCookie
     *         - old session-based cookie with token.
     */
    @POST
    @Path("logout")
    public Response logout(@QueryParam("token") String token,
                           @CookieParam("session-access-key") Cookie tokenAccessCookie,
                           @Context UriInfo uriInfo) {


        return dao.logout(token, tokenAccessCookie, uriInfo);

    }

}
