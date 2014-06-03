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

import com.codenvy.api.factory.parameter.FactoryParameter;
import com.codenvy.dto.shared.DTO;

import java.util.List;

import static com.codenvy.api.factory.FactoryFormat.ENCODED;
import static com.codenvy.api.factory.parameter.FactoryParameter.Obligation.OPTIONAL;
import static com.codenvy.api.factory.parameter.FactoryParameter.Version.V1_1;
import static com.codenvy.api.factory.parameter.FactoryParameter.Version.V1_2;

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
    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "projectattributes")
    ProjectAttributes getProjectattributes();

    void setProjectattributes(ProjectAttributes projectattributes);

    FactoryV1_1 withProjectattributes(ProjectAttributes projectattributes);

    /**
     * @return Codenow  button style: vertical, horisontal, dark, wite
     */
    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "style", format = ENCODED)
    String getStyle();

    void setStyle(String style);

    FactoryV1_1 withStyle(String style);

    /**
     * @return Description of the factory.
     */
    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "description", format = ENCODED)
    String getDescription();

    void setDescription(String description);

    FactoryV1_1 withDescription(String description);

    /**
     * @return Author's email provided as meta information.
     */
    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "contactmail")
    String getContactmail();

    void setContactmail(String contactmail);

    FactoryV1_1 withContactmail(String contactmail);

    /**
     * @return Author's as meta information.
     */
    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "author")
    String getAuthor();

    void setAuthor(String author);

    FactoryV1_1 withAuthor(String author);

    /**
     * @return The orgid will be a field that we use to identify an account that created the Factory
     * <p/>
     * This will allow us to group together many factories across a single account.
     */
    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "orgid")
    String getOrgid();

    void setOrgid(String orgid);

    FactoryV1_1 withOrgid(String orgid);

    /**
     * @return The affiliateid will be an Affiliate Code that we issue to partners that will give them certain
     * <p/>
     * referral fees on any business we generate from affiliates.
     */
    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "affiliateid")
    String getAffiliateid();

    void setAffiliateid(String affiliateid);

    FactoryV1_1 withAffiliateid(String affiliateid);

    /**
     * @return Allow to checkout to the latest commit in given branch
     */
    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "vcsbranch")
    String getVcsbranch();

    void setVcsbranch(String vcsbranch);

    FactoryV1_1 withVcsbranch(String vcsbranch);

    /**
     * @return Id of user that create factory, set by the server
     */
    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "userid", setByServer = true, format = ENCODED)
    String getUserid();

    void setUserid(String userid);

    FactoryV1_1 withUserid(String userid);

    /**
     * @return Creation time of factory, set by the server (in milliseconds, from Unix epoch, no timezone)
     */
    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "created", setByServer = true, format = ENCODED)
    long getCreated();

    void setCreated(long created);

    FactoryV1_1 withCreated(long created);

    /**
     * @return Allow to use text replacement in project files after clone
     */
    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "variables")
    List<Variable> getVariables();

    void setVariables(List<Variable> variable);

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
    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "welcome", format = ENCODED, trackedOnly = true)
    WelcomePage getWelcome();

    void setWelcome(WelcomePage welcome);

    FactoryV1_1 withWelcome(WelcomePage welcome);

    /**
     * @return path to the image
     */
    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "image", ignoredSince = V1_1)
    @Deprecated
    String getImage();

    @Deprecated
    void setImage(String image);

    @Deprecated
    FactoryV1_1 withImage(String image);
}
