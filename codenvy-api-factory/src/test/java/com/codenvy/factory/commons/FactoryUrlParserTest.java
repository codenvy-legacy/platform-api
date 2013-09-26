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

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;

import static com.codenvy.factory.commons.SimpleFactoryUrlFormatTest.enc;
import static org.testng.Assert.assertEquals;

public class FactoryUrlParserTest {
    @Test
    public void shouldReturnFactoryUrlObjectOnSuccessfulParsing() throws URISyntaxException, IOException, FactoryUrlException {
        //given
        File testRepository = Files.createTempDirectory("testrepository").toFile();
        ZipUtils.unzip(new File(Thread.currentThread().getContextClassLoader().getResource("testrepository.zip").toURI()), testRepository);

        SimpleFactoryUrl expectedFactoryUrl =
                new SimpleFactoryUrl("1.0", "git", "file://" + testRepository + "/testrepository", "1234567", "eee", "ttt");

        //when
        FactoryUrl factoryUrl =
                FactoryUrlParser.parse(new URL("http://codenvy.com/factory?v=1.0&vcs=git&idcommit=1234567&pname=eee&wname=ttt" +
                                               "&vcsurl=" + enc("file://" + testRepository + "/testrepository")));

        //then
        assertEquals(factoryUrl, expectedFactoryUrl);
    }

    @Test(dataProvider = "unsupportedUrls", expectedExceptions = FactoryUrlInvalidFormatException.class)
    public void shouldThrowFactoryUrlInvalidFormatExceptionForUnsupportedUrlFormat(String factoryUrl)
            throws FactoryUrlException, MalformedURLException {
        FactoryUrlParser.parse(new URL(factoryUrl));
    }

    @DataProvider(name = "unsupportedUrls")
    public Object[][] unsupportedUrlsProvider() throws UnsupportedEncodingException {
        return new Object[][]{// there is no format to satisfy that version
                              {"http://codenvy.com/factory?v=2.0&vcs=git&idcommit=1234567&pname=eee&wname=ttt&vcsurl=" +
                               enc("http://github/some/path?somequery=qwe&somequery=sss&somequery=rty")},
        };
    }
}
