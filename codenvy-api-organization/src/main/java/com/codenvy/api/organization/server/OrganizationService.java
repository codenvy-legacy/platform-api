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
package com.codenvy.api.organization.server;


import com.codenvy.api.organization.server.dao.OrganizationDao;
import com.codenvy.api.organization.server.exception.OrganizationAlreadyExistsException;
import com.codenvy.api.organization.server.exception.OrganizationIllegalAccessException;
import com.codenvy.api.organization.server.exception.OrganizationException;
import com.codenvy.api.organization.server.exception.OrganizationNotFoundException;
import com.codenvy.api.organization.server.exception.ServiceNotFoundException;
import com.codenvy.api.organization.server.exception.SubscriptionNotFoundException;
import com.codenvy.api.organization.shared.dto.Member;
import com.codenvy.api.organization.shared.dto.Organization;
import com.codenvy.api.organization.shared.dto.Subscription;
import com.codenvy.api.core.rest.Service;
import com.codenvy.api.core.rest.annotations.Description;
import com.codenvy.api.core.rest.annotations.GenerateLink;
import com.codenvy.api.core.rest.annotations.Required;
import com.codenvy.api.core.rest.shared.dto.Link;
import com.codenvy.api.core.rest.shared.dto.LinkParameter;
import com.codenvy.api.user.server.dao.UserDao;
import com.codenvy.api.user.server.exception.UserException;
import com.codenvy.api.user.server.exception.UserNotFoundException;
import com.codenvy.api.user.shared.dto.User;
import com.codenvy.commons.lang.NameGenerator;
import com.codenvy.dto.server.DtoFactory;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriBuilder;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Organization API
 *
 * @author Eugene Voevodin
 */
@Path("organization")
public class OrganizationService extends Service {

    private final OrganizationDao             organizationDao;
    private final UserDao                     userDao;
    private final SubscriptionServiceRegistry registry;

    @Inject
    public OrganizationService(OrganizationDao organizationDao, UserDao userDao, SubscriptionServiceRegistry registry) {
        this.organizationDao = organizationDao;
        this.userDao = userDao;
        this.registry = registry;
    }

    @POST
    @GenerateLink(rel = Constants.LINK_REL_CREATE_ORGANIZATION)
    @RolesAllowed("user")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response create(@Context SecurityContext securityContext,
                           @Required @Description("Organization to create") Organization newOrganization)
            throws OrganizationException, UserException {
        if (newOrganization == null) {
            throw new OrganizationException("Missed organization to create");
        }
        final Principal principal = securityContext.getUserPrincipal();
        final User current = userDao.getByAlias(principal.getName());
        if (current == null) {
            throw new UserNotFoundException(principal.getName());
        }
        if (organizationDao.getByOwner(current.getId()) != null) {
            throw OrganizationAlreadyExistsException.existsWithOwner(current.getId());
        }
        if (organizationDao.getByName(newOrganization.getName()) != null) {
            throw OrganizationAlreadyExistsException.existsWithName(newOrganization.getName());
        }
        String organizationId = NameGenerator.generate(Organization.class.getSimpleName(), Constants.ID_LENGTH);
        newOrganization.setId(organizationId);
        //organization should have owner
        newOrganization.setOwner(current.getId());
        organizationDao.create(newOrganization);
        injectLinks(newOrganization, securityContext);
        return Response.status(Response.Status.CREATED).entity(newOrganization).build();
    }

    @GET
    @GenerateLink(rel = Constants.LINK_REL_GET_CURRENT_ORGANIZATION)
    @RolesAllowed("user")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Organization> getCurrent(@Context SecurityContext securityContext) throws UserException, OrganizationException {
        final Principal principal = securityContext.getUserPrincipal();
        final User current = userDao.getByAlias(principal.getName());
        if (current == null) {
            throw new UserNotFoundException(principal.getName());
        }
        List<Organization> currentOrganizations = organizationDao.getByMember(current.getId());
        if (currentOrganizations == null) {
            currentOrganizations = new ArrayList<>();
        }
        final Organization organization = organizationDao.getByOwner(current.getId());
        if (organization != null) {
            currentOrganizations.add(organization);
        }
        injectLinks(organization, securityContext);
        return currentOrganizations;
    }

    @GET
    @Path("{id}")
    @GenerateLink(rel = Constants.LINK_REL_GET_ORGANIZATION_BY_ID)
    @RolesAllowed({"system/admin", "system/manager"})
    @Produces(MediaType.APPLICATION_JSON)
    public Organization getById(@Context SecurityContext securityContext, @PathParam("id") String id)
            throws OrganizationException, UserException {
        final Organization organization = organizationDao.getById(id);
        if (organization == null) {
            throw OrganizationNotFoundException.doesNotExistWithId(id);
        }
        injectLinks(organization, securityContext);
        return organization;
    }

    @GET
    @Path("find")
    @GenerateLink(rel = Constants.LINK_REL_GET_ORGANIZATION_BY_NAME)
    @RolesAllowed({"system/admin", "system/manager"})
    @Produces(MediaType.APPLICATION_JSON)
    public Organization getByName(@Context SecurityContext securityContext,
                                  @Required @Description("Organization name to search") @QueryParam("name") String name)
            throws OrganizationException, UserException {
        if (name == null) {
            throw new OrganizationException("Missed organization name");
        }
        final Organization organization = organizationDao.getByName(name);
        if (organization == null) {
            throw OrganizationNotFoundException.doesNotExistWithName(name);
        }
        injectLinks(organization, securityContext);
        return organization;
    }

    @POST
    @Path("{id}/members")
    @GenerateLink(rel = Constants.LINK_REL_ADD_MEMBER)
    @RolesAllowed({"organization/owner", "system/admin", "system/manager"})
    public void addMember(@Context SecurityContext securityContext, @PathParam("id") String organizationId,
                          @Required @Description("User id to be a new organization member") @QueryParam("userid") String userId)
            throws UserException, OrganizationException {
        if (userId == null) {
            throw new OrganizationException("Missed user id");
        }
        final Organization organization = organizationDao.getById(organizationId);
        if (organization == null) {
            throw OrganizationNotFoundException.doesNotExistWithId(organizationId);
        }
        final Principal principal = securityContext.getUserPrincipal();
        if (securityContext.isUserInRole("organization/owner")) {
            User owner = userDao.getByAlias(principal.getName());
            if (!organization.getOwner().equals(owner.getId())) {
                throw new OrganizationIllegalAccessException(organization.getId());
            }
        }
        if (userDao.getById(userId) == null) {
            throw new UserNotFoundException(userId);
        }
        Member newMember = DtoFactory.getInstance().createDto(Member.class).withOrganizationId(organizationId).withUserId(userId);
        newMember.setRoles(Arrays.asList("organization/member"));
        organizationDao.addMember(newMember);
    }

    @GET
    @Path("members")
    @GenerateLink(rel = Constants.LINK_REL_GET_MEMBERS)
    @RolesAllowed({"user"})
    @Produces(MediaType.APPLICATION_JSON)
    public List<Member> getMembersOfCurrentOrganization(@Context SecurityContext securityContext)
            throws UserException, OrganizationException {
        final Principal principal = securityContext.getUserPrincipal();
        final User current = userDao.getByAlias(principal.getName());
        if (current == null) {
            throw new UserNotFoundException(principal.getName());
        }
        final Organization organization = organizationDao.getByOwner(current.getId());
        if (organization == null) {
            throw OrganizationNotFoundException.doesNotExistWithOwner(current.getId());
        }
        final UriBuilder uriBuilder = getServiceContext().getServiceUriBuilder();
        final List<Member> members = organizationDao.getMembers(organization.getId());
        //injecting links
        for (Member member : members) {
            member.setLinks(Arrays.asList(createLink("DELETE", Constants.LINK_REL_REMOVE_MEMBER, null, null,
                                                     uriBuilder.clone().path(getClass(), "removeMember")
                                                               .build(organization.getId(), member.getUserId()).toString())));
        }
        return members;
    }

    @GET
    @Path("{id}/members")
    @GenerateLink(rel = Constants.LINK_REL_GET_MEMBERS)
    @RolesAllowed({"system/admin", "system/manager"})
    @Produces(MediaType.APPLICATION_JSON)
    public List<Member> getMembersOfSpecificOrganization(@PathParam("id") String id) throws OrganizationException, UserException {
        if (organizationDao.getById(id) == null) {
            throw OrganizationNotFoundException.doesNotExistWithId(id);
        }
        final UriBuilder uriBuilder = getServiceContext().getServiceUriBuilder();
        final List<Member> members = organizationDao.getMembers(id);
        for (Member member : members) {
            member.setLinks(Arrays.asList(createLink("DELETE", Constants.LINK_REL_REMOVE_MEMBER, null, null,
                                                     uriBuilder.clone().path(getClass(), "removeMember")
                                                               .build(id, member.getUserId()).toString())));
        }
        return members;
    }

    @DELETE
    @Path("{id}/members/{userid}")
    @GenerateLink(rel = Constants.LINK_REL_REMOVE_MEMBER)
    @RolesAllowed({"organization/owner", "system/admin", "system/manager"})
    public void removeMember(@Context SecurityContext securityContext, @PathParam("id") String organizationId,
                             @PathParam("userid") String userid) throws OrganizationException, UserException {
        final Organization organization = organizationDao.getById(organizationId);
        if (organizationDao.getById(organizationId) == null) {
            throw OrganizationNotFoundException.doesNotExistWithId(organizationId);
        }
        if (securityContext.isUserInRole("organization/owner")) {
            final Principal principal = securityContext.getUserPrincipal();
            final User current = userDao.getByAlias(principal.getName());
            if (!organization.getOwner().equals(current.getId())) {
                throw new OrganizationIllegalAccessException(current.getId());
            }
        }
        organizationDao.removeMember(organizationId, userid);
    }

    @POST
    @Path("{id}")
    @GenerateLink(rel = Constants.LINK_REL_UPDATE_ORGANIZATION)
    @RolesAllowed("user")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Organization update(@PathParam("id") String id,
                               @Required @Description("Organization to update") Organization organizationToUpdate)
            throws OrganizationException {
        if (organizationToUpdate == null) {
            throw new OrganizationException("Missed organization to update");
        }
        final Organization actual = organizationDao.getById(id);
        if (actual == null) {
            throw OrganizationNotFoundException.doesNotExistWithId(id);
        }
        if (!actual.getOwner().equals(organizationToUpdate.getOwner()) &&
            organizationDao.getByOwner(organizationToUpdate.getOwner()) != null) {
            throw OrganizationAlreadyExistsException.existsWithOwner(organizationToUpdate.getOwner());
        }
        if (!actual.getName().equals(organizationToUpdate.getName()) && organizationDao.getByName(organizationToUpdate.getName()) != null) {
            throw OrganizationAlreadyExistsException.existsWithName(organizationToUpdate.getName());
        }
        organizationToUpdate.setId(id);
        organizationDao.update(organizationToUpdate);
        return actual;
    }

    @GET
    @Path("subscriptions")
    @GenerateLink(rel = Constants.LINK_REL_GET_SUBSCRIPTIONS)
    @RolesAllowed("user")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Subscription> getSubscriptionsOfCurrentOrganization(@Context SecurityContext securityContext)
            throws UserException, OrganizationException {
        final Principal principal = securityContext.getUserPrincipal();
        final User current = userDao.getByAlias(principal.getName());
        if (current == null) {
            throw new UserNotFoundException(principal.getName());
        }
        final Organization organization = organizationDao.getByOwner(current.getId());
        if (organization == null) {
            throw OrganizationNotFoundException.doesNotExistWithOwner(current.getId());
        }
        return organizationDao.getSubscriptions(organization.getId());
    }

    @GET
    @Path("{id}/subscriptions")
    @GenerateLink(rel = Constants.LINK_REL_GET_SUBSCRIPTIONS)
    @RolesAllowed({"system/admin", "system/manager"})
    @Produces(MediaType.APPLICATION_JSON)
    public List<Subscription> getSubscriptionsOfSpecificOrganization(@PathParam("id") String id) throws OrganizationException {
        if (organizationDao.getById(id) == null) {
            throw OrganizationNotFoundException.doesNotExistWithId(id);
        }
        final UriBuilder uriBuilder = getServiceContext().getServiceUriBuilder();
        final List<Subscription> subscriptions = organizationDao.getSubscriptions(id);
        for (Subscription subscription : subscriptions) {
            subscription.setLinks(Arrays.asList(createLink("DELETE", Constants.LINK_REL_REMOVE_SUBSCRIPTION, null, null,
                                                           uriBuilder.clone().path(getClass(), "removeSubscription")
                                                                     .build(id, subscription.getServiceId()).toString())));
        }
        return subscriptions;
    }

    @POST
    @Path("{id}/subscriptions")
    @GenerateLink(rel = Constants.LINK_REL_ADD_SUBSCRIPTION)
    @RolesAllowed({"system/admin", "system/manager"})
    @Consumes(MediaType.APPLICATION_JSON)
    public void addSubscription(@PathParam("id") String id, @Required @Description("subscription to add") Subscription subscription)
            throws OrganizationException {
        if (subscription == null) {
            throw new OrganizationException("Missed subscription");
        }
        if (organizationDao.getById(id) == null) {
            throw OrganizationNotFoundException.doesNotExistWithId(id);
        }
        SubscriptionService service = registry.get(subscription.getServiceId());
        if (service == null) {
            throw new ServiceNotFoundException(subscription.getServiceId());
        }
        organizationDao.addSubscription(subscription, id);
        service.notifyHandlers(new SubscriptionEvent(subscription, SubscriptionEvent.EventType.CREATE));
    }

    @DELETE
    @Path("{id}/subscriptions/{serviceid}")
    @GenerateLink(rel = Constants.LINK_REL_REMOVE_SUBSCRIPTION)
    @RolesAllowed({"system/admin", "system/manager"})
    public void removeSubscription(@PathParam("serviceid") String serviceId, @PathParam("id") String organizationId)
            throws OrganizationException {
        SubscriptionService service = registry.get(serviceId);
        if (service == null) {
            throw new ServiceNotFoundException(serviceId);
        }
        List<Subscription> subscriptions = organizationDao.getSubscriptions(organizationId);
        Subscription needed = null;
        for (Subscription subscription : subscriptions) {
            if (subscription.getServiceId().equals(serviceId)) {
                needed = subscription;
                break;
            }
        }
        if (needed != null) {
            organizationDao.removeSubscription(organizationId, serviceId);
            service.notifyHandlers(new SubscriptionEvent(needed, SubscriptionEvent.EventType.REMOVE));
        } else {
            throw new SubscriptionNotFoundException(serviceId);
        }
    }

    @DELETE
    @Path("{id}")
    @GenerateLink(rel = Constants.LINK_REL_REMOVE_ORGANIZATION)
    @RolesAllowed("system/admin")
    public void remove(@PathParam("id") String id) throws OrganizationException {
        if (organizationDao.getById(id) == null) {
            throw OrganizationNotFoundException.doesNotExistWithId(id);
        }
        organizationDao.remove(id);
    }

    private void injectLinks(Organization organization, SecurityContext securityContext) {
        final List<Link> links = new ArrayList<>();
        final UriBuilder uriBuilder = getServiceContext().getServiceUriBuilder();
        if (securityContext.isUserInRole("organization/owner")) {
            links.add(createLink("GET", Constants.LINK_REL_GET_CURRENT_ORGANIZATION, null, MediaType.APPLICATION_JSON,
                                 uriBuilder.clone().path(getClass(), "getCurrent").build().toString()));
            links.add(createLink("POST", Constants.LINK_REL_UPDATE_ORGANIZATION, MediaType.APPLICATION_JSON, MediaType.APPLICATION_JSON,
                                 uriBuilder.clone().path(getClass(), "update").build(organization.getId()).toString())
                              .withParameters(Arrays.asList(DtoFactory.getInstance().createDto(LinkParameter.class)
                                                                      .withName("organizationToUpdate")
                                                                      .withRequired(true)
                                                                      .withDescription("Organization to update"))));
            links.add(createLink("GET", Constants.LINK_REL_GET_SUBSCRIPTIONS, null, MediaType.APPLICATION_JSON,
                                 uriBuilder.clone().path(getClass(), "getSubscriptionsOfCurrentOrganization").build().toString()));
            links.add(createLink("GET", Constants.LINK_REL_GET_MEMBERS, null, MediaType.APPLICATION_JSON,
                                 uriBuilder.clone().path(getClass(), "getMembersOfCurrentOrganization").build().toString()));
        }
        if (securityContext.isUserInRole("system/admin") || securityContext.isUserInRole("system/manager")) {
            links.add(createLink("GET", Constants.LINK_REL_GET_ORGANIZATION_BY_ID, null, MediaType.APPLICATION_JSON,
                                 uriBuilder.clone().path(getClass(), "getById").build(organization.getId()).toString()));
            links.add(createLink("GET", Constants.LINK_REL_GET_ORGANIZATION_BY_NAME, null, MediaType.APPLICATION_JSON,
                                 uriBuilder.clone().path(getClass(), "getByName").queryParam("name", organization.getName()).build()
                                           .toString()));
            links.add(createLink("GET", Constants.LINK_REL_GET_MEMBERS, null, MediaType.APPLICATION_JSON,
                                 uriBuilder.clone().path(getClass(), "getMembersOfSpecificOrganization").build(organization.getId())
                                           .toString()));
            links.add(createLink("GET", Constants.LINK_REL_GET_SUBSCRIPTIONS, null, MediaType.APPLICATION_JSON,
                                 uriBuilder.clone().path(getClass(), "getSubscriptionsOfSpecificOrganization").build(organization.getId())
                                           .toString()));

        }
        if (securityContext.isUserInRole("system/admin")) {
            links.add(createLink("DELETE", Constants.LINK_REL_REMOVE_ORGANIZATION, null, null,
                                 uriBuilder.clone().path(getClass(), "remove").build(organization.getId()).toString()));
        }
        organization.setLinks(links);
    }

    private Link createLink(String method, String rel, String consumes, String produces, String href) {
        return DtoFactory.getInstance().createDto(Link.class)
                         .withMethod(method)
                         .withRel(rel)
                         .withProduces(produces)
                         .withConsumes(consumes)
                         .withHref(href);
    }
}