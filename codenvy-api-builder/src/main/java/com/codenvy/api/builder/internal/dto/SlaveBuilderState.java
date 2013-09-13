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

import com.codenvy.api.core.rest.dto.DtoType;

/** @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a> */
@DtoType(BuilderDtoTypes.BUILDER_STATE_TYPE)
public class SlaveBuilderState {
    private String        name;
    private int           numberOfWorkers;
    private int           numberOfActiveWorkers;
    private int           internalQueueSize;
    private int           maxInternalQueueSize;
    private InstanceState instanceState;

    public SlaveBuilderState(String name,
                             int numberOfWorkers,
                             int numberOfActiveWorkers,
                             int internalQueueSize,
                             int maxInternalQueueSize,
                             InstanceState instanceState) {
        this.name = name;
        this.numberOfWorkers = numberOfWorkers;
        this.numberOfActiveWorkers = numberOfActiveWorkers;
        this.internalQueueSize = internalQueueSize;
        this.maxInternalQueueSize = maxInternalQueueSize;
        this.instanceState = instanceState;
    }

    public SlaveBuilderState() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getNumberOfWorkers() {
        return numberOfWorkers;
    }

    public void setNumberOfWorkers(int numberOfWorkers) {
        this.numberOfWorkers = numberOfWorkers;
    }

    public int getNumberOfActiveWorkers() {
        return numberOfActiveWorkers;
    }

    public void setNumberOfActiveWorkers(int numberOfActiveWorkers) {
        this.numberOfActiveWorkers = numberOfActiveWorkers;
    }

    public int getInternalQueueSize() {
        return internalQueueSize;
    }

    public void setInternalQueueSize(int internalQueueSize) {
        this.internalQueueSize = internalQueueSize;
    }

    public int getMaxInternalQueueSize() {
        return maxInternalQueueSize;
    }

    public void setMaxInternalQueueSize(int maxInternalQueueSize) {
        this.maxInternalQueueSize = maxInternalQueueSize;
    }

    public InstanceState getInstanceState() {
        return instanceState;
    }

    public void setInstanceState(InstanceState instanceState) {
        this.instanceState = instanceState;
    }

    @Override
    public String toString() {
        return "SlaveBuilderState{" +
               "name='" + name + '\'' +
               ", numberOfWorkers=" + numberOfWorkers +
               ", numberOfActiveWorkers=" + numberOfActiveWorkers +
               ", internalQueueSize=" + internalQueueSize +
               ", maxInternalQueueSize=" + maxInternalQueueSize +
               ", instanceState=" + instanceState +
               '}';
    }
}
