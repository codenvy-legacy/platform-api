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
package com.codenvy.api.local;

import com.codenvy.api.auth.AuthenticationDao;
import com.codenvy.api.auth.AuthenticationException;
import com.codenvy.api.auth.shared.dto.Credentials;

import javax.inject.Singleton;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

@Singleton
public class LocalAuthenticationDaoImpl implements AuthenticationDao {
    @Override
    public Response login(Credentials credentials, Cookie tokenAccessCookie, UriInfo uriInfo) throws AuthenticationException {
        return Response.ok().entity("{\"value\":\"123123\"}").build();
    }

    @Override
    public Response logout(String token, Cookie tokenAccessCookie, UriInfo uriInfo) {
        return Response.ok().build();
    }
}
