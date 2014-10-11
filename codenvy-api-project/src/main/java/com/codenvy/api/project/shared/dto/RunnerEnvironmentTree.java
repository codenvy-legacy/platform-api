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
 * Represents RunnerEnvironment as tree.
 *
 * @author andrew00x
 */
@DTO
public interface RunnerEnvironmentTree {
    /**
     * Gets runner environments on current tree level. If this method returns empty {@code List} that means there is no any runner
     * environments on current level. Always need check {@link #getChildren()} for child environments.
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
    List<RunnerEnvironment> getEnvironments();

    /**
     * Gets runner environments on current tree level.
     *
     * @see #getEnvironments()
     */
    void setEnvironments(List<RunnerEnvironment> environments);

    RunnerEnvironmentTree withEnvironments(List<RunnerEnvironment> environments);

    /** Gets node name. Need this for display tree on client side. */
    @Nonnull
    String getDisplayName();

    /**
     * Sets display name.
     *
     * @see #getDisplayName()
     */
    void setDisplayName(@Nonnull String name);

    RunnerEnvironmentTree withDisplayName(@Nonnull String name);

    /**
     * Gets child environments. Empty list means that current tree level is last in hierarchy.
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
