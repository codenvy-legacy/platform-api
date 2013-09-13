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
package com.codenvy.api.core.rest.dto;

/**
 * Types for all know DTO objects.
 *
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 * @see DtoType
 * @see JsonDto
 */
public class DtoTypes {
    public static final int LINK_TYPE                    = 1;
    public static final int PARAMETER_DESCRIPTOR_TYPE    = 2;
    public static final int PARAMETER_TYPE_TYPE          = 3;
    public static final int REQUEST_BODY_DESCRIPTOR_TYPE = 4;
    public static final int SERVICE_DESCRIPTOR_TYPE      = 5;
    public static final int SERVICE_ERROR_TYPE           = 6;

    private DtoTypes() {
    }
}
