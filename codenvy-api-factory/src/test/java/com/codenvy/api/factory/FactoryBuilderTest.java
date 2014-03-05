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
import com.codenvy.api.factory.parameter.FactoryParameter;
import com.codenvy.api.factory.parameter.IgnoreConverter;
import com.codenvy.dto.server.DtoFactory;

import org.everrest.core.impl.uri.UriBuilderImpl;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.*;

import javax.ws.rs.core.UriBuilder;
import java.io.*;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.file.Files;

import static org.testng.Assert.assertEquals;

/**
 * @author Sergii Kabashniuk
 */
@Listeners(MockitoTestNGListener.class)
public class FactoryBuilderTest {
    @Spy
    private IgnoreConverter ignoreConverter;

    @InjectMocks
    private FactoryBuilder factoryBuilder;

    private Factory factory;

    private Factory expectedFactory;

    @BeforeMethod
    public void setUp() throws Exception {
        factory = DtoFactory.getInstance().createDto(Factory.class);

        expectedFactory = DtoFactory.getInstance().createDto(Factory.class);
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
        factory.setV("1.0");
        factory.setVcs("vcs");
        factory.setVcsurl("vcsurl");
        factory.setIdcommit("idcommit");
        factory.setPtype("ptype");
        factory.setPname("pname");
        factory.setAction("action");
        factory.setWname("wname");

        expectedFactory.setV("1.0");
        expectedFactory.setVcs("vcs");
        expectedFactory.setVcsurl("vcsurl");
        expectedFactory.setCommitid("idcommit");
        ProjectAttributes projectAttributes = DtoFactory.getInstance().createDto(ProjectAttributes.class);
        projectAttributes.setPname("pname");
        projectAttributes.setPtype("ptype");
        expectedFactory.setProjectattributes(projectAttributes);
        expectedFactory.setAction("action");

        Factory newFactory;
        //long start= System.currentTimeMillis();
        //for (int i = 0; i < 1000; ++i) {
            newFactory = factoryBuilder.validateFactoryCompatibility(factory, FactoryParameter.Format.NONENCODED);
        //}
        //System.err.println((System.currentTimeMillis() - start));

        assertEquals(newFactory, expectedFactory);

    }

    @Test
    public void shouldBeAbleToValidateNonEncodedFactory1_2() throws FactoryUrlException, UnsupportedEncodingException {
        StringBuilder sb = new StringBuilder();
        sb.append("v=").append("1.2").append("&");
        sb.append("vcs=").append("git").append("&");
        sb.append("vcsurl=").append(URLEncoder.encode("https://github.com/codenvy/commons.git", "UTF-8")).append("&");
        sb.append("commitid=").append("7896464674879").append("&");
        sb.append("projectattributes.ptype=").append("type").append("&");
        sb.append("projectattributes.pname=").append("name").append("&");
        sb.append("action=").append("openReadme").append("&");
        sb.append("wname=").append("codenvy");

        expectedFactory.setV("1.2");
        expectedFactory.setVcs("git");
        expectedFactory.setVcsurl("https://github.com/codenvy/commons.git");
        expectedFactory.setCommitid("7896464674879");
        ProjectAttributes projectAttributes = DtoFactory.getInstance().createDto(ProjectAttributes.class);
        projectAttributes.setPname("name");
        projectAttributes.setPtype("type");
        expectedFactory.setProjectattributes(projectAttributes);
        expectedFactory.setAction("openReadme");

        Factory newFactory = factoryBuilder.buildNonEncoded(sb.toString());
        assertEquals(newFactory, expectedFactory);
    }

    @Test
    public void test() {

        factory.setV("1.2");
        factory.setVcs("vcs");
        factory.setVcsurl("vcsurl");
        factory.setCommitid("commitid");
        factory.setIdcommit("idcommit");
        factory.setPtype("ptype");
        factory.setPname("pname");
        factory.setAction("action");
        factory.setWname("wname");
        factory.setStyle("style");
        factory.setDescription("description");
        factory.setContactmail("contactmail");
        factory.setAuthor("author");
        factory.setOpenfile("openfile");
        factory.setOrgid("orgid");
        factory.setAffiliateid("affid");
        factory.setVcsinfo(true);
        factory.setVcsbranch("vcsbranch");
        factory.setUserid("userid");
        factory.setCreated(100001);
        factory.setValiduntil(1000002);
        factory.setValidsince(100000);
        factory.setImage("image");

        factory.setProjectattributes(new ProjectAttributes() {
            @Override
            public String getPname() {
                return "attr.pname";
            }

            @Override
            public void setPname(String pname) {

            }

            @Override
            public String getPtype() {
                return "attr.ptype";
            }

            @Override
            public void setPtype(String ptype) {

            }
        });
        factory.setWelcome(new WelcomePage() {
            @Override
            public WelcomeConfiguration getAuthenticated() {
                return new WelcomeConfiguration() {
                    @Override
                    public String getTitle() {
                        return "welcome.auth.title";
                    }

                    @Override
                    public void setTitle(String title) {

                    }

                    @Override
                    public String getIconurl() {
                        return "welcome.auth.iconurl";
                    }

                    @Override
                    public void setIconurl(String iconurl) {

                    }

                    @Override
                    public String getContenturl() {
                        return "welcome.auth.contenturl";
                    }

                    @Override
                    public void setContenturl(String contenturl) {

                    }
                };
            }

            @Override
            public void setAuthenticated(WelcomeConfiguration authenticated) {

            }

            @Override
            public WelcomeConfiguration getNonauthenticated() {
                return new WelcomeConfiguration() {
                    @Override
                    public String getTitle() {
                        return "welcome.nonauth.title";
                    }

                    @Override
                    public void setTitle(String title) {

                    }

                    @Override
                    public String getIconurl() {
                        return "welcome.nonauth.iconurl";
                    }

                    @Override
                    public void setIconurl(String iconurl) {

                    }

                    @Override
                    public String getContenturl() {
                        return "welcome.nonauth.contenturl";
                    }

                    @Override
                    public void setContenturl(String contenturl) {

                    }
                };
            }

            @Override
            public void setNonauthenticated(WelcomeConfiguration nonauthenticated) {

            }
        });

        //factory.setVariables();
        factory.setGit(new Git() {
            @Override
            public String getConfigremoteoriginfetch() {
                return "Configremoteoriginfetch";
            }

            @Override
            public void setConfigremoteoriginfetch(String configremoteoriginfetch) {

            }

            @Override
            public String getConfigpushdefault() {
                return "Configpushdefault";
            }

            @Override
            public void setConfigpushdefault(String configpushdefault) {

            }

            @Override
            public String getConfigbranchmerge() {
                return "Configbranchmerge";
            }

            @Override
            public void setConfigbranchmerge(String configbranchmerge) {

            }
        });

        factory.setRestriction(new Restriction() {
            @Override
            public long getValidsince() {
                return 11111111;
            }

            @Override
            public void setValidsince(long validsince) {

            }

            @Override
            public long getValiduntil() {
                return 1000007;
            }

            @Override
            public void setValiduntil(long validuntil) {

            }

            @Override
            public String getRefererhostname() {
                return "Refererhostname";
            }

            @Override
            public void setRefererhostname(String refererhostname) {

            }

            @Override
            public String getRestrictbypassword() {
                return "Restrictbypassword";
            }

            @Override
            public void setRestrictbypassword(String restrictbypassword) {

            }

            @Override
            public String getPassword() {
                return "Password";
            }

            @Override
            public void setPassword(String password) {

            }

            @Override
            public int getValidsessioncount() {
                return 2000017;
            }

            @Override
            public void setValidsessioncount(int validsessioncount) {

            }
        });
    }

}
