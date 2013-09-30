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
package com.codenvy.api.factory;

import com.codenvy.api.factory.store.FactoryStore;
import com.codenvy.api.factory.store.SavedFactoryData;
import com.codenvy.commons.json.JsonHelper;
import com.jayway.restassured.response.Response;

import org.everrest.assured.EverrestJetty;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.ITestContext;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.ExceptionMapper;
import java.util.HashSet;
import java.util.Set;

import static com.jayway.restassured.RestAssured.given;
import static javax.ws.rs.core.Response.Status;
import static org.everrest.assured.JettyHttpServer.*;
import static org.hamcrest.Matchers.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

@Listeners(value = {EverrestJetty.class, MockitoTestNGListener.class})
public class FactoryServiceTest {
    private final String          CORRECT_FACTORY_ID = "correctFactoryId";
    private final String          ILLEGAL_FACTORY_ID = "illegalFactoryId";
    private final String          SERVICE_PATH       = "/factory";
    private final ExceptionMapper exceptionMapper    = new FactoryServiceExceptionMapper();

    @Mock
    private FactoryStore factoryStore;

    @InjectMocks
    private FactoryService factoryService;

    @Test(enabled = false)
    public void shouldBeAbleToSaveFactory() throws Exception {
        // given
        AdvancedFactoryUrl factoryUrl = new AdvancedFactoryUrl();
        factoryUrl.setId(CORRECT_FACTORY_ID);
        factoryUrl.setCommitid("12345679");
        factoryUrl.setVcs("git");
        factoryUrl.setV("1.1");
        factoryUrl.setVcsurl("git@github.com:codenvy/cloud-ide.git");

        when(factoryStore.saveFactory((AdvancedFactoryUrl)any(), (Image)any()))
                .thenReturn(new SavedFactoryData(factoryUrl, new HashSet<Image>()));

        // when, then
        given().//
                multiPart("factoryUrl", JsonHelper.toJson(factoryUrl), MediaType.APPLICATION_JSON).//
                multiPart("image", "image.jpeg", new byte[500], "image/jpeg").//
                expect().//
                statusCode(Status.OK.getStatusCode()).//
                when().//
                post(SERVICE_PATH);
    }

    @Test
    public void shouldReturnStatus400IfSaveRequestHaveNotFactoryInfo() throws Exception {
        // given
        // when, then
        given().//
                multiPart("someOtherData", "Some content", MediaType.TEXT_PLAIN).//
                expect().//
                statusCode(Status.BAD_REQUEST.getStatusCode()).//
                body(equalTo("No factory URL information found in 'factoryUrl' section of multipart form-data")).//
                when().//
                post(SERVICE_PATH);
    }

    @Test
    public void shouldBeAbleToSaveFactoryWithOutImage() throws Exception {
        // given
        AdvancedFactoryUrl factoryUrl = new AdvancedFactoryUrl();
        factoryUrl.setId(CORRECT_FACTORY_ID);
        factoryUrl.setCommitid("12345679");
        factoryUrl.setVcs("git");
        factoryUrl.setV("1.1");
        factoryUrl.setVcsurl("git@github.com:codenvy/cloud-ide.git");

        when(factoryStore.saveFactory((AdvancedFactoryUrl)any(), (Image)any()))
                .thenReturn(new SavedFactoryData(factoryUrl, new HashSet<Image>()));

        // when, then
        given().//
                auth().basic(ADMIN_USER_NAME, ADMIN_USER_PASSWORD).
                multiPart("factoryUrl", JsonHelper.toJson(factoryUrl), MediaType.APPLICATION_JSON).//
                expect().//
                statusCode(Status.OK.getStatusCode()).//
                when().//
                post(SECURE_PATH + SERVICE_PATH);
    }

    @Test
    public void shouldBeAbleToGetFactory(ITestContext context) throws Exception {
        // given
        AdvancedFactoryUrl factoryUrl = new AdvancedFactoryUrl();
        factoryUrl.setId(CORRECT_FACTORY_ID);
        Image image = new Image(null, "image/jpeg", "image123456789.jpg");
        Set<Image> images = new HashSet<>();
        images.add(image);
        SavedFactoryData factoryData = new SavedFactoryData(factoryUrl, images);

        when(factoryStore.getFactory(CORRECT_FACTORY_ID)).thenReturn(factoryData);

        // when, then
        given().//
                expect().//
                statusCode(Status.OK.getStatusCode()).//
                body("links[0].rel", equalTo("self")).//
                body("links[0].href", startsWith(getServerUrl(context) + "/rest/factory")).//
                body("links[1].rel", equalTo("create-project")).//
                body("links[1].href", startsWith(getServerUrl(context) + "/factory")).//
                body("links[2].rel", equalTo("image")).//
                body("links[2].href", startsWith(getServerUrl(context) + "/rest/factory/" + CORRECT_FACTORY_ID)).//
                body("links[2].href", endsWith("image123456789.jpg")).//
                body("links[2].type", equalTo("image/jpeg")).//
                when().//
                get(SERVICE_PATH + "/" + CORRECT_FACTORY_ID);

        //assertEquals(response.getStatusCode(), Status.OK.getStatusCode());
    }

    @Test
    public void shouldReturnStatus400OnGetFactoryWithIllegalId() throws Exception {
        // given
        when(factoryStore.getFactory(ILLEGAL_FACTORY_ID)).thenReturn(null);

        // when, then
        given().//
                expect().//
                statusCode(400).//
                body(equalTo(String.format("Factory URL with id %s is not found.", ILLEGAL_FACTORY_ID))).//
                when().//
                get(SERVICE_PATH + "/" + ILLEGAL_FACTORY_ID);
    }

    @Test
    public void shouldBeAbleToGetFactoryImage() throws Exception {
        // given
        AdvancedFactoryUrl factoryUrl = new AdvancedFactoryUrl();
        factoryUrl.setId(CORRECT_FACTORY_ID);
        Image image = new Image(new byte[100], "image/png", "imageId.png");
        Set<Image> images = new HashSet<>();
        images.add(image);
        SavedFactoryData factoryData = new SavedFactoryData(factoryUrl, images);

        when(factoryStore.getFactory(CORRECT_FACTORY_ID)).thenReturn(factoryData);

        // when
        Response response = given().when().get(SERVICE_PATH + "/" + CORRECT_FACTORY_ID + "/imageId.png");

        // then
        assertEquals(response.getStatusCode(), 200);
        assertEquals(response.getContentType(), "image/png");
    }

    @Test
    public void shouldReturnStatus400OnGetFactoryImageWithIllegalId() throws Exception {
        // given
        AdvancedFactoryUrl factoryUrl = new AdvancedFactoryUrl();
        factoryUrl.setId(CORRECT_FACTORY_ID);
        SavedFactoryData factoryData = new SavedFactoryData(factoryUrl, new HashSet<Image>());

        when(factoryStore.getFactory(CORRECT_FACTORY_ID)).thenReturn(factoryData);

        // when, then
        given().//
                expect().//
                statusCode(400).//
                body(equalTo(String.format("Image with id %s is not found.", "illegalImageId.png"))).//
                when().//
                get(SERVICE_PATH + "/" + CORRECT_FACTORY_ID + "/illegalImageId.png");
    }

    @Test
    public void shouldResponse400OnGetImageIfFactoryDoesNotExist() throws Exception {
        // given
        when(factoryStore.getFactory(ILLEGAL_FACTORY_ID)).thenReturn(null);

        // when, then
        given().//
                expect().//
                statusCode(400).//
                body(equalTo(String.format("Factory URL with id %s is not found.", ILLEGAL_FACTORY_ID))).//
                when().//
                get(SERVICE_PATH + "/" + ILLEGAL_FACTORY_ID + "/ImageId.png");
    }

    @Test
    public void shouldBeAbleToReturnUrlSnippet(ITestContext context) throws Exception {
        // given
        when(factoryStore.getFactory(CORRECT_FACTORY_ID)).thenReturn(new SavedFactoryData(new AdvancedFactoryUrl(), new HashSet<Image>()));

        // when, then
        given().//
                expect().//
                statusCode(200).//
                contentType(MediaType.TEXT_PLAIN).//
                body(equalTo(getServerUrl(context) + "/factory-" + CORRECT_FACTORY_ID)).//
                when().//
                get(SERVICE_PATH + "/" + CORRECT_FACTORY_ID + "/snippet?type=url");
    }

    @Test
    public void shouldBeAbleToReturnHtmlSnippet(ITestContext context) throws Exception {
        // given
        when(factoryStore.getFactory(CORRECT_FACTORY_ID)).thenReturn(new SavedFactoryData(new AdvancedFactoryUrl(), new HashSet<Image>()));

        // when, then
        given().//
                expect().//
                statusCode(200).//
                contentType(MediaType.TEXT_PLAIN).//
                body(equalTo("<script type=\"text/javascript\" language=\"javascript\" src=\"" + getServerUrl(context) +
                             "/factory/factory.js\" target=\"" + getServerUrl(context) + "/factory-" + CORRECT_FACTORY_ID + "\"></script>"))
                .//
                        when().//
                get(SERVICE_PATH + "/" + CORRECT_FACTORY_ID + "/snippet?type=html");
    }

    @Test
    public void shouldBeAbleToReturnMarkdownSnippet(ITestContext context) throws Exception {
        // given
        when(factoryStore.getFactory(CORRECT_FACTORY_ID)).thenReturn(new SavedFactoryData(new AdvancedFactoryUrl(), new HashSet<Image>()));

        // when, then
        given().//
                expect().//
                statusCode(200).//
                contentType(MediaType.TEXT_PLAIN).//
                body(equalTo("[![alt](" + getServerUrl(context) + "/images/factory/factory.png)](" + getServerUrl(context) + "/factory-" +
                             CORRECT_FACTORY_ID + ")")).//
                when().//
                get(SERVICE_PATH + "/" + CORRECT_FACTORY_ID + "/snippet?type=markdown");
    }

    @Test
    public void shouldResponse400OnGetSnippetIfFactoryDoesNotExist() throws Exception {
        // given
        when(factoryStore.getFactory(ILLEGAL_FACTORY_ID)).thenReturn(null);

        // when, then
        given().//
                expect().//
                statusCode(400).//
                body(equalTo("Factory URL with id " + ILLEGAL_FACTORY_ID + " is not found.")).//
                when().//
                get(SERVICE_PATH + "/" + ILLEGAL_FACTORY_ID + "/snippet?type=url");
    }

    @Test(dataProvider = "badSnippetTypeProvider")
    public void shouldResponse400OnGetSnippetIfTypeIsIllegal(String type) throws Exception {
        // given
        when(factoryStore.getFactory(CORRECT_FACTORY_ID)).thenReturn(new SavedFactoryData(new AdvancedFactoryUrl(), new HashSet<Image>()));

        // when, then
        given().//
                expect().//
                statusCode(400).//
                body(equalTo(String.format("Snippet type \"%s\" is unsupported", type))).//
                when().//
                get(SERVICE_PATH + "/" + CORRECT_FACTORY_ID + "/snippet?type=" + type);
    }

    @DataProvider(name = "badSnippetTypeProvider")
    public String[][] badSnippetTypeProvider() {
        return new String[][]{{""},
                              {null},
                              {"mark"}};
    }

    private String getServerUrl(ITestContext context) {
        String serverPort = String.valueOf(context.getAttribute(EverrestJetty.JETTY_PORT));
        return "http://localhost:" + serverPort;
    }
}
