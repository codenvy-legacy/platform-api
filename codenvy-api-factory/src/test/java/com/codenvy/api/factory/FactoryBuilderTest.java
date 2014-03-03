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

import org.testng.annotations.*;

import java.io.*;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.*;

/**
 * @author Sergii Kabashniuk
 */
public class FactoryBuilderTest {
    Factory factory;

    @BeforeMethod
    public void setUp() throws Exception {

        factory = DtoFactory.getInstance().createDto(Factory.class);
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

        factory.setVariables(new Variable() {
            @Override
            public List<String> getFiles() {
                return Arrays.asList("file1", "file2");
            }

            @Override
            public void setFiles(List<String> files) {

            }

            @Override
            public List<Replacement> getEntries() {
                return Arrays.asList(new Replacement() {
                                         @Override
                                         public String getFind() {
                                             return "find1";
                                         }

                                         @Override
                                         public void setFind(String find) {

                                         }

                                         @Override
                                         public String getReplace() {
                                             return "replace1";
                                         }

                                         @Override
                                         public void setReplace(String replace) {

                                         }

                                         @Override
                                         public String getReplacemode() {
                                             return "replacemod1";
                                         }

                                         @Override
                                         public void setReplacemode(String replacemode) {

                                         }
                                     }, new Replacement() {

                                         @Override
                                         public String getFind() {
                                             return "find2";
                                         }

                                         @Override
                                         public void setFind(String find) {

                                         }

                                         @Override
                                         public String getReplace() {
                                             return "replace2";
                                         }

                                         @Override
                                         public void setReplace(String replace) {

                                         }

                                         @Override
                                         public String getReplacemode() {
                                             return "replacemod";
                                         }

                                         @Override
                                         public void setReplacemode(String replacemode) {

                                         }
                                     }
                                    );
            }

            @Override
            public void setEntries(List<Replacement> entries) {

            }
        });
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

        List<String> needless = new LinkedList<>();
        List<String> need = new LinkedList<>();

        Factory validFactory = DtoFactory.getInstance().clone(factory);

        String version = factory.getV();

        Method[] methods = factory.getClass().getMethods();

        for (Method method : methods) {
            Compatibility compatibility = FactoryBuilder.getAnnotation(method);
            if (compatibility != null) {
                //System.out.println(method.getName());
                need.add(method.getName());
            } else {
                needless.add(method.getName());
            }
        }

        System.out.println("Suitable:");
        for (String method : need) {
            System.out.println(method);
        }

        System.out.println("Not suitable:");
        for (String method : needless) {
            System.out.println(method);
        }
    }

}
