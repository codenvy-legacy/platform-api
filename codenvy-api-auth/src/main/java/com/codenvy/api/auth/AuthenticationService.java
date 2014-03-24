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



        System.out.println(">>>>>>>>>>>>>>>>>>>>>>> "+credentials+" "+tokenAccessCookie+" "+uriInfo);
        return dao.login(credentials, tokenAccessCookie, uriInfo);

//        if (credentials == null
//            || credentials.getPassword() == null
//            || credentials.getPassword().isEmpty()
//            || credentials.getUsername() == null
//            || credentials.getUsername().isEmpty()) {
//            return Response.status(Response.Status.BAD_REQUEST).build();
//        }

//        boolean secure = uriInfo.getRequestUri().getScheme().equals("https");
//        AuthenticationHandler handler;
//        if (realm == null) {
//            handler = handlerProvider.getDefaultHandler();
//        } else {
//            handler = handlerProvider.getHandler(realm);
//            if (handler == null) {
//                throw new AuthenticationException("Unknown authentication type " + realm);
//            }
//        }

//        UniquePrincipal principal = handler.authenticate(credentials.getUsername(), credentials.getPassword());
//        if (principal == null) {
//            throw new AuthenticationException("Provided user and password is not valid");
//        }

        // DO NOT REMOVE! This log will be used in statistic analyzing
//        LOG.info("EVENT#user-sso-logged-in# USING#{}# USER#{}# ", handler.getType(), principal.getName());
//        Response.ResponseBuilder builder = Response.ok();
//        if (tokenAccessCookie != null) {
//            AccessTicket accessTicket = ticketManager.getAccessTicket(tokenAccessCookie.getValue());
//            if (accessTicket != null) {
//                if (!principal.equals(accessTicket.getPrincipal())) {
//                    // DO NOT REMOVE! This log will be used in statistic analyzing
//                    LOG.info("EVENT#user-changed-name# OLD-USER#{}# NEW-USER#{}#",
//                             accessTicket.getPrincipal().getName(),
//                             principal.getName());
//                    LOG.info("EVENT#user-sso-logged-out# USER#{}#", accessTicket.getPrincipal().getName());
//                    // DO NOT REMOVE! This log will be used in statistic analyzing
//                    ticketManager.removeTicket(accessTicket.getAccessToken());
//                }
//            } else {
//                //cookie is outdated, clearing
//                if (cookieBuilder != null) {
//                    cookieBuilder.clearCookies(builder, tokenAccessCookie.getValue(), secure);
//                }
//
//            }
//        }
//        // If we obtained principal  - authentication is done.
//        String token = uniqueTokenGenerator.generate();
//        ticketManager.putAccessTicket(new AccessTicket(token, principal, handler.getType()));
//        if (cookieBuilder != null) {
//            cookieBuilder.setCookies(builder, token, secure);
//        }
//        builder.entity(Collections.singletonMap("token", token));
//        return builder.build();
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

//        Response.ResponseBuilder response;
//        String accessToken = token;
//        if (accessToken == null && tokenAccessCookie != null) {
//            accessToken = tokenAccessCookie.getValue();
//        }
//
//        boolean secure = uriInfo.getRequestUri().getScheme().equals("https");
//        if (accessToken != null) {
//            response = Response.ok();
//            AccessTicket accessTicket = ticketManager.removeTicket(accessToken);
//            if (accessTicket != null) {
//                Principal userPrincipal = accessTicket.getPrincipal();
//                // DO NOT REMOVE! This log will be used in statistic analyzing
//                LOG.info("EVENT#user-sso-logged-out# USER#{}#", userPrincipal.getName());
//            } else {
//                LOG.warn("AccessTicket not found. Nothing to do.");
//            }
//        } else {
//            response = Response.status(Response.Status.BAD_REQUEST);
//            LOG.warn("Token not found in request.");
//        }
//        if (cookieBuilder != null) {
//            cookieBuilder.clearCookies(response, accessToken, secure);
//        }
//        return response.build();
    }

//    public static class Credentials {
//        private String username;
//        private String password;
//
//        public Credentials() {
//        }
//
//        public Credentials(String username, String password) {
//            this.username = username;
//            this.password = password;
//        }
//
//        public String getUsername() {
//            return username;
//        }
//
//        public void setUsername(String username) {
//            this.username = username;
//        }
//
//        public String getPassword() {
//            return password;
//        }
//
//        public void setPassword(String password) {
//            this.password = password;
//        }
//    }
}
