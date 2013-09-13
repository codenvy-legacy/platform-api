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

/**
 * Build listener abstraction. FQN of classes which implement this interface and have a zero-argument constructor may be add in file
 * <pre>META-INF/services/com.codenvy.api.builder.BuildListener</pre>. In this case it will be loaded automatically by {@code Builder}.
 * Implementation of this interface may be registered in {@code Builder} with method {@link com.codenvy.api.builder.internal.Builder#addBuildListener(BuildListener)}.
 *
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 * @see com.codenvy.api.builder.internal.Builder#addBuildListener(BuildListener)
 * @see com.codenvy.api.builder.internal.Builder#removeBuildListener(BuildListener)
 */
public interface BuildListener {
    /** Builder invokes this method when build process starts. */
    void begin(BuildTask task);

    /** Builder invokes this method when build process ends. */
    void end(BuildTask task);
}
