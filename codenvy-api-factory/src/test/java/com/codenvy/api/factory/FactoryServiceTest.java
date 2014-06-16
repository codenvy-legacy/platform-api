/*******************************************************************************
 * Copyright (c) 2012-2014 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package com.codenvy.api.factory;

import com.codenvy.api.core.rest.shared.dto.Link;
import com.codenvy.api.core.util.Pair;
import com.codenvy.api.factory.dto.Factory;
import com.codenvy.api.factory.dto.*;
import com.codenvy.commons.env.EnvironmentContext;
import com.codenvy.commons.json.JsonHelper;
import com.codenvy.commons.user.UserImpl;
import com.codenvy.dto.server.DtoFactory;
import com.jayway.restassured.response.Response;

import org.everrest.assured.EverrestJetty;
import org.everrest.assured.JettyHttpServer;
import org.everrest.core.*;
import org.mockito.*;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.ITestContext;
import org.testng.annotations.*;

import javax.ws.rs.core.MediaType;
import java.io.File;
import java.nio.file.*;
import java.util.*;

import static com.jayway.restassured.RestAssured.given;
import static java.net.URLEncoder.encode;
import static javax.ws.rs.core.Response.Status;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anySet;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

@Listeners(value = {EverrestJetty.class, MockitoTestNGListener.class})
public class FactoryServiceTest {
    private final String                        CORRECT_FACTORY_ID = "correctFactoryId";
    private final String                        ILLEGAL_FACTORY_ID = "illegalFactoryId";
    private final String                        SERVICE_PATH       = "/factory";
    private final FactoryServiceExceptionMapper exceptionMapper    = new FactoryServiceExceptionMapper();

    private EnvironmentFilter filter = new EnvironmentFilter();

    @Mock
    private FactoryStore factoryStore;

    @Spy
    private LinksHelper linksHelper;

    @Spy
    private FactoryBuilder factoryBuilder;

    @Mock
    private FactoryUrlCreateValidator validator;

    @Mock
    private FactoryUrlAcceptValidator acceptValidator;

    @InjectMocks
    private FactoryService factoryService;

    @javax.ws.rs.Path("factory")
    @Filter
    public static class EnvironmentFilter implements RequestFilter {

        public void doFilter(GenericContainerRequest request) {
            EnvironmentContext context = EnvironmentContext.getCurrent();
            context.setUser(new UserImpl(JettyHttpServer.ADMIN_USER_NAME, "id-2314", "token-2323",
                                         Collections.<String>emptyList()));
        }

    }

    @Test
    public void shouldBeAbleToConvertQueryStringToFactory() throws Exception {
        // given
        Factory expected = DtoFactory.getInstance().createDto(Factory.class);
        expected.withWname("wname").withPtype("ptype").withPname("pname").withV("1.0").withVcs("git")
                .withVcsurl("http://github.com/codenvy/platform-api.git").withVcsinfo(true);

        StringBuilder queryString = new StringBuilder();
        queryString.append("v=").append(expected.getV());
        queryString.append("&vcs=").append(expected.getVcs());
        queryString.append("&vcsurl=").append(expected.getVcsurl());
        queryString.append("&pname=").append(expected.getPname());
        queryString.append("&ptype=").append(expected.getPtype());
        queryString.append("&wname=").append(expected.getWname());
        queryString.append("&vcsinfo=").append(expected.getVcsinfo());

        // when
        Response response = given().when().get(SERVICE_PATH + "/nonencoded?" + queryString);

        // then
        assertEquals(response.getStatusCode(), 200);
        Factory responseFactoryUrl =
                DtoFactory.getInstance().createDtoFromJson(response.getBody().asInputStream(), Factory.class);
        assertEquals(responseFactoryUrl, expected);
    }

    @Test
    public void shouldBeAbleToConvertQueryStringToLatestFactory() throws Exception {
        // given
        Factory expected = DtoFactory.getInstance().createDto(Factory.class);
        expected.withProjectattributes(
                DtoFactory.getInstance().createDto(ProjectAttributes.class).withPname("pname").withPtype("ptype"))
                .withV("1.2").withVcs("git").withVcsurl(
                "http://github.com/codenvy/platform-api.git");

        StringBuilder queryString = new StringBuilder();
        queryString.append("v=").append("1.0");
        queryString.append("&vcs=").append(expected.getVcs());
        queryString.append("&vcsurl=").append(expected.getVcsurl());
        queryString.append("&pname=").append(expected.getProjectattributes().getPname());
        queryString.append("&ptype=").append(expected.getProjectattributes().getPtype());
        queryString.append("&wname=").append("wname");

        // when
        Response response = given().when().get(SERVICE_PATH + "/nonencoded?legacy=true&" + queryString);

        // then
        assertEquals(response.getStatusCode(), 200);
        Factory responseFactoryUrl = DtoFactory.getInstance().createDtoFromJson(response.getBody().asInputStream(), Factory.class);
        assertEquals(responseFactoryUrl, expected);
    }

    @Test
    public void shouldBeAbleToReturnLatestFactory() throws Exception {
        // given
        Factory factoryUrl = DtoFactory.getInstance().createDto(Factory.class);
        factoryUrl.withIdcommit("132456").withWname("wname").withPtype("ptype").withPname("pname").withV("1.0");
        factoryUrl.setId(CORRECT_FACTORY_ID);

        ProjectAttributes attributes =
                DtoFactory.getInstance().createDto(ProjectAttributes.class).withPname(factoryUrl.getPname()).withPtype(
                        factoryUrl.getPtype());

        Factory expected = (Factory)DtoFactory.getInstance().createDto(
                Factory.class).withProjectattributes(attributes).withId(CORRECT_FACTORY_ID).withV("1.2")
                                              .withCommitid(factoryUrl.getIdcommit());

        when(factoryStore.getFactory(CORRECT_FACTORY_ID)).thenReturn(factoryUrl);
        when(factoryStore.getFactoryImages(CORRECT_FACTORY_ID, null)).thenReturn(Collections.EMPTY_SET);

        // when
        Response response = given().when().get(SERVICE_PATH + "/" + CORRECT_FACTORY_ID + "?legacy=true");

        // then
        assertEquals(response.getStatusCode(), 200);
        Factory responseFactoryUrl = DtoFactory.getInstance().createDtoFromJson(response.getBody().asInputStream(), Factory.class);
        responseFactoryUrl.setLinks(Collections.<Link>emptyList());
        assertEquals(responseFactoryUrl, expected);
    }

    @Test
    public void shouldReturnSavedFactoryIfUserDidNotUseSpecialMethod() throws Exception {
        // given
        Factory factoryUrl = DtoFactory.getInstance().createDto(Factory.class);
        factoryUrl.withValidsince(123456789).withValiduntil(12345679).withIdcommit("132456").withWname("wname").withPtype("ptype")
                  .withPname("pname").withV("1.0");
        factoryUrl.setId(CORRECT_FACTORY_ID);

        Factory expected = DtoFactory.getInstance().clone(factoryUrl);

        when(factoryStore.getFactory(CORRECT_FACTORY_ID)).thenReturn(factoryUrl);
        when(factoryStore.getFactoryImages(CORRECT_FACTORY_ID, null)).thenReturn(Collections.EMPTY_SET);

        // when
        Response response = given().when().get(SERVICE_PATH + "/" + CORRECT_FACTORY_ID);

        // then
        assertEquals(response.getStatusCode(), 200);
        Factory responseFactoryUrl = DtoFactory.getInstance().createDtoFromJson(response.getBody().asInputStream(), Factory.class);
        responseFactoryUrl.setLinks(Collections.<Link>emptyList());
        assertEquals(responseFactoryUrl, expected);
    }

    @Test
    public void shouldBeAbleToGetFactoryValidatedFactoryFromNonEncoded() throws Exception {
        // given

        // when
        Response response =
                given().when().queryParam("v", "1.1").queryParam("vcs", "git").queryParam("vcsurl", "git@github.com:codenvy/cloud-ide.git")
                        .queryParam("variables",
                                    ("[" + DtoFactory.getInstance().toJson(DtoFactory.getInstance().createDto(Variable.class)) + "]")).get(
                        SERVICE_PATH + "/nonencoded");

        // then
        Factory factory = DtoFactory.getInstance().createDtoFromJson(response.asInputStream(), Factory.class);
        assertEquals(DtoFactory.getInstance().createDto(Factory.class)
                               .withVariables(Arrays.asList(DtoFactory.getInstance().createDto(Variable.class))).withV(
                        "1.1").withVcs("git").withVcsurl("git@github.com:codenvy/cloud-ide.git"), factory);
    }


    @Test
    public void shouldBeAbleToSaveFactory(ITestContext context) throws Exception {
        // given
        Factory factoryUrl = DtoFactory.getInstance().createDto(Factory.class);
        factoryUrl.setCommitid("12345679");
        factoryUrl.setVcs("git");
        factoryUrl.setV("1.1");
        factoryUrl.setVcsurl("git@github.com:codenvy/cloud-ide.git");
        Path path = Paths.get(Thread.currentThread().getContextClassLoader().getResource("100x100_image.jpeg").toURI());

        when(factoryStore.saveFactory((Factory)any(), anySet())).thenReturn(CORRECT_FACTORY_ID);
        when(factoryStore.getFactory(CORRECT_FACTORY_ID)).thenReturn(
                (Factory)DtoFactory.getInstance().clone(factoryUrl).withId(CORRECT_FACTORY_ID));

        // when, then
        Response response =
                given().auth().basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD).//
                        multiPart("factoryUrl", JsonHelper.toJson(factoryUrl), MediaType.APPLICATION_JSON).//
                        multiPart("image", path.toFile(), "image/jpeg").//
                        when().//
                        post("/private" + SERVICE_PATH);

        assertEquals(response.getStatusCode(), 200);
        Factory responseFactoryUrl = DtoFactory.getInstance().createDtoFromJson(response.getBody().asInputStream(), Factory.class);
        boolean found = false;
        Iterator<Link> iter = responseFactoryUrl.getLinks().iterator();
        while (iter.hasNext()) {
            Link link = iter.next();
            if (link.getRel().equals("image") && link.getProduces().equals("image/jpeg") && !link.getHref().isEmpty())
                found = true;
        }
        assertTrue(found);
    }

    @Test
    public void shouldReturnStatus400IfSaveRequestHaveNotFactoryInfo() throws Exception {
        // given

        // when, then
        given().auth().basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD).//
                multiPart("someOtherData", "Some content", MediaType.TEXT_PLAIN).//
                expect().//
                statusCode(Status.BAD_REQUEST.getStatusCode()).//
                body(equalTo("No factory URL information found in 'factoryUrl' section of multipart/form-data.")).//
                when().//
                post("/private" + SERVICE_PATH);
    }

    @Test
    public void shouldBeAbleToSaveFactoryWithOutImage(ITestContext context) throws Exception {
        // given
        Factory factoryUrl = DtoFactory.getInstance().createDto(Factory.class);
        //factoryUrl.setId(CORRECT_FACTORY_ID);
        factoryUrl.setCommitid("12345679");
        factoryUrl.setVcs("git");
        factoryUrl.setV("1.1");
        factoryUrl.setVcsurl("git@github.com:codenvy/cloud-ide.git");
        Link expectedCreateProject =
                DtoFactory.getInstance().createDto(Link.class).withMethod("GET").withProduces("text/html").withRel("create-project")
                          .withHref(getServerUrl(context) + "/factory?id=" + CORRECT_FACTORY_ID);

        when(factoryStore.saveFactory((Factory)any(), anySet())).thenReturn(CORRECT_FACTORY_ID);
        when(factoryStore.getFactory(CORRECT_FACTORY_ID))
                .thenReturn((Factory)DtoFactory.getInstance().clone(factoryUrl).withId(CORRECT_FACTORY_ID));

        // when, then
        Response response =
                given().auth().basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD)//
                        .multiPart("factoryUrl", JsonHelper.toJson(factoryUrl), MediaType.APPLICATION_JSON).when()
                        .post("/private" + SERVICE_PATH);

        // then
        assertEquals(response.getStatusCode(), 200);
        Factory responseFactoryUrl = DtoFactory.getInstance().createDtoFromJson(response.getBody().asInputStream(), Factory.class);
        assertTrue(responseFactoryUrl.getLinks().contains(
                DtoFactory.getInstance().createDto(Link.class).withMethod("GET").withProduces("application/json")
                          .withHref(getServerUrl(context) + "/rest/private/factory/" +
                                    CORRECT_FACTORY_ID).withRel("self")));
        assertTrue(responseFactoryUrl.getLinks().contains(expectedCreateProject));
        assertTrue(responseFactoryUrl.getLinks()
                                     .contains(DtoFactory.getInstance().createDto(Link.class).withMethod("GET").withProduces("text/plain")
                                                         .withHref(getServerUrl(context) +
                                                                   "/rest/private/analytics/public-metric/factory_used?factory=" +
                                                                   encode(expectedCreateProject.getHref(), "UTF-8"))
                                                         .withRel("accepted")));
        assertTrue(responseFactoryUrl.getLinks()
                                     .contains(DtoFactory.getInstance().createDto(Link.class).withMethod("GET").withProduces("text/plain")
                                                         .withHref(getServerUrl(context) + "/rest/private/factory/" +
                                                                   CORRECT_FACTORY_ID + "/snippet?type=url")
                                                         .withRel("snippet/url")));
        assertTrue(responseFactoryUrl.getLinks()
                                     .contains(DtoFactory.getInstance().createDto(Link.class).withMethod("GET").withProduces("text/plain")
                                                         .withHref(getServerUrl(context) + "/rest/private/factory/" +
                                                                   CORRECT_FACTORY_ID + "/snippet?type=html")
                                                         .withRel("snippet/html")));
        assertTrue(responseFactoryUrl.getLinks()
                                     .contains(DtoFactory.getInstance().createDto(Link.class).withMethod("GET").withProduces("text/plain")
                                                         .withHref(getServerUrl(context) + "/rest/private/factory/" +
                                                                   CORRECT_FACTORY_ID + "/snippet?type=markdown")
                                                         .withRel("snippet/markdown")));


        List<Link> expectedLinks = new ArrayList<>(8);
        expectedLinks.add(expectedCreateProject);

        Link self = DtoFactory.getInstance().createDto(Link.class);
        self.setMethod("GET");
        self.setProduces("application/json");
        self.setHref(getServerUrl(context) + "/rest/private/factory/" + CORRECT_FACTORY_ID);
        self.setRel("self");
        expectedLinks.add(self);

        Link accepted = DtoFactory.getInstance().createDto(Link.class);
        accepted.setMethod("GET");
        accepted.setProduces("text/plain");
        accepted.setHref(getServerUrl(context) + "/rest/private/analytics/public-metric/factory_used?factory=" +
                         encode(expectedCreateProject.getHref(), "UTF-8"));
        accepted.setRel("accepted");
        expectedLinks.add(accepted);

        Link snippetUrl = DtoFactory.getInstance().createDto(Link.class);
        snippetUrl.setProduces("text/plain");
        snippetUrl.setHref(getServerUrl(context) + "/rest/private/factory/" + CORRECT_FACTORY_ID + "/snippet?type=url");
        snippetUrl.setRel("snippet/url");
        snippetUrl.setMethod("GET");
        expectedLinks.add(snippetUrl);

        Link snippetHtml = DtoFactory.getInstance().createDto(Link.class);
        snippetHtml.setProduces("text/plain");
        snippetHtml.setHref(getServerUrl(context) + "/rest/private/factory/" + CORRECT_FACTORY_ID +
                            "/snippet?type=html");
        snippetHtml.setMethod("GET");
        snippetHtml.setRel("snippet/html");
        expectedLinks.add(snippetHtml);

        Link snippetMarkdown = DtoFactory.getInstance().createDto(Link.class);
        snippetMarkdown.setProduces("text/plain");
        snippetMarkdown.setHref(getServerUrl(context) + "/rest/private/factory/" + CORRECT_FACTORY_ID +
                                "/snippet?type=markdown");
        snippetMarkdown.setRel("snippet/markdown");
        snippetMarkdown.setMethod("GET");
        expectedLinks.add(snippetMarkdown);
        
        Link snippetiFrame = DtoFactory.getInstance().createDto(Link.class);
        snippetiFrame.setProduces("text/plain");
        snippetiFrame.setHref(getServerUrl(context) + "/rest/private/factory/" + CORRECT_FACTORY_ID +
                                "/snippet?type=iframe");
        snippetiFrame.setRel("snippet/iframe");
        snippetiFrame.setMethod("GET");
        expectedLinks.add(snippetiFrame);

        for (Link link : responseFactoryUrl.getLinks()) {
            //This transposition need because proxy objects doesn't contains equals method.
            Link testLink = DtoFactory.getInstance().createDto(Link.class);
            testLink.setProduces(link.getProduces());
            testLink.setHref(link.getHref());
            testLink.setRel(link.getRel());
            testLink.setMethod("GET");
            assertTrue(expectedLinks.contains(testLink));
        }

        verify(factoryStore).saveFactory(Matchers.<Factory>any(), eq(Collections.<FactoryImage>emptySet()));
    }

    @Test
    public void shouldBeAbleToSaveFactoryWithOutImageWithOrgId(ITestContext context) throws Exception {
        // given
        Factory factoryUrl = DtoFactory.getInstance().createDto(Factory.class);
        //factoryUrl.setId(CORRECT_FACTORY_ID);
        factoryUrl.setCommitid("12345679");
        factoryUrl.setVcs("git");
        factoryUrl.setV("1.1");
        factoryUrl.setVcsurl("git@github.com:codenvy/cloud-ide.git");
        factoryUrl.setWelcome(DtoFactory.getInstance().createDto(WelcomePage.class));
        factoryUrl.setOrgid("orgid");

        when(factoryStore.saveFactory((Factory)any(), anySet())).thenReturn(CORRECT_FACTORY_ID);
        when(factoryStore.getFactory(CORRECT_FACTORY_ID))
                .thenReturn((Factory)DtoFactory.getInstance().clone(factoryUrl).withId(CORRECT_FACTORY_ID));

        // when, then
        Response response =
                given().auth().basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD)//
                        .multiPart("factoryUrl", JsonHelper.toJson(factoryUrl), MediaType.APPLICATION_JSON).when()
                        .post("/private" + SERVICE_PATH);

        // then
        assertEquals(response.getStatusCode(), 200);
    }

    @Test
    public void shouldRespond400OnSaveFactoryWithOrgIdNotOwnedByCurrentUser(ITestContext context) throws Exception {
        // given
        Factory factoryUrl = DtoFactory.getInstance().createDto(Factory.class);
        factoryUrl.setId(CORRECT_FACTORY_ID);
        factoryUrl.setCommitid("12345679");
        factoryUrl.setVcs("git");
        factoryUrl.setV("1.1");
        factoryUrl.setVcsurl("git@github.com:codenvy/cloud-ide.git");
        factoryUrl.setWelcome(DtoFactory.getInstance().createDto(WelcomePage.class));
        factoryUrl.setOrgid("orgid");

        doThrow(new FactoryUrlException("You are not authorized to use this orgid.")).when(validator)
                .validateOnCreate(Matchers.any(Factory.class));
        when(factoryStore.saveFactory(Matchers.any(Factory.class), anySet())).thenReturn(CORRECT_FACTORY_ID);
        when(factoryStore.getFactory(CORRECT_FACTORY_ID)).thenReturn(factoryUrl);

        // when, then
        Response response =
                given().auth().basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD)//
                        .multiPart("factoryUrl", JsonHelper.toJson(factoryUrl), MediaType.APPLICATION_JSON).when()
                        .post("/private" + SERVICE_PATH);

        // then
        assertEquals(response.getStatusCode(), 400);
    }

    @Test
    public void shouldBeAbleToSaveFactoryWithSetImageFieldButWithOutImageContent() throws Exception {
        // given
        Factory factoryUrl = DtoFactory.getInstance().createDto(Factory.class);
        factoryUrl.setCommitid("12345679");
        factoryUrl.setVcs("git");
        factoryUrl.setV("1.1");
        factoryUrl.setVcsurl("git@github.com:codenvy/cloud-ide.git");

        when(factoryStore.saveFactory((Factory)any(), anySet())).thenReturn(CORRECT_FACTORY_ID);
        when(factoryStore.getFactory(CORRECT_FACTORY_ID))
                .thenReturn((Factory)DtoFactory.getInstance().clone(factoryUrl).withId(CORRECT_FACTORY_ID));

        // when, then
        given().auth().basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD)//
                .multiPart("factoryUrl", DtoFactory.getInstance().toJson(factoryUrl), MediaType.APPLICATION_JSON)//
                .multiPart("image", File.createTempFile("123456", ".jpeg"), "image/jpeg")//
                .expect().statusCode(200)
                .when().post("/private" + SERVICE_PATH);

        verify(factoryStore).saveFactory(Matchers.<Factory>any(), eq(Collections.<FactoryImage>emptySet()));
    }

    @Test
    public void shouldReturnStatus400OnSaveFactoryIfImageHasUnsupportedMediaType() throws Exception {
        // given
        Factory factoryUrl = DtoFactory.getInstance().createDto(Factory.class);
        factoryUrl.setId(CORRECT_FACTORY_ID);
        factoryUrl.setCommitid("12345679");
        factoryUrl.setVcs("git");
        factoryUrl.setV("1.1");
        factoryUrl.setVcsurl("git@github.com:codenvy/cloud-ide.git");

        Path path = Paths.get(Thread.currentThread().getContextClassLoader().getResource("100x100_image.jpeg").toURI());

        when(factoryStore.saveFactory((Factory)any(), anySet())).thenReturn(CORRECT_FACTORY_ID);
        when(factoryStore.getFactory(CORRECT_FACTORY_ID)).thenReturn(factoryUrl);

        // when, then
        given().auth().basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD)//
                .multiPart("factoryUrl", JsonHelper.toJson(factoryUrl), MediaType.APPLICATION_JSON)//
                .multiPart("image", path.toFile(), "image/tiff")//
                .expect().statusCode(400)
                .body(equalTo("Image media type 'image/tiff' is unsupported."))
                .when().post("/private" + SERVICE_PATH);
    }

    @Test
    public void shouldBeAbleToGetFactory(ITestContext context) throws Exception {
        // given
        Factory factoryUrl = DtoFactory.getInstance().createDto(Factory.class);
        factoryUrl.setId(CORRECT_FACTORY_ID);
        Path path = Paths.get(Thread.currentThread().getContextClassLoader().getResource("100x100_image.jpeg").toURI());
        byte[] data = Files.readAllBytes(path);
        FactoryImage image1 = new FactoryImage(data, "image/jpeg", "image123456789");
        FactoryImage image2 = new FactoryImage(data, "image/png", "image987654321");
        Set<FactoryImage> images = new HashSet<>();
        images.add(image1);
        images.add(image2);
        Link expectedCreateProject = DtoFactory.getInstance().createDto(Link.class);
        expectedCreateProject.setProduces("text/html");
        expectedCreateProject.setHref(getServerUrl(context) + "/factory?id=" + CORRECT_FACTORY_ID);
        expectedCreateProject.setRel("create-project");

        when(factoryStore.getFactory(CORRECT_FACTORY_ID)).thenReturn(factoryUrl);
        when(factoryStore.getFactoryImages(CORRECT_FACTORY_ID, null)).thenReturn(images);

        // when
        Response response = given().when().get(SERVICE_PATH + "/" + CORRECT_FACTORY_ID);

        // then
        assertEquals(response.getStatusCode(), 200);
        Factory responseFactoryUrl = JsonHelper.fromJson(response.getBody().asInputStream(),
                                                         Factory.class, null);

        List<Link> expectedLinks = new ArrayList<>(9);
        expectedLinks.add(expectedCreateProject);

        Link self = DtoFactory.getInstance().createDto(Link.class);
        self.setProduces("application/json");
        self.setHref(getServerUrl(context) + "/rest/factory/" + CORRECT_FACTORY_ID);
        self.setRel("self");
        expectedLinks.add(self);

        Link imageJpeg = DtoFactory.getInstance().createDto(Link.class);
        imageJpeg.setProduces("image/jpeg");
        imageJpeg.setHref(getServerUrl(context) + "/rest/factory/" + CORRECT_FACTORY_ID +
                          "/image?imgId=image123456789");
        imageJpeg.setRel("image");
        expectedLinks.add(imageJpeg);

        Link imagePng = DtoFactory.getInstance().createDto(Link.class);
        imagePng.setProduces("image/png");
        imagePng.setHref(getServerUrl(context) + "/rest/factory/" + CORRECT_FACTORY_ID + "/image?imgId=image987654321");
        imagePng.setRel("image");
        expectedLinks.add(imagePng);

        Link accepted = DtoFactory.getInstance().createDto(Link.class);
        accepted.setProduces("text/plain");
        accepted.setHref(getServerUrl(context) + "/rest/analytics/public-metric/factory_used?factory=" +
                         encode(expectedCreateProject.getHref(), "UTF-8"));
        accepted.setRel("accepted");
        expectedLinks.add(accepted);

        Link snippetUrl = DtoFactory.getInstance().createDto(Link.class);
        snippetUrl.setProduces("text/plain");
        snippetUrl.setHref(getServerUrl(context) + "/rest/factory/" + CORRECT_FACTORY_ID + "/snippet?type=url");
        snippetUrl.setRel("snippet/url");
        expectedLinks.add(snippetUrl);

        Link snippetHtml = DtoFactory.getInstance().createDto(Link.class);
        snippetHtml.setProduces("text/plain");
        snippetHtml.setHref(getServerUrl(context) + "/rest/factory/" + CORRECT_FACTORY_ID + "/snippet?type=html");
        snippetHtml.setRel("snippet/html");
        expectedLinks.add(snippetHtml);

        Link snippetMarkdown = DtoFactory.getInstance().createDto(Link.class);
        snippetMarkdown.setProduces("text/plain");
        snippetMarkdown.setHref(getServerUrl(context) + "/rest/factory/" + CORRECT_FACTORY_ID +
                                "/snippet?type=markdown");
        snippetMarkdown.setRel("snippet/markdown");
        expectedLinks.add(snippetMarkdown);
        
        Link snippetiFrame = DtoFactory.getInstance().createDto(Link.class);
        snippetiFrame.setProduces("text/plain");
        snippetiFrame.setHref(getServerUrl(context) + "/rest/factory/" + CORRECT_FACTORY_ID +
                                "/snippet?type=iframe");
        snippetiFrame.setRel("snippet/iframe");
        expectedLinks.add(snippetiFrame);

        for (Link link : responseFactoryUrl.getLinks()) {
            Link testLink = DtoFactory.getInstance().createDto(Link.class);
            testLink.setProduces(link.getProduces());
            testLink.setHref(link.getHref());
            testLink.setRel(link.getRel());
            //This transposition need because proxy objects doesn't contains equals method.
            assertTrue(expectedLinks.contains(testLink));
        }
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
        when(factoryStore.getFactory(CORRECT_FACTORY_ID)).thenReturn(DtoFactory.getInstance().createDto
                (Factory.class));

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
        when(factoryStore.getFactory(CORRECT_FACTORY_ID)).thenReturn(DtoFactory.getInstance().createDto
                (Factory.class));

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
        when(factoryStore.getFactory(CORRECT_FACTORY_ID)).thenReturn(DtoFactory.getInstance().createDto
                (Factory.class));

        // when, then
        Response response = given().//
                expect().//
                statusCode(200).//
                contentType(MediaType.TEXT_PLAIN).//
                when().//
                get(SERVICE_PATH + "/" + CORRECT_FACTORY_ID + "/snippet?type=html");

        assertEquals(response.body().asString(), "<script type=\"text/javascript\" style=\"null\" src=\"" + getServerUrl(context) +
                                                 "/factory/resources/factory.js?" + CORRECT_FACTORY_ID + "\"></script>");
    }

    @Test
    public void shouldBeAbleToReturnMarkdownSnippetWithImage(ITestContext context) throws Exception {
        // given
        String imageName = "1241234";
        Factory furl = DtoFactory.getInstance().createDto(Factory.class);
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
                equalTo("[![alt](" + getServerUrl(context) + "/api/factory/" + CORRECT_FACTORY_ID + "/image?imgId=" +
                        imageName + ")](" +
                        getServerUrl(context) + "/factory?id=" +
                        CORRECT_FACTORY_ID + ")")).//
                when().//
                get(SERVICE_PATH + "/" + CORRECT_FACTORY_ID + "/snippet?type=markdown");
    }

    @Test
    public void shouldBeAbleToReturnMarkdownSnippetWithoutImage(ITestContext context) throws Exception {
        // given
        Factory furl = DtoFactory.getInstance().createDto(Factory.class);
        furl.setStyle("White");

        when(factoryStore.getFactory(CORRECT_FACTORY_ID)).thenReturn(furl);
        // when, then
        given().//
                expect().//
                statusCode(200).//
                contentType(MediaType.TEXT_PLAIN).//
                body(
                equalTo("[![alt](" + getServerUrl(context) + "/factory/resources/factory-white.png)](" + getServerUrl
                        (context) +
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
        when(factoryStore.getFactory(CORRECT_FACTORY_ID)).thenReturn(DtoFactory.getInstance().createDto(Factory.class));

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


    @Test
    public void shouldNotFindWhenNoAttributesProvided() throws Exception {
        // when
        Response response =
                given().auth().basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD).when().get(
                        "/private" + SERVICE_PATH + "/find");
        // then
        assertEquals(response.getStatusCode(), 500);
    }

    @Test
    public void shoutFindByAttribute() throws Exception {
        // given
        Factory factoryUrl = DtoFactory.getInstance().createDto(Factory.class);
        factoryUrl.setId(CORRECT_FACTORY_ID);
        factoryUrl.setCommitid("12345679");
        factoryUrl.setOrgid("testorg");
        factoryUrl.setVcs("git");
        factoryUrl.setV("1.1");
        factoryUrl.setVcsurl("git@github.com:codenvy/cloud-ide.git");
        when(factoryStore.findByAttribute(Pair.of("orgid", "testorg"))).thenReturn(
                Arrays.asList(factoryUrl, factoryUrl));

        // when
        Response response = given().auth().basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD).
                when().get("/private" + SERVICE_PATH + "/find?accountid=testorg" );

        // then
        assertEquals(response.getStatusCode(), 200);
        List<Link> responseLinks = DtoFactory.getInstance().createListDtoFromJson(response.getBody().asInputStream(), Link.class);
        assertEquals(responseLinks.size(), 2);
    }
}
