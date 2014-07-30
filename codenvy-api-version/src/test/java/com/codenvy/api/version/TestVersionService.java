/*******************************************************************************
 * Copyright (c) 2012-2014 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package com.codenvy.api.version;

import com.jayway.restassured.response.Response;

import org.everrest.assured.EverrestJetty;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Map;

import static com.jayway.restassured.RestAssured.given;
import static org.testng.Assert.*;

/**
 * @author Anatoliy Bazko
 */
@Listeners(value = {EverrestJetty.class, MockitoTestNGListener.class})
public class TestVersionService {

    private final VersionService versionService;

    {
        try {
            versionService = new VersionService();
        } catch (IOException e) {
            throw new IllegalArgumentException();
        }
    }

    @Test
    public void shouldReturnComponentVersion() throws Exception {
        Response response = given().when().get("version/codenvy.platform-api");
        assertEquals(response.statusCode(), javax.ws.rs.core.Response.Status.OK.getStatusCode());

        Map m = response.body().as(Map.class);

        assertEquals(m.size(), 2);
        assertEquals(m.get(VersionService.COMPONENT), "codenvy.platform-api");
        assertNotNull(m.get(VersionService.VERSION));
        assertNotEquals(m.get(VersionService.VERSION), "");
    }

    @Test
    public void shouldReturnNotFoundStatusIfComponentUnknown() throws Exception {
        Response response = given().when().get("version/la-la-la");
        assertEquals(response.statusCode(), javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }
}

