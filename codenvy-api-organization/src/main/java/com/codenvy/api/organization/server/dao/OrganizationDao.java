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
package com.codenvy.api.organization.server.dao;

import com.codenvy.api.organization.server.exception.OrganizationException;
import com.codenvy.api.organization.shared.dto.Organization;
import com.codenvy.api.organization.shared.dto.Member;
import com.codenvy.api.organization.shared.dto.Subscription;

import java.util.List;

/**
 * DAO interface offers means to perform CRUD operations with {@link com.codenvy.api.organization.shared.dto.Organization} data.
 * The implementation is not required
 * to be responsible for persistent layer data dto consistency. It simply transfers data from one layer to another,
 * so
 * if you're going to call any of implemented methods it is considered that all needed verifications are already done.
 * <p> <strong>Note:</strong> This particularly does not mean that method call will not make any inconsistency but this
 * mean that such kind of inconsistencies are expected by design and may be treated further. </p>
 *
 * @author Eugene Voevodin
 */
public interface OrganizationDao {

    /**
     * Adds new organization to persistent layer
     *
     * @param organization
     *         POJO representation of organization
     * @throws com.codenvy.api.organization.server.exception.OrganizationException
     */
    void create(Organization organization) throws OrganizationException;

    /**
     * Gets organization from persistent layer by it identifier
     *
     * @param id
     *         organization identifier
     * @return organization POJO, or <code>null</code> if nothing is found
     * @throws com.codenvy.api.organization.server.exception.OrganizationException
     */
    Organization getById(String id) throws OrganizationException;

    /**
     * Gets user from persistent layer it  name
     *
     * @param name
     *         organization name
     * @return organization POJO, or <code>null</code> if nothing is found
     * @throws com.codenvy.api.organization.server.exception.OrganizationException
     */
    Organization getByName(String name) throws OrganizationException;

    /**
     * Gets organization from persistent level by owner
     *
     * @param owner
     *         owner id
     * @return organization POJO, or empty list if nothing is found
     * @throws com.codenvy.api.organization.server.exception.OrganizationException
     */
    List<Organization> getByOwner(String owner) throws OrganizationException;

    /**
     * Updates already present in persistent level organization
     *
     * @param organization
     *         organization POJO to update
     * @throws com.codenvy.api.organization.server.exception.OrganizationException
     */
    void update(Organization organization) throws OrganizationException;

    /**
     * Removes organization from persistent layer
     *
     * @param id
     *         organization identifier
     * @throws com.codenvy.api.organization.server.exception.OrganizationException
     */
    void remove(String id) throws OrganizationException;

    /**
     * Adds new member to already present in persistent level organization
     *
     * @param member
     *         new member
     * @throws com.codenvy.api.organization.server.exception.OrganizationException
     */
    void addMember(Member member) throws OrganizationException;

    /**
     * Removes member from existing organization
     *
     * @param organizationId
     *         organization identifier
     * @param userId
     *         user identifier
     * @throws com.codenvy.api.organization.server.exception.OrganizationException
     */
    void removeMember(String organizationId, String userId) throws OrganizationException;

    /**
     * Adds new subscription to organization that already exists in persistent layer
     *
     * @param subscription
     *         subscription POJO
     */
    void addSubscription(Subscription subscription) throws OrganizationException;

    /**
     * Remove subscription related to existing organization
     *
     * @param subscriptionId
     *         subscription identifier for removal
     */
    void removeSubscription(String subscriptionId) throws OrganizationException;


    /**
     * Get subscription from persistent layer, if it doesn't exist {@code null} will be returned
     *
     * @param subscriptionId
     *         subscription identifier
     * @return Subscription POJO
     */
    Subscription getSubscriptionById(String subscriptionId) throws OrganizationException;

    /**
     * Gets list of existing in persistent layer subscriptions related to given organization
     *
     * @param organizationId
     *         organization id
     * @return list of subscriptions, or empty list if no subscriptions found
     */
    List<Subscription> getSubscriptions(String organizationId) throws OrganizationException;

    /**
     * Gets list of existing in persistent layer members related to given organization
     *
     * @param organizationId
     *         organization id
     * @return list of members, or empty list if no members found
     */
    List<Member> getMembers(String organizationId) throws OrganizationException;

    /**
     * Gets list of existing in persistent layer Organizations where given member is member
     *
     * @param userId
     *         user identifier to search
     * @return list of organizations, or empty list if no organizations found
     */
    public List<Organization> getByMember(String userId) throws OrganizationException;
}