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
package com.codenvy.api.factory;

import com.codenvy.api.account.server.dao.AccountDao;
import com.codenvy.api.account.server.dao.Member;
import com.codenvy.api.account.server.dao.Subscription;
import com.codenvy.api.core.ApiException;
import com.codenvy.api.core.ConflictException;
import com.codenvy.api.core.NotFoundException;
import com.codenvy.api.core.ServerException;
import com.codenvy.api.factory.dto.Factory;
import com.codenvy.api.factory.dto.Restriction;
import com.codenvy.api.user.server.dao.Profile;
import com.codenvy.api.user.server.dao.UserDao;
import com.codenvy.api.user.server.dao.UserProfileDao;
import com.codenvy.api.user.server.dao.User;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Validates values of factory parameters.
 *
 * @author Alexander Garagatyi
 */
public abstract class FactoryUrlBaseValidator {
    private static final Pattern PROJECT_NAME_VALIDATOR = Pattern.compile("^[\\\\\\w\\\\\\d]+[\\\\\\w\\\\\\d_.-]*$");

    private AccountDao accountDao;

    private UserDao userDao;

    private UserProfileDao profileDao;

    public FactoryUrlBaseValidator(AccountDao accountDao, UserDao userDao, UserProfileDao profileDao) {
        this.accountDao = accountDao;
        this.userDao = userDao;
        this.profileDao = profileDao;
    }

    protected void validateVcs(Factory factory) throws ApiException {
        // check that vcs value is correct (only git is supported for now)
        if (!"git".equals(factory.getVcs())) {
            throw new ConflictException("Parameter 'vcs' has illegal value. Only 'git' is supported for now.");
        }
        if (factory.getVcsurl() == null || factory.getVcsurl().isEmpty()) {
            throw new ConflictException(
                    String.format(FactoryConstants.PARAMETRIZED_ILLEGAL_PARAMETER_VALUE_MESSAGE, "vcsurl", factory.getVcsurl()));
        } else {
            try {
                URLDecoder.decode(factory.getVcsurl(), "UTF-8");
            } catch (IllegalArgumentException | UnsupportedEncodingException e) {
                throw new ConflictException(
                        String.format(FactoryConstants.PARAMETRIZED_ILLEGAL_PARAMETER_VALUE_MESSAGE, "vcsurl", factory.getVcsurl()));
            }
        }
    }

    protected void validateProjectName(Factory factory) throws ApiException {
        // validate project name
        String pname = null;
        if ("1.0".equals(factory.getV())) {
            pname = factory.getPname();
        } else if (factory.getProjectattributes() != null) {
            pname = factory.getProjectattributes().getPname();
        }
        if (null != pname && !PROJECT_NAME_VALIDATOR.matcher(pname).matches()) {
            throw new ConflictException(
                    "Project name must contain only Latin letters, digits or these following special characters -._.");
        }
    }


    protected void validateOrgid(Factory factory) throws ApiException {
        // validate orgid
        String orgid = "".equals(factory.getOrgid()) ? null : factory.getOrgid();
        if (null != orgid) {
            if (factory.getUserid() != null) {
                try {
                    User user = userDao.getById(factory.getUserid());
                    Profile profile = profileDao.getById(factory.getUserid());
                    if (profile.getAttributes() != null && "true".equals(profile.getAttributes().get("temporary")))
                        throw new ConflictException("Current user is not allowed for using this method.");
                    boolean isOwner = false;
                    List<Member> members = accountDao.getMembers(orgid);
                    if (members.isEmpty()) {
                        throw new ConflictException(
                                String.format(FactoryConstants.PARAMETRIZED_ILLEGAL_ORGID_PARAMETER_MESSAGE, factory.getOrgid()));
                    }
                    for (Member accountMember : members) {
                        if (accountMember.getUserId().equals(user.getId()) && accountMember.getRoles().contains(
                                "account/owner")) {
                            isOwner = true;
                            break;
                        }
                    }
                    if (!isOwner) {
                        throw new ConflictException("You are not authorized to use this orgid.");
                    }
                } catch (NotFoundException | ServerException e) {
                    throw new ConflictException("You are not authorized to use this orgid.");
                }
            }
        }

    }


    protected void validateTrackedFactoryAndParams(Factory factory) throws ApiException {
        // validate tracked parameters
        Restriction restriction = factory.getRestriction();
        String orgid = "".equals(factory.getOrgid()) ? null : factory.getOrgid();
        if (orgid != null) {
            try {
                List<Subscription> subscriptions = accountDao.getSubscriptions(orgid, "Factory");
                boolean isTracked = false;
                for (Subscription one : subscriptions) {
                    if ("Tracked".equals(one.getProperties().get("Package"))) {
                        isTracked = true;
                        break;
                    }
                }
                if (!isTracked) {
                    throw new ConflictException(
                            String.format(FactoryConstants.PARAMETRIZED_ILLEGAL_ORGID_PARAMETER_MESSAGE, orgid));
                }
            } catch (NotFoundException | ServerException | NumberFormatException e) {
                throw new ConflictException(
                        String.format(FactoryConstants.PARAMETRIZED_ILLEGAL_ORGID_PARAMETER_MESSAGE, orgid));
            }
        }


        if (restriction != null) {
            if (0 != restriction.getValidsince()) {
                if (null == orgid) {
                    throw new ConflictException(
                            String.format(FactoryConstants.PARAMETRIZED_ILLEGAL_TRACKED_PARAMETER_MESSAGE, null, "validsince"));
                }

                if (new Date().before(new Date(restriction.getValidsince()))) {
                    throw new ConflictException(FactoryConstants.ILLEGAL_VALIDSINCE_MESSAGE);
                }
            }

            if (0 != restriction.getValiduntil()) {
                if (null == orgid) {
                    throw new ConflictException(
                            String.format(FactoryConstants.PARAMETRIZED_ILLEGAL_TRACKED_PARAMETER_MESSAGE, null, "validuntil"));
                }

                if (new Date().after(new Date(restriction.getValiduntil()))) {
                    throw new ConflictException(FactoryConstants.ILLEGAL_VALIDUNTIL_MESSAGE);
                }
            }


            if (restriction.getRestrictbypassword()) {
                if (null == orgid) {
                    throw new ConflictException(
                            String.format(FactoryConstants.PARAMETRIZED_ILLEGAL_TRACKED_PARAMETER_MESSAGE, null, "restrictbypassword"));
                }

                // TODO implement
            }

            if (null != restriction.getPassword()) {
                if (null == orgid) {
                    throw new ConflictException(
                            String.format(FactoryConstants.PARAMETRIZED_ILLEGAL_TRACKED_PARAMETER_MESSAGE, null, "password"));
                }

                // TODO implement
            }

            if (0 != restriction.getMaxsessioncount()) {
                if (null == orgid) {
                    throw new ConflictException(
                            String.format(FactoryConstants.PARAMETRIZED_ILLEGAL_TRACKED_PARAMETER_MESSAGE, null, "maxsessioncount"));
                }

                // TODO implement
            }
        }

        if (null != factory.getWelcome()) {
            if (null == orgid) {
                throw new ConflictException(
                        String.format(FactoryConstants.PARAMETRIZED_ILLEGAL_TRACKED_PARAMETER_MESSAGE, null, "welcome"));
            }
        }
    }

}
