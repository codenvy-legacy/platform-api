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
package com.codenvy.api.organization.dao;

import com.codenvy.api.organization.exception.OrganizationServiceException;
import com.codenvy.api.organization.shared.dto.Member;

import java.util.List;

/**
 * DAO interface offers means to perform CRUD operations with {@link com.codenvy.api.organization.shared.dto.Member} data.
 * The implementation is not required
 * to be responsible for persistent layer data dto consistency. It simply transfers data from one layer to another,
 * so
 * if you're going to call any of implemented methods it is considered that all needed verifications are already done.
 * <p> <strong>Note:</strong> This particularly does not mean that method call will not make any inconsistency but this
 * mean that such kind of inconsistencies are expected by design and may be treated further. </p>
 */
public interface MemberDao {

    /**
     * Adds a new Member to persistent layer.
     * @param member
     * @throws OrganizationServiceException
     */

    void create (Member member) throws OrganizationServiceException;


    /**
     * Updates member in persistent layer.
     * @param member
     * @throws OrganizationServiceException
     */
    void update (Member member) throws OrganizationServiceException;

    /**
     * Gets a list of all members of the given workspace.
     *
     * @param wsId
     * @return
     * @throws OrganizationServiceException
     */

    List<Member> getWorkspaceMembers(String wsId) throws OrganizationServiceException;


    /**
     * Gets a list of all relationships of the given user and workspaces.
     *
     * @param userId
     * @return
     * @throws OrganizationServiceException
     */

    public List<Member> getUserRelationships(String userId) throws OrganizationServiceException;

    /**
     * Removes a given member from specified workspace.
     *
     * @param wsId
     * @param userId
     */

    void removeWorkspaceMember(String wsId, String userId);
}
