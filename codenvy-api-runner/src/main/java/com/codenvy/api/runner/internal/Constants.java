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
    public static final String RUNNER_SCRIPT_FILES               = "runner.run_scripts";
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

    // config properties
    /**
     * Directory for deploy applications. All implementation of {@link com.codenvy.api.runner.internal.Runner} create sub-directories in
     * this directory for deploying applications.
     */
    public static final String DEPLOY_DIRECTORY     = "runner.deploy_directory";
    /** After this time all information about not running application may be removed. */
    public static final String APP_CLEANUP_TIME     = "runner.cleanup_time";
    /** Default size of memory for application in megabytes. Value that is provided by this property may be overridden by user settings. */
    public static final String APP_DEFAULT_MEM_SIZE = "runner.default_app_mem_size";
    /**
     * Max waiting time in seconds of application for the start. If process is not started after this time, it will be removed from the
     * queue.
     */
    public static final String WAITING_TIME         = "runner.waiting_time";
    /** Default lifetime in seconds of an application. After this time application may be terminated. */
    public static final String APP_LIFETIME         = "runner.app_lifetime";
    /** Name of configuration parameter that sets amount of memory (in megabytes) for running application. */
    public static final String TOTAL_APPS_MEM_SIZE  = "runner.total_apps_mem_size_mb";

    /* =========================================== */
    /** @deprecated user {@link #WAITING_TIME} */
    public static final String MAX_TIME_IN_QUEUE = WAITING_TIME;

    private Constants() {
    }
}
