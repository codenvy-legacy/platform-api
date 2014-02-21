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
import com.codenvy.api.factory.dto.FactoryV1_1;
import com.codenvy.dto.server.DtoFactory;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;

/**
 * @author Sergii Kabashniuk
 */
public class FactoryParseTest {


    @Test(dataProvider = "jsonprovider")
    public void shouldBeAbleToParserJsonV1_1(String json) {

        Factory factory = DtoFactory.getInstance().createDtoFromJson(json, Factory.class);
        System.out.println(FactoryBuilder.buildNonEncoded(factory));
    }


    @DataProvider(name = "jsonprovider")
    public static Object[][] createData() throws URISyntaxException, IOException {
        File file = new File(FactoryParseTest.class.getResource("/logback-test.xml").toURI());
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

}
