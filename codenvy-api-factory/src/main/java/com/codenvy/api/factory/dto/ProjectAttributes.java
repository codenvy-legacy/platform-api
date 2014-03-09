/*
 * CODENVY CONFIDENTIAL
 * __________________
 * 
 *  [2012] - [2014] Codenvy, S.A. 
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
package com.codenvy.api.factory.dto;

import com.codenvy.api.factory.parameter.FactoryParameter;
import com.codenvy.dto.shared.DTO;

import static com.codenvy.api.factory.parameter.FactoryParameter.Obligation.OPTIONAL;

/** @author Sergii Kabashniuk */
@DTO
public interface ProjectAttributes {

    /**
     * @return Name of the project in temporary workspace after the exporting source code from vcsurl.
     * <p/>
     * Project queryParameterName should be in valid format,
     */
    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "pname")
    String getPname();

    public void setPname(String pname);

    /**
     * @return Project type.
     */
    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "ptype")
    public String getPtype();

    public void setPtype(String ptype);

}
