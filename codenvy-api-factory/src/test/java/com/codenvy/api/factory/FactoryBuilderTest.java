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
package com.codenvy.api.factory;

import com.codenvy.api.factory.dto.Factory;
import com.codenvy.api.factory.dto.*;
import com.codenvy.dto.server.DtoFactory;

import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.*;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

import static com.codenvy.api.factory.FactoryFormat.ENCODED;
import static com.codenvy.api.factory.FactoryFormat.NONENCODED;
import static org.testng.Assert.assertEquals;

/**
 * @author Sergii Kabashniuk
 */
@Listeners(MockitoTestNGListener.class)
public class FactoryBuilderTest {

    private FactoryBuilder factoryBuilder;

    private Factory actual;

    private Factory expected;

    @BeforeMethod
    public void setUp() throws Exception {
        factoryBuilder = new FactoryBuilder();
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

    @Test
    public void shouldBeAbleToValidateFactory1_0() throws FactoryUrlException {
        actual.withV("1.0").withVcs("vcs").withVcsurl("vcsurl").withIdcommit("idcommit").withPtype("ptype").withPname("pname")
              .withAction("action").withWname("wname");

        factoryBuilder.checkValid(actual, FactoryFormat.NONENCODED);
    }

    @Test
    public void shouldBeAbleToValidateEncodedFactory1_1() throws FactoryUrlException {
        ((FactoryV1_1)actual.withV("1.1").withVcs("vcs").withVcsurl("vcsurl").withCommitid("commitid").withAction("action")).withStyle(
                "style").withDescription("description").withContactmail("contactmail").withAuthor("author").withOpenfile(
                "openfile").withOrgid("orgid").withAffiliateid("affid").withVcsinfo(true).withVcsbranch("branch").withValidsince(
                123456789).withValiduntil(234567899);

        actual.setProjectattributes(DtoFactory.getInstance().createDto(ProjectAttributes.class).withPname("pname").withPtype("ptype"));

        Replacement replacement = DtoFactory.getInstance().createDto(Replacement.class).withReplacemode(
                "replacemod").withReplace("replace").withFind("find");

        actual.setVariables(Arrays.asList(DtoFactory.getInstance().createDto(Variable.class).withEntries(Arrays.asList(replacement))
                                                    .withFiles(Arrays.asList("file1"))));

        WelcomeConfiguration wc = DtoFactory.getInstance().createDto(WelcomeConfiguration.class);
        actual.setWelcome(DtoFactory.getInstance().createDto(WelcomePage.class).withAuthenticated(wc).withNonauthenticated(wc));

        factoryBuilder.checkValid(actual, ENCODED);
    }

    @Test
    public void shouldBeAbleToValidateNonEncodedFactory1_1() throws FactoryUrlException {
        ((FactoryV1_1)actual.withV("1.1").withVcs("vcs").withVcsurl("vcsurl").withCommitid("commitid").withAction("action"))
                .withContactmail(
                        "contactmail").withAuthor("author").withOpenfile("openfile").withOrgid("orgid").withAffiliateid("affid")
                .withVcsinfo(true).withVcsbranch("branch").withValidsince(
                123456789).withValiduntil(234567899);

        actual.setProjectattributes(DtoFactory.getInstance().createDto(ProjectAttributes.class).withPname("pname").withPtype("ptype"));

        Replacement replacement = DtoFactory.getInstance().createDto(Replacement.class).withReplacemode(
                "replacemod").withReplace("replace").withFind("find");

        actual.setVariables(Arrays.asList(DtoFactory.getInstance().createDto(Variable.class).withEntries(Arrays.asList(replacement))
                                                    .withFiles(Arrays.asList("file1"))));

        factoryBuilder.checkValid(actual, NONENCODED);
    }

    @Test
    public void shouldBeAbleToValidateEncodedFactory1_2() throws FactoryUrlException {
        ((FactoryV1_1)actual.withV("1.2").withVcs("vcs").withVcsurl("vcsurl").withCommitid("commitid").withAction("action")).withStyle(
                "style").withDescription("description").withContactmail("contactmail").withAuthor("author").withOpenfile(
                "openfile").withOrgid("orgid").withAffiliateid("affid").withVcsinfo(true).withVcsbranch("branch");

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
                          .withValiduntil(123456789).withValidsince(12345678).withValidsessioncount(123).withRestrictbypassword(true));

        factoryBuilder.checkValid(actual, ENCODED);
    }

    @Test
    public void shouldBeAbleToValidateNonEncodedFactory1_2() throws FactoryUrlException {
        ((FactoryV1_1)actual.withV("1.2").withVcs("vcs").withVcsurl("vcsurl").withCommitid("commitid").withAction("action"))
                .withContactmail(
                        "contactmail").withAuthor("author").withOpenfile(
                "openfile").withOrgid("orgid").withAffiliateid("affid").withVcsinfo(true).withVcsbranch("branch");

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
                          .withValiduntil(123456789).withValidsince(12345678).withValidsessioncount(123).withRestrictbypassword(true));


        factoryBuilder.checkValid(actual, NONENCODED);
    }

    @Test(expectedExceptions = FactoryUrlException.class, dataProvider = "TFParamsProvider")
    public void shouldNotAllowUsingParamsForTrackedFactoriesIfOrgidDoesntSet(String version, Object arg, String methodName, Class argClass)
            throws InvocationTargetException, IllegalAccessException, FactoryUrlException, NoSuchMethodException {
        actual.withV(version).withVcs("vcs").withVcsurl("vcsurl");

        Factory.class.getMethod(methodName, argClass).invoke(actual, arg);

        factoryBuilder.checkValid(actual, ENCODED);
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
                {"1.2", DtoFactory.getInstance().createDto(Restriction.class).withValidsessioncount(123), "setRestriction",
                 Restriction.class},
                {"1.2", DtoFactory.getInstance().createDto(Restriction.class).withValidsince(123456789), "setRestriction",
                 Restriction.class},
                {"1.2", DtoFactory.getInstance().createDto(Restriction.class).withValiduntil(1234567989), "setRestriction",
                 Restriction.class},
        };
    }

    @Test(expectedExceptions = FactoryUrlException.class, dataProvider = "setByServerParamsProvider")
    public void shouldNotAllowUsingParamsThatCanBeSetOnlyByServer(String version, String methodName, Object arg, Class argClass)
            throws InvocationTargetException, IllegalAccessException, FactoryUrlException, NoSuchMethodException {
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

    @Test
    public void shouldBeAbleToConvertToLatest() throws FactoryUrlException {
        ((FactoryV1_1)actual.withIdcommit("idcommit").withPname("pname").withPtype("ptype").withWname("wname")).withValiduntil(123456)
                                                                                                               .withValidsince(123456789);

        expected.withRestriction(DtoFactory.getInstance().createDto(Restriction.class).withValidsince(actual.getValidsince())
                                           .withValiduntil(actual.getValiduntil())).withProjectattributes(
                DtoFactory.getInstance().createDto(ProjectAttributes.class).withPname(actual.getPname()).withPtype(actual.getPtype()))
                .withCommitid(actual.getIdcommit()).withV("1.2");

        assertEquals(factoryBuilder.convertToLatest(actual), expected);
    }

    @Test(expectedExceptions = FactoryUrlException.class, dataProvider = "notValidParamsProvider")
    public <T> void shouldNotAllowUsingNotValidParams(String version, String methodName, T arg, Class<T> argClass, FactoryFormat encoded)
            throws InvocationTargetException, IllegalAccessException, FactoryUrlException, NoSuchMethodException {
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
                {"1.0", "setOpenfile", "smth", String.class, ENCODED},
                {"1.0", "setOrgid", "smth", String.class, ENCODED},
                {"1.0", "setAffiliateid", "smth", String.class, ENCODED},
                {"1.0", "setVcsinfo", true, boolean.class, ENCODED},
                {"1.0", "setVcsbranch", "smth", String.class, ENCODED},
                {"1.0", "setValidsince", 132, long.class, ENCODED},
                {"1.0", "setValiduntil", 123, long.class, ENCODED},
                {"1.2", "setValidsince", 132, long.class, ENCODED},
                {"1.2", "setValiduntil", 123, long.class, ENCODED},
                {"1.0", "setVariables", Arrays.asList(DtoFactory.getInstance().createDto(Variable.class)), List.class, ENCODED},
                {"1.0", "setWelcome", DtoFactory.getInstance().createDto(WelcomePage.class), WelcomePage.class, ENCODED},
                {"1.1", "setWelcome", DtoFactory.getInstance().createDto(WelcomePage.class), WelcomePage.class, NONENCODED},
                {"1.0", "setImage", "smth", String.class, ENCODED},
                {"1.1", "setImage", "smth", String.class, ENCODED},
                {"1.2", "setImage", "smth", String.class, ENCODED},
                {"1.0", "setRestriction", DtoFactory.getInstance().createDto(Restriction.class), Restriction.class, ENCODED},
                {"1.1", "setRestriction", DtoFactory.getInstance().createDto(Restriction.class), Restriction.class, ENCODED},
                {"1.0", "setGit", DtoFactory.getInstance().createDto(Git.class), Git.class, ENCODED},
                {"1.1", "setGit", DtoFactory.getInstance().createDto(Git.class), Git.class, ENCODED}
        };
    }

    @Test(enabled = false, expectedExceptions = FactoryUrlException.class)
    public void shouldThrowExceptionIfInitializedParameterIsUnsupportedInVersion1_0(Method method, Object parameter)
            throws FactoryUrlException, InvocationTargetException, IllegalAccessException {
        actual.withV("1.0").withVcs("vcs").withVcsurl("vcsurl").withIdcommit("idcommit").withPtype("ptype").withPname("pname")
              .withAction("action").withWname(
                "wname");

        method.invoke(actual, parameter);

        factoryBuilder.checkValid(actual, NONENCODED);
    }

    private String encode(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return value;
        }
    }

    @Test(enabled = false)
    public void shouldBeAbleToParseAndValidateNonEncodedFactory1_2() throws FactoryUrlException, UnsupportedEncodingException {
        StringBuilder sb = new StringBuilder();
        sb.append("v=").append("1.2").append("&");
        sb.append("vcs=").append("git").append("&");
        sb.append("vcsurl=").append(encode("https://github.com/codenvy/commons.git")).append("&");
        sb.append("commitid=").append("7896464674879").append("&");
        //sb.append("idcommit=").append("7896464674879").append("&");
        sb.append("projectattributes.ptype=").append("type").append("&");
        sb.append("projectattributes.pname=").append("name").append("&");
        //sb.append("ptype=").append("type").append("&");
        //sb.append("pname=").append("name").append("&");
        sb.append("action=").append("openReadme").append("&");
        sb.append("wname=").append("codenvy").append("&");
        //sb.append("style=").append("Black").append("&");
        //sb.append("description=").append("desc").append("&");
        sb.append("contactmail=").append(encode("developer@codenvy.com")).append("&");
        sb.append("author=").append("codenvy").append("&");
        sb.append("openfile=").append(encode("/src/test.java")).append("&");
        sb.append("orgid=").append("orgid").append("&");
        sb.append("affiliateid=").append("affiliateid").append("&");
        sb.append("vcsinfo=").append("true").append("&");
        //sb.append("vcsinfo=").append("false").append("&");
        sb.append("vcsbranch=").append("release").append("&");
        //sb.append("userid=").append("hudfsauidfais").append("&");
        //sb.append("created=").append("12145646").append("&");
        //sb.append("validsince=").append("1222222").append("&");
        //sb.append("validuntil=").append("1222223").append("&");
        //sb.append("welcome=").append("true").append("&");
        //sb.append("image=").append(encode("http://codenvy/icon.ico")).append("&");
        sb.append("restriction.refererhostname=").append("stackoverflow.com").append("&");
        sb.append("restriction.validsince=").append("1654879849").append("&");
        sb.append("restriction.validuntil=").append("5679841595").append("&");
        //sb.append("restriction.restrictbypassword=").append("true").append("&");
        sb.append("restriction.password=").append("password2323").append("&");
        sb.append("restriction.validsessioncount=").append("3").append("&");
        sb.append("git.configremoteoriginfetch=").append(encode("changes/41/1841/1")).append("&");
        sb.append("git.configbranchmerge=").append(encode("refs/for/master")).append("&");
        sb.append("git.configpushdefault=").append("upstream").append("&");

        Variable variable = DtoFactory.getInstance().createDto(Variable.class);
        Replacement replacement = DtoFactory.getInstance().createDto(Replacement.class);
        replacement.setFind("find1");
        replacement.setReplace("replace1");
        replacement.setReplacemode("mode1");
        variable.setFiles(Arrays.asList("file1.java, file2.java"));
        variable.setEntries(Arrays.asList(replacement));

        sb.append("variables=").append(encode("[" + DtoFactory.getInstance().toJson(variable) + "]")).append("");

        ((FactoryV1_1)expected.withV("1.2").withVcs("git").withVcsurl("https://github.com/codenvy/commons.git")
                              .withCommitid("7896464674879").withAction("openReadme")).withProjectattributes(
                DtoFactory.getInstance().createDto(ProjectAttributes.class).withPtype("ptype").withPname("pname"));

        Factory newFactory = factoryBuilder.buildNonEncoded(sb.toString());
        //assertEquals(newFactory, expectedFactory);
    }

    @Test(enabled = false)
    public void speedTest() throws FactoryUrlException {
        actual.withV("1.0").withVcs("vcs").withVcsurl("vcsurl").withIdcommit("idcommit").withPtype("ptype").withPname("pname")
              .withAction("action").withWname("wname");

        long start = System.currentTimeMillis();
        for (int i = 0; i < 1000; ++i) {
            factoryBuilder.checkValid(actual, NONENCODED);
        }
        System.err.println((System.currentTimeMillis() - start));
    }

}
