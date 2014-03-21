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
package com.codenvy.api.builder;

/**
 * Thrown when particular build process is request but isn't available.
 *
 * @author andrew00x
 * @see BuildQueue#getTask(Long)
 * @see com.codenvy.api.builder.internal.Builder#getBuildTask(Long)
 */
@SuppressWarnings("serial")
public final class NoSuchBuildTaskException extends BuilderException {
    public NoSuchBuildTaskException(Long taskId) {
        this(String.format("Invalid build task id: %d", taskId));
    }

    public NoSuchBuildTaskException(String message) {
        super(message);
    }
}
