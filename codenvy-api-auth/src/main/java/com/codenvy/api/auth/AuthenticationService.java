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
package com.codenvy.api.auth;

import com.codenvy.api.auth.server.AuthenticationDao;
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
//    private static final Logger LOG = LoggerFactory.getLogger(AuthenticationService.class);
//    @Inject
//    protected AuthenticationHandlerProvider handlerProvider;
//    @Inject
//    protected TicketManager                 ticketManager;
//    @Inject
//    protected TokenGenerator                uniqueTokenGenerator;
//    @Nullable
//    @Inject
//    protected CookieBuilder                 cookieBuilder;

    @Inject
    AuthenticationDao dao;

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
