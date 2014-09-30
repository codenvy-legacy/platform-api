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
package com.codenvy.api.factory.dto;

import com.codenvy.api.core.factory.FactoryParameter;
import com.codenvy.dto.shared.DTO;

import static com.codenvy.api.core.factory.FactoryParameter.Obligation.OPTIONAL;

/**
 * Additional git parameters.
 *
 * @author Sergii Kabashniuk
 */
@DTO
@Deprecated
public interface Git {
    /**
     * @return Allows get ref-specs of the changes of remote repository
     */
    @Deprecated
    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "configremoteoriginfetch")
    String getConfigremoteoriginfetch();

    void setConfigremoteoriginfetch(String configremoteoriginfetch);

    Git withConfigremoteoriginfetch(String configremoteoriginfetch);

    /**
     * See https://www.kernel.org/pub/software/scm/git/docs/git-config.html push.default
     *
     * @return
     */
    @Deprecated
    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "configpushdefault")
    String getConfigpushdefault();

    void setConfigpushdefault(String configpushdefault);

    Git withConfigpushdefault(String configpushdefault);

    /**
     * @return Defines the upstream branch for vcsbranch.
     * <p/>
     * Git configuration branch.<queryParameterName>.merge makes sense only for
     * <p/>
     * vcsbranch because in this task we need push only in vcsbranch.
     * <p/>
     * In gerrit case it will be:"refs/for/" + queryParameterName branch for which this changes are proposed
     * <p/>
     * vcsbranch should be not null.
     */
    @Deprecated
    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "configbranchmerge")
    String getConfigbranchmerge();

    void setConfigbranchmerge(String configbranchmerge);

    Git withConfigbranchmerge(String configbranchmerge);
}
