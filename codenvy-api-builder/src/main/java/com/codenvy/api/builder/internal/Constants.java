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
package com.codenvy.api.builder.internal;

/** @author andrew00x */
public class Constants {
    // properties of project which are interesting for builder
    public static final String BUILDER_NAME                        = "builder.name";
    public static final String BUILDER_TARGETS                     = "builder.${builder}.targets";
    public static final String BUILDER_OPTIONS                     = "builder.${builder}.options";
    // rels for known builder links
    public static final String LINK_REL_REGISTER_BUILDER_SERVICE   = "register builder service";
    public static final String LINK_REL_UNREGISTER_BUILDER_SERVICE = "unregister builder service";
    public static final String LINK_REL_REGISTERED_BUILDER_SERVER  = "registered runner server";
    public static final String LINK_REL_QUEUE_STATE                = "queue state";

    public static final String LINK_REL_AVAILABLE_BUILDERS    = "available builders";
    public static final String LINK_REL_BUILDER_STATE         = "builder state";
    public static final String LINK_REL_SERVER_STATE          = "server state";
    public static final String LINK_REL_BUILD                 = "build";
    public static final String LINK_REL_DEPENDENCIES_ANALYSIS = "analyze dependencies";

    public static final String LINK_REL_GET_STATUS      = "get status";
    public static final String LINK_REL_VIEW_LOG        = "view build log";
    public static final String LINK_REL_VIEW_REPORT     = "view report";
    public static final String LINK_REL_DOWNLOAD_RESULT = "download result";
    public static final String LINK_REL_BROWSE          = "browse";
    public static final String LINK_REL_CANCEL          = "cancel";

    // config properties
    /** Name of configuration parameter that points to the directory where all builds stored. */
    public static final String BASE_DIRECTORY     = "builder.base_directory";
    /**
     * Name of configuration parameter that sets the number of build workers. In other words it set the number of build
     * process that can be run at the same time. If this parameter is set to -1 then the number of available processors
     * used, e.g. {@code Runtime.getRuntime().availableProcessors();}
     */
    public static final String NUMBER_OF_WORKERS  = "builder.workers_number";
    /**
     * Name of configuration parameter that sets time (in seconds) of keeping the results (artifact and logs) of build. After this time the
     * results of build may be removed.
     */
    public static final String KEEP_RESULT_TIME   = "builder.keep_result_time";
    /**
     * Name of parameter that set the max size of build queue. The number of build task in queue may not be greater than provided by this
     * parameter.
     */
    public static final String QUEUE_SIZE         = "builder.queue_size";
    /**
     * Max waiting time in seconds for starting build process. If process is not started after this time, it will be removed from the
     * queue.
     */
    public static final String WAITING_TIME       = "builder.waiting_time";
    /** Max execution time in seconds for a build process. After this time build may be terminated. */
    public static final String MAX_EXECUTION_TIME = "builder.max_execution_time";

    /* ================================================= */

    /** @deprecated use {@link #BASE_DIRECTORY} */
    public static final String REPOSITORY          = BASE_DIRECTORY;
    /** @deprecated use {@link #KEEP_RESULT_TIME} */
    public static final String CLEANUP_RESULT_TIME = KEEP_RESULT_TIME;
    /** @deprecated use {@link #QUEUE_SIZE} */
    public static final String INTERNAL_QUEUE_SIZE = QUEUE_SIZE;
    /** @deprecated user {@link #WAITING_TIME} */
    public static final String MAX_TIME_IN_QUEUE   = WAITING_TIME;
    /** @deprecated use {@link #MAX_EXECUTION_TIME} */
    public static final String BUILD_TIMEOUT       = MAX_EXECUTION_TIME;

    private Constants() {
    }
}
