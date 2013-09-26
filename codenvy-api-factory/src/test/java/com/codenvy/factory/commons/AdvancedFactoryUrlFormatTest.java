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
import com.codenvy.factory.client.FactoryClient;

import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.nio.file.Files;

import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

@Listeners(value = {MockitoTestNGListener.class})
public class AdvancedFactoryUrlFormatTest {
    @Mock
    private FactoryClient factoryClient;

    private AdvancedFactoryUrlFormat factoryUrlFormat;

    @BeforeMethod
    public void setUp() throws Exception {
        this.factoryUrlFormat = new AdvancedFactoryUrlFormat(factoryClient);
    }

    @Test
    public void shouldParseGoodUrl() throws Exception {
        //given
        File testRepository = Files.createTempDirectory("testrepository").toFile();
        ZipUtils.unzip(new File(Thread.currentThread().getContextClassLoader().getResource("testrepository.zip").toURI()), testRepository);

        AdvancedFactoryUrl expectedFactoryUrl =
                new AdvancedFactoryUrl("1.1", "git", "file://" + testRepository + "/testrepository", "commit123456789");
        expectedFactoryUrl.setId("123456789");

        URL factoryUrl = new URL("http://codenvy.com/factory?id=123456789");
        when(factoryClient.getFactory(factoryUrl, "123456789")).thenReturn(expectedFactoryUrl);

        //when
        AdvancedFactoryUrl actualFactoryUrl = factoryUrlFormat.parse(factoryUrl);

        //then
        assertEquals(actualFactoryUrl, expectedFactoryUrl);
    }

    @Test(expectedExceptions = FactoryUrlInvalidFormatException.class)
    public void shouldThrowFactoryUrlIllegalFormatExceptionIfIdIsMissing() throws Exception {
        factoryUrlFormat.parse(new URL("http://codenvy.com/factory"));
    }

    @Test(expectedExceptions = FactoryUrlInvalidArgumentException.class)
    public void shouldThrowFactoryUrlInvalidArgumentExceptionIfFactoryWithSuchIdIsNotFound()
            throws Exception {
        //given
        URL factoryUrl = new URL("http://codenvy.com/factory?id=123456789");
        when(factoryClient.getFactory(factoryUrl, "123456789")).thenReturn(null);
        //when
        AdvancedFactoryUrl actualFactoryUrl = factoryUrlFormat.parse(factoryUrl);
    }

    @Test(dataProvider = "badAdvancedFactoryUrlProvider", expectedExceptions = FactoryUrlInvalidArgumentException.class)
    public void shouldThrowFactoryUrlInvalidArgumentExceptionIfUrlHasInvalidParameters(AdvancedFactoryUrl storedFactoryUrl)
            throws Exception {
        //given
        URL factoryUrl = new URL("http://codenvy.com/factory?id=123456789");
        when(factoryClient.getFactory(factoryUrl, "123456789")).thenReturn(storedFactoryUrl);
        //when
        AdvancedFactoryUrl actualFactoryUrl = factoryUrlFormat.parse(factoryUrl);
    }

    @DataProvider(name = "badAdvancedFactoryUrlProvider")
    public Object[][] invalidParametersFactoryUrlProvider() throws UnsupportedEncodingException {
        return new Object[][]{{new AdvancedFactoryUrl("1.1", "notagit", "file://testRepository/testrepository", "commit123456789")},
                              {new AdvancedFactoryUrl("1.1", "git", null, "commit123456789")},
                              {new AdvancedFactoryUrl("1.1", "git", "", "commit123456789")},
                              {new AdvancedFactoryUrl("1.1", "git", "file://testRepository/testrepository", "")},
                              {new AdvancedFactoryUrl("1.1", "git", "file://testRepository/testrepository", null)}
        };
    }
}
