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

import com.codenvy.dto.shared.DTO;

/**
 * Provides information about state of slave builder.
 *
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 */
@DTO
public interface SlaveBuilderState {
    String getName();

    SlaveBuilderState withName(String name);

    void setName(String name);

    int getNumberOfWorkers();

    SlaveBuilderState withNumberOfWorkers(int numberOfWorkers);

    void setNumberOfWorkers(int numberOfWorkers);

    int getNumberOfActiveWorkers();

    SlaveBuilderState withNumberOfActiveWorkers(int numberOfActiveWorkers);

    void setNumberOfActiveWorkers(int numberOfActiveWorkers);

    int getInternalQueueSize();

    SlaveBuilderState withInternalQueueSize(int internalQueueSize);

    void setInternalQueueSize(int internalQueueSize);

    int getMaxInternalQueueSize();

    SlaveBuilderState withMaxInternalQueueSize(int maxInternalQueueSize);

    void setMaxInternalQueueSize(int maxInternalQueueSize);

    InstanceState getInstanceState();

    SlaveBuilderState withInstanceState(InstanceState instanceState);

    void setInstanceState(InstanceState instanceState);
}
