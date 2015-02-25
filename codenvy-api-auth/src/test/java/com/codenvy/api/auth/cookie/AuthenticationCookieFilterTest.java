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
package com.codenvy.api.auth.cookie;

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
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.config.HttpClientConfig;
import com.jayway.restassured.config.RestAssuredConfig;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.mapper.ObjectMapper;
import com.jayway.restassured.response.Cookies;

import org.everrest.assured.EverrestJetty;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.lang.reflect.Field;
import java.util.Map;

@Listeners(value = {EverrestJetty.class, MockitoTestNGListener.class})
public class AuthenticationCookieFilterTest {


    @Mock
    UserDao      userDao;
    @Mock
    User         user;
    @Mock
    TokenManager tokenManager;


    AuthenticationCookieFilter filter = new AuthenticationCookieFilter(tokenManager, new CookieTokenExtractor(),
                                                                       new AuthenticationCookiesBuilder("/access/path", 5000));

    @InjectMocks
    AuthenticationService service;

    @BeforeMethod
    public void init() throws NoSuchFieldException, IllegalAccessException {
        Field tokenManagerField = AuthenticationCookieFilter.class.getDeclaredField("tokenManager");
        tokenManagerField.setAccessible(true);
        tokenManagerField.set(filter, tokenManager);
        //http.protocol.single-cookie-header

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

        RestAssured.config =
                RestAssuredConfig.config().httpClient(
                        HttpClientConfig.httpClientConfig().setParam("http.protocol.single-cookie-header", true));

        Cookies actual = given()

                .contentType(ContentType.JSON)
                .body(
                        DtoFactory.getInstance().createDto(Credentials.class)
                                  .withUsername("User")
                                  .withPassword("password")
                     )
                .then()
                .expect().statusCode(200)
                .when()
                .log().all(true)
                .post("/auth/login").detailedCookies();
        //then
        assertEquals(actual, expected);
//        ArgumentCaptor<String> login = ArgumentCaptor.forClass(String.class);
//        ArgumentCaptor<String> password = ArgumentCaptor.forClass(String.class);
//        verify(userDao).authenticate(login.capture(), password.capture());
//
//        assertEquals(login.getValue(), "User");
//        assertEquals(password.getValue(), "password");

    }

}