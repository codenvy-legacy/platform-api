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
package com.codenvy.api.organization;


import sun.security.acl.PrincipalImpl;

import com.codenvy.api.organization.server.OrganizationService;
import com.codenvy.api.organization.server.Constants;
import com.codenvy.api.organization.server.SubscriptionEvent;
import com.codenvy.api.organization.server.SubscriptionService;
import com.codenvy.api.organization.server.SubscriptionServiceRegistry;
import com.codenvy.api.organization.server.dao.OrganizationDao;
import com.codenvy.api.organization.server.exception.OrganizationException;
import com.codenvy.api.organization.shared.dto.Attribute;
import com.codenvy.api.organization.shared.dto.Member;
import com.codenvy.api.organization.shared.dto.Organization;
import com.codenvy.api.core.rest.Service;
import com.codenvy.api.core.rest.shared.dto.Link;
import com.codenvy.api.organization.shared.dto.OrganizationMembership;
import com.codenvy.api.organization.shared.dto.Subscription;
import com.codenvy.api.user.server.dao.UserDao;
import com.codenvy.api.user.shared.dto.User;
import com.codenvy.commons.json.JsonHelper;
import com.codenvy.dto.server.DtoFactory;

import org.everrest.core.impl.ApplicationContextImpl;
import org.everrest.core.impl.ApplicationProviderBinder;
import org.everrest.core.impl.ContainerResponse;
import org.everrest.core.impl.EnvironmentContext;
import org.everrest.core.impl.EverrestConfiguration;
import org.everrest.core.impl.ProviderBinder;
import org.everrest.core.impl.RequestDispatcher;
import org.everrest.core.impl.RequestHandlerImpl;
import org.everrest.core.impl.ResourceBinderImpl;
import org.everrest.core.tools.DependencySupplierImpl;
import org.everrest.core.tools.ResourceLauncher;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

/**
 * Tests for Organization Service
 *
 * @author Eugene Voevodin
 * @see com.codenvy.api.organization.server.OrganizationService
 */
@Listeners(value = {MockitoTestNGListener.class})
public class OrganizationServiceTest {

    private final String BASE_URI          = "http://localhost/service";
    private final String SERVICE_PATH      = BASE_URI + "/organization";
    private final String USER_ID           = "user123abc456def";
    private final String ORGANIZATION_ID   = "organization0xffffffffff";
    private final String SUBSCRIPTION_ID   = "Subscription0xffffffffff";
    private final String ORGANIZATION_NAME = "codenvy";
    private final String SERVICE_ID        = "IDE_SERVICE";

    @Mock
    private OrganizationDao organizationDao;

    @Mock
    private UserDao userDao;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private SubscriptionServiceRegistry serviceRegistry;

    @Mock
    private SubscriptionService subscriptionService;

    @Mock
    private EnvironmentContext environmentContext;

    private Organization organization;

    protected ProviderBinder     providers;
    protected ResourceBinderImpl resources;
    protected RequestHandlerImpl requestHandler;
    protected ResourceLauncher   launcher;

    @BeforeMethod
    public void setUp() throws Exception {
        resources = new ResourceBinderImpl();
        providers = new ApplicationProviderBinder();
        DependencySupplierImpl dependencies = new DependencySupplierImpl();
        dependencies.addComponent(UserDao.class, userDao);
        dependencies.addComponent(OrganizationDao.class, organizationDao);
        dependencies.addComponent(SubscriptionServiceRegistry.class, serviceRegistry);
        resources.addResource(OrganizationService.class, null);
        requestHandler = new RequestHandlerImpl(new RequestDispatcher(resources),
                                                providers, dependencies, new EverrestConfiguration());
        ApplicationContextImpl.setCurrent(new ApplicationContextImpl(null, null, ProviderBinder.getInstance()));
        launcher = new ResourceLauncher(requestHandler);
        organization = DtoFactory.getInstance().createDto(Organization.class)
                                 .withId(ORGANIZATION_ID)
                                 .withOwner(USER_ID)
                                 .withName(ORGANIZATION_NAME)
                                 .withAttributes(new ArrayList<>(Arrays.asList(DtoFactory.getInstance().createDto(Attribute.class)
                                                                                         .withName("secret")
                                                                                         .withValue("big secret")
                                                                                         .withDescription(
                                                                                                 "DON'T TELL ANYONE ABOUT IT!"))));
        String USER_EMAIL = "organization@mail.com";
        User user = DtoFactory.getInstance().createDto(User.class).withId(USER_ID).withEmail(USER_EMAIL);

        when(environmentContext.get(SecurityContext.class)).thenReturn(securityContext);
        when(securityContext.getUserPrincipal()).thenReturn(new PrincipalImpl(USER_EMAIL));
        when(userDao.getById(USER_ID)).thenReturn(user);
        when(userDao.getByAlias(USER_EMAIL)).thenReturn(user);
        when(organizationDao.getById(ORGANIZATION_ID)).thenReturn(organization);
        when(organizationDao.getByName(ORGANIZATION_NAME)).thenReturn(organization);
    }

    @Test
    public void shouldBeAbleToCreateOrganization() throws Exception {
        when(organizationDao.getByName(organization.getName())).thenReturn(null);
        String role = "user";
        prepareSecurityContext(role);

        ContainerResponse response = makeRequest("POST", SERVICE_PATH, MediaType.APPLICATION_JSON, organization);

        assertEquals(response.getStatus(), Response.Status.CREATED.getStatusCode());
        Organization created = (Organization)response.getEntity();
        verifyLinksRel(created.getLinks(), generateRels(role));
        verify(organizationDao, times(1)).create(any(Organization.class));
    }

    @Test
    public void shouldBeAbleToGetMemberships() throws Exception {
        when(organizationDao.getByOwner(USER_ID)).thenReturn(Arrays.asList(organization));
        when(organizationDao.getByMember(USER_ID)).thenReturn(new ArrayList<Organization>());

        ContainerResponse response = makeRequest("GET", SERVICE_PATH, null, null);

        assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
        //safe cast cause OrganizationService#getMemberships always returns List<OrganizationMembership
        @SuppressWarnings("unchecked") List<OrganizationMembership> currentOrganizations =
                (List<OrganizationMembership>)response.getEntity();
        assertEquals(currentOrganizations.size(), 1);
        assertEquals(currentOrganizations.get(0).getRoles().get(0), "organization/owner");
        verify(organizationDao, times(1)).getByOwner(USER_ID);
        verify(organizationDao, times(1)).getByMember(USER_ID);
    }

    @Test
    public void shouldBeAbleToGetMembershipsOfSpecificUser() throws Exception {
        when(organizationDao.getByOwner(USER_ID)).thenReturn(Arrays.asList(organization));
        when(organizationDao.getByMember(USER_ID)).thenReturn(Arrays.asList(DtoFactory.getInstance().createDto(Organization.class)
                                                                                      .withId("fake_id")
                                                                                      .withName("fake_name")
                                                                                      .withOwner("fake_user")));

        ContainerResponse response = makeRequest("GET", SERVICE_PATH + "/list?userid=" + USER_ID, null, null);
        //safe cast cause OrganizationService#getMembershipsOfSpecificUser always returns List<OrganizationMembership>
        @SuppressWarnings("unchecked") List<OrganizationMembership> currentOrganizations =
                (List<OrganizationMembership>)response.getEntity();
        assertEquals(currentOrganizations.size(), 2);
        assertEquals(currentOrganizations.get(0).getRoles().get(0), "organization/member");
        assertEquals(currentOrganizations.get(1).getRoles().get(0), "organization/owner");
        verify(organizationDao, times(1)).getByOwner(USER_ID);
        verify(organizationDao, times(1)).getByMember(USER_ID);
    }

    @Test
    public void shouldBeAbleToGetOrganizationById() throws Exception {
        String[] roles = getRoles(OrganizationService.class, "getById");

        for (String role : roles) {
            prepareSecurityContext(role);

            ContainerResponse response = makeRequest("GET", SERVICE_PATH + "/" + ORGANIZATION_ID, null, null);

            assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
            Organization actual = (Organization)response.getEntity();
            verifyLinksRel(actual.getLinks(), generateRels(role));
        }
        verify(organizationDao, times(roles.length)).getById(ORGANIZATION_ID);
    }

    @Test
    public void shouldBeAbleToUpdateOrganization() throws Exception {
        List<Attribute> attributes = Arrays.asList(DtoFactory.getInstance().createDto(Attribute.class)
                                                             .withName("newAttribute")
                                                             .withValue("someValue")
                                                             .withDescription("Description"));
        Organization toUpdate = DtoFactory.getInstance().createDto(Organization.class)
                                          .withName("newName")
                                          .withAttributes(attributes);

        prepareSecurityContext("user");
        ContainerResponse response = makeRequest("POST", SERVICE_PATH + "/" + ORGANIZATION_ID, MediaType.APPLICATION_JSON, toUpdate);

        assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
        Organization actual = (Organization)response.getEntity();
        assertEquals(actual.getAttributes().size(), 2);
        assertEquals(actual.getName(), "newName");
        assertEquals(actual.getOwner(), USER_ID);
    }

    @Test
    public void shouldBeAbleToRewriteAttributesWhenUpdatingOrganization() throws Exception {
        List<Attribute> currentAttributes = new ArrayList<>();
        currentAttributes.add(DtoFactory.getInstance().createDto(Attribute.class)
                                        .withName("newAttribute")
                                        .withValue("someValue")
                                        .withDescription("Description"));
        currentAttributes.add(DtoFactory.getInstance().createDto(Attribute.class)
                                        .withName("oldAttribute")
                                        .withValue("oldValue")
                                        .withDescription("Description"));
        organization.setAttributes(currentAttributes);

        List<Attribute> updates = new ArrayList<>(Arrays.asList(DtoFactory.getInstance().createDto(Attribute.class)
                                                                          .withName("newAttribute")
                                                                          .withValue("OTHER_VALUE")
                                                                          .withDescription("Description"),
                                                                DtoFactory.getInstance().createDto(Attribute.class)
                                                                          .withName("newAttribute2")
                                                                          .withValue("someValue2")
                                                                          .withDescription("Description2")));

        Organization toUpdate = DtoFactory.getInstance().createDto(Organization.class).withAttributes(updates);

        prepareSecurityContext("user");
        ContainerResponse response = makeRequest("POST", SERVICE_PATH + "/" + ORGANIZATION_ID, MediaType.APPLICATION_JSON, toUpdate);

        assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
        Organization actual = (Organization)response.getEntity();
        assertEquals(actual.getName(), ORGANIZATION_NAME);
        assertEquals(actual.getOwner(), USER_ID);
        assertEquals(actual.getAttributes().size(), 3);
        for (Attribute attribute : actual.getAttributes()) {
            if (attribute.getName().equals("newAttribute") && !attribute.getValue().equals("OTHER_VALUE")) {
                fail("Attribute should be replaced");
            }
        }
    }

    @Test
    public void shouldBeAbleToAddNewAttribute() throws Exception {
        Attribute newAttribute = DtoFactory.getInstance().createDto(Attribute.class)
                                           .withName("newAttribute")
                                           .withValue("someValue")
                                           .withDescription("Description");
        int countBefore = organization.getAttributes().size();

        ContainerResponse response =
                makeRequest("POST", SERVICE_PATH + "/" + ORGANIZATION_ID + "/attribute", MediaType.APPLICATION_JSON, newAttribute);

        assertEquals(response.getStatus(), Response.Status.NO_CONTENT.getStatusCode());
        assertEquals(organization.getAttributes().size(), countBefore + 1);
        verify(organizationDao, times(1)).update(organization);
    }

    @Test
    public void shouldNotBeAbleToAddAttributeWithIncorrectName() throws Exception {
        Attribute newAttribute = DtoFactory.getInstance().createDto(Attribute.class)
                                           .withName("codenvy_newAttribute")
                                           .withValue("someValue")
                                           .withDescription("Description");
        ContainerResponse response =
                makeRequest("POST", SERVICE_PATH + "/" + ORGANIZATION_ID + "/attribute", MediaType.APPLICATION_JSON, newAttribute);

        assertNotEquals(response.getStatus(), Response.Status.NO_CONTENT.getStatusCode());
        assertEquals(response.getEntity().toString(), "Attribute name 'codenvy_newAttribute' is not valid");

        newAttribute.setName("");

        response = makeRequest("POST", SERVICE_PATH + "/" + ORGANIZATION_ID + "/attribute", MediaType.APPLICATION_JSON, newAttribute);

        assertNotEquals(response.getStatus(), Response.Status.NO_CONTENT.getStatusCode());
        assertEquals(response.getEntity().toString(), "Attribute name '' is not valid");

        newAttribute.setName(null);

        response = makeRequest("POST", SERVICE_PATH + "/" + ORGANIZATION_ID + "/attribute", MediaType.APPLICATION_JSON, newAttribute);

        assertNotEquals(response.getStatus(), Response.Status.NO_CONTENT.getStatusCode());
        assertEquals(response.getEntity().toString(), "Attribute name 'null' is not valid");
    }

    @Test
    public void shouldBeAbleToRemoveAttribute() throws Exception {
        int countBefore = organization.getAttributes().size();
        assertTrue(countBefore > 0);
        Attribute existed = organization.getAttributes().get(0);
        ContainerResponse response =
                makeRequest("DELETE", SERVICE_PATH + "/" + ORGANIZATION_ID + "/attribute?name=" + existed.getName(), null, null);

        assertEquals(response.getStatus(), Response.Status.NO_CONTENT.getStatusCode());
        assertEquals(organization.getAttributes().size(), countBefore - 1);
    }

    @Test
    public void shouldNotBeAbleToUpdateOrganizationWithAlreadyExistedName() throws Exception {
        when(organizationDao.getByName("TO_UPDATE"))
                .thenReturn(DtoFactory.getInstance().createDto(Organization.class).withName("TO_UPDATE"));

        prepareSecurityContext("user");

        Organization toUpdate = DtoFactory.getInstance().createDto(Organization.class).withName("TO_UPDATE");

        ContainerResponse response = makeRequest("POST", SERVICE_PATH + "/" + ORGANIZATION_ID, MediaType.APPLICATION_JSON, toUpdate);
        assertNotEquals(response.getStatus(), Response.Status.OK);
        assertEquals(response.getEntity().toString(), "Organization with name TO_UPDATE already exists");
    }

    @Test
    public void shouldBeAbleToGetOrganizationByName() throws Exception {
        String[] roles = getRoles(OrganizationService.class, "getByName");
        for (String role : roles) {
            prepareSecurityContext(role);

            ContainerResponse response = makeRequest("GET", SERVICE_PATH + "/find?name=" + ORGANIZATION_NAME, null, null);

            assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
            Organization actual = (Organization)response.getEntity();
            verifyLinksRel(actual.getLinks(), generateRels(role));
        }
        verify(organizationDao, times(roles.length)).getByName(ORGANIZATION_NAME);
    }

    @Test
    public void shouldBeAbleToGetSubscriptionsOfSpecificOrganization() throws Exception {
        when(organizationDao.getSubscriptions(ORGANIZATION_ID)).thenReturn(Arrays.asList(
                DtoFactory.getInstance().createDto(Subscription.class)
                          .withId(SUBSCRIPTION_ID)
                          .withStartDate(System.currentTimeMillis())
                          .withEndDate(System.currentTimeMillis())
                          .withServiceId(SERVICE_ID)
                          .withProperties(Collections.<String, String>emptyMap())));

        prepareSecurityContext("system/admin");

        ContainerResponse response = makeRequest("GET", SERVICE_PATH + "/" + ORGANIZATION_ID + "/subscriptions", null, null);

        assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
        //safe cast cause OrganizationService#getSubscriptions always returns List<Subscription>
        @SuppressWarnings("unchecked") List<Subscription> subscriptions = (List<Subscription>)response.getEntity();
        assertEquals(subscriptions.size(), 1);
        assertEquals(subscriptions.get(0).getLinks().size(), 1);
        Link removeSubscription = subscriptions.get(0).getLinks().get(0);
        assertEquals(removeSubscription, DtoFactory.getInstance().createDto(Link.class)
                                                   .withRel(Constants.LINK_REL_REMOVE_SUBSCRIPTION)
                                                   .withMethod("DELETE")
                                                   .withHref(SERVICE_PATH + "/subscriptions/" + SUBSCRIPTION_ID));
        verify(organizationDao, times(1)).getSubscriptions(ORGANIZATION_ID);
    }

    @Test
    public void shouldNotBeAbleToGetSubscriptionsFromOrganizationWhereCurrentUserIsNotMember() throws Exception {
        when(organizationDao.getByMember(USER_ID)).thenReturn(new ArrayList<Organization>());
        when(organizationDao.getByOwner(USER_ID))
                .thenReturn(Arrays.asList(DtoFactory.getInstance().createDto(Organization.class).withId("NOT_SAME")));

        prepareSecurityContext("user");

        ContainerResponse response = makeRequest("GET", SERVICE_PATH + "/" + ORGANIZATION_ID + "/subscriptions", null, null);

        assertNotEquals(Response.Status.OK, response.getStatus());
    }

    @Test
    public void shouldBeAbleToAddSubscription() throws Exception {
        Subscription subscription = DtoFactory.getInstance().createDto(Subscription.class)
                                              .withOrganizationId(ORGANIZATION_ID)
                                              .withServiceId(SERVICE_ID)
                                              .withStartDate(System.currentTimeMillis())
                                              .withEndDate(System.currentTimeMillis())
                                              .withProperties(Collections.<String, String>emptyMap());
        when(serviceRegistry.get(SERVICE_ID)).thenReturn(subscriptionService);

        ContainerResponse response =
                makeRequest("POST", SERVICE_PATH + "/subscriptions", MediaType.APPLICATION_JSON, subscription);

        assertEquals(response.getStatus(), Response.Status.NO_CONTENT.getStatusCode());
        verify(organizationDao, times(1)).addSubscription(any(Subscription.class));
        verify(serviceRegistry, times(1)).get(SERVICE_ID);
        verify(subscriptionService, times(1)).notifyHandlers(any(SubscriptionEvent.class));
    }

    @Test
    public void shouldBeAbleToRemoveSubscription() throws Exception {
        when(serviceRegistry.get(SERVICE_ID)).thenReturn(subscriptionService);
        when(organizationDao.getSubscriptionById(SUBSCRIPTION_ID)).thenReturn(
                DtoFactory.getInstance().createDto(Subscription.class)
                          .withId(SUBSCRIPTION_ID)
                          .withStartDate(System.currentTimeMillis())
                          .withEndDate(System.currentTimeMillis())
                          .withServiceId(SERVICE_ID)
                          .withProperties(Collections.<String, String>emptyMap()));

        ContainerResponse response =
                makeRequest("DELETE", SERVICE_PATH + "/subscriptions/" + SUBSCRIPTION_ID, null, null);

        assertEquals(response.getStatus(), Response.Status.NO_CONTENT.getStatusCode());
        verify(serviceRegistry, times(1)).get(SERVICE_ID);
        verify(organizationDao, times(1)).removeSubscription(SUBSCRIPTION_ID);
        verify(subscriptionService, times(1)).notifyHandlers(any(SubscriptionEvent.class));
    }

    @Test
    public void shouldBeAbleToGetOrganizationMembers() throws Exception {
        when(organizationDao.getMembers(organization.getId()))
                .thenReturn(Arrays.asList(
                        DtoFactory.getInstance().createDto(Member.class).withRoles(Collections.<String>emptyList()).withUserId(USER_ID)
                                  .withOrganizationId(organization.getId())));

        ContainerResponse response = makeRequest("GET", SERVICE_PATH + "/" + organization.getId() + "/members", null, null);

        assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
        verify(organizationDao, times(1)).getMembers(organization.getId());
        //safe cast cause OrganizationService#getMembers always returns List<Member>
        @SuppressWarnings("unchecked") List<Member> members = (List<Member>)response.getEntity();
        assertEquals(members.size(), 1);
        Member member = members.get(0);
        assertEquals(member.getLinks().size(), 1);
        Link removeMember = members.get(0).getLinks().get(0);
        assertEquals(removeMember, DtoFactory.getInstance().createDto(Link.class)
                                             .withRel(Constants.LINK_REL_REMOVE_MEMBER)
                                             .withHref(SERVICE_PATH + "/" + member.getOrganizationId() + "/members/" + member.getUserId())
                                             .withMethod("DELETE"));
    }

    @Test
    public void shouldBeAbleToAddMember() throws Exception {
        ContainerResponse response =
                makeRequest("POST", SERVICE_PATH + "/" + organization.getId() + "/members?userid=" + USER_ID, null, null);

        assertEquals(response.getStatus(), Response.Status.NO_CONTENT.getStatusCode());
        verify(organizationDao, times(1)).addMember(any(Member.class));
    }

    @Test
    public void shouldBeAbleToRemoveMember() throws Exception {
        ContainerResponse response = makeRequest("DELETE", SERVICE_PATH + "/" + ORGANIZATION_ID + "/members/" + USER_ID, null, null);

        assertEquals(response.getStatus(), Response.Status.NO_CONTENT.getStatusCode());
        verify(organizationDao, times(1)).removeMember(ORGANIZATION_ID, USER_ID);
    }

    protected void verifyLinksRel(List<Link> links, List<String> rels) {
        assertEquals(links.size(), rels.size());
        for (String rel : rels) {
            boolean linkPresent = false;
            int i = 0;
            for (; i < links.size() && !linkPresent; i++) {
                linkPresent = links.get(i).getRel().equals(rel);
            }
            if (!linkPresent) {
                fail(String.format("Given links do not contain link with rel = %s", rel));
            }
        }
    }

    private String[] getRoles(Class<? extends Service> clazz, String methodName) {
        for (Method one : clazz.getMethods()) {
            if (one.getName().equals(methodName)) {
                if (one.isAnnotationPresent(RolesAllowed.class)) {
                    return one.getAnnotation(RolesAllowed.class).value();
                } else {
                    return new String[0];
                }
            }
        }
        throw new IllegalArgumentException(String.format("Class %s does not have method with name %s", clazz.getName(), methodName));
    }

    private List<String> generateRels(String role) {
        final List<String> rels = new LinkedList<>();
        rels.add(Constants.LINK_REL_GET_MEMBERS);
        rels.add(Constants.LINK_REL_GET_ORGANIZATIONS);
        rels.add(Constants.LINK_REL_GET_SUBSCRIPTIONS);
        switch (role) {
            case "system/admin":
                rels.add(Constants.LINK_REL_REMOVE_ORGANIZATION);
            case "system/manager":
                rels.add(Constants.LINK_REL_GET_ORGANIZATION_BY_NAME);
                rels.add(Constants.LINK_REL_GET_ORGANIZATION_BY_ID);
                break;
        }
        return rels;
    }

    protected ContainerResponse makeRequest(String method, String path, String contentType, Object toSend) throws Exception {
        Map<String, List<String>> headers = null;
        if (contentType != null) {
            headers = new HashMap<>();
            headers.put("Content-Type", Arrays.asList(contentType));
        }
        byte[] data = null;
        if (toSend != null) {
            data = JsonHelper.toJson(toSend).getBytes();
        }
        return launcher.service(method, path, BASE_URI, headers, data, null, environmentContext);
    }

    protected void prepareSecurityContext(String role) {
        when(securityContext.isUserInRole(anyString())).thenReturn(false);
        if (!role.equals("system/admin") && !role.equals("system/manager")) {
            when(securityContext.isUserInRole("user")).thenReturn(true);
        }
        when(securityContext.isUserInRole(role)).thenReturn(true);
    }
}