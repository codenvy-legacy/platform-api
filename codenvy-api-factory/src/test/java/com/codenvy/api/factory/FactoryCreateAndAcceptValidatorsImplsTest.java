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
public class FactoryCreateAndAcceptValidatorsImplsTest {

    @Mock
    private AccountDao accountDao;

    @Mock
    private UserDao userDao;

    @Mock
    private UserProfileDao profileDao;

    @Mock
    private Factory factoryUrl;

    @InjectMocks
    private FactoryUrlAcceptValidatorImpl acceptValidator;

    @InjectMocks
    private FactoryUrlCreateValidatorImpl createValidator;


    @Test
    public void testValidateOnCreate() throws ApiException {
        FactoryUrlCreateValidatorImpl spy = spy(createValidator);
        doNothing().when(spy).validateSource(any(Factory.class));
        doNothing().when(spy).validateOrgid(any(Factory.class));
        doNothing().when(spy).validateTrackedFactoryAndParams(any(Factory.class));
        doNothing().when(spy).validateProjectName(any(Factory.class));
        doNothing().when(spy).validateCurrentTimeBeforeSinceUntil(any(Factory.class));

        //main invoke
        spy.validateOnCreate(factoryUrl);

        verify(spy).validateSource(any(Factory.class));
        verify(spy).validateOrgid(any(Factory.class));
        verify(spy).validateTrackedFactoryAndParams(any(Factory.class));
        verify(spy).validateProjectName(any(Factory.class));
        verify(spy).validateCurrentTimeBeforeSinceUntil(any(Factory.class));
        verify(spy).validateOnCreate(any(Factory.class));
        verifyNoMoreInteractions(spy);
    }

    @Test
    public void testOnAcceptNonEncoded() throws ApiException {
        FactoryUrlAcceptValidatorImpl spy = spy(acceptValidator);
        doNothing().when(spy).validateSource(any(Factory.class));
        doNothing().when(spy).validateTrackedFactoryAndParams(any(Factory.class));
        doNothing().when(spy).validateProjectName(any(Factory.class));
        doNothing().when(spy).validateCurrentTimeBetweenSinceUntil(any(Factory.class));

        //main invoke
        spy.validateOnAccept(factoryUrl, false);

        verify(spy).validateSource(any(Factory.class));
        verify(spy).validateTrackedFactoryAndParams(any(Factory.class));
        verify(spy).validateProjectName(any(Factory.class));
        verify(spy).validateCurrentTimeBetweenSinceUntil(any(Factory.class));
        verify(spy).validateOnAccept(any(Factory.class), eq(false));
        verifyNoMoreInteractions(spy);
    }

    @Test
    public void testOnAcceptEncoded() throws ApiException {
        FactoryUrlAcceptValidatorImpl spy = spy(acceptValidator);
        doNothing().when(spy).validateTrackedFactoryAndParams(any(Factory.class));
        doNothing().when(spy).validateCurrentTimeBetweenSinceUntil(any(Factory.class));

        //main invoke
        spy.validateOnAccept(factoryUrl, true);

        verify(spy).validateTrackedFactoryAndParams(any(Factory.class));
        verify(spy).validateCurrentTimeBetweenSinceUntil(any(Factory.class));
        verify(spy).validateOnAccept(any(Factory.class), eq(true));
        verifyNoMoreInteractions(spy);
    }



}
