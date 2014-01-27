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
package com.codenvy.api.workspace.exception;

import com.codenvy.api.core.ApiException;
import com.codenvy.api.core.rest.shared.dto.ServiceError;

/**
 * @author Eugene Voevodin
 */
public class WorkspaceException extends ApiException {


    public WorkspaceException(ServiceError serviceError) {
        super(serviceError);
    }

    public WorkspaceException(String message) {
        super(message);
    }

    public WorkspaceException(String message, Throwable cause) {
        super(message, cause);
    }

    public WorkspaceException(Throwable cause) {
        super(cause);
    }
}
