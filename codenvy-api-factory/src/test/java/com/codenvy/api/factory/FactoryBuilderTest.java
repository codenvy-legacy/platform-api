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
import com.codenvy.api.factory.dto.Factory;
import com.codenvy.api.factory.dto.FactoryV1_1;
import com.codenvy.api.factory.dto.Git;
import com.codenvy.api.factory.dto.ProjectAttributes;
import com.codenvy.api.factory.dto.Replacement;
import com.codenvy.api.factory.dto.Restriction;
import com.codenvy.api.factory.dto.Variable;
import com.codenvy.api.factory.dto.WelcomeConfiguration;
import com.codenvy.api.factory.dto.WelcomePage;
import com.codenvy.dto.server.DtoFactory;

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
import java.util.List;

import static com.codenvy.api.core.factory.FactoryParameter.FactoryFormat;
import static com.codenvy.api.core.factory.FactoryParameter.FactoryFormat.ENCODED;
import static com.codenvy.api.core.factory.FactoryParameter.FactoryFormat.NONENCODED;
import static org.testng.Assert.assertEquals;

/** @author Sergii Kabashniuk */
@Listeners(MockitoTestNGListener.class)
public class FactoryBuilderTest {

    private FactoryBuilder factoryBuilder;

    private Factory actual;

    private Factory expected;

    @BeforeMethod
    public void setUp() throws Exception {
        factoryBuilder = new FactoryBuilder(new SourceParametersValidator());
        actual = DtoFactory.getInstance().createDto(Factory.class);

        expected = DtoFactory.getInstance().createDto(Factory.class);
    }

    @Test(dataProvider = "jsonprovider")
    public void shouldBeAbleToParserJsonV1_1(String json) {

        Factory factory = DtoFactory.getInstance().createDtoFromJson(json, Factory.class);
        //System.out.println(FactoryBuilder.buildNonEncoded(factory));
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

//    @Test
    public void shouldBeAbleToValidateFactory1_0() throws ApiException {
        actual.withV("1.0").withVcs("vcs").withVcsurl("vcsurl").withIdcommit("idcommit").withPtype("ptype").withPname("pname")
              .withAction("action").withWname("wname").withVcsinfo(true).withOpenfile("openfile");

        factoryBuilder.checkValid(actual, NONENCODED);
    }

//    @Test
    public void shouldBeAbleToValidateEncodedFactory1_1() throws ApiException {
        ((FactoryV1_1)actual.withV("1.1").withVcs("vcs").withVcsurl("vcsurl").withCommitid("commitid").withVcsinfo(
                true).withOpenfile("openfile").withAction("action")).withStyle("style").withDescription("description").withContactmail(
                "contactmail").withAuthor("author").withOrgid("orgid").withAffiliateid("affid").withVcsbranch("branch")
                                                                    .withValidsince(123456789).withValiduntil(234567899);

        actual.setProjectattributes(DtoFactory.getInstance().createDto(ProjectAttributes.class).withPname("pname").withPtype("ptype"));

        Replacement replacement = DtoFactory.getInstance().createDto(Replacement.class).withReplacemode(
                "replacemod").withReplace("replace").withFind("find");

        actual.setVariables(Arrays.asList(DtoFactory.getInstance().createDto(Variable.class).withEntries(Arrays.asList(replacement))
                                                    .withFiles(Arrays.asList("file1"))));

        WelcomeConfiguration wc = DtoFactory.getInstance().createDto(WelcomeConfiguration.class);
        actual.setWelcome(DtoFactory.getInstance().createDto(WelcomePage.class).withAuthenticated(wc).withNonauthenticated(wc));

        factoryBuilder.checkValid(actual, ENCODED);
    }

//    @Test
    public void shouldBeAbleToValidateNonEncodedFactory1_1() throws ApiException {
        ((FactoryV1_1)actual.withV("1.1").withVcs("vcs").withVcsurl("vcsurl").withCommitid("commitid").withAction("action").withVcsinfo(
                true).withOpenfile("openfile")).withContactmail("contactmail").withAuthor("author").withOrgid("orgid")
                                               .withAffiliateid("affid").withVcsbranch(
                "branch").withValidsince(123456789).withValiduntil(234567899);

        actual.setProjectattributes(DtoFactory.getInstance().createDto(ProjectAttributes.class).withPname("pname").withPtype("ptype"));

        Replacement replacement = DtoFactory.getInstance().createDto(Replacement.class).withReplacemode(
                "replacemod").withReplace("replace").withFind("find");

        actual.setVariables(Arrays.asList(DtoFactory.getInstance().createDto(Variable.class).withEntries(Arrays.asList(replacement))
                                                    .withFiles(Arrays.asList("file1"))));

        factoryBuilder.checkValid(actual, NONENCODED);
    }

//    @Test
    public void shouldBeAbleToValidateEncodedFactory1_2() throws ApiException {
        ((FactoryV1_1)actual.withV("1.2").withVcs("vcs").withVcsinfo(true).withOpenfile("openfile").withVcsurl("vcsurl")
                            .withCommitid("commitid").withAction(
                        "action")).withStyle("style").withDescription("description").withContactmail("contactmail").withAuthor("author")
                                  .withOrgid("orgid").withAffiliateid("affid").withVcsbranch("branch");

        actual.setProjectattributes(DtoFactory.getInstance().createDto(ProjectAttributes.class).withPname("pname").withPtype("ptype"));

        Replacement replacement = DtoFactory.getInstance().createDto(Replacement.class).withReplacemode(
                "replacemod").withReplace("replace").withFind("find");

        actual.setVariables(Arrays.asList(DtoFactory.getInstance().createDto(Variable.class).withEntries(Arrays.asList(replacement))
                                                    .withFiles(Arrays.asList("file1"))));

        WelcomeConfiguration wc = DtoFactory.getInstance().createDto(WelcomeConfiguration.class);
        actual.setWelcome(DtoFactory.getInstance().createDto(WelcomePage.class).withAuthenticated(wc).withNonauthenticated(wc));

        actual.withGit(DtoFactory.getInstance().createDto(Git.class).withConfigbranchmerge(
                "configbranchmerge").withConfigpushdefault("configpushdefault").withConfigremoteoriginfetch(
                "configremoteoriginfetch"));

        actual.withRestriction(
                DtoFactory.getInstance().createDto(Restriction.class).withPassword("password").withRefererhostname("codenvy-dev.com")
                          .withValiduntil(123456789).withValidsince(12345678).withMaxsessioncount(123).withRestrictbypassword(true)
                              );

        factoryBuilder.checkValid(actual, ENCODED);
    }

//    @Test
    public void shouldBeAbleToValidateNonEncodedFactory1_2() throws ApiException {
        ((FactoryV1_1)actual.withV("1.2").withVcs("vcs").withVcsurl("vcsurl").withVcsinfo(true).withCommitid("commitid").withOpenfile(
                "openfile").withAction("action")).withContactmail("contactmail").withAuthor(
                "author").withOrgid("orgid").withAffiliateid("affid").withVcsbranch("branch");

        actual.setProjectattributes(DtoFactory.getInstance().createDto(ProjectAttributes.class).withPname("pname").withPtype("ptype"));

        Replacement replacement = DtoFactory.getInstance().createDto(Replacement.class).withReplacemode(
                "replacemod").withReplace("replace").withFind("find");

        actual.setVariables(Arrays.asList(DtoFactory.getInstance().createDto(Variable.class).withEntries(Arrays.asList(replacement))
                                                    .withFiles(Arrays.asList("file1"))));

        actual.withGit(DtoFactory.getInstance().createDto(Git.class).withConfigbranchmerge(
                "configbranchmerge").withConfigpushdefault("configpushdefault").withConfigremoteoriginfetch(
                "configremoteoriginfetch"));

        actual.withRestriction(
                DtoFactory.getInstance().createDto(Restriction.class).withPassword("password").withRefererhostname("codenvy-dev.com")
                          .withValiduntil(123456789).withValidsince(12345678).withMaxsessioncount(123).withRestrictbypassword(true)
                              );


        factoryBuilder.checkValid(actual, NONENCODED);
    }

//    @Test(expectedExceptions = ApiException.class, dataProvider = "TFParamsProvider")
    public void shouldNotAllowUsingParamsForTrackedFactoriesIfOrgidDoesntSet(String version, Object arg, String methodName, Class argClass)
            throws InvocationTargetException, IllegalAccessException, ApiException, NoSuchMethodException {
        actual.withV(version).withVcs("vcs").withVcsurl("vcsurl");

        Factory.class.getMethod(methodName, argClass).invoke(actual, arg);

        factoryBuilder.checkValid(actual, ENCODED);
    }

//    @Test(expectedExceptions = ApiException.class)
    public void shouldNotAllowInNonencodedVersionUsingParamsOnlyForEncodedVersion() throws ApiException, URISyntaxException {
        StringBuilder sb = new StringBuilder("?");
        sb.append("v=").append("1.0").append("&");
        sb.append("vcs=").append("git").append("&");
        sb.append("welcome=").append("welcome").append("&");

        factoryBuilder.buildNonEncoded(new URI(sb.toString()));
    }

//    @Test(expectedExceptions = ApiException.class)
    public void shouldNotValidateUnparseableFactory() throws ApiException, URISyntaxException {
        factoryBuilder.checkValid(null, NONENCODED);
    }

    @DataProvider(name = "TFParamsProvider")
    public static Object[][] tFParamsProvider() throws URISyntaxException, IOException, NoSuchMethodException {
        return new Object[][]{
                {"1.1", DtoFactory.getInstance().createDto(WelcomePage.class), "setWelcome", WelcomePage.class},
                {"1.2", DtoFactory.getInstance().createDto(WelcomePage.class), "setWelcome", WelcomePage.class},
                {"1.2", DtoFactory.getInstance().createDto(Restriction.class).withPassword("pass"), "setRestriction", Restriction.class},
                {"1.2", DtoFactory.getInstance().createDto(Restriction.class).withRefererhostname(
                        "codenvy.com"), "setRestriction", Restriction.class},
                {"1.2", DtoFactory.getInstance().createDto(Restriction.class).withRestrictbypassword(true), "setRestriction",
                 Restriction.class},
                {"1.2", DtoFactory.getInstance().createDto(Restriction.class).withMaxsessioncount(123), "setRestriction",
                 Restriction.class},
                {"1.2", DtoFactory.getInstance().createDto(Restriction.class).withValidsince(123456789), "setRestriction",
                 Restriction.class},
                {"1.2", DtoFactory.getInstance().createDto(Restriction.class).withValiduntil(1234567989), "setRestriction",
                 Restriction.class},
        };
    }

//    @Test(expectedExceptions = ApiException.class, dataProvider = "setByServerParamsProvider")
    public void shouldNotAllowUsingParamsThatCanBeSetOnlyByServer(String version, String methodName, Object arg, Class argClass)
            throws InvocationTargetException, IllegalAccessException, ApiException, NoSuchMethodException {
        actual.withV(version).withVcs("vcs").withVcsurl("vcsurl");

        Factory.class.getMethod(methodName, argClass).invoke(actual, arg);

        factoryBuilder.checkValid(actual, ENCODED);
    }

    @DataProvider(name = "setByServerParamsProvider")
    public static Object[][] setByServerParamsProvider() throws URISyntaxException, IOException, NoSuchMethodException {
        return new Object[][]{
                {"1.1", "setId", "id", String.class},
                {"1.1", "setUserid", "userid", String.class},
                {"1.1", "setCreated", 123465798, long.class},
                {"1.2", "setRestriction", DtoFactory.getInstance().createDto(Restriction.class).withRestrictbypassword(true),
                 Restriction.class}
        };
    }

//    @Test
    public void shouldBeAbleToConvertToLatest() throws ApiException {
        actual.withIdcommit("idcommit").withPname("pname").withPtype("ptype").withWname("wname");

        expected.withProjectattributes(
                DtoFactory.getInstance().createDto(ProjectAttributes.class).withPname(actual.getPname()).withPtype(actual.getPtype()))
                .withCommitid(actual.getIdcommit()).withV("1.2");

        assertEquals(factoryBuilder.convertToLatest(actual), expected);
    }

//    @Test(expectedExceptions = ApiException.class, dataProvider = "notValidParamsProvider")
    public <T> void shouldNotAllowUsingNotValidParams(String version, String methodName, T arg, Class<T> argClass, FactoryFormat encoded)
            throws InvocationTargetException, IllegalAccessException, ApiException, NoSuchMethodException {
        actual.withV(version).withVcs("vcs").withVcsurl("vcsurl");

        Factory.class.getMethod(methodName, argClass).invoke(actual, arg);

        factoryBuilder.checkValid(actual, encoded);
    }

    @DataProvider(name = "notValidParamsProvider")
    public static Object[][] notValidParamsProvider() throws URISyntaxException, IOException, NoSuchMethodException {
        return new Object[][]{
                {"1.1", "setWname", "smth", String.class, ENCODED},
                {"1.2", "setWname", "smth", String.class, ENCODED},
                {"1.1", "setIdcommit", "smth", String.class, ENCODED},
                {"1.1", "setPname", "smth", String.class, ENCODED},
                {"1.1", "setPtype", "smth", String.class, ENCODED},
                {"1.2", "setIdcommit", "smth", String.class, ENCODED},
                {"1.2", "setPname", "smth", String.class, ENCODED},
                {"1.2", "setPtype", "smth", String.class, ENCODED},
                {"1.1", "setStyle", "smth", String.class, NONENCODED},
                {"1.2", "setStyle", "smth", String.class, NONENCODED},
                {"1.1", "setDescription", "smth", String.class, NONENCODED},
                {"1.2", "setDescription", "smth", String.class, NONENCODED},
                {"1.0", "setProjectattributes", DtoFactory.getInstance().createDto(ProjectAttributes.class), ProjectAttributes.class,
                 ENCODED},
                {"1.0", "setStyle", "smth", String.class, ENCODED},
                {"1.0", "setDescription", "smth", String.class, ENCODED},
                {"1.0", "setContactmail", "smth", String.class, ENCODED},
                {"1.0", "setAuthor", "smth", String.class, ENCODED},
                {"1.0", "setOrgid", "smth", String.class, ENCODED},
                {"1.0", "setAffiliateid", "smth", String.class, ENCODED},
                {"1.0", "setVcsbranch", "smth", String.class, ENCODED},
                {"1.0", "setValidsince", 132, long.class, ENCODED},
                {"1.0", "setValiduntil", 123, long.class, ENCODED},
                {"1.2", "setValidsince", 132, long.class, ENCODED},
                {"1.2", "setValiduntil", 123, long.class, ENCODED},
                {"1.0", "setVariables", Arrays.asList(DtoFactory.getInstance().createDto(Variable.class)), List.class, ENCODED},
                {"1.0", "setWelcome", DtoFactory.getInstance().createDto(WelcomePage.class), WelcomePage.class, ENCODED},
                {"1.1", "setWelcome", DtoFactory.getInstance().createDto(WelcomePage.class), WelcomePage.class, NONENCODED},
                {"1.0", "setImage", "smth", String.class, ENCODED},
                {"1.0", "setRestriction", DtoFactory.getInstance().createDto(Restriction.class), Restriction.class, ENCODED},
                {"1.1", "setRestriction", DtoFactory.getInstance().createDto(Restriction.class), Restriction.class, ENCODED},
                {"1.0", "setGit", DtoFactory.getInstance().createDto(Git.class), Git.class, ENCODED},
                {"1.1", "setGit", DtoFactory.getInstance().createDto(Git.class), Git.class, ENCODED}
        };
    }

    private String encode(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return value;
        }
    }

//    @Test
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

        Factory newFactory = factoryBuilder.buildNonEncoded(new URI(sb.toString()));
        assertEquals(newFactory, expected);
    }

//    @Test
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

        Factory newFactory = factoryBuilder.buildNonEncoded(new URI(sb.toString()));
        assertEquals(newFactory, expected);
    }

//    @Test
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
        expected.setValidsince(123456);
        expected.setValiduntil(1234567);

        ProjectAttributes attributes = DtoFactory.getInstance().createDto(ProjectAttributes.class).withPtype("ptype").withPname("pname");

        Variable variable = DtoFactory.getInstance().createDto(Variable.class);
        Replacement replacement = DtoFactory.getInstance().createDto(Replacement.class);
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
        sb.append("validsince=").append(expected.getValidsince()).append("&");
        sb.append("validuntil=").append(expected.getValiduntil()).append("&");
        sb.append("variables=").append(encode("[" + DtoFactory.getInstance().toJson(variable) + "]"));

        Factory newFactory = factoryBuilder.buildNonEncoded(new URI(sb.toString()));
        assertEquals(newFactory, expected);
    }

//    @Test
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

        Restriction restriction = DtoFactory.getInstance().createDto(Restriction.class).withMaxsessioncount(3).withPassword("password2323")
                                            .withValiduntil(5679841595l).withValidsince(1654879849)
                                            .withRefererhostname("stackoverflow.com");

        Git git =
                DtoFactory.getInstance().createDto(Git.class).withConfigbranchmerge("refs/for/master").withConfigpushdefault("upstream")
                          .withConfigremoteoriginfetch("changes/41/1841/1");

        ProjectAttributes attributes =
                DtoFactory.getInstance().createDto(ProjectAttributes.class).withPtype("ptype").withPname("pname")
                          .withRunnername("runnername").withRunnerenvironmentid("runnerenvironmentid")
                          .withBuildername("buildername");

        Variable variable = DtoFactory.getInstance().createDto(Variable.class);
        Replacement replacement = DtoFactory.getInstance().createDto(Replacement.class);
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
        sb.append("variables=").append(encode("[" + DtoFactory.getInstance().toJson(variable) + "]"));

        Factory newFactory = factoryBuilder.buildNonEncoded(new URI(sb.toString()));
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
