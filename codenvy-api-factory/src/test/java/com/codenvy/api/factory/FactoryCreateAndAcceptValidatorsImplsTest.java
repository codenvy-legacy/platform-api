/*******************************************************************************
 * Copyright (c) 2012-2015 Codenvy, S.A.
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

import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link FactoryAcceptValidatorImpl} and {@link FactoryCreateValidatorImpl}
 */
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

    private FactoryAcceptValidatorImpl acceptValidator;

    private FactoryCreateValidatorImpl createValidator;

    @BeforeMethod
    public void setUp() throws Exception {
        acceptValidator = new FactoryAcceptValidatorImpl(accountDao, userDao, profileDao, false);
        createValidator = new FactoryCreateValidatorImpl(accountDao, userDao, profileDao, false);
    }

    @Test
    public void testValidateOnCreate() throws ApiException {
        FactoryCreateValidatorImpl spy = spy(createValidator);
        doNothing().when(spy).validateSource(any(Factory.class));
        doNothing().when(spy).validateAccountId(any(Factory.class));
        doNothing().when(spy).validateTrackedFactoryAndParams(any(Factory.class));
        doNothing().when(spy).validateProjectName(any(Factory.class));
        doNothing().when(spy).validateCurrentTimeBeforeSinceUntil(any(Factory.class));
        doNothing().when(spy).validateProjectActions(any(Factory.class));

        //main invoke
        spy.validateOnCreate(factoryUrl);

        verify(spy).validateSource(any(Factory.class));
        verify(spy).validateAccountId(any(Factory.class));
        verify(spy).validateTrackedFactoryAndParams(any(Factory.class));
        verify(spy).validateProjectName(any(Factory.class));
        verify(spy).validateCurrentTimeBeforeSinceUntil(any(Factory.class));
        verify(spy).validateOnCreate(any(Factory.class));
        verify(spy).validateProjectActions(any(Factory.class));
        verifyNoMoreInteractions(spy);
    }

    @Test
    public void testOnAcceptNonEncoded() throws ApiException {
        FactoryAcceptValidatorImpl spy = spy(acceptValidator);
        doNothing().when(spy).validateSource(any(Factory.class));
        doNothing().when(spy).validateTrackedFactoryAndParams(any(Factory.class));
        doNothing().when(spy).validateProjectName(any(Factory.class));
        doNothing().when(spy).validateCurrentTimeBetweenSinceUntil(any(Factory.class));
        doNothing().when(spy).validateProjectActions(any(Factory.class));

        //main invoke
        spy.validateOnAccept(factoryUrl, false);

        verify(spy).validateSource(any(Factory.class));
        verify(spy).validateTrackedFactoryAndParams(any(Factory.class));
        verify(spy).validateProjectName(any(Factory.class));
        verify(spy).validateCurrentTimeBetweenSinceUntil(any(Factory.class));
        verify(spy).validateOnAccept(any(Factory.class), eq(false));
        verify(spy).validateProjectActions(any(Factory.class));
        verifyNoMoreInteractions(spy);
    }

    @Test
    public void testOnAcceptEncoded() throws ApiException {
        FactoryAcceptValidatorImpl spy = spy(acceptValidator);
        doNothing().when(spy).validateTrackedFactoryAndParams(any(Factory.class));
        doNothing().when(spy).validateCurrentTimeBetweenSinceUntil(any(Factory.class));
        doNothing().when(spy).validateProjectActions(any(Factory.class));

        //main invoke
        spy.validateOnAccept(factoryUrl, true);

        verify(spy).validateTrackedFactoryAndParams(any(Factory.class));
        verify(spy).validateCurrentTimeBetweenSinceUntil(any(Factory.class));
        verify(spy).validateOnAccept(any(Factory.class), eq(true));
        verify(spy).validateProjectActions(any(Factory.class));
        verifyNoMoreInteractions(spy);
    }



}
