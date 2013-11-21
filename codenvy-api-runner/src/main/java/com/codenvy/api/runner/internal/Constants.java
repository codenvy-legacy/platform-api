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

/** @author <a href="mailto:aparfonov@codenvy.com">Andrey Parfonov</a> */
public class Constants {
    // properties of project which are interesting for runner
    public static final String RUNNER_NAME                        = "runner_name";
    public static final String RUNNER_MEMORY_SIZE                 = "runner_${runner}_memsize";
    public static final String RUNNER_DEBUG_MODE                  = "runner_${runner}_debugmode";
    public static final String RUNNER_OPTIONS                     = "runner_${runner}_options";
    // rels for known runner links
    public static final String LINK_REL_REGISTER_RUNNER_SERVICE   = "register runner service";
    public static final String LINK_REL_UNREGISTER_RUNNER_SERVICE = "unregister runner service";
    public static final String LINK_REL_AVAILABLE_RUNNERS         = "available runners";
    public static final String LINK_REL_RUNNER_STATE              = "runners state";
    public static final String LINK_REL_RUN                       = "run";
    public static final String LINK_REL_GET_STATUS                = "get status";
    public static final String LINK_REL_VIEW_LOG                  = "view logs";
    public static final String LINK_REL_STOP                      = "stop";

    private Constants() {
    }
}
