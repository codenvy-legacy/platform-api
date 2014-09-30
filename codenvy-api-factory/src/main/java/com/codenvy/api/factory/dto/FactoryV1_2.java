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
import static com.codenvy.api.core.factory.FactoryParameter.Version.V2_0;

/**
 * Factory of version 1.2
 *
 * @author Sergii Kabashniuk
 */
@DTO
public interface FactoryV1_2 extends FactoryV1_1 {

    /**
     * @return additional git configuration
     */
    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "git", deprecatedSince = V2_0)
    @Deprecated
    Git getGit();

    @Deprecated
    void setGit(Git git);

    @Deprecated
    FactoryV1_2 withGit(Git git);

    /**
     * @return Factory acceptance restrictions
     */
    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "restriction", trackedOnly = true, deprecatedSince = V2_0)
    @Deprecated
    Restriction getRestriction();

    @Deprecated
    void setRestriction(Restriction restriction);

    @Deprecated
    FactoryV1_2 withRestriction(Restriction restriction);

    /**
     * @return warn on leave page
     */
    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "warnonclose", deprecatedSince = V2_0)
    @Deprecated
    boolean getWarnonclose();

    @Deprecated
    void setWarnonclose(boolean warnonclose);

    @Deprecated
    FactoryV1_2 withWarnonclose(boolean warnonclose);


    /**
     * @return hide copy to my workspace button
     */
    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "hidecopybutton", deprecatedSince = V2_0)
    @Deprecated
    boolean getHidecopybutton();

    @Deprecated
    void setHidecopybutton(boolean hidecopybutton);

    @Deprecated
    FactoryV1_2 withHidecopybutton(boolean hidecopybutton);



    /**
     * @return the optional directory to keep from the checkout
     */
    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "keepdirectory", deprecatedSince = V2_0)
    @Deprecated
    String getKeepdirectory();

    @Deprecated
    void setKeepdirectory(String keepDirectory);

    @Deprecated
    FactoryV1_2 withKeepdirectory(String keepDirectory);

}
