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
package com.codenvy.api.account.server.dao;

import com.codenvy.api.account.server.exception.AccountException;
import com.codenvy.api.account.shared.dto.Account;
import com.codenvy.api.account.shared.dto.Member;
import com.codenvy.api.account.shared.dto.Subscription;
import com.codenvy.api.core.ConflictException;
import com.codenvy.api.core.NotFoundException;
import com.codenvy.api.core.ServerException;

import java.util.List;

/**
 * DAO interface offers means to perform CRUD operations with {@link com.codenvy.api.account.shared.dto.Account} data.
 * The implementation is not required
 * to be responsible for persistent layer data dto consistency. It simply transfers data from one layer to another,
 * so
 * if you're going to call any of implemented methods it is considered that all needed verifications are already done.
 * <p> <strong>Note:</strong> This particularly does not mean that method call will not make any inconsistency but this
 * mean that such kind of inconsistencies are expected by design and may be treated further. </p>
 *
 * @author Eugene Voevodin
 */
public interface AccountDao {

    /**
     * Adds new account to persistent layer
     *
     * @param account
     *         POJO representation of account
     * @throws com.codenvy.api.account.server.exception.AccountException
     */
    void create(Account account) throws ConflictException, ServerException;

    /**
     * Gets account from persistent layer by it identifier
     *
     * @param id
     *         account identifier
     * @return account POJO, or <code>null</code> if nothing is found
     * @throws com.codenvy.api.account.server.exception.AccountException
     */
    Account getById(String id) throws NotFoundException, ServerException;

    /**
     * Gets user from persistent layer it  name
     *
     * @param name
     *         account name
     * @return account POJO, or <code>null</code> if nothing is found
     * @throws com.codenvy.api.account.server.exception.AccountException
     */
    Account getByName(String name) throws NotFoundException, ServerException;

    /**
     * Gets account from persistent level by owner
     *
     * @param owner
     *         owner id
     * @return account POJO, or empty list if nothing is found
     * @throws com.codenvy.api.account.server.exception.AccountException
     */
    List<Account> getByOwner(String owner) throws ServerException;

    /**
     * Updates already present in persistent level account
     *
     * @param account
     *         account POJO to update
     * @throws com.codenvy.api.account.server.exception.AccountException
     */
    void update(Account account) throws NotFoundException, ServerException;

    /**
     * Removes account from persistent layer
     *
     * @param id
     *         account identifier
     * @throws com.codenvy.api.account.server.exception.AccountException
     */
    void remove(String id) throws NotFoundException,ServerException, ConflictException;

    /**
     * Adds new member to already present in persistent level account
     *
     * @param member
     *         new member
     * @throws com.codenvy.api.account.server.exception.AccountException
     */
    void addMember(Member member) throws NotFoundException, ConflictException, ServerException;

    /**
     * Removes member from existing account
     *
     * @param accountId
     *         account identifier
     * @param userId
     *         user identifier
     * @throws com.codenvy.api.account.server.exception.AccountException
     */
    void removeMember(String accountId, String userId) throws NotFoundException, ServerException;

    /**
     * Adds new subscription to account that already exists in persistent layer
     *
     * @param subscription
     *         subscription POJO
     */
    void addSubscription(Subscription subscription) throws NotFoundException, ConflictException, ServerException;

    /**
     * Remove subscription related to existing account
     *
     * @param subscriptionId
     *         subscription identifier for removal
     */
    void removeSubscription(String subscriptionId) throws NotFoundException, ServerException;


    /**
     * Get subscription from persistent layer, if it doesn't exist {@code null} will be returned
     *
     * @param subscriptionId
     *         subscription identifier
     * @return Subscription POJO
     */
    Subscription getSubscriptionById(String subscriptionId) throws NotFoundException, ServerException;

    /**
     * Gets list of existing in persistent layer subscriptions related to given account
     *
     * @param accountId
     *         account id
     * @return list of subscriptions, or empty list if no subscriptions found
     */
    List<Subscription> getSubscriptions(String accountId) throws ServerException;

    /**
     * Gets list of existing in persistent layer members related to given account
     *
     * @param accountId
     *         account id
     * @return list of members, or empty list if no members found
     */
    List<Member> getMembers(String accountId) throws ServerException;

    /**
     * Gets list of existing in persistent layer Account where given member is member
     *
     * @param userId
     *         user identifier to search
     * @return list of accounts, or empty list if no accounts found
     */
    List<Account> getByMember(String userId) throws NotFoundException, ServerException;
}