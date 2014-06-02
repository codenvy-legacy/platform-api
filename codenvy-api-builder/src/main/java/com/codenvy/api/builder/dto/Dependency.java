/*
 * CODENVY CONFIDENTIAL
 * __________________
 * 
 *  [2012] - [2013] Codenvy, S.A. 
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
package com.codenvy.api.builder.dto;

import com.codenvy.dto.shared.DTO;

/**
 * Describes project dependency.
 *
 * @author <a href="mailto:aparfonov@codenvy.com">Andrey Parfonov</a>
 */
@DTO
public interface Dependency {
    /**
     * Full name of project dependency. Typically name should provide information about name of library including a version number.
     * Different build system may sub-classes of this class to provide more details about dependency.
     *
     * @return name of project dependency
     */
    String getFullName();

    Dependency withFullName(String name);

    /**
     * Set name of project dependency.
     *
     * @param name
     *         name of project dependency
     * @see #getFullName()
     */
    void setFullName(String name);
}
