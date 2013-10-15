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

/** @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a> */
public class Constants {
    // properties of project which are interesting for builder
    public static final String BUILDER_NAME                        = "builder.name";
    public static final String BUILDER_TARGETS                     = "builder.targets";
    public static final String BUILDER_OPTIONS                     = "builder.options";
    // rels for known links
    public static final String LINK_REL_REGISTER_BUILDER_SERVICE   = "register builder service";
    public static final String LINK_REL_UNREGISTER_BUILDER_SERVICE = "unregister builder service";
    public static final String LINK_REL_QUEUE_STATE                = "queue state";

    public static final String LINK_REL_AVAILABLE_BUILDERS    = "available builders";
    public static final String LINK_REL_BUILDER_STATE         = "builder state";
    public static final String LINK_REL_BUILD                 = "build";
    public static final String LINK_REL_DEPENDENCIES_ANALYSIS = "analyze dependencies";

    public static final String LINK_REL_GET_STATUS      = "get status";
    public static final String LINK_REL_VIEW_LOG        = "view build log";
    public static final String LINK_REL_VIEW_REPORT     = "view report";
    public static final String LINK_REL_DOWNLOAD_RESULT = "download result";
    public static final String LINK_REL_BROWSE          = "browse";
    public static final String LINK_REL_CANCEL          = "cancel";

    private Constants() {
    }
}
