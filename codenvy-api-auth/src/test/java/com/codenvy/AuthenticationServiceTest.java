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
package com.codenvy.cloudide.auth.server;


import com.codenvy.api.auth.AuthenticationService;
import com.codenvy.api.auth.sso.client.AuthorizedPrincipal;
import com.codenvy.api.auth.sso.server.RolesExtractor;
import com.codenvy.api.auth.sso.server.SsoClientManager;
import com.codenvy.api.auth.sso.server.TicketManager;
import com.codenvy.api.auth.sso.server.TokenGenerator;
import com.jayway.restassured.response.Cookie;
import com.jayway.restassured.response.Response;

import org.everrest.assured.EverrestJetty;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.*;

import java.security.Principal;

import static com.jayway.restassured.RestAssured.given;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

/**
 * Test for AuthenticationService class.
 *
 * @author Sergii Kabashniuk
 */
@Listeners(value = {EverrestJetty.class, MockitoTestNGListener.class})
public class AuthenticationServiceTest {
    @Mock
    TicketManager         ticketManager;
    @Mock
    SsoClientManager      clientManager;
    @Mock
    TokenGenerator        uniqueTokenGenerator;
    @Mock
    RolesExtractor        rolesExtractor;
    @InjectMocks
    AuthenticationService authenticationService;

    @BeforeMethod
    public void init() {
        when(uniqueTokenGenerator.generate(12)).thenReturn("0123456789012");
        when(rolesExtractor.extractRoles(any(Principal.class))).thenReturn(new AuthorizedPrincipal("user"));
    }

    @Test(enabled = false)
    public void shouldReturn400IfAuthHandlerNotFound() throws Exception {
        // given
        // when
        Response response = given()
                .queryParam("authType", "unexisted")
                .when()
                .post("/user/authenticate");

        // then
        assertEquals(response.statusCode(), 400);

    }

    @Test(enabled = false)
    public void shouldAuthenticateWithCorrectParams() throws Exception {
        // when
        Response response = given()
                .queryParam("authType", "jaas")
                .then()
                .expect()
                .when()
                .post("/user/authenticate");

        Cookie logged_in = response.detailedCookie("token-access-key");
        // then
        assertEquals(response.statusCode(), 200);
    }
}
