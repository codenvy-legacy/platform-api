package com.codenvy.api.factory;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

/**
 * Test for Advanced factory validator
 * @author Vladyslav Zhukovskii
 */
public class AdvancedFactoryValidatorTest {

    @Test(dataProvider = "validProjectNamesProvider")
    public void shouldTestProjectValidNamePattern(String projectName) throws Exception {
        assertEquals(AdvancedFactoryUrlValidator.isProjectNameValid(projectName), true);
    }

    @Test(dataProvider = "invalidProjectNamesProvider")
    public void shouldTestProjectInvalidNamePattern(String projectName) throws Exception {
        assertEquals(AdvancedFactoryUrlValidator.isProjectNameValid(projectName), false);
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
                {"untitled.pro....111"}
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
}
