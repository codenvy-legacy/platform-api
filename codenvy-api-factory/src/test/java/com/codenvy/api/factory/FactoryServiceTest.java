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

import com.codenvy.commons.json.JsonHelper;
import com.codenvy.organization.client.UserManager;
import com.jayway.restassured.response.Response;

import org.everrest.assured.EverrestJetty;
import org.everrest.assured.JettyHttpServer;
import org.mockito.*;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.ITestContext;
import org.testng.annotations.*;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.ExceptionMapper;
import java.io.File;
import java.net.URLEncoder;
import java.nio.file.*;
import java.util.*;

import static com.jayway.restassured.RestAssured.given;
import static javax.ws.rs.core.Response.Status;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

@Listeners(value = {EverrestJetty.class, MockitoTestNGListener.class})
public class FactoryServiceTest {
    private final String          CORRECT_FACTORY_ID = "correctFactoryId";
    private final String          ILLEGAL_FACTORY_ID = "illegalFactoryId";
    private final String          SERVICE_PATH       = "/factory";
    private final ExceptionMapper exceptionMapper    = new FactoryServiceExceptionMapper();
    private com.codenvy.organization.model.User user;

    @Mock
    private FactoryStore factoryStore;

    @Mock
    private AdvancedFactoryUrlValidator factoryUrlValidator;

    @Mock
    private UserManager userManager;

    @InjectMocks
    private FactoryService factoryService;

    @BeforeMethod
    public void setUp() throws Exception {
        user = new com.codenvy.organization.model.User();
        user.setId("123456789");
    }

    @Test
    public void shouldBeAbleToSaveFactory(ITestContext context) throws Exception {
        // given
        AdvancedFactoryUrl factoryUrl = new AdvancedFactoryUrl();
        factoryUrl.setId(CORRECT_FACTORY_ID);
        factoryUrl.setCommitid("12345679");
        factoryUrl.setVcs("git");
        factoryUrl.setV("1.1");
        factoryUrl.setVcsurl("git@github.com:codenvy/cloud-ide.git");
        Path path = Paths.get(Thread.currentThread().getContextClassLoader().getResource("100x100_image.jpeg").toURI());

        when(factoryStore.saveFactory((AdvancedFactoryUrl)any(), anySet())).thenReturn(CORRECT_FACTORY_ID);
        when(factoryStore.getFactory(CORRECT_FACTORY_ID)).thenReturn(factoryUrl);
        when(userManager.getUserByAlias(JettyHttpServer.ADMIN_USER_NAME)).thenReturn(user);

        // when, then
        Response response = given().auth().basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD).//
                multiPart("factoryUrl", JsonHelper.toJson(factoryUrl), MediaType.APPLICATION_JSON).//
                multiPart("image", path.toFile(), "image/jpeg").//
                when().//
                post("/private" + SERVICE_PATH);

        assertEquals(response.getStatusCode(), 200);
        AdvancedFactoryUrl responseFactoryUrl = JsonHelper.fromJson(response.getBody().asInputStream(), AdvancedFactoryUrl.class, null);
        boolean found = false;
        Iterator<Link> iter = responseFactoryUrl.getLinks().iterator();
        while (iter.hasNext()) {
            Link link = iter.next();
            if (link.getRel().equals("image") && link.getType().equals("image/jpeg") && !link.getHref().isEmpty())
                found = true;
        }
        assertTrue(found);
    }

    @Test
    public void shouldReturnStatus400IfSaveRequestHaveNotFactoryInfo() throws Exception {
        // given
        when(userManager.getUserByAlias(JettyHttpServer.ADMIN_USER_NAME)).thenReturn(user);

        // when, then
        given().auth().basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD).//
                multiPart("someOtherData", "Some content", MediaType.TEXT_PLAIN).//
                expect().//
                statusCode(Status.BAD_REQUEST.getStatusCode()).//
                body(equalTo("No factory URL information found in 'factoryUrl' section of multipart form-data.")).//
                when().//
                post("/private" + SERVICE_PATH);
    }

    @Test
    public void shouldBeAbleToSaveFactoryWithOutImage(ITestContext context) throws Exception {
        // given
        AdvancedFactoryUrl factoryUrl = new AdvancedFactoryUrl();
        factoryUrl.setId(CORRECT_FACTORY_ID);
        factoryUrl.setCommitid("12345679");
        factoryUrl.setVcs("git");
        factoryUrl.setV("1.1");
        factoryUrl.setVcsurl("git@github.com:codenvy/cloud-ide.git");
        Link expectedCreateProject = new Link("text/html", getServerUrl(context) + "/factory?id=" + CORRECT_FACTORY_ID, "create-project");

        when(factoryStore.saveFactory((AdvancedFactoryUrl)any(), anySet())).thenReturn(CORRECT_FACTORY_ID);
        when(factoryStore.getFactory(CORRECT_FACTORY_ID)).thenReturn(factoryUrl);
        when(userManager.getUserByAlias(JettyHttpServer.ADMIN_USER_NAME)).thenReturn(user);

        // when, then
        Response response =
                given().auth().basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD)//
                        .multiPart("factoryUrl", JsonHelper.toJson(factoryUrl), MediaType.APPLICATION_JSON).when()
                        .post("/private" + SERVICE_PATH);

        // then
        assertEquals(response.getStatusCode(), 200);
        AdvancedFactoryUrl responseFactoryUrl = JsonHelper.fromJson(response.getBody().asInputStream(), AdvancedFactoryUrl.class, null);
        assertTrue(responseFactoryUrl.getLinks().contains(
                new Link("application/json", getServerUrl(context) + "/rest/private/factory/" + CORRECT_FACTORY_ID, "self")));
        assertTrue(responseFactoryUrl.getLinks().contains(expectedCreateProject));
        assertTrue(responseFactoryUrl.getLinks().contains(
                new Link("text/plain", getServerUrl(context) + "/rest/private/analytics/metric/FACTORY_URL_ACCEPTED_NUMBER?factory_url=" +
                                       URLEncoder.encode(expectedCreateProject.getHref(), "UTF-8"), "accepted")));
        assertTrue(responseFactoryUrl.getLinks().contains(
                new Link("text/plain", getServerUrl(context) + "/rest/private/factory/" + CORRECT_FACTORY_ID + "/snippet?type=url",
                         "snippet/url")));
        assertTrue(responseFactoryUrl.getLinks().contains(
                new Link("text/plain", getServerUrl(context) + "/rest/private/factory/" + CORRECT_FACTORY_ID + "/snippet?type=html",
                         "snippet/html")));
        assertTrue(responseFactoryUrl.getLinks().contains(
                new Link("text/plain", getServerUrl(context) + "/rest/private/factory/" + CORRECT_FACTORY_ID + "/snippet?type=markdown",
                         "snippet/markdown")));

        verify(factoryStore).saveFactory(Matchers.<AdvancedFactoryUrl>any(), eq(Collections.<FactoryImage>emptySet()));
    }

    @Test
    public void shouldBeAbleToSaveFactoryWithSetImageFieldButWithOutImageContent() throws Exception {
        // given
        AdvancedFactoryUrl factoryUrl = new AdvancedFactoryUrl();
        factoryUrl.setId(CORRECT_FACTORY_ID);
        factoryUrl.setCommitid("12345679");
        factoryUrl.setVcs("git");
        factoryUrl.setV("1.1");
        factoryUrl.setVcsurl("git@github.com:codenvy/cloud-ide.git");

        when(factoryStore.saveFactory((AdvancedFactoryUrl)any(), anySet())).thenReturn(CORRECT_FACTORY_ID);
        when(factoryStore.getFactory(CORRECT_FACTORY_ID)).thenReturn(factoryUrl);
        when(userManager.getUserByAlias(JettyHttpServer.ADMIN_USER_NAME)).thenReturn(user);

        // when, then
        given().auth().basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD)//
                .multiPart("factoryUrl", JsonHelper.toJson(factoryUrl), MediaType.APPLICATION_JSON)//
                .multiPart("image", File.createTempFile("123456", ".jpeg"), "image/jpeg")//
                .expect().statusCode(200)
                .when().post("/private" + SERVICE_PATH);

        verify(factoryStore).saveFactory(Matchers.<AdvancedFactoryUrl>any(), eq(Collections.<FactoryImage>emptySet()));
    }

    @Test
    public void shouldReturnStatus400OnSaveFactoryIfImageHasUnsupportedMediaType() throws Exception {
        // given
        AdvancedFactoryUrl factoryUrl = new AdvancedFactoryUrl();
        factoryUrl.setId(CORRECT_FACTORY_ID);
        factoryUrl.setCommitid("12345679");
        factoryUrl.setVcs("git");
        factoryUrl.setV("1.1");
        factoryUrl.setVcsurl("git@github.com:codenvy/cloud-ide.git");

        Path path = Paths.get(Thread.currentThread().getContextClassLoader().getResource("100x100_image.jpeg").toURI());

        when(factoryStore.saveFactory((AdvancedFactoryUrl)any(), anySet())).thenReturn(CORRECT_FACTORY_ID);
        when(factoryStore.getFactory(CORRECT_FACTORY_ID)).thenReturn(factoryUrl);
        when(userManager.getUserByAlias(JettyHttpServer.ADMIN_USER_NAME)).thenReturn(user);

        // when, then
        given().auth().basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD)//
                .multiPart("factoryUrl", JsonHelper.toJson(factoryUrl), MediaType.APPLICATION_JSON)//
                .multiPart("image", path.toFile(), "image/tiff")//
                .expect().statusCode(400)
                .body(equalTo("image/tiff is unsupported media type."))
                .when().post("/private" + SERVICE_PATH);
    }

    @Test
    public void shouldBeAbleToSetVcsAsGitIfVcsIsNotSet() throws Exception {
        // given
        AdvancedFactoryUrl factoryUrl = new AdvancedFactoryUrl();
        factoryUrl.setId(CORRECT_FACTORY_ID);
        factoryUrl.setCommitid("12345679");
        factoryUrl.setV("1.1");
        factoryUrl.setVcsurl("git@github.com:codenvy/cloud-ide.git");

        ArgumentCaptor<AdvancedFactoryUrl> argumentCaptor = ArgumentCaptor.forClass(AdvancedFactoryUrl.class);

        when(factoryStore.saveFactory((AdvancedFactoryUrl)any(), anySet())).thenReturn(CORRECT_FACTORY_ID);
        when(factoryStore.getFactory(CORRECT_FACTORY_ID)).thenReturn(factoryUrl);
        when(userManager.getUserByAlias(JettyHttpServer.ADMIN_USER_NAME)).thenReturn(user);

        // when, then
        given().auth().basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD).//
                multiPart("factoryUrl", JsonHelper.toJson(factoryUrl), MediaType.APPLICATION_JSON).//
                expect().//
                statusCode(Status.OK.getStatusCode()).//
                when().//
                post("/private" + SERVICE_PATH);

        verify(factoryStore).saveFactory(argumentCaptor.capture(), anySet());

        assertEquals(argumentCaptor.getValue().getVcs(), "git");
    }

    @Test
    public void shouldBeAbleToGetFactory(ITestContext context) throws Exception {
        // given
        AdvancedFactoryUrl factoryUrl = new AdvancedFactoryUrl();
        factoryUrl.setId(CORRECT_FACTORY_ID);
        Path path = Paths.get(Thread.currentThread().getContextClassLoader().getResource("100x100_image.jpeg").toURI());
        byte[] data = Files.readAllBytes(path);
        FactoryImage image1 = new FactoryImage(data, "image/jpeg", "image123456789");
        FactoryImage image2 = new FactoryImage(data, "image/png", "image987654321");
        Set<FactoryImage> images = new HashSet<>();
        images.add(image1);
        images.add(image2);
        Link expectedCreateProject = new Link("text/html", getServerUrl(context) + "/factory?id=" + CORRECT_FACTORY_ID, "create-project");

        when(factoryStore.getFactory(CORRECT_FACTORY_ID)).thenReturn(factoryUrl);
        when(factoryStore.getFactoryImages(CORRECT_FACTORY_ID, null)).thenReturn(images);

        // when
        Response response = given().when().get(SERVICE_PATH + "/" + CORRECT_FACTORY_ID);

        // then
        assertEquals(response.getStatusCode(), 200);
        AdvancedFactoryUrl responseFactoryUrl = JsonHelper.fromJson(response.getBody().asInputStream(), AdvancedFactoryUrl.class, null);
        assertTrue(responseFactoryUrl.getLinks().contains(
                new Link("application/json", getServerUrl(context) + "/rest/factory/" + CORRECT_FACTORY_ID, "self")));
        assertTrue(responseFactoryUrl.getLinks().contains(expectedCreateProject));
        assertTrue(responseFactoryUrl.getLinks().contains(
                new Link("image/jpeg", getServerUrl(context) + "/rest/factory/" + CORRECT_FACTORY_ID + "/image?imgId=image123456789",
                         "image")));
        assertTrue(responseFactoryUrl.getLinks().contains(
                new Link("image/png", getServerUrl(context) + "/rest/factory/" + CORRECT_FACTORY_ID + "/image?imgId=image987654321",
                         "image")));
        assertTrue(responseFactoryUrl.getLinks().contains(
                new Link("text/plain", getServerUrl(context) + "/rest/analytics/metric/FACTORY_URL_ACCEPTED_NUMBER?factory_url=" +
                                       URLEncoder.encode(expectedCreateProject.getHref(), "UTF-8"), "accepted")));
        assertTrue(responseFactoryUrl.getLinks().contains(
                new Link("text/plain", getServerUrl(context) + "/rest/factory/" + CORRECT_FACTORY_ID + "/snippet?type=url",
                         "snippet/url")));
        assertTrue(responseFactoryUrl.getLinks().contains(
                new Link("text/plain", getServerUrl(context) + "/rest/factory/" + CORRECT_FACTORY_ID + "/snippet?type=html",
                         "snippet/html")));
        assertTrue(responseFactoryUrl.getLinks().contains(
                new Link("text/plain", getServerUrl(context) + "/rest/factory/" + CORRECT_FACTORY_ID + "/snippet?type=markdown",
                         "snippet/markdown")));
    }

    @Test
    public void shouldReturnStatus404OnGetFactoryWithIllegalId() throws Exception {
        // given
        when(factoryStore.getFactory(ILLEGAL_FACTORY_ID)).thenReturn(null);

        // when, then
        given().//
                expect().//
                statusCode(404).//
                body(equalTo(String.format("Factory URL with id %s is not found.", ILLEGAL_FACTORY_ID))).//
                when().//
                get(SERVICE_PATH + "/" + ILLEGAL_FACTORY_ID);
    }

    @Test
    public void shouldBeAbleToGetFactoryImage() throws Exception {
        // given
        Path path = Paths.get(Thread.currentThread().getContextClassLoader().getResource("100x100_image.jpeg").toURI());
        byte[] imageContent = Files.readAllBytes(path);
        FactoryImage image = new FactoryImage(imageContent, "image/jpeg", "imageName");

        when(factoryStore.getFactoryImages(CORRECT_FACTORY_ID, null)).thenReturn(new HashSet<>(Arrays.asList(image)));

        // when
        Response response = given().when().get(SERVICE_PATH + "/" + CORRECT_FACTORY_ID + "/image?imgId=imageName");

        // then
        assertEquals(response.getStatusCode(), 200);
        assertEquals(response.getContentType(), "image/jpeg");
        assertEquals(response.getHeader("content-length"), String.valueOf(imageContent.length));
        assertEquals(response.asByteArray(), imageContent);
    }

    @Test
    public void shouldBeAbleToGetFactoryDefaultImage() throws Exception {
        // given
        Path path = Paths.get(Thread.currentThread().getContextClassLoader().getResource("100x100_image.jpeg").toURI());
        byte[] imageContent = Files.readAllBytes(path);
        FactoryImage image = new FactoryImage(imageContent, "image/jpeg", "imageName");

        when(factoryStore.getFactoryImages(CORRECT_FACTORY_ID, null)).thenReturn(new HashSet<>(Arrays.asList(image)));

        // when
        Response response = given().when().get(SERVICE_PATH + "/" + CORRECT_FACTORY_ID + "/image");

        // then
        assertEquals(response.getStatusCode(), 200);
        assertEquals(response.getContentType(), "image/jpeg");
        assertEquals(response.getHeader("content-length"), String.valueOf(imageContent.length));
        assertEquals(response.asByteArray(), imageContent);
    }

    @Test
    public void shouldReturnStatus404OnGetFactoryImageWithIllegalId() throws Exception {
        // given
        when(factoryStore.getFactoryImages(CORRECT_FACTORY_ID, null)).thenReturn(new HashSet<FactoryImage>());

        // when, then
        given().//
                expect().//
                statusCode(404).//
                body(equalTo(String.format("Image with id %s is not found.", "illegalImageId"))).//
                when().//
                get(SERVICE_PATH + "/" + CORRECT_FACTORY_ID + "/image?imgId=illegalImageId");
    }

    @Test
    public void shouldResponse404OnGetImageIfFactoryDoesNotExist() throws Exception {
        // given
        when(factoryStore.getFactoryImages(ILLEGAL_FACTORY_ID, null)).thenReturn(null);

        // when, then
        given().//
                expect().//
                statusCode(404).//
                body(equalTo(String.format("Factory URL with id %s is not found.", ILLEGAL_FACTORY_ID))).//
                when().//
                get(SERVICE_PATH + "/" + ILLEGAL_FACTORY_ID + "/image?imgId=ImageId");
    }

    @Test
    public void shouldBeAbleToReturnUrlSnippet(ITestContext context) throws Exception {
        // given
        when(factoryStore.getFactory(CORRECT_FACTORY_ID)).thenReturn(new AdvancedFactoryUrl());

        // when, then
        given().//
                expect().//
                statusCode(200).//
                contentType(MediaType.TEXT_PLAIN).//
                body(equalTo(getServerUrl(context) + "/factory?id=" + CORRECT_FACTORY_ID)).//
                when().//
                get(SERVICE_PATH + "/" + CORRECT_FACTORY_ID + "/snippet?type=url");
    }

    @Test
    public void shouldBeAbleToReturnUrlSnippetIfTypeIsNotSet(ITestContext context) throws Exception {
        // given
        when(factoryStore.getFactory(CORRECT_FACTORY_ID)).thenReturn(new AdvancedFactoryUrl());

        // when, then
        given().//
                expect().//
                statusCode(200).//
                contentType(MediaType.TEXT_PLAIN).//
                body(equalTo(getServerUrl(context) + "/factory?id=" + CORRECT_FACTORY_ID)).//
                when().//
                get(SERVICE_PATH + "/" + CORRECT_FACTORY_ID + "/snippet");
    }

    @Test
    public void shouldBeAbleToReturnHtmlSnippet(ITestContext context) throws Exception {
        // given
        when(factoryStore.getFactory(CORRECT_FACTORY_ID)).thenReturn(new AdvancedFactoryUrl());

        // when, then
        given().//
                expect().//
                statusCode(200).//
                contentType(MediaType.TEXT_PLAIN).//
                body(equalTo("<script type=\"text/javascript\" language=\"javascript\" src=\"" + getServerUrl(context) +
                             "/factory/resources/embed.js?" + CORRECT_FACTORY_ID + "\"></script>"))
                .//
                        when().//
                get(SERVICE_PATH + "/" + CORRECT_FACTORY_ID + "/snippet?type=html");
    }

    @Test
    public void shouldBeAbleToReturnMarkdownSnippetWithImage(ITestContext context) throws Exception {
        // given
        String imageName = "1241234";
        AdvancedFactoryUrl furl = new AdvancedFactoryUrl();
        furl.setStyle("Advanced");
        FactoryImage image = new FactoryImage();
        image.setName(imageName);

        when(factoryStore.getFactory(CORRECT_FACTORY_ID)).thenReturn(furl);
        when(factoryStore.getFactoryImages(CORRECT_FACTORY_ID, null)).thenReturn(new HashSet<>(Arrays.asList(image)));
        // when, then
        given().//
                expect().//
                statusCode(200).//
                contentType(MediaType.TEXT_PLAIN).//
                body(
                equalTo("[![alt](" + getServerUrl(context) + "/api/factory/" + CORRECT_FACTORY_ID + "/image?imgId=" + imageName + ")](" +
                        getServerUrl(context) + "/factory?id=" +
                        CORRECT_FACTORY_ID + ")")).//
                when().//
                get(SERVICE_PATH + "/" + CORRECT_FACTORY_ID + "/snippet?type=markdown");
    }

    @Test
    public void shouldBeAbleToReturnMarkdownSnippetWithoutImage(ITestContext context) throws Exception {
        // given
        AdvancedFactoryUrl furl = new AdvancedFactoryUrl();
        furl.setStyle("White");

        when(factoryStore.getFactory(CORRECT_FACTORY_ID)).thenReturn(furl);
        // when, then
        given().//
                expect().//
                statusCode(200).//
                contentType(MediaType.TEXT_PLAIN).//
                body(
                equalTo("[![alt](" + getServerUrl(context) + "/factory/resources/factory-white.png)](" + getServerUrl(context) +
                        "/factory?id=" +
                        CORRECT_FACTORY_ID + ")")).//
                when().//
                get(SERVICE_PATH + "/" + CORRECT_FACTORY_ID + "/snippet?type=markdown");
    }

    @Test
    public void shouldResponse404OnGetSnippetIfFactoryDoesNotExist() throws Exception {
        // given
        when(factoryStore.getFactory(ILLEGAL_FACTORY_ID)).thenReturn(null);

        // when, then
        given().//
                expect().//
                statusCode(404).//
                body(equalTo("Factory URL with id " + ILLEGAL_FACTORY_ID + " is not found.")).//
                when().//
                get(SERVICE_PATH + "/" + ILLEGAL_FACTORY_ID + "/snippet?type=url");
    }

    @Test(dataProvider = "badSnippetTypeProvider")
    public void shouldResponse400OnGetSnippetIfTypeIsIllegal(String type) throws Exception {
        // given
        when(factoryStore.getFactory(CORRECT_FACTORY_ID)).thenReturn(new AdvancedFactoryUrl());

        // when, then
        given().//
                expect().//
                statusCode(400).//
                body(equalTo(String.format("Snippet type \"%s\" is unsupported.", type))).//
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
