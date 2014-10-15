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

import com.codenvy.api.core.ApiException;
import com.codenvy.api.core.factory.FactoryParameter;
import com.codenvy.api.factory.dto.Actions;
import com.codenvy.api.factory.dto.Author;
import com.codenvy.api.factory.dto.Button;
import com.codenvy.api.factory.dto.ButtonAttributes;
import com.codenvy.api.factory.dto.Factory;
import com.codenvy.api.factory.dto.FactoryV1_1;
import com.codenvy.api.factory.dto.Git;
import com.codenvy.api.factory.dto.Policies;
import com.codenvy.api.factory.dto.ProjectAttributes;
import com.codenvy.api.factory.dto.Replacement;
import com.codenvy.api.factory.dto.Restriction;
import com.codenvy.api.project.shared.dto.RunnerSource;
import com.codenvy.api.project.shared.dto.Source;
import com.codenvy.api.factory.dto.Variable;
import com.codenvy.api.factory.dto.WelcomeConfiguration;
import com.codenvy.api.factory.dto.WelcomePage;
import com.codenvy.api.factory.dto.Workspace;
import com.codenvy.api.project.shared.dto.BuildersDescriptor;
import com.codenvy.api.project.shared.dto.ImportSourceDescriptor;
import com.codenvy.api.project.shared.dto.NewProject;
import com.codenvy.api.project.shared.dto.RunnerConfiguration;
import com.codenvy.api.project.shared.dto.RunnersDescriptor;
import com.codenvy.dto.server.DtoFactory;

import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;

import static com.codenvy.api.core.factory.FactoryParameter.FactoryFormat;
import static com.codenvy.api.core.factory.FactoryParameter.FactoryFormat.ENCODED;
import static com.codenvy.api.core.factory.FactoryParameter.FactoryFormat.NONENCODED;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.testng.Assert.assertEquals;

/**
 * Tests for {@link com.codenvy.api.factory.dto.Factory}
 *
 * @author Alexander Garagatyi
 * @author Sergii Kabashniuk
 */
@Listeners(MockitoTestNGListener.class)
public class FactoryBuilderTest {

    private static DtoFactory dto = DtoFactory.getInstance();

    private FactoryBuilder factoryBuilder;

    private Factory actual;

    private Factory expected;

    @Mock
    private SourceProjectParametersValidator sourceProjectParametersValidator;

    @BeforeMethod
    public void setUp() throws Exception {
        factoryBuilder = new FactoryBuilder(sourceProjectParametersValidator);
        actual = dto.createDto(Factory.class);

        expected = dto.createDto(Factory.class);
    }

    @Test(dataProvider = "jsonprovider")
    public void shouldBeAbleToParserJsonV1_1(String json) {

        Factory factory = dto.createDtoFromJson(json, Factory.class);
        //System.out.println(FactoryBuilder.buildEncoded(factory));
    }


    @DataProvider(name = "jsonprovider")
    public static Object[][] createData() throws URISyntaxException, IOException {
        File file = new File(FactoryBuilderTest.class.getResource("/logback-test.xml").toURI());
        File resourcesDirectory = file.getParentFile();
        String[] list = resourcesDirectory.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".json");
            }
        });
        Object[][] result = new Object[list.length][1];
        for (int i = 0; i < list.length; i++) {
            result[i][0] = new String(Files.readAllBytes(new File(resourcesDirectory, list[i]).toPath()), "UTF-8");
        }

        return result;
    }

    @Test
    public void shouldBeAbleToValidateFactory1_0() throws ApiException {
        actual.withV("1.0").withVcs("vcs").withVcsurl("vcsurl").withIdcommit("idcommit").withPtype("ptype").withPname("pname")
              .withAction("action").withWname("wname").withVcsinfo(true).withOpenfile("openfile");

        factoryBuilder.checkValid(actual, NONENCODED);
    }

    @Test
    public void shouldBeAbleToValidateEncodedFactory1_1() throws ApiException {
        ((FactoryV1_1)actual.withV("1.1").withVcs("vcs").withVcsurl("vcsurl").withCommitid("commitid").withVcsinfo(
                true).withOpenfile("openfile").withAction("action")).withStyle("style").withDescription("description").withContactmail(
                "contactmail").withAuthor("author").withOrgid("orgid").withAffiliateid("affid").withVcsbranch("branch")
                                                                    .withValidsince(123456789l).withValiduntil(234567899l);

        actual.setProjectattributes(dto.createDto(ProjectAttributes.class).withPname("pname").withPtype("ptype"));

        Replacement replacement = dto.createDto(Replacement.class).withReplacemode(
                "replacemod").withReplace("replace").withFind("find");

        actual.setVariables(Arrays.asList(dto.createDto(Variable.class).withEntries(Arrays.asList(replacement))
                                             .withFiles(Arrays.asList("file1"))));

        WelcomeConfiguration wc = dto.createDto(WelcomeConfiguration.class);
        actual.setWelcome(dto.createDto(WelcomePage.class).withAuthenticated(wc).withNonauthenticated(wc));

        factoryBuilder.checkValid(actual, ENCODED);
    }

    @Test
    public void shouldBeAbleToValidateNonEncodedFactory1_1() throws ApiException {
        ((FactoryV1_1)actual.withV("1.1").withVcs("vcs").withVcsurl("vcsurl").withCommitid("commitid").withAction("action").withVcsinfo(
                true).withOpenfile("openfile")).withContactmail("contactmail").withAuthor("author").withOrgid("orgid")
                                               .withAffiliateid("affid").withVcsbranch(
                "branch").withValidsince(123456789l).withValiduntil(234567899l);

        actual.setProjectattributes(dto.createDto(ProjectAttributes.class).withPname("pname").withPtype("ptype"));

        Replacement replacement = dto.createDto(Replacement.class).withReplacemode(
                "replacemod").withReplace("replace").withFind("find");

        actual.setVariables(Arrays.asList(dto.createDto(Variable.class).withEntries(Arrays.asList(replacement))
                                             .withFiles(Arrays.asList("file1"))));

        factoryBuilder.checkValid(actual, NONENCODED);
    }

    @Test
    public void shouldBeAbleToValidateEncodedFactory1_2() throws ApiException {
        ((FactoryV1_1)actual.withV("1.2").withVcs("vcs").withVcsinfo(true).withOpenfile("openfile").withVcsurl("vcsurl")
                            .withCommitid("commitid").withAction(
                        "action")).withStyle("style").withDescription("description").withContactmail("contactmail").withAuthor("author")
                                  .withOrgid("orgid").withAffiliateid("affid").withVcsbranch("branch");

        actual.setProjectattributes(dto.createDto(ProjectAttributes.class).withPname("pname").withPtype("ptype"));

        Replacement replacement = dto.createDto(Replacement.class).withReplacemode(
                "replacemod").withReplace("replace").withFind("find");

        actual.setVariables(Arrays.asList(dto.createDto(Variable.class).withEntries(Arrays.asList(replacement))
                                             .withFiles(Arrays.asList("file1"))));

        WelcomeConfiguration wc = dto.createDto(WelcomeConfiguration.class);
        actual.setWelcome(dto.createDto(WelcomePage.class).withAuthenticated(wc).withNonauthenticated(wc));

        actual.withGit(dto.createDto(Git.class).withConfigbranchmerge(
                "configbranchmerge").withConfigpushdefault("configpushdefault").withConfigremoteoriginfetch(
                "configremoteoriginfetch"));

        actual.withRestriction(
                dto.createDto(Restriction.class).withPassword("password").withRefererhostname("codenvy-dev.com")
                   .withValiduntil(123456789l).withValidsince(12345678l).withMaxsessioncount(123l));

        factoryBuilder.checkValid(actual, ENCODED);
    }

    @Test
    public void shouldBeAbleToValidateNonEncodedFactory1_2() throws ApiException {
        ((FactoryV1_1)actual.withV("1.2").withVcs("vcs").withVcsurl("vcsurl").withVcsinfo(true).withCommitid("commitid").withOpenfile(
                "openfile").withAction("action")).withContactmail("contactmail").withAuthor(
                "author").withOrgid("orgid").withAffiliateid("affid").withVcsbranch("branch");

        actual.setProjectattributes(dto.createDto(ProjectAttributes.class).withPname("pname").withPtype("ptype"));

        Replacement replacement = dto.createDto(Replacement.class).withReplacemode(
                "replacemod").withReplace("replace").withFind("find");

        actual.setVariables(Arrays.asList(dto.createDto(Variable.class).withEntries(Arrays.asList(replacement))
                                             .withFiles(Arrays.asList("file1"))));

        actual.withGit(dto.createDto(Git.class).withConfigbranchmerge(
                "configbranchmerge").withConfigpushdefault("configpushdefault").withConfigremoteoriginfetch(
                "configremoteoriginfetch"));

        actual.withRestriction(
                dto.createDto(Restriction.class).withPassword("password").withRefererhostname("codenvy-dev.com")
                   .withValiduntil(123456789l).withValidsince(12345678l).withMaxsessioncount(123l));


        factoryBuilder.checkValid(actual, NONENCODED);
    }

    @Test
    public void shouldBeAbleToValidateEncodedV2_0() throws Exception {
        actual.withV("2.0")
              .withSource(dto.createDto(Source.class)
                             .withProject(dto.createDto(ImportSourceDescriptor.class)
                                             .withType("git")
                                             .withLocation("location")
                                             .withParameters(singletonMap("key", "value")))
                             .withRunners(singletonMap("runEnv", dto.createDto(RunnerSource.class)
                                                                    .withLocation("location")
                                                                    .withParameters(singletonMap("key", "value")))))
              .withProject(dto.createDto(NewProject.class)
                              .withType("type")
                              .withAttributes(singletonMap("key", singletonList("value")))
                              .withBuilders(dto.createDto(BuildersDescriptor.class).withDefault("default"))
                              .withDescription("description")
                              .withName("name")
                              .withRunners(dto.createDto(RunnersDescriptor.class)
                                              .withDefault("default")
                                              .withConfigs(singletonMap("key", dto.createDto(RunnerConfiguration.class)
                                                                                  .withRam(768)
                                                                                  .withOptions(singletonMap("key", "value"))
                                                                                  .withVariables(singletonMap("key", "value")))))
                              .withVisibility("private"))
              .withCreator(dto.createDto(Author.class)
                              .withAccountId("accountId")
                              .withEmail("email")
                              .withName("name"))
              .withPolicies(dto.createDto(Policies.class)
                               .withRefererHostname("referrer")
                               .withValidSince(123l)
                               .withValidUntil(123l))
              .withActions(dto.createDto(Actions.class)
                              .withFindReplace(singletonList(dto.createDto(Variable.class)
                                                                .withFiles(singletonList("file"))
                                                                .withEntries(singletonList(dto.createDto(Replacement.class)
                                                                                              .withFind("find")
                                                                                              .withReplace("replace")
                                                                                              .withReplacemode("mode")))))
//                              .withMacro("macro")
                              .withOpenFile("openFile")
                              .withWarnOnClose(true)
                              .withWelcome(dto.createDto(WelcomePage.class)
                                              .withAuthenticated(dto.createDto(WelcomeConfiguration.class)
                                                                    .withContenturl("url")
                                                                    .withIconurl("url")
                                                                    .withNotification("notification")
                                                                    .withTitle("title"))
                                              .withNonauthenticated(dto.createDto(WelcomeConfiguration.class)
                                                                       .withContenturl("url")
                                                                       .withIconurl("url")
                                                                       .withNotification("notification")
                                                                       .withTitle("title"))))
              .withButton(dto.createDto(Button.class)
                             .withType(Button.ButtonType.logo)
                             .withAttributes(dto.createDto(ButtonAttributes.class)
                                                .withColor("color")
                                                .withCounter(true)
                                                .withLogo("logo")
                                                .withStyle("style")))
              .withWorkspace(dto.createDto(Workspace.class)
                                .withTemp(true)
                                .withAttributes(singletonMap("key", "value")));

        factoryBuilder.checkValid(actual, ENCODED);

        verify(sourceProjectParametersValidator).validate(any(ImportSourceDescriptor.class), eq(FactoryParameter.Version.V2_0));
    }

    @Test
    public void shouldBeAbleToValidateNonEncodedV2_0() throws Exception {
        actual.withV("2.0")
              .withSource(dto.createDto(Source.class)
                             .withProject(dto.createDto(ImportSourceDescriptor.class)
                                             .withType("git")
                                             .withLocation("location")
                                             .withParameters(singletonMap("key", "value")))
                             .withRunners(singletonMap("runEnv", dto.createDto(RunnerSource.class)
                                                                    .withLocation("location")
                                                                    .withParameters(singletonMap("key", "value")))))
              .withProject(dto.createDto(NewProject.class)
                              .withType("type")
                              .withAttributes(singletonMap("key", singletonList("value")))
                              .withBuilders(dto.createDto(BuildersDescriptor.class).withDefault("default"))
                              .withDescription("description")
                              .withName("name")
                              .withRunners(dto.createDto(RunnersDescriptor.class)
                                              .withDefault("default")
                                              .withConfigs(singletonMap("key", dto.createDto(RunnerConfiguration.class)
                                                                                  .withRam(768)
                                                                                  .withOptions(singletonMap("key", "value"))
                                                                                  .withVariables(singletonMap("key", "value")))))
                              .withVisibility("private"))
              .withCreator(dto.createDto(Author.class)
                              .withAccountId("accountId")
                              .withEmail("email")
                              .withName("name"))
              .withPolicies(dto.createDto(Policies.class)
                               .withRefererHostname("referrer")
                               .withValidSince(123l)
                               .withValidUntil(123l))
              .withActions(dto.createDto(Actions.class)
                              .withFindReplace(singletonList(dto.createDto(Variable.class)
                                                                .withFiles(singletonList("file"))
                                                                .withEntries(singletonList(dto.createDto(Replacement.class)
                                                                                              .withFind("find")
                                                                                              .withReplace("replace")
                                                                                              .withReplacemode("mode")))))
//                              .withMacro("macro")
                              .withOpenFile("openFile")
                              .withWarnOnClose(true))
              .withWorkspace(dto.createDto(Workspace.class)
                                .withTemp(true)
                                .withAttributes(singletonMap("key", "value")));

        factoryBuilder.checkValid(actual, NONENCODED);

        verify(sourceProjectParametersValidator).validate(any(ImportSourceDescriptor.class), eq(FactoryParameter.Version.V2_0));
    }

    @Test(expectedExceptions = ApiException.class, dataProvider = "TFParamsProvider",
          expectedExceptionsMessageRegExp = "You have provided a Tracked Factory parameter .*, and you do not have a valid orgId.*")
    public void shouldNotAllowUsingParamsForTrackedFactoriesIfOrgidDoesntSet(Factory factory)
            throws InvocationTargetException, IllegalAccessException, ApiException, NoSuchMethodException {
        factoryBuilder.checkValid(factory, ENCODED);
    }

    @DataProvider(name = "TFParamsProvider")
    public static Object[][] tFParamsProvider() throws URISyntaxException, IOException, NoSuchMethodException {
        Factory v1 = (Factory)dto.createDto(Factory.class).withV("1.2").withVcs("vcs").withVcsurl("vcsurl");
        Factory v2 = dto.createDto(Factory.class).withV("2.0").withSource(dto.createDto(Source.class).withProject(
                dto.createDto(ImportSourceDescriptor.class).withType("git").withLocation("location")));
        return new Object[][]{
                {dto.clone(v1).withV("1.1").withWelcome(dto.createDto(WelcomePage.class))},
                {dto.clone(v1).withWelcome(dto.createDto(WelcomePage.class))},
                {dto.clone(v1).withRestriction(dto.createDto(Restriction.class).withPassword("pass"))},
                {dto.clone(v1).withRestriction(dto.createDto(Restriction.class).withRefererhostname("codenvy.com"))},
                {dto.clone(v1).withRestriction(dto.createDto(Restriction.class).withRestrictbypassword(true))},
                {dto.clone(v1).withRestriction(dto.createDto(Restriction.class).withMaxsessioncount(123l))},
                {dto.clone(v1).withRestriction(dto.createDto(Restriction.class).withValidsince(123456789l))},
                {dto.clone(v1).withRestriction(dto.createDto(Restriction.class).withValiduntil(1234567989l))},
                {dto.clone(v2).withActions(dto.createDto(Actions.class).withWelcome(dto.createDto(WelcomePage.class)))},
                {dto.clone(v2).withPolicies(dto.createDto(Policies.class))}
        };
    }

    @Test(expectedExceptions = ApiException.class)
    public void shouldNotAllowInNonencodedVersionUsingParamsOnlyForEncodedVersion() throws ApiException, URISyntaxException {
        StringBuilder sb = new StringBuilder("?");
        sb.append("v=").append("1.0").append("&");
        sb.append("vcs=").append("git").append("&");
        sb.append("welcome=").append("welcome").append("&");

        factoryBuilder.buildEncoded(new URI(sb.toString()));
    }

    @Test(expectedExceptions = ApiException.class)
    public void shouldNotValidateUnparseableFactory() throws ApiException, URISyntaxException {
        factoryBuilder.checkValid(null, NONENCODED);
    }

    @Test(expectedExceptions = ApiException.class, dataProvider = "setByServerParamsProvider",
          expectedExceptionsMessageRegExp = "You have provided an invalid parameter .* for this version of Factory parameters.*")
    public void shouldNotAllowUsingParamsThatCanBeSetOnlyByServer(Factory factory)
            throws InvocationTargetException, IllegalAccessException, ApiException, NoSuchMethodException {
        factoryBuilder.checkValid(factory, ENCODED);
    }

    @DataProvider(name = "setByServerParamsProvider")
    public static Object[][] setByServerParamsProvider() throws URISyntaxException, IOException, NoSuchMethodException {
        Factory v1 = (Factory)dto.createDto(Factory.class).withV("1.1").withVcs("vcs").withVcsurl("vcsurl");
        Factory v2 = dto.createDto(Factory.class).withV("2.0").withSource(dto.createDto(Source.class).withProject(
                dto.createDto(ImportSourceDescriptor.class).withType("git").withLocation("location")));
        return new Object[][]{
                {dto.clone(v1).withId("id")},
                {dto.clone(v1).withUserid("id")},
                {dto.clone(v1).withCreated(123l)},
                {dto.clone(v1).withV("1.2").withId("id")},
                {dto.clone(v1).withV("1.2").withUserid("id")},
                {dto.clone(v1).withV("1.2").withCreated(123l)},
                {dto.clone(v1).withV("1.2").withRestriction(dto.createDto(Restriction.class).withRestrictbypassword(true)).withOrgid(
                        "orgid")},
                {dto.clone(v2).withId("id")},
                {dto.clone(v2).withCreator(dto.createDto(Author.class).withUserId("id"))},
                {dto.clone(v2).withCreator(dto.createDto(Author.class).withCreated(123l))}
        };
    }

    //    @Test
    public void shouldBeAbleToConvertToLatest() throws ApiException {
        actual.withIdcommit("idcommit").withPname("pname").withPtype("ptype").withWname("wname");

        expected.withProjectattributes(
                dto.createDto(ProjectAttributes.class).withPname(actual.getPname()).withPtype(actual.getPtype()))
                .withCommitid(actual.getIdcommit()).withV("1.2");

        assertEquals(factoryBuilder.convertToLatest(actual), expected);
    }

    @Test(expectedExceptions = ApiException.class, dataProvider = "notValidParamsProvider")
    public void shouldNotAllowUsingNotValidParams(Factory factory, FactoryFormat encoded)
            throws InvocationTargetException, IllegalAccessException, ApiException, NoSuchMethodException {
        factoryBuilder.checkValid(factory, encoded);
    }

    @DataProvider(name = "notValidParamsProvider")
    public static Object[][] notValidParamsProvider() throws URISyntaxException, IOException, NoSuchMethodException {
        Factory v1 = (Factory)dto.createDto(Factory.class).withV("1.1").withOrgid("id").withVcs("vcs").withVcsurl("vcsurl");
        return new Object[][]{
                {dto.clone(v1).withWname("name"), ENCODED},
                {dto.clone(v1).withV("1.2").withWname("name"), ENCODED},
                {dto.clone(v1).withIdcommit("id"), ENCODED},
                {dto.clone(v1).withPname("name"), ENCODED},
                {dto.clone(v1).withPtype("type"), ENCODED},
                {dto.clone(v1).withV("1.2").withIdcommit("id"), ENCODED},
                {dto.clone(v1).withV("1.2").withPname("name"), ENCODED},
                {dto.clone(v1).withV("1.2").withPtype("type"), ENCODED},
                {dto.clone(v1).withStyle("style"), NONENCODED},
                {dto.clone(v1).withV("1.2").withStyle("style"), NONENCODED},
                {dto.clone(v1).withDescription("desc"), NONENCODED},
                {dto.clone(v1).withV("1.2").withDescription("desc"), NONENCODED},
                {dto.clone(v1).withV("1.0").withOrgid(null).withProjectattributes(dto.createDto(ProjectAttributes.class)),                 ENCODED},
                {dto.clone(v1).withV("1.0").withOrgid(null).withStyle("style"), ENCODED},
                {dto.clone(v1).withV("1.0").withOrgid(null).withDescription("desc"), ENCODED},
                {dto.clone(v1).withV("1.0").withOrgid(null).withContactmail("mail"), ENCODED},
                {dto.clone(v1).withV("1.0").withOrgid(null).withAuthor("author"), ENCODED},
                {dto.clone(v1).withV("1.0").withOrgid(null).withOrgid("id"), ENCODED},
                {dto.clone(v1).withV("1.0").withOrgid(null).withAffiliateid("id"), ENCODED},
                {dto.clone(v1).withV("1.0").withVcsbranch("br").withOrgid(null), ENCODED},
                {dto.clone(v1).withV("1.0").withOrgid(null).withValidsince(123l), ENCODED},
                {dto.clone(v1).withV("1.0").withOrgid(null).withValiduntil(123l), ENCODED},
                {dto.clone(v1).withV("1.2").withValidsince(123l), ENCODED},
                {dto.clone(v1).withV("1.2").withValiduntil(123l), ENCODED},
                {dto.clone(v1).withV("1.0").withOrgid(null).withVariables(Arrays.asList(dto.createDto(Variable.class))), ENCODED},
                {dto.clone(v1).withV("1.0").withOrgid(null).withWelcome(dto.createDto(WelcomePage.class)), ENCODED},
                {dto.clone(v1).withWelcome(dto.createDto(WelcomePage.class)), NONENCODED},
                {dto.clone(v1).withV("1.0").withOrgid(null).withImage("im"), ENCODED},
                {dto.clone(v1).withRestriction(dto.createDto(Restriction.class)).withOrgid(null).withV("1.0"), ENCODED},
                {dto.clone(v1).withRestriction(dto.createDto(Restriction.class)), ENCODED},
                {dto.clone(v1).withV("1.0").withGit(dto.createDto(Git.class)).withOrgid(null), ENCODED},
                {dto.clone(v1).withGit(dto.createDto(Git.class)), ENCODED}
        };
    }

    private String encode(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return value;
        }
    }

    @Test
    public void shouldBeAbleToParseAndValidateNonEncodedFactory1_0()
            throws ApiException, UnsupportedEncodingException, URISyntaxException {
        StringBuilder sb = new StringBuilder("?");
        sb.append("v=").append("1.0").append("&");
        sb.append("vcs=").append("git").append("&");
        sb.append("vcsurl=").append("https://github.com/codenvy/commons.git").append("&");
        sb.append("commitid=").append("7896464674879").append("&");
        sb.append("ptype=").append("ptype").append("&");
        sb.append("pname=").append("pname").append("&");
        sb.append("action=").append("openReadme").append("&");
        sb.append("wname=").append("codenvy").append("&");
        sb.append("vcsinfo=").append("true").append("&");
        sb.append("openfile=").append("openfile").append("&");

        expected.withV("1.0").withVcs("git").withVcsurl("https://github.com/codenvy/commons.git").withCommitid("7896464674879")
                .withAction("openReadme").withPtype("ptype").withPname("pname").withWname("codenvy").withVcsinfo(true)
                .withOpenfile("openfile");

        Factory newFactory = factoryBuilder.buildEncoded(new URI(sb.toString()));
        assertEquals(newFactory, expected);
    }

    @Test
    public void shouldBeAbleToParseAndValidateNonEncodedFactory1_0WithIdCommit()
            throws ApiException, UnsupportedEncodingException, URISyntaxException {
        StringBuilder sb = new StringBuilder("?");
        sb.append("v=").append("1.0").append("&");
        sb.append("vcs=").append("git").append("&");
        sb.append("vcsurl=").append("https://github.com/codenvy/commons.git").append("&");
        sb.append("idcommit=").append("7896464674879").append("&");
        sb.append("ptype=").append("ptype").append("&");
        sb.append("pname=").append("pname").append("&");
        sb.append("action=").append("openReadme").append("&");
        sb.append("wname=").append("codenvy").append("&");
        sb.append("vcsinfo=").append("true").append("&");
        sb.append("openfile=").append("openfile").append("&");

        expected.withV("1.0").withVcs("git").withVcsurl("https://github.com/codenvy/commons.git").withIdcommit("7896464674879")
                .withAction("openReadme").withPtype("ptype").withPname("pname").withWname("codenvy").withVcsinfo(true).withOpenfile(
                "openfile");

        Factory newFactory = factoryBuilder.buildEncoded(new URI(sb.toString()));
        assertEquals(newFactory, expected);
    }

    @Test
    public void shouldBeAbleToParseAndValidateNonEncodedFactory1_1()
            throws ApiException, UnsupportedEncodingException, URISyntaxException {

        expected.setV("1.1");
        expected.setVcs("git");
        expected.setVcsurl("https://github.com/codenvy/commons.git");
        expected.setCommitid("7896464674879");
        expected.setAction("openReadme");
        expected.setContactmail("developer@codenvy.com");
        expected.setAuthor("codenvy");
        expected.setOpenfile("/src/test.java");
        expected.setOrgid("orgid");
        expected.setAffiliateid("affiliateid");
        expected.setVcsinfo(true);
        expected.setVcsbranch("release");
        expected.setValidsince(123456l);
        expected.setValiduntil(1234567l);

        ProjectAttributes attributes = dto.createDto(ProjectAttributes.class).withPtype("ptype").withPname("pname");

        Variable variable = dto.createDto(Variable.class);
        Replacement replacement = dto.createDto(Replacement.class);
        replacement.setFind("find1");
        replacement.setReplace("replace1");
        replacement.setReplacemode("mode1");
        variable.setFiles(Arrays.asList("file1.java, file2.java"));
        variable.setEntries(Arrays.asList(replacement));

        expected.setProjectattributes(attributes);
        expected.setVariables(Arrays.asList(variable));

        StringBuilder sb = new StringBuilder("?");
        sb.append("v=").append(expected.getV()).append("&");
        sb.append("vcs=").append(expected.getVcs()).append("&");
        sb.append("vcsurl=").append(expected.getVcsurl()).append("&");
        sb.append("commitid=").append(expected.getCommitid()).append("&");
        sb.append("projectattributes.ptype=").append(expected.getProjectattributes().getPtype()).append("&");
        sb.append("projectattributes.pname=").append(expected.getProjectattributes().getPname()).append("&");
        sb.append("action=").append(expected.getAction()).append("&");
        sb.append("contactmail=").append(expected.getContactmail()).append("&");
        sb.append("author=").append(expected.getAuthor()).append("&");
        sb.append("openfile=").append(expected.getOpenfile()).append("&");
        sb.append("orgid=").append(expected.getOrgid()).append("&");
        sb.append("affiliateid=").append(expected.getAffiliateid()).append("&");
        sb.append("vcsinfo=").append(expected.getVcsinfo()).append("&");
        sb.append("vcsbranch=").append(expected.getVcsbranch()).append("&");
        if (expected.getValidsince() != null) {
            sb.append("validsince=").append(expected.getValidsince()).append("&");
        }
        if (expected.getValiduntil() != null) {
            sb.append("validuntil=").append(expected.getValiduntil()).append("&");
        }
        sb.append("variables=").append(encode("[" + dto.toJson(variable) + "]"));

        Factory newFactory = factoryBuilder.buildEncoded(new URI(sb.toString()));
        assertEquals(newFactory, expected);
    }

    @Test
    public void shouldBeAbleToParseAndValidateNonEncodedFactory1_2()
            throws ApiException, UnsupportedEncodingException, URISyntaxException {

        expected.setV("1.2");
        expected.setVcs("git");
        expected.setVcsurl("https://github.com/codenvy/commons.git");
        expected.setCommitid("7896464674879");
        expected.setAction("openReadme");
        expected.setContactmail("developer@codenvy.com");
        expected.setAuthor("codenvy");
        expected.setOpenfile("/src/test.java");
        expected.setOrgid("orgid");
        expected.setAffiliateid("affiliateid");
        expected.setVcsinfo(true);
        expected.setVcsbranch("release");

        Restriction restriction = dto.createDto(Restriction.class).withMaxsessioncount(3l).withPassword("password2323")
                                     .withValiduntil(5679841595l).withValidsince(1654879849l)
                                     .withRefererhostname("stackoverflow.com");

        Git git =
                dto.createDto(Git.class).withConfigbranchmerge("refs/for/master").withConfigpushdefault("upstream")
                   .withConfigremoteoriginfetch("changes/41/1841/1");

        ProjectAttributes attributes =
                dto.createDto(ProjectAttributes.class).withPtype("ptype").withPname("pname")
                   .withRunnername("runnername").withRunnerenvironmentid("runnerenvironmentid")
                   .withBuildername("buildername");

        Variable variable = dto.createDto(Variable.class);
        Replacement replacement = dto.createDto(Replacement.class);
        replacement.setFind("find1");
        replacement.setReplace("replace1");
        replacement.setReplacemode("mode1");
        variable.setFiles(Arrays.asList("file1.java, file2.java"));
        variable.setEntries(Arrays.asList(replacement));

        expected.setRestriction(restriction);
        expected.setGit(git);
        expected.setProjectattributes(attributes);
        expected.setVariables(Arrays.asList(variable));

        StringBuilder sb = new StringBuilder("?");
        sb.append("v=").append(expected.getV()).append("&");
        sb.append("vcs=").append(expected.getVcs()).append("&");
        sb.append("vcsurl=").append(expected.getVcsurl()).append("&");
        sb.append("commitid=").append(expected.getCommitid()).append("&");
        sb.append("projectattributes.ptype=").append(expected.getProjectattributes().getPtype()).append("&");
        sb.append("projectattributes.pname=").append(expected.getProjectattributes().getPname()).append("&");
        sb.append("projectattributes.runnername=").append(expected.getProjectattributes().getRunnername()).append("&");
        sb.append("projectattributes.buildername=").append(expected.getProjectattributes().getBuildername()).append("&");
        sb.append("projectattributes.runnerenvironmentid=").append(expected.getProjectattributes().getRunnerenvironmentid()).append("&");
        sb.append("action=").append(expected.getAction()).append("&");
        sb.append("contactmail=").append(expected.getContactmail()).append("&");
        sb.append("author=").append(expected.getAuthor()).append("&");
        sb.append("openfile=").append(expected.getOpenfile()).append("&");
        sb.append("orgid=").append(expected.getOrgid()).append("&");
        sb.append("affiliateid=").append(expected.getAffiliateid()).append("&");
        sb.append("vcsinfo=").append(expected.getVcsinfo()).append("&");
        sb.append("vcsbranch=").append(expected.getVcsbranch()).append("&");
        sb.append("restriction.refererhostname=").append(expected.getRestriction().getRefererhostname()).append("&");
        sb.append("restriction.validsince=").append(expected.getRestriction().getValidsince()).append("&");
        sb.append("restriction.validuntil=").append(expected.getRestriction().getValiduntil()).append("&");
        sb.append("restriction.password=").append(expected.getRestriction().getPassword()).append("&");
        sb.append("restriction.maxsessioncount=").append(expected.getRestriction().getMaxsessioncount()).append("&");
        sb.append("git.configremoteoriginfetch=").append(expected.getGit().getConfigremoteoriginfetch()).append("&");
        sb.append("git.configbranchmerge=").append(expected.getGit().getConfigbranchmerge()).append("&");
        sb.append("git.configpushdefault=").append(expected.getGit().getConfigpushdefault()).append("&");
        sb.append("variables=").append(encode("[" + dto.toJson(variable) + "]"));

        Factory newFactory = factoryBuilder.buildEncoded(new URI(sb.toString()));
        assertEquals(newFactory, expected);
    }

    @Test
    public void shouldBeAbleToParseAndValidateNonEncodedV2_0() throws Exception {
        Factory expected = dto.createDto(Factory.class);
        expected.withV("2.0")
              .withSource(dto.createDto(Source.class)
                             .withProject(dto.createDto(ImportSourceDescriptor.class)
                                             .withType("git")
                                             .withLocation("location")
                                             .withParameters(new HashMap<String, String>()     {
                                                 {
                                                     put("keepVcs", "true");
                                                     put("branch", "master");
                                                     put("commitId", "123");
                                                 }
                                             }))
                             .withRunners(singletonMap("runEnv", dto.createDto(RunnerSource.class)
                                                                    .withLocation("location")
                                                                    .withParameters(singletonMap("key", "value")))))
              .withProject(dto.createDto(NewProject.class)
                              .withType("type")
                              .withAttributes(singletonMap("key", singletonList("value")))
                              .withBuilders(dto.createDto(BuildersDescriptor.class).withDefault("default"))
                              .withDescription("description")
                              .withName("name")
                              .withRunners(dto.createDto(RunnersDescriptor.class)
                                              .withDefault("default")
                                              .withConfigs(singletonMap("key", dto.createDto(RunnerConfiguration.class)
                                                                                  .withRam(768)
                                                                                  .withOptions(new HashMap<String, String>() {
                                                                                      {
                                                                                          put("key1", "value1");
                                                                                          put("key2", "value2");
                                                                                      }
                                                                                  })
                                                                                  .withVariables(new HashMap<String, String>() {
                                                                                      {
                                                                                          put("key1", "value1");
                                                                                          put("key2", "value2");
                                                                                      }
                                                                                  }))))
                              .withVisibility("private"))
              .withCreator(dto.createDto(Author.class)
                              .withAccountId("accountId")
                              .withEmail("email")
                              .withName("name"))
              .withPolicies(dto.createDto(Policies.class)
                               .withRefererHostname("referrer")
                               .withValidSince(123l)
                               .withValidUntil(123l))
              .withActions(dto.createDto(Actions.class)
                              .withFindReplace(singletonList(dto.createDto(Variable.class)
                                                                .withFiles(singletonList("file"))
                                                                .withEntries(singletonList(dto.createDto(Replacement.class)
                                                                                              .withFind("find")
                                                                                              .withReplace("replace")
                                                                                              .withReplacemode("mode")))))
                              .withOpenFile("openFile")
                              .withWarnOnClose(true))
              .withWorkspace(dto.createDto(Workspace.class)
                                .withTemp(true)
                                .withAttributes(new HashMap<String, String>() {
                                    {
                                        put("key1", "value1");
                                        put("key2", "value2");
                                    }
                                }));

        StringBuilder sb = new StringBuilder("?");
        sb.append("v=2.0").append("&");
        sb.append("actions.openFile=openFile").append("&");
        sb.append("actions.warnOnClose=true").append("&");
        sb.append("actions.findReplace=").append(
                URLEncoder.encode(DtoFactory.getInstance().toJson(expected.getActions().getFindReplace()), "UTF-8")).append("&");
        sb.append("policies.refererHostname=referrer").append("&");
        sb.append("policies.validSince=123").append("&");
        sb.append("policies.validUntil=123").append("&");
        sb.append("creator.accountId=accountId").append("&");
        sb.append("creator.email=email").append("&");
        sb.append("creator.name=name").append("&");
        sb.append("workspace.temp=true").append("&");
        sb.append("workspace.attributes.key1=value1").append("&");
        sb.append("workspace.attributes.key2=value2").append("&");
        sb.append("source.project.type=git").append("&");
        sb.append("source.project.location=location").append("&");
        sb.append("source.project.parameters.keepVcs=true").append("&");
        sb.append("source.project.parameters.commitId=123").append("&");
        sb.append("source.project.parameters.branch=master").append("&");
        sb.append("source.runners.runEnv.location=location").append("&");
        sb.append("source.runners.runEnv.parameters.key=value").append("&");
        sb.append("project.type=type").append("&");
        sb.append("project.name=name").append("&");
        sb.append("project.description=description").append("&");
        sb.append("project.attributes.key=value").append("&");
        sb.append("project.visibility=private").append("&");
        sb.append("project.builders.default=default").append("&");
        sb.append("project.runners.default=default").append("&");
        sb.append("project.runners.configs.key.ram=768").append("&");
        sb.append("project.runners.configs.key.options.key1=value1").append("&");
        sb.append("project.runners.configs.key.options.key2=value2").append("&");
        sb.append("project.runners.configs.key.variables.key1=value1").append("&");
        sb.append("project.runners.configs.key.variables.key2=value2").append("&");

        Factory newFactory = factoryBuilder.buildEncoded(new URI(sb.toString()));
        assertEquals(newFactory, expected);
    }

    @Test(enabled = false)
    public void speedTest() throws ApiException {
        actual.withV("1.0").withVcs("vcs").withVcsurl("vcsurl").withIdcommit("idcommit").withPtype("ptype").withPname("pname")
              .withAction("action").withWname("wname").withVcsinfo(true);

        long start = System.currentTimeMillis();
        for (int i = 0; i < 1000; ++i) {
            factoryBuilder.checkValid(actual, NONENCODED);
        }
        System.err.println((System.currentTimeMillis() - start));
    }

}
