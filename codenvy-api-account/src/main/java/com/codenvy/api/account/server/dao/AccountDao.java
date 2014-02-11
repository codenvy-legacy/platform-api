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
     * @throws AccountException
     */
    void create(Account account) throws AccountException;

    /**
     * Gets account from persistent layer by it identifier
     *
     * @param id
     *         account identifier
     * @return account POJO, or <code>null</code> if nothing is found
     * @throws AccountException
     */
    Account getById(String id) throws AccountException;

    /**
     * Gets user from persistent layer it  name
     *
     * @param name
     *         account name
     * @return account POJO, or <code>null</code> if nothing is found
     * @throws AccountException
     */
    Account getByName(String name) throws AccountException;

    /**
     * Gets account from persistent level by owner
     *
     * @param owner
     *         owner id
     * @return account POJO, or <code>null</code> if nothing is found
     * @throws AccountException
     */
    Account getByOwner(String owner) throws AccountException;

    /**
     * Updates already present in persistent level account
     *
     * @param account
     *         account POJO to update
     * @throws AccountException
     */
    void update(Account account) throws AccountException;

    /**
     * Removes account from persistent layer
     *
     * @param id
     *         account identifier
     * @throws AccountException
     */
    void remove(String id) throws AccountException;

    /**
     * Adds new member to already present in persistent level account
     *
     * @param accountId
     *         account identifier
     * @param userId
     *         user identifier
     * @throws AccountException
     */
    void addMember(String accountId, String userId) throws AccountException;

    /**
     * Removes member from existing account
     *
     * @param accountId
     *         account identifier
     * @param userId
     *         user identifier
     * @throws AccountException
     */
    void removeMember(String accountId, String userId) throws AccountException;

    /**
     * Adds new subscription to account that already exists in persistent layer
     *
     * @param subscription
     *         subscription POJO
     * @param accountId
     *         account identifier
     * @throws AccountException
     */
    void addSubscription(Subscription subscription, String accountId) throws AccountException;

    /**
     * Remove subscription related to existing account
     *
     * @param serviceId
     *         service identifier
     * @param accountId
     *         account identifier
     * @throws AccountException
     */
    void removeSubscription(String accountId, String serviceId) throws AccountException;

    /**
     * Gets list of existing in persistent layer subscriptions related to given account
     *
     * @param accountId
     *         account id
     * @return list of subscriptions
     */
    List<Subscription> getSubscriptions(String accountId) throws AccountException;

    /**
     * Gets list of existing in persistent layer members related to given account
     *
     * @param accountId
     *         account id
     * @return list of members
     */
    List<Member> getMembers(String accountId) throws AccountException;
}