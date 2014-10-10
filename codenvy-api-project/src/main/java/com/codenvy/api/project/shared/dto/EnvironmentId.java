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
import com.codenvy.dto.shared.DelegateRule;
import com.codenvy.dto.shared.DelegateTo;

import javax.annotation.Nonnull;
import java.util.Map;

/**
 * @author andrew00x
 */
@DTO
public interface EnvironmentId {
    enum Scope {
        system,
        project;

        // Use this enum to "implement" method in DTO.
        public static String toString(EnvironmentId environment) {
            String category = environment.getCategory();
            if (category == null) {
                category = "";
            }
            return environment.getScope() + ":/" + category + "/" + environment.getName();
        }
    }

    @DelegateTo(client = @DelegateRule(type = Scope.class, method = "toString"),
                server = @DelegateRule(type = Scope.class, method = "toString"))
    String getFqn();

    /** Gets scope of this runner environment. Scope helps identify how environment was delivered, e.g. "project", "system". */
    @Nonnull
    Scope getScope();

    /**
     * Sets scope of this runner environment.
     *
     * @see #getScope()
     */
    void setScope(@Nonnull Scope scope);

    EnvironmentId withScope(@Nonnull Scope scope);

    /**
     * Gets category of this runner environment. Category is represented by string that is separated with '/' character. Category helps
     * represent runner environments as hierarchically-organized system.
     */
    String getCategory();

    /**
     * Sets category of this runner environment.
     *
     * @see #getCategory()
     */
    void setCategory(String category);

    EnvironmentId withCategory(String category);

    /**
     * Gets name of this runner environment. Scope together with category and name gives fully-qualified name of runner environment. FQN of
     * runner environment has a following syntax: <i>&lt;scope&gt;:/&lt;category&gt;/&lt;name&gt;</i>.
     */
    @Nonnull
    String getName();

    /** Sets name of this runner environment. */
    void setName(@Nonnull String name);

    EnvironmentId withName(@Nonnull String name);
}
