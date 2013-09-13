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
package com.codenvy.api.builder.internal.dto;

/**
 * Types for all know DTO objects of Builder API.
 *
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 * @see com.codenvy.api.core.rest.dto.DtoType
 * @see com.codenvy.api.core.rest.dto.JsonDto
 */
public class BuilderDtoTypes {
    public static final int BUILDER_LIST_TYPE          = 100;
    public static final int BUILD_TASK_DESCRIPTOR_TYPE = 101;
    public static final int BUILDER_DESCRIPTOR_TYPE    = 102;
    public static final int BUILD_REQUEST_TYPE         = 103;
    public static final int BUILD_STATUS_TYPE          = 104;
    public static final int DEPENDENCY_REQUEST_TYPE    = 105;
    public static final int BUILDER_STATE_TYPE         = 106;
    public static final int INSTANCE_STATE_TYPE        = 107;

    private BuilderDtoTypes() {
    }
}
