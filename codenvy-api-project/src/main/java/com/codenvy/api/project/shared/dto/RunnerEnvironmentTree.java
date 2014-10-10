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
import javax.annotation.Nullable;
import java.util.List;

/**
 * Represents RunnerEnvironment as tree that is based on {@link com.codenvy.api.project.shared.EnvironmentId#getCategory()}.
 *
 * @author andrew00x
 */
@DTO
public interface RunnerEnvironmentTree {
    /**
     * Gets runner environment on current tree level. If this method returns {@code null} that means there is no any runner environments on
     * current level. Always need check {@link #getChildren()} for child environments.
     * <pre>
     *     + Java
     *       |- Web
     *         |- Spring
     *           |- Tomcat7
     *           |- JBoss7
     *           |- Jetty9
     *         |- Struts
     *           |- ...
     *     + Python
     *       |- Web
     *         |- ...
     *     + Ruby
     *       |- Web
     *         |- Rails
     *           |- ...
     * </pre>
     * In example above there is no any environment on level Java, Java/Web.
     */
    @Nullable
    RunnerEnvironment getEnvironment();

    /**
     * Gets runner environment on current tree level.
     *
     * @see #getEnvironment()
     */
    void setEnvironment(RunnerEnvironment environment);

    RunnerEnvironmentTree withEnvironment(RunnerEnvironment environment);

    /** Gets category. Need this for display tree on client side if there is no runner environment on current level. */
    @Nonnull
    String getCategory();

    /**
     * Sets category.
     *
     * @see #getCategory()
     */
    void setCategory(@Nonnull String category);

    RunnerEnvironmentTree withCategory(@Nonnull String category);

    /**
     * Gets child environments. Empty list means that current tree level is last in hierarchy and method {@link #getEnvironment()} must not
     * return {@code null}.
     */
    @Nonnull
    List<RunnerEnvironmentTree> getChildren();

    /**
     * Sets child environments.
     *
     * @see #getChildren()
     */
    void setChildren(List<RunnerEnvironmentTree> children);

    RunnerEnvironmentTree withChildren(List<RunnerEnvironmentTree> children);
}
