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
package com.codenvy.api.runner.internal;

/** @author andrew00x */
public class Constants {
    // properties of project which are interesting for runner
    public static final String RUNNER_NAME                       = "runner.name";
    public static final String RUNNER_CUSTOM_LAUNCHER            = "runner.user_defined_launcher";
    public static final String RUNNER_MEMORY_SIZE                = "runner.${runner}.memsize";
    public static final String RUNNER_DEBUG_MODE                 = "runner.${runner}.debugmode";
    public static final String RUNNER_OPTIONS                    = "runner.${runner}.options";
    // rels for known runner links
    public static final String LINK_REL_REGISTER_RUNNER_SERVER   = "register runner server";
    public static final String LINK_REL_UNREGISTER_RUNNER_SERVER = "unregister runner server";
    public static final String LINK_REL_REGISTERED_RUNNER_SERVER = "registered runner server";
    public static final String LINK_REL_RUNNER_TASKS             = "runner tasks";
    public static final String LINK_REL_AVAILABLE_RUNNERS        = "available runners";
    public static final String LINK_REL_SERVER_STATE             = "server state";
    public static final String LINK_REL_RUNNER_STATE             = "runner state";
    public static final String LINK_REL_RUN                      = "run";
    public static final String LINK_REL_GET_STATUS               = "get status";
    public static final String LINK_REL_VIEW_LOG                 = "view logs";
    public static final String LINK_REL_WEB_URL                  = "web url";
    public static final String LINK_REL_STOP                     = "stop";

    private Constants() {
    }
}
