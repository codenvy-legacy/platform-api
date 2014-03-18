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
package com.codenvy.api.workspace.server;

/**
 * Constants for Workspace API
 *
 * @author Eugene Voevodin
 */
public final class Constants {

    public static final String LINK_REL_CREATE_WORKSPACE             = "create workspace";
    public static final String LINK_REL_GET_CURRENT_USER_WORKSPACES  = "current user workspaces";
    public static final String LINK_REL_GET_WORKSPACES_BY_ACCOUNT    = "all workspaces of given account";
    public static final String LINK_REL_GET_CONCRETE_USER_WORKSPACES = "concrete user workspaces";
    public static final String LINK_REL_GET_WORKSPACE_BY_ID          = "workspace by id";
    public static final String LINK_REL_GET_WORKSPACE_BY_NAME        = "workspace by name";
    public static final String LINK_REL_UPDATE_WORKSPACE_BY_ID       = "update by id";
    public static final String LINK_REL_GET_WORKSPACE_MEMBERS        = "get members";
    public static final String LINK_REL_ADD_WORKSPACE_MEMBER         = "add member";
    public static final String LINK_REL_REMOVE_WORKSPACE_MEMBER      = "remove member";
    public static final String LINK_REL_REMOVE_WORKSPACE             = "remove workspace";
    public static final String LINK_REL_CREATE_TEMP_WORKSPACE        = "create temp workspace";
    public static final int    ID_LENGTH                             = 16;

    private Constants() {
    }
}