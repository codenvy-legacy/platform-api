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

/**
 * Implementation of this interface is used for registering in {@link com.codenvy.api.runner.internal.Runner} and disposing the data
 * associated with application process. Disposers associated with {@link com.codenvy.api.runner.internal.ApplicationProcess} and method
 * {@link #dispose()} called before remove {@link com.codenvy.api.runner.internal.ApplicationProcess} from internal Runner's registry.
 * NOTE: Method {@link #dispose()} isn't called just after stopping of application. Runners may call method {@link #dispose()} at any time
 * after stopping of application.
 *
 * @author andrew00x
 * @see com.codenvy.api.runner.internal.Runner#registerDisposer(ApplicationProcess, Disposer)
 */
public interface Disposer {
    /**
     * Disposes the data associated with {@link com.codenvy.api.runner.internal.ApplicationProcess}.
     *
     * @see com.codenvy.api.runner.internal.Runner#registerDisposer(ApplicationProcess, Disposer)
     */
    void dispose();
}
