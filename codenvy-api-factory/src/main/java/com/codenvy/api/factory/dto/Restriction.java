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
 * Security restriction for the factory.
 *
 * @author Sergii Kabashniuk
 */
@DTO
@Deprecated
public interface Restriction {

    /**
     * @return The time when the factory becomes valid (in milliseconds, from Unix epoch, no timezone)
     */
    @Deprecated
    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "validsince", trackedOnly = true)
    long getValidsince();

    void setValidsince(long validsince);

    Restriction withValidsince(long validsince);

    /**
     * @return The time when the factory becomes invalid (in milliseconds, from Unix epoch, no timezone)
     */
    @Deprecated
    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "validuntil", trackedOnly = true)
    long getValiduntil();

    void setValiduntil(long validuntil);

    Restriction withValiduntil(long validuntil);

    /**
     * @return referer dns queryParameterName
     */
    @Deprecated
    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "refererhostname", trackedOnly = true)
    String getRefererhostname();

    void setRefererhostname(String refererhostname);

    Restriction withRefererhostname(String refererhostname);

    /**
     * @return Indicates that factory is password protected. Set by server
     */
    @Deprecated
    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "restrictbypassword", trackedOnly = true, setByServer = true)
    boolean getRestrictbypassword();

    void setRestrictbypassword(boolean restrictbypassword);

    Restriction withRestrictbypassword(boolean restrictbypassword);

    /**
     * @return Password asked for factory activation. Not exposed in any case.
     */
    @Deprecated
    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "password", trackedOnly = true)
    String getPassword();

    void setPassword(String password);

    Restriction withPassword(String password);

    /**
     * @return It is a number that indicates the maximum number of sessions this factory is allowed to have.
     */
    @Deprecated
    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "maxsessioncount", trackedOnly = true)
    long getMaxsessioncount();

    void setMaxsessioncount(long maxsessioncount);

    Restriction withMaxsessioncount(long maxsessioncount);
}
