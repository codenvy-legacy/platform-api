/*
 * CODENVY CONFIDENTIAL
 * __________________
 *
 *  [2012] - [2013] Codenvy, S.A.
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
package com.codenvy.factory.commons;

import com.codenvy.commons.lang.ZipUtils;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.Files;

import static org.testng.Assert.assertEquals;

public class SimpleFactoryUrlFormatTest {
    private SimpleFactoryUrlFormat factoryUrlFormat;

    @BeforeMethod
    public void setUp() throws Exception {
        this.factoryUrlFormat = new SimpleFactoryUrlFormat();
    }

    @Test
    public void shouldParseGoodUrl() throws Exception {
        //given
        File testRepository = Files.createTempDirectory("testrepository").toFile();
        ZipUtils.unzip(new File(Thread.currentThread().getContextClassLoader().getResource("testrepository.zip").toURI()), testRepository);

        SimpleFactoryUrl expectedFactoryUrl =
                new SimpleFactoryUrl("1.0", "git", "file://" + testRepository + "/testrepository", "1234567", "eee", "ttt");

        //when
        SimpleFactoryUrl factoryUrl =
                factoryUrlFormat
                        .parse(new URL("http://codenvy.com/factory?v=1.0&vcs=git&idcommit=1234567&pname=eee&wname=ttt&vcsurl=" + enc(
                                "file://" + testRepository + "/testrepository")));

        //then
        assertEquals(factoryUrl, expectedFactoryUrl);
    }

    @Test(dataProvider = "badUrlProvider-InvalidFormat", expectedExceptions = FactoryUrlInvalidFormatException.class)
    public void shouldThrowFactoryUrlIllegalFormatExceptionIfUrlParametersIsMissing(String url) throws Exception {
        factoryUrlFormat.parse(new URL(url));
    }

    @DataProvider(name = "badUrlProvider-InvalidFormat")
    public Object[][] missingParametersFactoryUrlProvider() throws UnsupportedEncodingException {
        return new Object[][]{// there is no format to satisfy that version
                              {"http://codenvy.com/factory?v=2.0&vcs=git&idcommit=1234567&pname=eee&wname=ttt&vcsurl=" +
                               enc("http://github/some/path?somequery=qwe&somequery=sss&somequery=rty")},
                              {"http://codenvy.com/factory?vcs=git&idcommit=1234567&pname=eee&wname=ttt&vcsurl=" +
                               enc("http://github/some/path?somequery=qwe&somequery=sss&somequery=rty")},// v par is missing
        };
    }

    @Test(dataProvider = "badUrlProvider-InvalidArgument", expectedExceptions = FactoryUrlInvalidArgumentException.class)
    public void shouldThrowFactoryUrlInvalidArgumentExceptionIfUrlHasInvalidParameters(String url) throws Exception {
        factoryUrlFormat.parse(new URL(url));
    }

    @DataProvider(name = "badUrlProvider-InvalidArgument")
    public Object[][] invalidParametersFactoryUrlProvider() throws UnsupportedEncodingException {
        return new Object[][]{{"http://codenvy.com/factory?v=1.0&v=2.0&vcs=git&idcommit=1234567&pname=eee&wname=ttt&vcsurl=" +
                               enc("http://github/some/path?somequery=qwe&somequery=sss&somequery=rty")},// v par has is duplicated
                              {"http://codenvy.com/factory?v=1.0&vcs=git&vcs=notagit&idcommit=1234567&pname=eee&wname=ttt&vcsurl=" +
                               enc("http://github/some/path?somequery=qwe&somequery=sss&somequery=rty")},// vcs par is duplicated
                              {"http://codenvy.com/factory?v=1.0&vcs=&idcommit=1234567&pname=eee&wname=ttt&vcsurl=" + enc(
                                      "http://github/some/path?somequery=qwe&somequery=sss&somequery=rty")},// vcs par has empty value
                              {"http://codenvy.com/factory?v=1.0&vcs=mercurial&idcommit=1234567&pname=eee&wname=ttt&vcsurl=" + enc(
                                      "http://github/some/path?somequery=qwe&somequery=sss&somequery=rty")},// vcs par has illegal value
                              {"http://codenvy.com/factory?v=1.0&idcommit=1234567&pname=eee&wname=ttt&vcsurl=" +
                               enc("http://github/some/path?somequery=qwe&somequery=sss&somequery=rty")},// vcs par is missing
                              {"http://codenvy.com/factory?v=1.0&vcs=git&pname=eee&wname=ttt&vcsurl=" + enc(
                                      "http://github/some/path?somequery=qwe&somequery=sss&somequery=rty")},// idcommit par is missing
                              {"http://codenvy.com/factory?v=1.0&vcs=git&idcommit=1234567&wname=ttt&vcsurl=" + enc(
                                      "http://github/some/path?somequery=qwe&somequery=sss&somequery=rty")},// pname par is missing
                              {"http://codenvy.com/factory?v=1.0&vcs=git&idcommit=1234567&pname=eee&vcsurl=" + enc(
                                      "http://github/some/path?somequery=qwe&somequery=sss&somequery=rty")},// wname par is missing
                              {"http://codenvy.com/factory?v=1.0&vcs=git&idcommit=1234567&pname=eee&wname=ttt"} // vcsurl par is missing
        };
    }

    static String enc(String str) throws UnsupportedEncodingException {
        return URLEncoder.encode(str, "UTF-8");
    }
}
