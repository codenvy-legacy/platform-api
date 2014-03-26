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
package com.codenvy.api.auth.oauth;

import com.codenvy.api.auth.shared.dto.OAuthToken;

import java.io.IOException;

/** Retrieves user token from OAuth providers. */
public interface OAuthTokenProvider {
    /**
     * Get oauth token.
     *
     * @param oauthProviderName
     *         - name of provider.
     * @param userId
     *         - user
     * @return oauth token or <code>null</code>
     * @throws IOException
     *         if i/o error occurs when try to refresh expired oauth token
     */
    OAuthToken getToken(String oauthProviderName, String userId) throws IOException;
}
