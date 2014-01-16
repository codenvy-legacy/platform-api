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
package com.codenvy.api.organization.model2;

import com.codenvy.api.core.rest.shared.dto.Link;
import com.codenvy.dto.shared.DTO;

import java.util.List;

/**
 * @author andrew00x
 */
@DTO
public interface User {
    String getId();

    void setId(String id);

    User withId(String id);

    List<String> getAliases();

    void setAliases(List<String> aliases);

    User withAliases(List<String> aliases);

    String getEmail();

    void setEmail(String email);

    User withEmail(String email);

    String getPassword();

    void setPassword(String password);

    User withPassword(String password);

    String getProfileId();

    void setProfileId(String profileId);

    User withProfileId(String profileId);

    List<Link> getLinks();

    void setLinks(List<Link> links);

    User withLinks(List<Link> links);
}
