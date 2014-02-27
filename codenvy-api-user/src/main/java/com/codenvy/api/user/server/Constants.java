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
package com.codenvy.api.user.server;

/**
 * Constants for User API and UserProfile API
 *
 * @author Eugene Voevodin
 * @author Max Shaposhnik
 */
public final class Constants {

    public static final String LINK_REL_GET_CURRENT_USER_PROFILE    = "current user profile";
    public static final String LINK_REL_UPDATE_CURRENT_USER_PROFILE = "update current user profile";
    public static final String LINK_REL_GET_USER_PROFILE_BY_ID      = "user profile by id";
    public static final String LINK_REL_UPDATE_USER_PROFILE_BY_ID   = "update user profile by id";
    public static final String LINK_REL_CREATE_USER                 = "create user";
    public static final String LINK_REL_GET_CURRENT_USER            = "get current";
    public static final String LINK_REL_UPDATE_PASSWORD             = "update password";
    public static final String LINK_REL_GET_USER_BY_ID              = "get user by id";
    public static final String LINK_REL_GET_USER_BY_EMAIL           = "get user by email";
    public static final String LINK_REL_REMOVE_USER_BY_ID           = "remove user by id";
    public static final String LINK_REL_UPDATE_PREFS                = "update prefs";
    public static final int    ID_LENGTH                            = 16;
    public static final int    PASSWORD_LENGTH                      = 10;

    private Constants() {
    }
}
