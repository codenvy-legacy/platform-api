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
package com.codenvy;


import com.codenvy.api.auth.*;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.mapper.ObjectMapper;

import org.everrest.assured.EverrestJetty;
import org.mockito.*;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.*;

import javax.ws.rs.ext.ExceptionMapper;
import java.security.Principal;

import static com.jayway.restassured.RestAssured.given;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
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
    protected AuthenticationHandler handler;
    @Mock
    protected Principal             principal;
    @Mock
    protected Principal             oldPrincipal;
    @Mock
    protected TicketManager         ticketManager;
    @Spy
    protected CookieBuilder         cookieBuilder;
    @Mock
    protected TokenGenerator        uniqueTokenGenerator;
    @InjectMocks
    protected AuthenticationService authenticationService;
    protected String                token;
    protected String                tokenOld;
    private ExceptionMapper exceptionMapper = new AuthenticationExceptionMapper();

    @BeforeMethod
    public void init() {
        TokenGenerator tokenGenerator = new TokenGenerator();
        token = tokenGenerator.generate();
        tokenOld = tokenGenerator.generate();
        when(ticketManager.getAccessTicket(eq(tokenOld))).thenReturn(new AccessTicket(tokenOld, oldPrincipal));

        when(uniqueTokenGenerator.generate()).thenReturn(token);
    }

    @Test
    public void shouldAuthenticateWithCorrectParams() throws Exception {
        //given
        when(handler.authenticate(eq("user@site.com"), eq("secret"))).thenReturn(new Principal() {
            @Override
            public String getName() {
                return "user@site.com";
            }
        });

        // when
        TokenResponse response = given()
                .contentType(ContentType.JSON)
                .body(new AuthenticationService.Credentials("user@site.com", "secret"))
                .then()
                .expect().statusCode(200)
                .cookie("token-access-key", token)
                .cookie("session-access-key", token)
                .cookie("logged_in", "true")

                .when()
                .post("/auth/login").as(TokenResponse.class, ObjectMapper.GSON);

        ArgumentCaptor<AccessTicket> argument = ArgumentCaptor.forClass(AccessTicket.class);
        verify(ticketManager).putAccessTicket(argument.capture());
        assertEquals(response.getToken(), token);
        assertEquals(argument.getValue().getAccessToken(), token);
    }

    @Test
    public void shouldReturnBadRequestIfLoginIsNotSet() throws Exception {
        //given
        // when
        given()
                .contentType(ContentType.JSON)
                .body(new AuthenticationService.Credentials(null, "secret"))
                .then()
                .expect().statusCode(400)
                .when()
                .post("/auth/login");
    }

    @Test
    public void shouldReturnBadRequestIfLoginIsEmpty() throws Exception {
        //given
        // when
        given()
                .contentType(ContentType.JSON)
                .body(new AuthenticationService.Credentials("", "secret"))
                .then()
                .expect().statusCode(400)
                .when()
                .post("/auth/login");
    }

    @Test
    public void shouldReturnBadRequestIfPassowordIsNotSet() throws Exception {
        //given
        // when
        given()
                .contentType(ContentType.JSON)
                .body(new AuthenticationService.Credentials("user@site.com", null))
                .then()
                .expect().statusCode(400)
                .when()
                .post("/auth/login");
    }

    @Test
    public void shouldReturnBadRequestIfPasswordIsEmpty() throws Exception {
        //given
        // when
        given()
                .contentType(ContentType.JSON)
                .body(new AuthenticationService.Credentials("user@site.com", ""))
                .then()
                .expect().statusCode(400)
                .when()
                .post("/auth/login");
    }

    @Test
    public void shouldReturnBadRequestIfHandlerNotAbleToAuthenticate() throws Exception {
        //given
        // when
        given()
                .contentType(ContentType.JSON)
                .body(new AuthenticationService.Credentials("user@site.com", "asdfasdf"))
                .then()
                .expect().statusCode(400)
                .when()
                .post("/auth/login");
    }

    @Test
    public void shouldLogoutFirstIfUserAlreadyLoggedIn() throws Exception {
        //given
        when(handler.authenticate(eq("user@site.com"), eq("secret"))).thenReturn(new Principal() {
            @Override
            public String getName() {
                return "user@site.com";
            }
        });

        when(oldPrincipal.getName()).thenReturn("old@site.com");


        // when
        TokenResponse response = given()
                .contentType(ContentType.JSON)
                .cookie("session-access-key", tokenOld)
                .body(new AuthenticationService.Credentials("user@site.com", "secret"))
                .then()
                .expect().statusCode(200)
                .cookie("token-access-key", token)
                .cookie("session-access-key", token)
                .cookie("logged_in", "true")

                .when()
                .post("/auth/login").as(TokenResponse.class, ObjectMapper.GSON);

        ArgumentCaptor<AccessTicket> argument = ArgumentCaptor.forClass(AccessTicket.class);
        verify(ticketManager).removeTicket(eq(tokenOld));
        verify(ticketManager).putAccessTicket(argument.capture());
        assertEquals(response.getToken(), token);
        assertEquals(argument.getValue().getAccessToken(), token);

    }

    @Test
    public void shouldBeAbleToLogoutByQueryParameter() throws Exception {
        //given
        when(ticketManager.removeTicket(eq(tokenOld))).thenReturn(new AccessTicket(tokenOld, oldPrincipal));
        // when
        given()
                .contentType(ContentType.JSON)
                .queryParam("token", tokenOld)
                .then()
                .expect().statusCode(200)
                .when()
                .post("/auth/logout");
        //then
        verify(ticketManager).removeTicket(eq(tokenOld));
    }

    @Test
    public void shouldBeAbleToLogoutByCookie() throws Exception {
        //given
        when(ticketManager.removeTicket(eq(tokenOld))).thenReturn(new AccessTicket(tokenOld, oldPrincipal));
        // when
        given()
                .contentType(ContentType.JSON)
                .cookie("session-access-key", tokenOld)
                .then()
                .expect().statusCode(200)
                .when()
                .post("/auth/logout");
        //then
        verify(ticketManager).removeTicket(eq(tokenOld));
    }

    @Test
    public void shouldFailToLogoutWithoutToken() throws Exception {
        //given
        // when
        given()
                .contentType(ContentType.JSON)
                .then()
                .expect().statusCode(400)
                .when()
                .post("/auth/logout");

    }

    public static class TokenResponse {
        private String token;

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }
    }
}
