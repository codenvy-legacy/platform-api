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
import com.codenvy.api.factory.dto.Actions;
import com.codenvy.api.factory.dto.Factory;
import com.codenvy.api.factory.dto.Policies;
import com.codenvy.api.factory.dto.ProjectAttributes;
import com.codenvy.api.factory.dto.Restriction;
import com.codenvy.api.factory.dto.WelcomePage;
import com.codenvy.api.user.server.dao.Profile;
import com.codenvy.api.user.server.dao.UserDao;
import com.codenvy.api.user.server.dao.UserProfileDao;
import com.codenvy.api.user.server.dao.User;
import com.codenvy.dto.server.DtoFactory;

import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

@Listeners(value = {MockitoTestNGListener.class})
public class FactoryUrlBaseValidatorTest {

    private static String VALID_REPOSITORY_URL = "http://github.com/codenvy/cloudide";

    private static final String ID = "id";

    @Mock
    private AccountDao accountDao;

    @Mock
    private UserDao userDao;

    @Mock
    private UserProfileDao profileDao;

    @Mock
    private FactoryBuilder builder;

    @Mock
    private HttpServletRequest request;

    private TestFactoryUrlBaseValidator validator;

    private Member member;

    private Factory factory;


    @BeforeMethod
    public void setUp() throws ParseException, NotFoundException, ServerException {
        Factory nonencoded = DtoFactory.getInstance().createDto(Factory.class);
        nonencoded.setV("1.2");
        nonencoded.setVcs("git");
        nonencoded.setVcsurl(VALID_REPOSITORY_URL);
        factory = nonencoded;

        User user = new User().withId("userid");

        Subscription subscription = new Subscription()
                .withServiceId("Factory")
                .withProperties(Collections.singletonMap("Package", "Tracked"));
        member = new Member().withUserId("userid").withRoles(Arrays.asList("account/owner"));
        when(accountDao.getSubscriptions(ID, "Factory")).thenReturn(Arrays.asList(subscription));
        when(accountDao.getMembers(anyString())).thenReturn(Arrays.asList(member));
        when(userDao.getById("userid")).thenReturn(user);
        when(profileDao.getById(anyString())).thenReturn(new Profile());
        factory.setOrgid(ID);
        factory.setUserid("userid");

        validator = new TestFactoryUrlBaseValidator(accountDao, userDao, profileDao, false);
    }

    @Test
    public void shouldBeAbleToValidateFactoryUrlObject() throws ApiException {
        validator.validateSource(factory);
        validator.validateProjectName(factory);
        validator.validateOrgid(factory);
        validator.validateTrackedFactoryAndParams(factory);
    }

    @Test
    public void shouldBeAbleToValidateFactoryUrlObjectIfVcsIsESBWSO2() throws ApiException {
        factory.setVcs("esbwso2");

        validator.validateSource(factory);
        validator.validateProjectName(factory);
        validator.validateOrgid(factory);
        validator.validateTrackedFactoryAndParams(factory);
    }

    @Test(expectedExceptions = ApiException.class,
          expectedExceptionsMessageRegExp =
                  "The parameter vcsurl has a value submitted http://codenvy.com/git/04%2 with a value that is unexpected. " +
                  "For more information, please visit: http://docs.codenvy.com/user/creating-factories/factory-parameter-reference/.")
    public void shouldNotValidateIfVcsurlContainIncorrectEncodedSymbol() throws ApiException {
        // given
        factory.setVcsurl("http://codenvy.com/git/04%2");

        // when, then
        validator.validateSource(factory);
    }

    @Test
    public void shouldValidateIfVcsurlIsCorrectSsh() throws ApiException {
        // given
        factory.setVcsurl("ssh://codenvy@review.gerrithub.io:29418/codenvy/exampleProject");

        // when, then
        validator.validateSource(factory);
    }

    @Test
    public void shouldValidateIfVcsurlIsCorrectHttps() throws ApiException {
        // given
        factory.setVcsurl("https://github.com/codenvy/example.git");

        // when, then
        validator.validateSource(factory);
    }

    @Test(dataProvider = "badAdvancedFactoryUrlProvider", expectedExceptions = ApiException.class)
    public void shouldNotValidateIfVcsOrVcsUrlIsInvalid(Factory factoryUrl) throws ApiException {
        validator.validateSource(factoryUrl);
    }

    @DataProvider(name = "badAdvancedFactoryUrlProvider")
    public Object[][] invalidParametersFactoryUrlProvider() throws UnsupportedEncodingException {
        Factory adv1 = DtoFactory.getInstance().createDto(Factory.class);
        adv1.setV("1.1");
        adv1.setVcs("notagit");
        adv1.setVcsurl(VALID_REPOSITORY_URL);

        Factory adv2 = DtoFactory.getInstance().createDto(Factory.class);
        adv2.setV("1.1");
        adv2.setVcs("git");
        adv2.setVcsurl(null);

        Factory adv3 = DtoFactory.getInstance().createDto(Factory.class);
        adv3.setV("1.1");
        adv3.setVcs("git");
        adv3.setVcsurl("");

        return new Object[][]{
                {adv1},// invalid vcs
                {adv2},// invalid vcsurl
                {adv3}// invalid vcsurl
        };
    }

    @Test(dataProvider = "invalidProjectNamesProvider", expectedExceptions = ApiException.class,
          expectedExceptionsMessageRegExp = "Project name must contain only Latin letters, digits or these following special characters -._.")
    public void shouldThrowFactoryUrlExceptionIfProjectNameInvalid(String projectName) throws Exception {
        // given
        factory.setProjectattributes(DtoFactory.getInstance().createDto(ProjectAttributes.class).withPname(projectName));

        // when, then
        validator.validateProjectName(factory);
    }

    @Test(dataProvider = "validProjectNamesProvider")
    public void shouldBeAbleToValidateValidProjectName(String projectName) throws Exception {
        // given
        factory.setProjectattributes(DtoFactory.getInstance().createDto(ProjectAttributes.class).withPname(projectName));

        // when, then
        validator.validateProjectName(factory);
    }

    @DataProvider(name = "validProjectNamesProvider")
    public Object[][] validProjectNames() {
        return new Object[][]{
                {"untitled"},
                {"Untitled"},
                {"untitled.project"},
                {"untitled-project"},
                {"untitled_project"},
                {"untitled01"},
                {"000011111"},
                {"0untitled"},
                {"UU"},
                {"untitled-proj12"},
                {"untitled.pro....111"},
                {"SampleStruts"}
        };
    }

    @DataProvider(name = "invalidProjectNamesProvider")
    public Object[][] invalidProjectNames() {
        return new Object[][]{
                {"-untitled"},
                {"untitled->3"},
                {"untitled__2%"},
                {"untitled_!@#$%^&*()_+?><"}
        };
    }

    @Test
    public void shouldBeAbleToValidateIfOrgIdIsValid() throws ApiException, ParseException {
        validator.validateOrgid(factory);
    }

    @Test
    public void shouldBeAbleToValidateIfOrgIdAndOwnerAreValid()
            throws ApiException, ParseException {
        // when, then
        validator.validateOrgid(factory);
    }

    @Test(expectedExceptions = ApiException.class)
    public void shouldNotValidateIfAccountDoesNotExist() throws ApiException {
        when(accountDao.getMembers(anyString())).thenReturn(Collections.<Member>emptyList());

        validator.validateOrgid(factory);
    }

    @Test(expectedExceptions = ApiException.class, expectedExceptionsMessageRegExp = "You are not authorized to use this orgid.")
    public void shouldNotValidateIfFactoryOwnerIsNotOrgidOwner()
            throws ApiException, ParseException {
        Member wronMember = member;
        wronMember.setUserId("anotheruserid");
        when(accountDao.getMembers(anyString())).thenReturn(Arrays.asList(wronMember));

        // when, then
        validator.validateOrgid(factory);
    }

    @Test(expectedExceptions = ApiException.class)
    public void shouldNotValidateIfAccountDoesntHaveFactorySubscriptions()
            throws ApiException, ParseException {
        // given
        when(accountDao.getSubscriptions(ID, "Factory")).thenReturn(Collections.<Subscription>emptyList());
        // when, then
        validator.validateTrackedFactoryAndParams(factory);
    }

    @Test(expectedExceptions = ApiException.class)
    public void shouldNotValidateIfPackageIsNotTracked()
            throws ApiException, ParseException {
        // given
        Subscription subscription = new Subscription()
                .withServiceId("Factory")
                .withProperties(Collections.singletonMap("Package", "Another"));
        when(accountDao.getSubscriptions(ID, "Factory")).thenReturn(Arrays.asList(subscription));
        // when, then
        validator.validateTrackedFactoryAndParams(factory);
    }

    @Test
    public void shouldValidateIfHostNameIsLegal() throws ApiException, ParseException {
        // given
        factory.setRestriction(DtoFactory.getInstance().createDto(Restriction.class).withRefererhostname("notcodenvy.com"));

        when(request.getHeader("Referer")).thenReturn("http://notcodenvy.com/factories-examples");

        // when, then
        validator.validateTrackedFactoryAndParams(factory);
    }

    @Test
    public void shouldValidateIfRefererIsRelativeAndCurrentHostnameIsEqualToRequiredHostName()
            throws ApiException, ParseException {
        // given
        factory.setRestriction(DtoFactory.getInstance().createDto(Restriction.class).withRefererhostname("next.codenvy.com"));

        when(request.getHeader("Referer")).thenReturn("/factories-examples");
        when(request.getServerName()).thenReturn("next.codenvy.com");

        // when, then
        validator.validateTrackedFactoryAndParams(factory);
    }

    @Test(expectedExceptions = ApiException.class)
    public void shouldNotValidateEncodedFactoryWithWelcomePageIfOrgIdIsEmpty() throws ApiException {
        // given
        WelcomePage welcome = DtoFactory.getInstance().createDto(WelcomePage.class);

        factory.setWelcome(welcome);
        factory.setOrgid("");

        // when, then
        validator.validateTrackedFactoryAndParams(factory);
    }

    @Test(dataProvider = "trackedFactoryParametersProvider", expectedExceptions = ApiException.class)
    public void shouldNotValidateIfThereIsTrackedOnlyParameterAndOrgidIsNull(Factory factory)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, ApiException {
        factory.setOrgid(null);
        validator.validateTrackedFactoryAndParams(factory);
    }

    @Test
    public void shouldValidateIfCurrentTimeBeforeSinceUntil() throws ConflictException {
        Long currentTime = new Date().getTime();
        factory.withRestriction(DtoFactory.getInstance().createDto(Restriction.class)
                                          .withValidsince(currentTime + 10000l)
                                          .withValiduntil(currentTime + 20000l));
        validator.validateCurrentTimeBeforeSinceUntil(factory);
    }

    @Test(expectedExceptions = ApiException.class,
          expectedExceptionsMessageRegExp = FactoryConstants.INVALID_VALIDSINCE_MESSAGE)
    public void shouldNotValidateIfValidSinceBeforeCurrent() throws ApiException {
        factory.withRestriction(DtoFactory.getInstance().createDto(Restriction.class).withValidsince(1l));
        validator.validateCurrentTimeBeforeSinceUntil(factory);
    }

    @Test(expectedExceptions = ApiException.class,
          expectedExceptionsMessageRegExp = FactoryConstants.INVALID_VALIDUNTIL_MESSAGE)
    public void shouldNotValidateIfValidUntilBeforeCurrent() throws ApiException {
        factory.withRestriction(DtoFactory.getInstance().createDto(Restriction.class).withValiduntil(1l));
        validator.validateCurrentTimeBeforeSinceUntil(factory);
    }

    @Test(expectedExceptions = ApiException.class,
          expectedExceptionsMessageRegExp = FactoryConstants.INVALID_VALIDSINCEUNTIL_MESSAGE)
    public void shouldNotValidateIfValidUntilBeforeValidSince() throws ApiException {
        factory.withRestriction(DtoFactory.getInstance().createDto(Restriction.class)
                                          .withValiduntil(1l)
                                          .withValidsince(2l));
        validator.validateCurrentTimeBeforeSinceUntil(factory);
    }

    @Test(expectedExceptions = ApiException.class,
          expectedExceptionsMessageRegExp = FactoryConstants.ILLEGAL_FACTORY_BY_VALIDUNTIL_MESSAGE)
    public void shouldNotValidateIfValidUntilBeforeCurrentTime() throws ApiException {
        Long currentTime = new Date().getTime();
        factory.withRestriction(DtoFactory.getInstance().createDto(Restriction.class)
                                          .withValiduntil(currentTime - 10000l));
        validator.validateCurrentTimeBetweenSinceUntil(factory);
    }

    @Test
    public void shouldValidateIfCurrentTimeBetweenValidUntilSince() throws ApiException {
        Long currentTime = new Date().getTime();
        factory.withRestriction(DtoFactory.getInstance().createDto(Restriction.class)
                                          .withValidsince(currentTime - 10000l)
                                          .withValiduntil(currentTime + 10000l));
        validator.validateCurrentTimeBetweenSinceUntil(factory);
    }

    @Test(expectedExceptions = ApiException.class,
          expectedExceptionsMessageRegExp = FactoryConstants.ILLEGAL_FACTORY_BY_VALIDSINCE_MESSAGE)
    public void shouldNotValidateIfValidUntilSinceAfterCurrentTime() throws ApiException {
        Long currentTime = new Date().getTime();
        factory.withRestriction(DtoFactory.getInstance().createDto(Restriction.class)
                                          .withValidsince(currentTime + 10000l));
        validator.validateCurrentTimeBetweenSinceUntil(factory);
    }

    @DataProvider(name = "trackedFactoryParametersProvider")
    public Object[][] trackedFactoryParametersProvider() throws URISyntaxException, IOException, NoSuchMethodException {
        return new Object[][]{
                {factory.withWelcome(DtoFactory.getInstance().createDto(WelcomePage.class))},
                {factory.withRestriction(DtoFactory.getInstance().createDto(Restriction.class).withValidsince(123456l))},
                {factory.withRestriction(DtoFactory.getInstance().createDto(Restriction.class).withValiduntil(123456798l))},
                {factory.withRestriction(DtoFactory.getInstance().createDto(Restriction.class).withPassword("123456"))},
                {factory.withRestriction(DtoFactory.getInstance().createDto(Restriction.class).withMaxsessioncount(1234l))},
                {factory.withRestriction(DtoFactory.getInstance().createDto(Restriction.class).withRefererhostname("host"))},
                {factory.withRestriction(DtoFactory.getInstance().createDto(Restriction.class).withRestrictbypassword(true))}
        };
    }

    @Test
    public void shouldValidateTrackedParamsIfOrgIdIsMissingButOnPremisesTrue() throws Exception {
        final DtoFactory dtoFactory = DtoFactory.getInstance();
        Factory factory = dtoFactory.createDto(Factory.class);
        factory.withV("2.0")
               .withPolicies(dtoFactory.createDto(Policies.class)
                                       .withValidSince(System.currentTimeMillis() + 1_000_000)
                                       .withValidUntil(System.currentTimeMillis() + 10_000_000)
                                       .withRefererHostname("codenvy.com"))
               .withActions(dtoFactory.createDto(Actions.class)
                                      .withWelcome(dtoFactory.createDto(WelcomePage.class)));
        validator = new TestFactoryUrlBaseValidator(accountDao, userDao, profileDao, true);

        validator.validateOrgid(factory);
        validator.validateTrackedFactoryAndParams(factory);
    }

    @Test(dataProvider = "trackedFactoryParameterWithoutOrgIdProvider",
            expectedExceptions = ConflictException.class,
            expectedExceptionsMessageRegExp = "(?s)You do not have a valid orgID. Your Factory configuration has a parameter that.*")
    public void shouldNotValidateTrackedParamsIfOrgIdIsMissingAndOnPremisesFalse(Factory factory) throws Exception {
        validator = new TestFactoryUrlBaseValidator(accountDao, userDao, profileDao, false);

        validator.validateTrackedFactoryAndParams(factory);
    }

    @DataProvider(name = "trackedFactoryParameterWithoutOrgIdProvider")
    public Object[][] dataProvider() {
        final DtoFactory dtoFactory = DtoFactory.getInstance();
        Factory factory = dtoFactory.createDto(Factory.class);
        factory.withV("2.0");
        return new Object[][] {
                {dtoFactory.clone(factory).withActions(dtoFactory.createDto(Actions.class)
                                                                 .withWelcome(dtoFactory.createDto(WelcomePage.class)))},
                {dtoFactory.clone(factory).withPolicies(dtoFactory.createDto(Policies.class).withValidSince(
                        System.currentTimeMillis() + 1_000_000))},
                {dtoFactory.clone(factory).withPolicies(dtoFactory.createDto(Policies.class).withValidUntil(
                        System.currentTimeMillis() + 10_000_000))}
        };
    }
}
