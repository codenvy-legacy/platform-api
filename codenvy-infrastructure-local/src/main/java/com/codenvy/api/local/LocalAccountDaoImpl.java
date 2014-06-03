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
package com.codenvy.api.local;

import com.codenvy.api.account.server.dao.AccountDao;
import com.codenvy.api.account.shared.dto.Account;
import com.codenvy.api.account.shared.dto.AccountMembership;
import com.codenvy.api.account.shared.dto.Attribute;
import com.codenvy.api.account.shared.dto.Member;
import com.codenvy.api.account.shared.dto.Subscription;
import com.codenvy.dto.server.DtoFactory;

import javax.inject.Singleton;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * @author Eugene Voevodin
 */
@Singleton
public class LocalAccountDaoImpl implements AccountDao {
    @Override
    public void create(Account account) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public Account getById(String id) {
        return DtoFactory.getInstance().createDto(Account.class)
                         .withName("acc_name")
                         .withId(id)
                         .withAttributes(Arrays.asList(
                                 DtoFactory.getInstance().createDto(Attribute.class).withName("attribute1").withValue("value")
                                           .withDescription("important attribute")
                                                      ));
    }

    @Override
    public Account getByName(String name) {
        return DtoFactory.getInstance().createDto(Account.class)
                         .withName(name)
                         .withId("acc0xffaassdeereqWsss")
                         .withAttributes(Arrays.asList(
                                 DtoFactory.getInstance().createDto(Attribute.class).withName("attribute1").withValue("value")
                                           .withDescription("important attribute")
                                                      ));
    }

    @Override
    public List<Account> getByOwner(String owner) {
        return Arrays.asList(DtoFactory.getInstance().createDto(Account.class)
                                       .withName("acc_name")
                                       .withId("acc0xffaassdeereqWsss")
                                       .withAttributes(Arrays.asList(
                                               DtoFactory.getInstance().createDto(Attribute.class).withName("attribute1").withValue("value")
                                                         .withDescription("important attribute")
                                                                    )));
    }

    @Override
    public void update(Account account) {
        throw new RuntimeException("Not implemented");

    }

    @Override
    public void remove(String id) {
        throw new RuntimeException("Not implemented");

    }

    @Override
    public void addMember(Member member) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void removeMember(Member member) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void addSubscription(Subscription subscription) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void removeSubscription(String subscriptionId) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public Subscription getSubscriptionById(String subscriptionId) {
        return DtoFactory.getInstance().createDto(Subscription.class)
                         .withId("Subscription0xfffffffff")
                         .withStartDate(System.currentTimeMillis())
                         .withEndDate(System.currentTimeMillis())
                         .withServiceId("serviceId")
                         .withProperties(new HashMap<String, String>());
    }

    @Override
    public List<Subscription> getSubscriptions(String accountId) {
        return Arrays.asList(DtoFactory.getInstance().createDto(Subscription.class)
                                       .withId("Subscription0xfffffffff")
                                       .withStartDate(System.currentTimeMillis())
                                       .withEndDate(System.currentTimeMillis())
                                       .withServiceId("serviceId")
                                       .withProperties(new HashMap<String, String>())
                            );
    }

    @Override
    public List<Member> getMembers(String accountId) {
        return Arrays.asList(DtoFactory.getInstance().createDto(Member.class).withAccountId(accountId)
                                       .withUserId("userId122332133123").withRoles(Arrays.asList("account/owner")),
                             DtoFactory.getInstance().createDto(Member.class).withAccountId(accountId)
                                       .withUserId("userId112233322239").withRoles(Arrays.asList("account/member"))
                            );
    }

    @Override
    public List<AccountMembership> getByMember(String userId) {
        AccountMembership am = DtoFactory.getInstance().createDto(AccountMembership.class);
        am.setId("cc0xffaassdeereqWsss");
        am.setName("acc_name");
        am.setRoles(Arrays.asList("account/member"));
        am.setAttributes(Arrays.asList(
                DtoFactory.getInstance().createDto(Attribute.class).withName("attribute1").withValue("value")
                          .withDescription("important attribute")
                                      ));
        return Arrays.asList(am);
    }
}
