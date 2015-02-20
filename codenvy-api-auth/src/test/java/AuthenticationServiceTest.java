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

import static com.jayway.restassured.RestAssured.given;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

import com.codenvy.api.auth.AuthenticationService;
import com.codenvy.api.auth.TokenManager;
import com.codenvy.api.auth.server.dto.DtoServerImpls;
import com.codenvy.api.auth.shared.dto.Credentials;
import com.codenvy.api.auth.shared.dto.Token;
import com.codenvy.api.core.ApiException;
import com.codenvy.api.user.server.dao.User;
import com.codenvy.api.user.server.dao.UserDao;
import com.codenvy.dto.server.DtoFactory;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.mapper.ObjectMapper;

import org.everrest.assured.EverrestJetty;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import javax.ws.rs.core.Response;

@Listeners(value = {EverrestJetty.class, MockitoTestNGListener.class})
public class AuthenticationServiceTest {

    @Mock
    UserDao      userDao;
    @Mock
    User         user;
    @Mock
    TokenManager tokenManager;

    @InjectMocks
    AuthenticationService service;

    @Test
    public void shouldFailIfNoBodyOnLogin() {
        given()
                .contentType(ContentType.JSON)
                .then()
                .expect().statusCode(Response.Status.BAD_REQUEST.getStatusCode())
                .when()
                .post("/auth/login");
    }

    @Test
    public void shouldFailIfUserNameIsNull() {
        given()
                .contentType(ContentType.JSON)
                .body(
                        DtoFactory.getInstance().createDto(Credentials.class)
                                  .withUsername(null)
                                  .withPassword("secret")
                     )
                .then()
                .expect().statusCode(Response.Status.BAD_REQUEST.getStatusCode())
                .when()
                .post("/auth/login");
    }

    @Test
    public void shouldFailIfUserNameIsEmpty() {
        given()
                .contentType(ContentType.JSON)
                .body(
                        DtoFactory.getInstance().createDto(Credentials.class)
                                  .withUsername("")
                                  .withPassword("secret")
                     )
                .then()
                .expect().statusCode(Response.Status.BAD_REQUEST.getStatusCode())
                .when()
                .post("/auth/login");
    }

    @Test
    public void shouldFailIfPasswordIsEmpty() {
        given()
                .contentType(ContentType.JSON)
                .body(
                        DtoFactory.getInstance().createDto(Credentials.class)
                                  .withUsername("User")
                                  .withPassword("")
                     )
                .then()
                .expect().statusCode(Response.Status.BAD_REQUEST.getStatusCode())
                .when()
                .post("/auth/login");
    }

    @Test
    public void shouldFailIfPasswordIsNull() {
        given()
                .contentType(ContentType.JSON)
                .body(
                        DtoFactory.getInstance().createDto(Credentials.class)
                                  .withUsername("User")
                                  .withPassword(null)
                     )
                .then()
                .expect().statusCode(Response.Status.BAD_REQUEST.getStatusCode())
                .when()
                .post("/auth/login");
    }


    @Test
    public void shouldReturnToken() throws ApiException {
        //given
        Token expected = DtoFactory.getInstance().createDto(Token.class).withValue("v1");
        when(userDao.authenticate(eq("User"), eq("password"))).thenReturn(true);
        when(userDao.getByAlias(eq("User"))).thenReturn(user);
        when(user.getId()).thenReturn("u-1");
        when(tokenManager.createToken(eq("u-1"))).thenReturn("v1");
        //when

        Token actual = given()
                .contentType(ContentType.JSON)
                .body(
                        DtoFactory.getInstance().createDto(Credentials.class)
                                  .withUsername("User")
                                  .withPassword("password")
                     )
                .then()
                .expect().statusCode(200)
                .when()
                .post("/auth/login").as(DtoServerImpls.TokenImpl.class, ObjectMapper.GSON);
        //then
        assertEquals(actual, expected);
        ArgumentCaptor<String> login = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> password = ArgumentCaptor.forClass(String.class);
        verify(userDao).authenticate(login.capture(), password.capture());

        assertEquals(login.getValue(), "User");
        assertEquals(password.getValue(), "password");

    }

    @Test
    public void shouldFailToLogoutIfTokenIsNull() {
        given()
                .then()
                .expect().statusCode(Response.Status.BAD_REQUEST.getStatusCode())
                .when()
                .post("/auth/logout");
    }

    @Test
    public void shouldCallLogoutOnDao() {
        //given
        //when
        given()
                .then()
                .expect().statusCode(204)
                .when()
                .post("/auth/logout?token=er00349");
        ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
        //then
        verify(tokenManager).removeToken(argument.capture());
        assertEquals(argument.getValue(), "er00349");

    }

}