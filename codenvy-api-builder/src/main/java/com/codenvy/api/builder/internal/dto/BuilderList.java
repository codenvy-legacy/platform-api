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

import java.util.ArrayList;
import java.util.List;

/** @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a> */
@DtoType(BuilderDtoTypes.BUILDER_LIST_TYPE)
public class BuilderList {
    private List<BuilderDescriptor> builders;

    public BuilderList(List<BuilderDescriptor> builders) {
        if (builders != null) {
            this.builders = new ArrayList<>(builders);
        }
    }

    public BuilderList() {
    }

    public List<BuilderDescriptor> getBuilders() {
        if (builders == null) {
            builders = new ArrayList<>();
        }
        return builders;
    }

    public void setBuilders(List<BuilderDescriptor> builders) {
        if (builders == null) {
            this.builders = null;
        } else {
            this.builders = new ArrayList<>(builders);
        }
    }

    @Override
    public String toString() {
        return "BuilderList{" +
               "builders=" + builders +
               '}';
    }
}
