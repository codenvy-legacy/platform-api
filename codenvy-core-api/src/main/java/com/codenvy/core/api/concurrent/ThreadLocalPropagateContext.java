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
package com.codenvy.core.api.concurrent;

import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Helps propagating ThreadLocal variables to the child Threads, e.g. when {@link java.util.concurrent.ExecutorService} is in use.
 * <p/>
 * Usage example:
 * <pre>
 * private static ThreadLocal&lt;MyClass&gt; myThreadLocal = new ThreadLocal&lt;&gt;();
 * static {
 *    // register ThreadLocal variable
 *    ThreadLocalPropagateContext.addThreadLocal(myThreadLocal);
 * }
 *
 * private ExecutorService executor = ...;
 *
 * ...
 *
 * // If need to start Runnable task with executor.
 * myThreadLocal.set(new MyClass()); // initialize ThreadLocal in 'main' thread
 * Runnable myRunnable = new Runnable() {
 *     public void run() {
 *         MyClass v = myThreadLocal.get(); // get ThreadLocal in 'child' thread and do something with it
 *     }
 * }
 * executor.submit(ThreadLocalPropagateContext.wrap(myRunnable)); // wrap Runnable and submit it to executor
 * </pre>
 *
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 */
public class ThreadLocalPropagateContext {

    private static CopyOnWriteArrayList<ThreadLocal<?>> toPropagate = new CopyOnWriteArrayList<>();

    /**
     * Register ThreadLocal in this context. After registration value of ThreadLocal from parent Thread is copied to the child thread when
     * method {@link #wrap(Runnable)} or {@link #wrap(java.util.concurrent.Callable)}.
     */
    public static void addThreadLocal(ThreadLocal<?> threadLocal) {
        if (threadLocal == null) {
            throw new IllegalArgumentException();
        }
        toPropagate.addIfAbsent(threadLocal);
    }

    /**
     * Get list of all registered ThreadLocal.
     *
     * @return list of all registered ThreadLocal
     */
    public static ThreadLocal<?>[] getThreadLocals() {
        return toPropagate.toArray(new ThreadLocal[toPropagate.size()]);
    }

    /**
     * Register ThreadLocal from this context.
     *
     * @see #addThreadLocal(ThreadLocal)
     */
    public static void removeThreadLocal(ThreadLocal<?> threadLocal) {
        if (threadLocal == null) {
            return;
        }
        toPropagate.remove(threadLocal);
    }

    /** Clear all registered ThreadLocal variables. */
    public void clear() {
        toPropagate.clear();
    }

    public static Runnable wrap(Runnable task) {
        return new CopyThreadLocalRunnable(task);
    }

    public static <T> Callable<T> wrap(Callable<? extends T> task) {
        return new CopyThreadLocalCallable<>(task);
    }

    static ThreadLocalState currentThreadState() {
        final ThreadLocal[] threadLocals = toPropagate.toArray(new ThreadLocal[toPropagate.size()]);
        final Object[] values = new Object[threadLocals.length];
        for (int i = 0, l = threadLocals.length; i < l; i++) {
            values[i] = threadLocals[i].get();
        }
        return new ThreadLocalState(threadLocals, values);
    }

    static class ThreadLocalState {
        private final ThreadLocal[] threadLocals;
        private final Object[]      values;
        private       Object[]      previousValues;

        private ThreadLocalState(ThreadLocal[] threadLocals, Object[] values) {
            this.threadLocals = threadLocals;
            this.values = values;
        }

        @SuppressWarnings("unchecked")
        void propagate() {
            previousValues = new Object[threadLocals.length];
            for (int i = 0, l = values.length; i < l; i++) {
                previousValues[i] = threadLocals[i].get();
                threadLocals[i].set(values[i]);
            }
        }

        @SuppressWarnings("unchecked")
        void cleanup() {
            if (previousValues == null) {
                return; // method propagate wasn't called
            }
            for (int i = 0, l = previousValues.length; i < l; i++) {
                threadLocals[i].set(previousValues[i]);
            }
        }
    }

    private ThreadLocalPropagateContext() {
    }
}
