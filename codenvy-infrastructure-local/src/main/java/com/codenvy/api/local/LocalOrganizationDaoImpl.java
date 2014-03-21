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
package com.codenvy.api.local;

import com.codenvy.api.organization.server.dao.OrganizationDao;
import com.codenvy.api.organization.server.exception.OrganizationException;
import com.codenvy.api.organization.shared.dto.Organization;
import com.codenvy.api.organization.shared.dto.Attribute;
import com.codenvy.api.organization.shared.dto.Member;
import com.codenvy.api.organization.shared.dto.Subscription;
import com.codenvy.dto.server.DtoFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * @author Eugene Voevodin
 */
public class LocalOrganizationDaoImpl implements OrganizationDao {
    @Override
    public void create(Organization organization) throws OrganizationException {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public Organization getById(String id) throws OrganizationException {
        return DtoFactory.getInstance().createDto(Organization.class)
                         .withName("org_name")
                         .withId(id)
                         .withOwner("userId122332133123")
                         .withAttributes(Arrays.asList(
                                 DtoFactory.getInstance().createDto(Attribute.class).withName("attribute1").withValue("value")
                                           .withDescription("important attribute")));
    }

    @Override
    public Organization getByName(String name) throws OrganizationException {
        return DtoFactory.getInstance().createDto(Organization.class)
                         .withName(name)
                         .withId("org0xffaassdeereqWsss")
                         .withOwner("userId122332133123")
                         .withAttributes(Arrays.asList(
                                 DtoFactory.getInstance().createDto(Attribute.class).withName("attribute1").withValue("value")
                                           .withDescription("important attribute")));
    }

    @Override
    public Organization getByOwner(String owner) throws OrganizationException {
        return DtoFactory.getInstance().createDto(Organization.class)
                         .withName("org_name")
                         .withId("org0xffaassdeereqWsss")
                         .withOwner(owner)
                         .withAttributes(Arrays.asList(
                                 DtoFactory.getInstance().createDto(Attribute.class).withName("attribute1").withValue("value")
                                           .withDescription("important attribute")));
    }

    @Override
    public void update(Organization organization) throws OrganizationException {
        throw new RuntimeException("Not implemented");

    }

    @Override
    public void remove(String id) throws OrganizationException {
        throw new RuntimeException("Not implemented");

    }

    @Override
    public void addMember(Member member) throws OrganizationException {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void removeMember(String organizationId, String userId) throws OrganizationException {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void addSubscription(Subscription subscription, String organizationId) throws OrganizationException {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void removeSubscription(String organizationId, String serviceId) throws OrganizationException {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public List<Subscription> getSubscriptions(String organizationId) throws OrganizationException {
        return Arrays.asList(DtoFactory.getInstance().createDto(Subscription.class)
                                       .withStartDate("2013-12-01")
                                       .withEndDate("2013-12-01")
                                       .withServiceId("serviceId")
                                       .withProperties(new HashMap<String, String>())
                            );
    }

    @Override
    public List<Member> getMembers(String organizationId) throws OrganizationException {
        return Arrays.asList(DtoFactory.getInstance().createDto(Member.class).withOrganizationId(organizationId)
                                       .withUserId("userId122332133123").withRoles(Arrays.asList("organization/owner")),
                             DtoFactory.getInstance().createDto(Member.class).withOrganizationId(organizationId)
                                       .withUserId("userId112233322239").withRoles(Arrays.asList("organization/member")));
    }

    @Override
    public List<Organization> getByMember(String userId) throws OrganizationException {
        return Arrays.asList(DtoFactory.getInstance().createDto(Organization.class)
                                       .withName("org_name")
                                       .withId("org0xffaassdeereqWsss")
                                       .withOwner("owner")
                                       .withAttributes(Arrays.asList(
                                               DtoFactory.getInstance().createDto(Attribute.class).withName("attribute1").withValue("value")
                                                         .withDescription("important attribute"))));
    }
}
