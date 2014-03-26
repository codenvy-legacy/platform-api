/*
 * CODENVY CONFIDENTIAL
 * __________________
 * 
 *  [2012] - [2014] Codenvy, S.A. 
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
package com.codenvy.api.organization.server;

/**
 * Constants for Organization API
 *
 * @author Eugene Voevodin
 */
public final class Constants {

    public static final String LINK_REL_CREATE_ORGANIZATION       = "create";
    public static final String LINK_REL_GET_ORGANIZATION_BY_ID    = "get by id";
    public static final String LINK_REL_GET_ORGANIZATION_BY_NAME  = "get by name";
    public static final String LINK_REL_UPDATE_ORGANIZATION       = "update";
    public static final String LINK_REL_GET_SUBSCRIPTIONS         = "subscriptions";
    public static final String LINK_REL_ADD_SUBSCRIPTION          = "add subscription";
    public static final String LINK_REL_REMOVE_SUBSCRIPTION       = "remove subscription";
    public static final String LINK_REL_REMOVE_ORGANIZATION       = "remove";
    public static final String LINK_REL_GET_MEMBERS               = "members";
    public static final String LINK_REL_ADD_MEMBER                = "add member";
    public static final String LINK_REL_REMOVE_MEMBER             = "remove member";
    public static final String LINK_REL_GET_CURRENT_ORGANIZATIONS = "get organizations";
    public static final int    ID_LENGTH                          = 16;

    private Constants() {
    }
}
