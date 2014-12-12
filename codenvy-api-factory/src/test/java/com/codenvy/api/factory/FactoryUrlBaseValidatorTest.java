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
import com.codenvy.api.factory.dto.Action;
import com.codenvy.api.factory.dto.Actions;
import com.codenvy.api.factory.dto.Author;
import com.codenvy.api.factory.dto.Factory;
import com.codenvy.api.factory.dto.Ide;
import com.codenvy.api.factory.dto.OnAppLoaded;
import com.codenvy.api.factory.dto.OnProjectOpened;
import com.codenvy.api.factory.dto.Policies;
import com.codenvy.api.factory.dto.WelcomePage;
import com.codenvy.api.project.shared.dto.ImportSourceDescriptor;
import com.codenvy.api.project.shared.dto.NewProject;
import com.codenvy.api.project.shared.dto.Source;
import com.codenvy.api.user.server.dao.Profile;
import com.codenvy.api.user.server.dao.User;
import com.codenvy.api.user.server.dao.UserDao;
import com.codenvy.api.user.server.dao.UserProfileDao;
import com.codenvy.dto.server.DtoFactory;
import com.google.common.collect.ImmutableMap;

import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import static java.util.Collections.singletonList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
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

    private DtoFactory dto = DtoFactory.getInstance();


    @BeforeMethod
    public void setUp() throws ParseException, NotFoundException, ServerException {
        factory = dto.createDto(Factory.class)
                     .withV("2.1")
                     .withSource(dto.createDto(Source.class)
                                    .withProject(dto.createDto(ImportSourceDescriptor.class)
                                                    .withType("git")
                                                    .withLocation(VALID_REPOSITORY_URL)))
                     .withCreator(dto.createDto(Author.class)
                                     .withAccountId(ID)
                                     .withUserId("userid")

                                 );


        User user = new User().withId("userid");

        Subscription subscription = new Subscription()
                .withServiceId("Factory")
                .withProperties(Collections.singletonMap("Package", "Tracked"));
        member = new Member().withUserId("userid").withRoles(Arrays.asList("account/owner"));
        when(accountDao.getSubscriptions(ID, "Factory")).thenReturn(Arrays.asList(subscription));
        when(accountDao.getMembers(anyString())).thenReturn(Arrays.asList(member));
        when(userDao.getById("userid")).thenReturn(user);
        when(profileDao.getById(anyString())).thenReturn(new Profile());

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
        factory = factory.withSource(dto.createDto(Source.class)
                                        .withProject(dto.createDto(ImportSourceDescriptor.class)
                                                        .withType("esbwso2")
                                                        .withLocation(VALID_REPOSITORY_URL)));


        validator.validateSource(factory);
        validator.validateProjectName(factory);
        validator.validateOrgid(factory);
        validator.validateTrackedFactoryAndParams(factory);
    }

    @Test(expectedExceptions = ApiException.class,
          expectedExceptionsMessageRegExp =
                  "The parameter source.project.location has a value submitted http://codenvy.com/git/04%2 with a value that is " +
                  "unexpected. " +
                  "For more information, please visit http://docs.codenvy.com/user/project-lifecycle/#configuration-reference")
    public void shouldNotValidateIfVcsurlContainIncorrectEncodedSymbol() throws ApiException {
        // given
        factory = factory.withSource(dto.createDto(Source.class)
                                        .withProject(dto.createDto(ImportSourceDescriptor.class)
                                                        .withType("git")
                                                        .withLocation("http://codenvy.com/git/04%2")));


        // when, then
        validator.validateSource(factory);
    }

    @Test
    public void shouldValidateIfVcsurlIsCorrectSsh() throws ApiException {
        // given
        factory = factory.withSource(dto.createDto(Source.class)
                                        .withProject(dto.createDto(ImportSourceDescriptor.class)
                                                        .withType("git")
                                                        .withLocation("ssh://codenvy@review.gerrithub.io:29418/codenvy/exampleProject")));


        // when, then
        validator.validateSource(factory);
    }

    @Test
    public void shouldValidateIfVcsurlIsCorrectHttps() throws ApiException {
        // given

        factory = factory.withSource(dto.createDto(Source.class)
                                        .withProject(dto.createDto(ImportSourceDescriptor.class)
                                                        .withType("git")
                                                        .withLocation("https://github.com/codenvy/example.git")));

        // when, then
        validator.validateSource(factory);
    }

    @Test(dataProvider = "badAdvancedFactoryUrlProvider", expectedExceptions = ApiException.class)
    public void shouldNotValidateIfVcsOrVcsUrlIsInvalid(Factory factoryUrl) throws ApiException {
        validator.validateSource(factoryUrl);
    }

    @DataProvider(name = "badAdvancedFactoryUrlProvider")
    public Object[][] invalidParametersFactoryUrlProvider() throws UnsupportedEncodingException {
        Factory adv1 = DtoFactory.getInstance().createDto(Factory.class)
                                 .withV("2.1")
                                 .withSource(dto.createDto(Source.class)
                                                .withProject(dto.createDto(ImportSourceDescriptor.class)
                                                                .withType("notagit")
                                                                .withLocation(VALID_REPOSITORY_URL)));


        Factory adv2 = DtoFactory.getInstance().createDto(Factory.class).withV("2.1")
                                 .withSource(dto.createDto(Source.class)
                                                .withProject(dto.createDto(ImportSourceDescriptor.class)
                                                                .withType("git")));


        Factory adv3 = DtoFactory.getInstance().createDto(Factory.class).withV("2.1")
                                 .withSource(dto.createDto(Source.class)
                                                .withProject(dto.createDto(ImportSourceDescriptor.class)
                                                                .withType("git").withLocation("")));


        return new Object[][]{
                {adv1},// invalid vcs
                {adv2},// invalid vcsurl
                {adv3}// invalid vcsurl
        };
    }

    @Test(dataProvider = "invalidProjectNamesProvider", expectedExceptions = ApiException.class,
          expectedExceptionsMessageRegExp = "Project name must contain only Latin letters, digits or these following special characters" +
                                            " -._.")
    public void shouldThrowFactoryUrlExceptionIfProjectNameInvalid(String projectName) throws Exception {
        // given
        factory.withProject(dto.createDto(NewProject.class)
                               .withType("type")
                               .withName(projectName));


        // when, then
        validator.validateProjectName(factory);
    }

    @Test(dataProvider = "validProjectNamesProvider")
    public void shouldBeAbleToValidateValidProjectName(String projectName) throws Exception {
        // given
        factory.withProject(dto.createDto(NewProject.class)
                               .withType("type")
                               .withName(projectName));


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
        factory.withPolicies(dto.createDto(Policies.class).withRefererHostname("notcodenvy.com"));


        when(request.getHeader("Referer")).thenReturn("http://notcodenvy.com/factories-examples");

        // when, then
        validator.validateTrackedFactoryAndParams(factory);
    }

    @Test
    public void shouldValidateIfRefererIsRelativeAndCurrentHostnameIsEqualToRequiredHostName()
            throws ApiException, ParseException {
        // given
        factory.withPolicies(dto.createDto(Policies.class).withRefererHostname("next.codenvy.com"));

        when(request.getHeader("Referer")).thenReturn("/factories-examples");
        when(request.getServerName()).thenReturn("next.codenvy.com");

        // when, then
        validator.validateTrackedFactoryAndParams(factory);
    }

    @Test(expectedExceptions = ApiException.class)
    public void shouldNotValidateEncodedFactoryWithWelcomePageInOnProjectOpenedIfOrgIdIsEmpty() throws ApiException {
        // given
        factory.withIde(dto.createDto(Ide.class)
                           .withOnProjectOpened(dto.createDto(OnProjectOpened.class)
                                                   .withActions(singletonList(dto.createDto(Action.class)
                                                                                 .withId("welcomePanel")
                                                                                 .withProperties(ImmutableMap
                                                                                                         .<String,
                                                                                                                 String>builder()
                                                                                                         .put("authenticatedTitle",
                                                                                                              "title")
                                                                                                         .put("authenticatedIconUrl",
                                                                                                              "url")
                                                                                                         .put("authenticatedContentUrl",
                                                                                                              "url")
                                                                                                         .put("nonAuthenticatedTitle",
                                                                                                              "title")
                                                                                                         .put("nonAuthenticatedIconUrl",
                                                                                                              "url")
                                                                                                         .put("nonAuthenticatedContentUrl",
                                                                                                              "url")
                                                                                                         .build()))
                                                               )));
        factory.withCreator(dto.createDto(Author.class)
                               .withAccountId("")
                               .withUserId("userid"));

        // when, then
        validator.validateTrackedFactoryAndParams(factory);
    }

    @Test(expectedExceptions = ApiException.class)
    public void shouldNotValidateEncodedFactoryWithWelcomePageInOnAppLoadedIfOrgIdIsEmpty() throws ApiException {
        // given
        factory.withIde(dto.createDto(Ide.class)
                           .withOnAppLoaded(dto.createDto(OnAppLoaded.class)
                                                   .withActions(singletonList(dto.createDto(Action.class)
                                                                                 .withId("welcomePanel")
                                                                                 .withProperties(ImmutableMap
                                                                                                         .<String,
                                                                                                                 String>builder()
                                                                                                         .put("authenticatedTitle",
                                                                                                              "title")
                                                                                                         .put("authenticatedIconUrl",
                                                                                                              "url")
                                                                                                         .put("authenticatedContentUrl",
                                                                                                              "url")
                                                                                                         .put("nonAuthenticatedTitle",
                                                                                                              "title")
                                                                                                         .put("nonAuthenticatedIconUrl",
                                                                                                              "url")
                                                                                                         .put("nonAuthenticatedContentUrl",
                                                                                                              "url")
                                                                                                         .build()))
                                                               )));
        factory.withCreator(dto.createDto(Author.class)
                               .withAccountId("")
                               .withUserId("userid"));

        // when, then
        validator.validateTrackedFactoryAndParams(factory);
    }


    @Test
    public void shouldValidateIfCurrentTimeBeforeSinceUntil() throws ConflictException {
        Long currentTime = new Date().getTime();

        factory.withPolicies(dto.createDto(Policies.class)
                                .withValidSince(currentTime + 10000l)
                                .withValidUntil(currentTime + 20000l)
                            );
        validator.validateCurrentTimeBeforeSinceUntil(factory);
    }

    @Test(expectedExceptions = ApiException.class,
          expectedExceptionsMessageRegExp = FactoryConstants.INVALID_VALIDSINCE_MESSAGE)
    public void shouldNotValidateIfValidSinceBeforeCurrent() throws ApiException {
        factory.withPolicies(dto.createDto(Policies.class)
                                .withValidSince(1l)
                            );
        validator.validateCurrentTimeBeforeSinceUntil(factory);
    }

    @Test(expectedExceptions = ApiException.class,
          expectedExceptionsMessageRegExp = FactoryConstants.INVALID_VALIDUNTIL_MESSAGE)
    public void shouldNotValidateIfValidUntilBeforeCurrent() throws ApiException {
        factory.withPolicies(dto.createDto(Policies.class)
                                .withValidUntil(1l)
                            );
        validator.validateCurrentTimeBeforeSinceUntil(factory);
    }

    @Test(expectedExceptions = ApiException.class,
          expectedExceptionsMessageRegExp = FactoryConstants.INVALID_VALIDSINCEUNTIL_MESSAGE)
    public void shouldNotValidateIfValidUntilBeforeValidSince() throws ApiException {
        factory.withPolicies(dto.createDto(Policies.class)
                                .withValidSince(2l)
                                .withValidUntil(1l)
                            );

        validator.validateCurrentTimeBeforeSinceUntil(factory);
    }

    @Test(expectedExceptions = ApiException.class,
          expectedExceptionsMessageRegExp = FactoryConstants.ILLEGAL_FACTORY_BY_VALIDUNTIL_MESSAGE)
    public void shouldNotValidateIfValidUntilBeforeCurrentTime() throws ApiException {
        Long currentTime = new Date().getTime();
        factory.withPolicies(dto.createDto(Policies.class)
                                .withValidUntil(currentTime - 10000l)
                            );


        validator.validateCurrentTimeBetweenSinceUntil(factory);
    }

    @Test
    public void shouldValidateIfCurrentTimeBetweenValidUntilSince() throws ApiException {
        Long currentTime = new Date().getTime();

        factory.withPolicies(dto.createDto(Policies.class)
                                .withValidSince(currentTime - 10000l)
                                .withValidUntil(currentTime + 10000l)
                            );

        validator.validateCurrentTimeBetweenSinceUntil(factory);
    }

    @Test(expectedExceptions = ApiException.class,
          expectedExceptionsMessageRegExp = FactoryConstants.ILLEGAL_FACTORY_BY_VALIDSINCE_MESSAGE)
    public void shouldNotValidateIfValidUntilSinceAfterCurrentTime() throws ApiException {
        Long currentTime = new Date().getTime();
        factory.withPolicies(dto.createDto(Policies.class)
                                .withValidSince(currentTime + 10000l)
                            );

        validator.validateCurrentTimeBetweenSinceUntil(factory);
    }


    @Test
    public void shouldValidateTrackedParamsIfOrgIdIsMissingButOnPremisesTrue() throws Exception {
        final DtoFactory dtoFactory = DtoFactory.getInstance();
        Factory factory = dtoFactory.createDto(Factory.class);
        factory.withV("2.1")
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


    @Test(dataProvider = "trackedFactoryParameterWithoutValidAccountId",
          expectedExceptions = ConflictException.class,
          expectedExceptionsMessageRegExp = "(?s)You do not have a valid orgID. Your Factory configuration has a parameter that.*")
    public void shouldNotValidateTrackedParamsIfOrgIdIsMissingAndOnPremisesFalse(Factory factory) throws Exception {
        validator = new TestFactoryUrlBaseValidator(accountDao, userDao, profileDao, false);

        validator.validateTrackedFactoryAndParams(dto.clone(factory).withCreator(dto.createDto(Author.class).withAccountId(null)));
    }


    @Test(dataProvider = "trackedFactoryParameterWithoutValidAccountId")
    public void shouldNotFailValidateTrackedParamsIfOrgIdIsMissingAndOnPremisesTrue(Factory factory) throws Exception {
        validator = new TestFactoryUrlBaseValidator(accountDao, userDao, profileDao, true);

        validator.validateTrackedFactoryAndParams(dto.clone(factory).withCreator(dto.createDto(Author.class).withAccountId(null)));
    }


    @Test(expectedExceptions = ConflictException.class)
    public void shouldNotValidateTrackedParamsIfSubscribtionIsMissing() throws Exception {
        //given
        validator = new TestFactoryUrlBaseValidator(accountDao, userDao, profileDao, false);
        Factory factoryWithAccountId = dto.clone(factory).withCreator(dto.createDto(Author.class).withAccountId("accountId-1243"));
        when(accountDao.getSubscriptions(eq("accountId-1243"), eq("Factory"))).thenReturn(Collections.<Subscription>emptyList());
        //when
        validator.validateTrackedFactoryAndParams(factoryWithAccountId);
    }

    @Test(expectedExceptions = ConflictException.class)
    public void shouldNotValidateTrackedParamsIfSubscribtionIsNotFound() throws Exception {
        //given
        validator = new TestFactoryUrlBaseValidator(accountDao, userDao, profileDao, false);
        Factory factoryWithAccountId = dto.clone(factory).withCreator(dto.createDto(Author.class).withAccountId("accountId-1243"));
        when(accountDao.getSubscriptions(eq("accountId-1243"), eq("Factory"))).thenThrow(NotFoundException.class);
        //when
        validator.validateTrackedFactoryAndParams(factoryWithAccountId);
    }


    @DataProvider(name = "trackedFactoryParameterWithoutValidAccountId")
    public Object[][] trackedFactoryParameterWithoutValidAccountId() throws URISyntaxException, IOException, NoSuchMethodException {
        return new Object[][]{
                {
                        dto.createDto(Factory.class)
                           .withV("2.1")
                           .withIde(dto.createDto(Ide.class)
                                       .withOnProjectOpened(dto.createDto(OnProjectOpened.class)
                                                               .withActions(singletonList(dto.createDto(Action.class)
                                                                                             .withId("welcomePanel")
                                                                                             .withProperties(
                                                                                                     ImmutableMap
                                                                                                             .<String,
                                                                                                                     String>builder()
                                                                                                             .put("authenticatedTitle",
                                                                                                                  "title")
                                                                                                             .put("authenticatedIconUrl",
                                                                                                                  "url")
                                                                                                             .put("authenticatedContentUrl",
                                                                                                                  "url")
                                                                                                             .put("nonAuthenticatedTitle",
                                                                                                                  "title")
                                                                                                             .put("nonAuthenticatedIconUrl",
                                                                                                                  "url")
                                                                                                             .put("nonAuthenticatedContentUrl",
                                                                                                                  "url")
                                                                                                             .build()))
                                                                           )))},

                {dto.createDto(Factory.class).withV("2.1").withPolicies(dto.createDto(Policies.class).withValidSince(10000l))},
                {dto.createDto(Factory.class).withV("2.1").withPolicies(dto.createDto(Policies.class).withValidUntil(10000l))},
                {dto.createDto(Factory.class).withV("2.0")
                    .withActions(dto.createDto(Actions.class).withWelcome(dto.createDto(WelcomePage.class)))},
                {dto.createDto(Factory.class).withV("2.1").withPolicies(dto.createDto(Policies.class).withRefererHostname("host"))}
        };
    }

}
