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
package com.codenvy.api.core.concurrent;

import java.util.concurrent.Callable;

/** @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a> */
public class CopyThreadLocalCallable<T> implements Callable<T> {
    private final Callable<? extends T>                        wrapped;
    private final ThreadLocalPropagateContext.ThreadLocalState threadLocalState;

    CopyThreadLocalCallable(Callable<? extends T> wrapped) {
        // Called from main thread. Copy the current values of all the ThreadLocal variables which registered in ThreadLocalPropagateContext.
        this.wrapped = wrapped;
        this.threadLocalState = ThreadLocalPropagateContext.currentThreadState();
    }

    @Override
    public T call() throws Exception {
        try {
            threadLocalState.propagate();
            return wrapped.call();
        } finally {
            threadLocalState.cleanup();
        }
    }

    public Callable<? extends T> getWrapped() {
        return wrapped;
    }
}
