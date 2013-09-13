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
package com.codenvy.api.builder.manager.dto;

import com.codenvy.api.core.rest.dto.DtoType;

/** @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a> */
@DtoType(BuilderManagerDtoTypes.BUILDER_STATE_TYPE)
public class BuilderState {
    private int totalNum;
    private int waitingNum;

    public BuilderState(int totalNum, int waitingNum) {
        this.totalNum = totalNum;
        this.waitingNum = waitingNum;
    }

    public BuilderState() {
    }

    public int getTotalNum() {
        return totalNum;
    }

    public void setTotalNum(int totalNum) {
        this.totalNum = totalNum;
    }

    public int getWaitingNum() {
        return waitingNum;
    }

    public void setWaitingNum(int waitingNum) {
        this.waitingNum = waitingNum;
    }

    @Override
    public String toString() {
        return "QueueState{" +
               "totalNum=" + totalNum +
               ", waitingNum=" + waitingNum +
               '}';
    }
}
