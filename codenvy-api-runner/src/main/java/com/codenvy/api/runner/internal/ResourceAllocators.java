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

import com.codenvy.api.core.config.SingletonConfiguration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Semaphore;

/** @author <a href="mailto:aparfonov@codenvy.com">Andrey Parfonov</a> */
public class ResourceAllocators {
    public static final String TOTAL_APPS_MEM_SIZE = "runner.total_apps_mem_size_mb";

    private static final Logger LOG = LoggerFactory.getLogger(ResourceAllocators.class);

    private static class ResourceAllocatorsHolder {
        static final ResourceAllocators INSTANCE = new ResourceAllocators(SingletonConfiguration.get().getInt(TOTAL_APPS_MEM_SIZE, -1));
    }

    public static ResourceAllocators getInstance() {
        return ResourceAllocatorsHolder.INSTANCE;
    }

    private final int       memSize;
    private final Semaphore memSemaphore;

    private ResourceAllocators(int memSize) {
        if (memSize <= 0) {
            throw new IllegalArgumentException(String.format("Invalid mem size %d", memSize));
        }
        this.memSize = memSize;
        memSemaphore = new Semaphore(memSize);
    }

    public ResourceAllocator newMemoryAllocator(int size) {
        return new MemoryAllocator(size);
    }


    public int freeMemory() {
        return memSemaphore.availablePermits();
    }

    public int totalMemory() {
        return memSize;
    }

    //

    public static interface ResourceAllocator {
        ResourceAllocator allocate() throws AllocateResourceException;

        void release();
    }

    private class MemoryAllocator implements ResourceAllocator {
        final int size;

        MemoryAllocator(int size) {
            this.size = size;
        }

        @Override
        public MemoryAllocator allocate() throws AllocateResourceException {
            if (!memSemaphore.tryAcquire(size)) {
                throw new AllocateResourceException(String.format("Couldn't allocate %dM for starting application", size));
            }
            LOG.info("allocate memory: {}M, available: {}M", size, memSemaphore.availablePermits()); // TODO: debug
            return this;
        }

        @Override
        public void release() {
            memSemaphore.release(size);
            LOG.info("release memory: {}M, available: {}M", size, memSemaphore.availablePermits());  // TODO: debug
        }
    }
}
