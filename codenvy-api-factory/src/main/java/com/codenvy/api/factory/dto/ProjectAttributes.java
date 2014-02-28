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

import com.codenvy.api.factory.Compatibility;
import com.codenvy.dto.shared.DTO;

import static com.codenvy.api.factory.Compatibility.Optionality.OPTIONAL;

/** @author Sergii Kabashniuk */
@DTO
public interface ProjectAttributes {

    /**
     * @return Name of the project in temporary workspace after the exporting source code from vcsurl.
     * <p/>
     * Project name should be in valid format,
     */
    @Compatibility(optionality = OPTIONAL)
    String getPname();

    public void setPname(String pname);

    /**
     * @return Project type.
     */
    @Compatibility(optionality = OPTIONAL)
    public String getPtype();

    public void setPtype(String ptype);

}
