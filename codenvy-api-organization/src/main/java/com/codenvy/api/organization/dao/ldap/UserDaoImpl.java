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
package com.codenvy.api.organization.dao.ldap;

import com.codenvy.api.organization.dao.UserDao;
import com.codenvy.api.organization.exception.OrganizationServiceException;
import com.codenvy.api.organization.shared.dto.User;

import javax.inject.Inject;

/**
 *
 */
public class UserDaoImpl extends BaseLdapDao implements UserDao{


    @Inject
    public UserDaoImpl(LdapService ldapService) {
        super(ldapService);
    }

    @Override
    public void create(User user) throws OrganizationServiceException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void update(User user) throws OrganizationServiceException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void removeById(String id) throws OrganizationServiceException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public User getByAlias(String alias) throws OrganizationServiceException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public User getById(String id) throws OrganizationServiceException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
