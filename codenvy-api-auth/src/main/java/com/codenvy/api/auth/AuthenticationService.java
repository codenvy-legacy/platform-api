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

import com.codenvy.api.auth.sso.server.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.net.URI;
import java.security.Principal;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * Public rest service to access user specific information.
 *
 * @author Sergii Kabashniuk
 * @author Alexander Garagatyi
 */
@Path("auth")
public class AuthenticationService {
    public final static  long   TICKET_LIFE_TIME_SECONDS = TimeUnit.DAYS.toSeconds(3);
    private static final Logger LOG                      = LoggerFactory.getLogger(AuthenticationService.class);
    private final static URI    REDIRECT_AFTER_LOGOUT    = URI.create("/site/login");
    @Inject
    protected AuthenticationHandler handler;
    @Inject
    protected TicketManager         ticketManager;
    @Inject
    protected SsoClientManager      clientManager;
    @Inject
    protected TokenGenerator        uniqueTokenGenerator;

    /**
     * Get token to be able to call secure api methods.
     *
     * @param tokenAccessCookie
     *         - old session-based cookie with token
     * @param credentials
     *         - username and password
     * @param httpServletRequest
     * @return - auth token in JSON, session-based and persistent cookies
     * @throws AuthenticationException
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("login")
    public Response authenticate(@CookieParam("session-access-key") Cookie tokenAccessCookie,
                                 Credentials credentials,
                                 @Context HttpServletRequest httpServletRequest)
            throws AuthenticationException {

        if (handler == null) {
            LOG.error("Jaas authenticator is null.");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }

        if (credentials == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        Principal principal = handler.authenticate(credentials.getUsername(), credentials.getPassword(), httpServletRequest);

        Response.ResponseBuilder response = Response.ok();

        // DO NOT REMOVE! This log will be used in statistic analyzing
        LOG.info("EVENT#user-sso-logged-in# USING#{}# USER#{}# ", "jaas", principal.getName());
        if (tokenAccessCookie != null) {
            AccessTicket accessTicket = ticketManager.getAccessTicket(tokenAccessCookie.getValue());
            if (accessTicket != null) {
                if (!principal.equals(accessTicket.getPrincipal())) {
                    // DO NOT REMOVE! This log will be used in statistic analyzing
                    LOG.info("EVENT#user-changed-name# OLD-USER#{}# NEW-USER#{}#", accessTicket.getPrincipal().getName(),
                             principal.getName());
                    LOG.info("EVENT#user-sso-logged-out# USER#{}#", accessTicket.getPrincipal().getName());
                    // DO NOT REMOVE! This log will be used in statistic analyzing
                    clientManager.logout(accessTicket);
                }
            } else {
                //cookie is outdated
                CookieTools.clearCookies(response, tokenAccessCookie.getValue(), httpServletRequest.isSecure());
            }
        }

        final AccessTicket ticket = new AccessTicket(uniqueTokenGenerator.generate(12), principal);
        ticketManager.putAccessTicket(ticket);

        CookieTools.setCookies(response, ticket.getAccessToken(), httpServletRequest.isSecure(), false);

        return response.entity(Collections.singletonMap("token", ticket.getAccessToken())).build();
    }

    @GET
    @Path("logout")
    public Response logout(@QueryParam("token") String token, @CookieParam("session-access-key") Cookie tokenAccessCookie,
                           @Context HttpServletRequest httpServletRequest) {
        Response.ResponseBuilder response;

        String accessToken = token;
        if (accessToken == null && tokenAccessCookie != null) {
            accessToken = tokenAccessCookie.getValue();
        }

        if (accessToken != null) {
            response = Response.temporaryRedirect(REDIRECT_AFTER_LOGOUT);
            AccessTicket accessTicket = ticketManager.getAccessTicket(accessToken);
            if (accessTicket != null) {
                Principal userPrincipal = accessTicket.getPrincipal();
                clientManager.logout(accessTicket);
                // DO NOT REMOVE! This log will be used in statistic analyzing
                LOG.info("EVENT#user-sso-logged-out# USER#{}#", userPrincipal.getName());

                CookieTools.clearCookies(response, accessTicket.getAccessToken(), httpServletRequest.isSecure());
            } else {
                LOG.error("AccessTicket not found. Not able to do SSO logout. ");
                response = Response.status(Response.Status.NOT_FOUND);
            }
        } else {
            LOG.error("Token not found in request.");
            response = Response.status(Response.Status.BAD_REQUEST);
        }

        response.cookie(new NewCookie("logged_in", "true", "/", null, null, 0, httpServletRequest.isSecure()));

        return response.build();
    }

    public static class Credentials {
        private String username;
        private String password;

        public Credentials() {
        }

        public Credentials(String username, String password) {
            this.username = username;
            this.password = password;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }
}
