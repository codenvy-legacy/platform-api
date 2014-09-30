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

import java.util.List;

import static com.codenvy.api.core.factory.FactoryParameter.FactoryFormat.ENCODED;
import static com.codenvy.api.core.factory.FactoryParameter.Obligation.OPTIONAL;
import static com.codenvy.api.core.factory.FactoryParameter.Version.V1_1;
import static com.codenvy.api.core.factory.FactoryParameter.Version.V1_2;
import static com.codenvy.api.core.factory.FactoryParameter.Version.V2_0;

/**
 * Factory of version 1.1
 *
 * @author Sergii Kabashniuk
 */
@DTO
public interface FactoryV1_1 extends FactoryV1_0 {
    /**
     * @return - id of stored factory object
     */
    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "id", format = ENCODED, setByServer = true)
    String getId();

    void setId(String id);

    FactoryV1_1 withId(String id);

    /**
     * @return - project attributes, such 'ptype', 'pname'
     */
    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "projectattributes", deprecatedSince = V2_0)
    @Deprecated
    ProjectAttributes getProjectattributes();

    @Deprecated
    void setProjectattributes(ProjectAttributes projectattributes);

    @Deprecated
    FactoryV1_1 withProjectattributes(ProjectAttributes projectattributes);

    /**
     * @return Codenow  button style: vertical, horisontal, dark, wite
     */
    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "style", format = ENCODED, deprecatedSince = V2_0)
    @Deprecated
    String getStyle();

    @Deprecated
    void setStyle(String style);

    @Deprecated
    FactoryV1_1 withStyle(String style);

    /**
     * @return Description of the factory.
     */
    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "description", format = ENCODED, deprecatedSince = V2_0)
    @Deprecated
    String getDescription();

    @Deprecated
    void setDescription(String description);

    @Deprecated
    FactoryV1_1 withDescription(String description);

    /**
     * @return Author's email provided as meta information.
     */
    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "contactmail", deprecatedSince = V2_0)
    @Deprecated
    String getContactmail();

    @Deprecated
    void setContactmail(String contactmail);

    @Deprecated
    FactoryV1_1 withContactmail(String contactmail);

    /**
     * @return Author's as meta information.
     */
    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "author", deprecatedSince = V2_0)
    @Deprecated
    String getAuthor();

    @Deprecated
    void setAuthor(String author);

    @Deprecated
    FactoryV1_1 withAuthor(String author);

    /**
     * @return The orgid will be a field that we use to identify an account that created the Factory
     * <p/>
     * This will allow us to group together many factories across a single account.
     */
    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "orgid", deprecatedSince = V2_0)
    @Deprecated
    String getOrgid();

    @Deprecated
    void setOrgid(String orgid);

    @Deprecated
    FactoryV1_1 withOrgid(String orgid);

    /**
     * @return The affiliateid will be an Affiliate Code that we issue to partners that will give them certain
     * <p/>
     * referral fees on any business we generate from affiliates.
     */
    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "affiliateid", deprecatedSince = V2_0)
    @Deprecated
    String getAffiliateid();

    @Deprecated
    void setAffiliateid(String affiliateid);

    @Deprecated
    FactoryV1_1 withAffiliateid(String affiliateid);

    /**
     * @return Allow to checkout to the latest commit in given branch
     */
    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "vcsbranch", deprecatedSince = V2_0)
    @Deprecated
    String getVcsbranch();

    @Deprecated
    void setVcsbranch(String vcsbranch);

    @Deprecated
    FactoryV1_1 withVcsbranch(String vcsbranch);

    /**
     * @return Id of user that create factory, set by the server
     */
    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "userid", setByServer = true, format = ENCODED, deprecatedSince = V2_0)
    @Deprecated
    String getUserid();

    @Deprecated
    void setUserid(String userid);

    @Deprecated
    FactoryV1_1 withUserid(String userid);

    /**
     * @return Creation time of factory, set by the server (in milliseconds, from Unix epoch, no timezone)
     */
    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "created", setByServer = true, format = ENCODED, deprecatedSince = V2_0)
    @Deprecated
    long getCreated();

    @Deprecated
    void setCreated(long created);

    @Deprecated
    FactoryV1_1 withCreated(long created);

    /**
     * @return Allow to use text replacement in project files after clone
     */
    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "variables", deprecatedSince = V2_0)
    @Deprecated
    List<Variable> getVariables();

    @Deprecated
    void setVariables(List<Variable> variable);

    @Deprecated
    FactoryV1_1 withVariables(List<Variable> variable);

    /**
     * @return The time when the factory becomes valid (in milliseconds, from Unix epoch, no timezone)
     */
    @Deprecated
    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "validsince", ignoredSince = V1_1, deprecatedSince = V1_2
    )
    long getValidsince();

    @Deprecated
    void setValidsince(long validsince);

    @Deprecated
    FactoryV1_1 withValidsince(long validsince);

    /**
     * @return The time when the factory becomes invalid (in milliseconds, from Unix epoch, no timezone)
     */
    @Deprecated
    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "validuntil", ignoredSince = V1_1, deprecatedSince = V1_2
    )
    long getValiduntil();

    @Deprecated
    void setValiduntil(long validuntil);

    @Deprecated
    FactoryV1_1 withValiduntil(long validuntil);

    /**
     * @return welcome page configuration.
     */
    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "welcome", format = ENCODED, trackedOnly = true, deprecatedSince = V2_0)
    @Deprecated
    WelcomePage getWelcome();

    @Deprecated
    void setWelcome(WelcomePage welcome);

    @Deprecated
    FactoryV1_1 withWelcome(WelcomePage welcome);

    /**
     * @return path to the image
     */
    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "image", ignoredSince = V1_1, deprecatedSince = V2_0)
    @Deprecated
    String getImage();

    @Deprecated
    void setImage(String image);

    @Deprecated
    FactoryV1_1 withImage(String image);
}
