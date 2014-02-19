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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import java.security.Principal;

import static com.jayway.restassured.RestAssured.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
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
    @Mock
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
        token = "t1";
        tokenOld = "t2";
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

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                Response.ResponseBuilder builder = (Response.ResponseBuilder)args[0];
                String token = (String)args[1];
                boolean secure = (boolean)args[2];
                if (token != null && !token.isEmpty()) {
                    builder.header("Set-Cookie",
                                   new NewCookie("token-access-key", token, "/sso/server", null, null, 0, secure) + ";HttpOnly");
                    builder.header("Set-Cookie", new NewCookie("session-access-key", token, "/", null, null, 0, secure) + ";HttpOnly");
                }
                builder.cookie(new NewCookie("logged_in", "true", "/", null, null, 0, secure));
                return null;
            }
        }).when(cookieBuilder).setCookies(any(Response.ResponseBuilder.class), anyString(), anyBoolean());


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
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                Response.ResponseBuilder builder = (Response.ResponseBuilder)args[0];
                String token = (String)args[1];
                boolean secure = (boolean)args[2];
                if (token != null && !token.isEmpty()) {
                    builder.header("Set-Cookie",
                                   new NewCookie("token-access-key", token, "/sso/server", null, null, 0, secure) + ";HttpOnly");
                    builder.header("Set-Cookie", new NewCookie("session-access-key", token, "/", null, null, 0, secure) + ";HttpOnly");
                }
                builder.cookie(new NewCookie("logged_in", "true", "/", null, null, 0, secure));
                return null;
            }
        }).when(cookieBuilder).setCookies(any(Response.ResponseBuilder.class), anyString(), anyBoolean());


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
