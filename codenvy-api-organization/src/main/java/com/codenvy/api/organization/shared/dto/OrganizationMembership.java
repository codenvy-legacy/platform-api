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
package com.codenvy.api.organization.shared.dto;

import com.codenvy.dto.shared.DTO;

import java.util.List;

/**
 * Describes organization with roles. It should be used with
 * {@link com.codenvy.api.organization.server.OrganizationService#getMemberships(javax.ws.rs.core.SecurityContext)}
 *
 * @author Eugene Voevodin
 */
@DTO
public interface OrganizationMembership extends Organization {

    List<String> getRoles();

    void setRoles(List<String> roles);

    OrganizationMembership withRoles(List<String> roles);
}
