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
package com.codenvy.api.runner.internal;

/** @author andrew00x */
public class Constants {
    // properties of project which are interesting for runner
    public static final String RUNNER_NAME                       = "runner.name";
    public static final String RUNNER_ENV_ID                     = "runner.env_id";
    public static final String RUNNER_CUSTOM_LAUNCHER            = "runner.user_defined_launcher";
    public static final String RUNNER_SCRIPT_FILES               = "runner.run_scripts";
    public static final String RUNNER_MEMORY_SIZE                = "runner.${runner}.memsize";
    public static final String RUNNER_DEBUG_MODE                 = "runner.${runner}.debugmode";
    public static final String RUNNER_OPTIONS                    = "runner.${runner}.options";
    public static final String RUNNER_WS_MAX_MEMORY_SIZE         = "runner.workspace.max_memsize";
    public static final String RUNNER_SLAVE_RUNNER_URLS          = "runner.slave_runner_urls";
    public static final String RUNNER_MAX_MEMORY_SIZE            = "codenvy:runner_ram";
    public static final String RUNNER_LIFETIME                   = "codenvy:runner_lifetime";
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
    public static final String LINK_REL_RUNNER_RECIPE            = "runner recipe";

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
