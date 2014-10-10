/*******************************************************************************
 * Copyright (c) 2012-2014 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package com.codenvy.api.project.shared.dto;

import com.codenvy.dto.shared.DTO;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Represents RunnerEnvironment as tree that is based on {@link EnvironmentId#getCategory()}.
 *
 * @author andrew00x
 */
@DTO
public interface RunnerEnvironmentTree {
    @Nonnull
    String getSubCategory();

    void setSubCategory(@Nonnull String subCategory);

    RunnerEnvironmentTree withSubCategory(@Nonnull String subCategory);

    @Nonnull
    List<RunnerEnvironmentTree> getChildren();

    void setChildren(List<RunnerEnvironmentTree> children);

    RunnerEnvironmentTree withChildren(List<RunnerEnvironmentTree> children);

    /** Gets RunnerEnvironments of current tree level. */
    @Nonnull
    List<RunnerEnvironment> getEnvironments();

    void setEnvironments(List<RunnerEnvironment> environments);

    RunnerEnvironmentTree withEnvironments(List<RunnerEnvironment> environments);
}
