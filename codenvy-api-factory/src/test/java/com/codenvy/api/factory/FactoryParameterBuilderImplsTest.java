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
import com.codenvy.api.core.ApiException;
import com.codenvy.api.factory.dto.Factory;
import com.codenvy.api.user.server.dao.UserDao;
import com.codenvy.api.user.server.dao.UserProfileDao;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import static org.mockito.Mockito.*;

@Listeners(value = {MockitoTestNGListener.class})
public class FactoryParameterBuilderImplsTest {

    @Mock
    private AccountDao accountDao;

    @Mock
    private UserDao userDao;

    @Mock
    UserProfileDao profileDao;

    @Mock
    Factory factoryUrl;

    @InjectMocks
    private FactoryUrlAcceptValidatorImpl acceptValidator;

    @InjectMocks
    private FactoryUrlCreateValidatorImpl createValidator;


    @Test
    public void shouldCallAllMethodsOnSave() throws ApiException {

        FactoryUrlCreateValidatorImpl spy = spy(createValidator);
        doNothing().when(spy).validateSource(any(Factory.class));
        doNothing().when(spy).validateOrgid(any(Factory.class));
        doNothing().when(spy).validateTrackedFactoryAndParams(any(Factory.class));
        doNothing().when(spy).validateProjectName(any(Factory.class));

        //main invoke
        spy.validateOnCreate(factoryUrl);

        verify(spy, atLeastOnce()).validateSource(any(Factory.class));
        verify(spy, atLeastOnce()).validateOrgid(any(Factory.class));
        verify(spy, atLeastOnce()).validateTrackedFactoryAndParams(any(Factory.class));
        verify(spy, atLeastOnce()).validateProjectName(any(Factory.class));
    }

    @Test
    public void shouldCallAllMethodsOnAcceptNonEncoded() throws ApiException {

        FactoryUrlAcceptValidatorImpl spy = spy(acceptValidator);
        doNothing().when(spy).validateSource(any(Factory.class));
        doNothing().when(spy).validateOrgid(any(Factory.class));
        doNothing().when(spy).validateTrackedFactoryAndParams(any(Factory.class));
        doNothing().when(spy).validateProjectName(any(Factory.class));

        //main invoke
        spy.validateOnAccept(factoryUrl, false);

        verify(spy, atLeastOnce()).validateSource(any(Factory.class));
        verify(spy, atLeastOnce()).validateTrackedFactoryAndParams(any(Factory.class));
        verify(spy, atLeastOnce()).validateProjectName(any(Factory.class));
    }

    @Test
    public void shouldCallGivenMethodOnAcceptEncoded() throws ApiException {

        FactoryUrlAcceptValidatorImpl spy = spy(acceptValidator);
        doThrow(RuntimeException.class).when(spy).validateSource(any(Factory.class));
        doThrow(RuntimeException.class).when(spy).validateOrgid(any(Factory.class));
        doThrow(RuntimeException.class).when(spy).validateProjectName(any(Factory.class));
        doNothing().when(spy).validateTrackedFactoryAndParams(any(Factory.class));

        //main invoke
        spy.validateOnAccept(factoryUrl, true);

        verify(spy, atLeastOnce()).validateTrackedFactoryAndParams(any(Factory.class));
    }



}
