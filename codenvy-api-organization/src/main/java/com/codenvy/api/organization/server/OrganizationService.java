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
import com.codenvy.api.organization.shared.dto.OrganizationMembership;
import com.codenvy.api.organization.shared.dto.Subscription;
import com.codenvy.api.organization.shared.dto.Attribute;
import com.codenvy.api.core.rest.Service;
import com.codenvy.api.core.rest.annotations.Description;
import com.codenvy.api.core.rest.annotations.GenerateLink;
import com.codenvy.api.core.rest.annotations.Required;
import com.codenvy.api.core.rest.shared.dto.Link;
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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
        if (organizationDao.getByOwner(current.getId()).size() != 0) {
            throw OrganizationAlreadyExistsException.existsWithOwner(current.getId());
        }
        if (organizationDao.getByName(newOrganization.getName()) != null) {
            throw OrganizationAlreadyExistsException.existsWithName(newOrganization.getName());
        }
        if (newOrganization.getAttributes() != null) {
            for (Attribute attribute : newOrganization.getAttributes()) {
                validateAttributeName(attribute.getName());
            }
        }
        String organizationId = NameGenerator.generate(Organization.class.getSimpleName().toLowerCase(), Constants.ID_LENGTH);
        newOrganization.setId(organizationId);
        //organization should have owner
        newOrganization.setOwner(current.getId());
        organizationDao.create(newOrganization);
        injectLinks(newOrganization, securityContext);
        return Response.status(Response.Status.CREATED).entity(newOrganization).build();
    }

    @GET
    @GenerateLink(rel = Constants.LINK_REL_GET_ORGANIZATIONS)
    @RolesAllowed("user")
    @Produces(MediaType.APPLICATION_JSON)
    public List<OrganizationMembership> getMemberships(@Context SecurityContext securityContext)
            throws UserException, OrganizationException {
        final Principal principal = securityContext.getUserPrincipal();
        final User current = userDao.getByAlias(principal.getName());
        if (current == null) {
            throw new UserNotFoundException(principal.getName());
        }
        final List<OrganizationMembership> result = convert(organizationDao.getByMember(current.getId()), "organization/member");
        result.addAll(convert(organizationDao.getByOwner(current.getId()), "organization/owner"));
        for (Organization organization : result) {
            injectLinks(organization, securityContext);
        }
        return result;
    }

    @GET
    @Path("list")
    @GenerateLink(rel = Constants.LINK_REL_GET_ORGANIZATIONS)
    @RolesAllowed({"system/admin", "system/manager"})
    @Produces(MediaType.APPLICATION_JSON)
    public List<OrganizationMembership> getMembershipsOfSpecificUser(
            @Required @Description("User id to get organizations") @QueryParam("userid") String userId,
            @Context SecurityContext securityContext)
            throws UserException, OrganizationException {
        if (userId == null) {
            throw new OrganizationException("Missed userid to search");
        }
        final User user = userDao.getById(userId);
        if (user == null) {
            throw new UserNotFoundException(userId);
        }
        final List<OrganizationMembership> result = convert(organizationDao.getByMember(user.getId()), "organization/member");
        result.addAll(convert(organizationDao.getByOwner(user.getId()), "organization/owner"));
        for (Organization organization : result) {
            injectLinks(organization, securityContext);
        }
        return result;
    }

    @POST
    @Path("{id}/attribute")
    @GenerateLink(rel = Constants.LINK_REL_ADD_ATTRIBUTE)
    @RolesAllowed({"user", "system/admin", "system/manager"})
    @Consumes(MediaType.APPLICATION_JSON)
    public void addAttribute(@PathParam("id") String organizationId, @Required @Description("New attribute") Attribute newAttribute,
                             @Context SecurityContext securityContext)
            throws OrganizationException, UserException {
        final Organization organization = organizationDao.getById(organizationId);
        if (organization == null) {
            throw OrganizationNotFoundException.doesNotExistWithId(organizationId);
        }
        if (newAttribute == null) {
            throw new OrganizationException("Attribute required");
        }
        ensureCurrentUserIsOrganizationOwner(organization, securityContext);
        validateAttributeName(newAttribute.getName());
        final List<Attribute> actual = organization.getAttributes();
        removeAttribute(actual, newAttribute.getName());
        actual.add(newAttribute);
        organizationDao.update(organization);
    }

    @DELETE
    @Path("{id}/attribute")
    @GenerateLink(rel = Constants.LINK_REL_REMOVE_ATTRIBUTE)
    @RolesAllowed({"user", "system/admin", "system/manager"})
    public void removeAttribute(@PathParam("id") String organizationId,
                                @Required @Description("The name of attribute") @QueryParam("name") String attributeName,
                                @Context SecurityContext securityContext)
            throws OrganizationException, UserException {
        final Organization organization = organizationDao.getById(organizationId);
        if (organization == null) {
            throw OrganizationNotFoundException.doesNotExistWithId(organizationId);
        }
        ensureCurrentUserIsOrganizationOwner(organization, securityContext);
        validateAttributeName(attributeName);
        removeAttribute(organization.getAttributes(), attributeName);
        organizationDao.update(organization);
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
    @RolesAllowed({"user", "system/admin", "system/manager"})
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
        ensureCurrentUserIsOrganizationOwner(organization, securityContext);
        if (userDao.getById(userId) == null) {
            throw new UserNotFoundException(userId);
        }
        Member newMember = DtoFactory.getInstance().createDto(Member.class).withOrganizationId(organizationId).withUserId(userId);
        newMember.setRoles(Arrays.asList("organization/member"));
        organizationDao.addMember(newMember);
    }

    @GET
    @Path("{id}/members")
    @GenerateLink(rel = Constants.LINK_REL_GET_MEMBERS)
    @RolesAllowed({"user", "system/admin", "system/manager"})
    @Produces(MediaType.APPLICATION_JSON)
    public List<Member> getMembers(@PathParam("id") String id, @Context SecurityContext securityContext)
            throws OrganizationException, UserException {
        final Organization organization = organizationDao.getById(id);
        if (organization == null) {
            throw OrganizationNotFoundException.doesNotExistWithId(id);
        }
        ensureCurrentUserIsOrganizationOwner(organization, securityContext);
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
    @RolesAllowed({"user", "system/admin", "system/manager"})
    public void removeMember(@Context SecurityContext securityContext, @PathParam("id") String organizationId,
                             @PathParam("userid") String userid) throws OrganizationException, UserException {
        final Organization organization = organizationDao.getById(organizationId);
        if (organizationDao.getById(organizationId) == null) {
            throw OrganizationNotFoundException.doesNotExistWithId(organizationId);
        }
        ensureCurrentUserIsOrganizationOwner(organization, securityContext);
        organizationDao.removeMember(organizationId, userid);
    }

    @POST
    @Path("{id}")
    @GenerateLink(rel = Constants.LINK_REL_UPDATE_ORGANIZATION)
    @RolesAllowed({"user"})
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Organization update(@PathParam("id") String id,
                               @Required @Description("Organization to update") Organization organizationToUpdate,
                               @Context SecurityContext securityContext)
            throws OrganizationException, UserException {
        final Principal principal = securityContext.getUserPrincipal();
        final User current = userDao.getByAlias(principal.getName());
        if (current == null) {
            throw new UserNotFoundException(principal.getName());
        }
        if (organizationToUpdate == null) {
            throw new OrganizationException("Missed organization to update");
        }
        final Organization actual = organizationDao.getById(id);
        if (actual == null) {
            throw OrganizationNotFoundException.doesNotExistWithId(id);
        }
        if (!actual.getOwner().equals(current.getId())) {
            throw new OrganizationIllegalAccessException(current.getId());
        }
        if (organizationToUpdate.getOwner() == null) {
            organizationToUpdate.setOwner(actual.getOwner());
        }
        if (organizationToUpdate.getName() == null) {
            organizationToUpdate.setName(actual.getName());
        }
        //TODO replace with subscriptions check later
        //if organization owner changed
        if (!actual.getOwner().equals(organizationToUpdate.getOwner()) &&
            organizationDao.getByOwner(organizationToUpdate.getOwner()) != null) {
            throw new OrganizationException("For now one user may be owner of one organization");
        }
        //if organization name changed
        if (!actual.getName().equals(organizationToUpdate.getName()) && organizationDao.getByName(organizationToUpdate.getName()) != null) {
            throw OrganizationAlreadyExistsException.existsWithName(organizationToUpdate.getName());
        }
        //add new attributes and rewrite existed with same name
        if (organizationToUpdate.getAttributes() != null) {
            Map<String, Attribute> updates = new LinkedHashMap<>(organizationToUpdate.getAttributes().size());
            for (Attribute toUpdate : organizationToUpdate.getAttributes()) {
                validateAttributeName(toUpdate.getName());
                updates.put(toUpdate.getName(), toUpdate);
            }
            for (Iterator<Attribute> it = actual.getAttributes().iterator(); it.hasNext(); ) {
                Attribute attribute = it.next();
                if (updates.containsKey(attribute.getName())) {
                    it.remove();
                }
            }
            actual.getAttributes().addAll(organizationToUpdate.getAttributes());
        }
        actual.setOwner(organizationToUpdate.getOwner());
        actual.setName(organizationToUpdate.getName());
        organizationDao.update(actual);
        injectLinks(actual, securityContext);
        return actual;
    }

    @GET
    @Path("{id}/subscriptions")
    @GenerateLink(rel = Constants.LINK_REL_GET_SUBSCRIPTIONS)
    @RolesAllowed({"user", "system/admin", "system/manager"})
    @Produces(MediaType.APPLICATION_JSON)
    public List<Subscription> getSubscriptions(@PathParam("id") String organizationId,
                                               @Context SecurityContext securityContext)
            throws OrganizationException, UserException {
        if (organizationDao.getById(organizationId) == null) {
            throw OrganizationNotFoundException.doesNotExistWithId(organizationId);
        }
        if (securityContext.isUserInRole("user")) {
            List<OrganizationMembership> currentUserOrganizations = getMemberships(securityContext);
            boolean isOrganizationPresent = false;
            for (int i = 0; i < currentUserOrganizations.size() && !isOrganizationPresent; ++i) {
                isOrganizationPresent = currentUserOrganizations.get(i).getId().equals(organizationId);
            }
            if (!isOrganizationPresent) {
                throw new OrganizationException("Access denied");
            }
        }
        final UriBuilder uriBuilder = getServiceContext().getServiceUriBuilder();
        final List<Subscription> subscriptions = organizationDao.getSubscriptions(organizationId);
        for (Subscription subscription : subscriptions) {
            subscription.setLinks(Arrays.asList(createLink("DELETE", Constants.LINK_REL_REMOVE_SUBSCRIPTION, null, null,
                                                           uriBuilder.clone().path(getClass(), "removeSubscription")
                                                                     .build(subscription.getId()).toString())));
        }
        return subscriptions;
    }

    @POST
    @Path("subscriptions")
    @GenerateLink(rel = Constants.LINK_REL_ADD_SUBSCRIPTION)
    @RolesAllowed({"system/admin", "system/manager"})
    @Consumes(MediaType.APPLICATION_JSON)
    public void addSubscription(@Required @Description("subscription to add") Subscription subscription)
            throws OrganizationException {
        if (subscription == null) {
            throw new OrganizationException("Missed subscription");
        }
        if (organizationDao.getById(subscription.getOrganizationId()) == null) {
            throw OrganizationNotFoundException.doesNotExistWithId(subscription.getOrganizationId());
        }
        SubscriptionService service = registry.get(subscription.getServiceId());
        if (service == null) {
            throw new ServiceNotFoundException(subscription.getServiceId());
        }
        String subscriptionId = NameGenerator.generate(Subscription.class.getSimpleName().toLowerCase(), Constants.ID_LENGTH);
        subscription.setId(subscriptionId);
        organizationDao.addSubscription(subscription);
        service.notifyHandlers(new SubscriptionEvent(subscription, SubscriptionEvent.EventType.CREATE));
    }

    @DELETE
    @Path("subscriptions/{id}")
    @GenerateLink(rel = Constants.LINK_REL_REMOVE_SUBSCRIPTION)
    @RolesAllowed({"system/admin", "system/manager"})
    public void removeSubscription(@PathParam("id") @Description("Subscription identifier") String subscriptionId)
            throws OrganizationException {
        Subscription toRemove = organizationDao.getSubscriptionById(subscriptionId);
        if (toRemove == null) {
            throw new SubscriptionNotFoundException(subscriptionId);
        }
        SubscriptionService service = registry.get(toRemove.getServiceId());
        if (service == null) {
            throw new ServiceNotFoundException(toRemove.getServiceId());
        }
        organizationDao.removeSubscription(subscriptionId);
        service.notifyHandlers(new SubscriptionEvent(toRemove, SubscriptionEvent.EventType.REMOVE));
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

    private void validateAttributeName(String attributeName) throws OrganizationException {
        if (attributeName == null || attributeName.isEmpty() || attributeName.toLowerCase().startsWith("codenvy")) {
            throw new OrganizationException(String.format("Attribute name '%s' is not valid", attributeName));
        }
    }

    private void removeAttribute(List<Attribute> src, String attributeName) {
        for (Iterator<Attribute> it = src.iterator(); it.hasNext(); ) {
            Attribute current = it.next();
            if (current.getName().equals(attributeName)) {
                it.remove();
                break;
            }
        }
    }

    private void ensureCurrentUserIsOrganizationOwner(Organization organization, SecurityContext securityContext)
            throws UserException, OrganizationIllegalAccessException {
        if (securityContext.isUserInRole("user")) {
            final Principal principal = securityContext.getUserPrincipal();
            final User owner = userDao.getByAlias(principal.getName());
            if (!organization.getOwner().equals(owner.getId())) {
                throw new OrganizationIllegalAccessException(organization.getId());
            }
        }
    }

    private List<OrganizationMembership> convert(List<Organization> organizations, String role) {
        final List<OrganizationMembership> result = new ArrayList<>(organizations.size());
        for (Organization organization : organizations) {
            OrganizationMembership membership = DtoFactory.getInstance().createDto(OrganizationMembership.class);
            membership.setName(organization.getName());
            membership.setId(organization.getId());
            membership.setAttributes(organization.getAttributes());
            membership.setOwner(organization.getOwner());
            membership.setRoles(Arrays.asList(role));
            result.add(membership);
        }
        return result;
    }

    private void injectLinks(Organization organization, SecurityContext securityContext) {
        final List<Link> links = new ArrayList<>();
        final UriBuilder uriBuilder = getServiceContext().getServiceUriBuilder();
        links.add(createLink("GET", Constants.LINK_REL_GET_ORGANIZATIONS, null, MediaType.APPLICATION_JSON,
                             uriBuilder.clone().path(getClass(), "getMemberships").build().toString()));
        links.add(createLink("GET", Constants.LINK_REL_GET_SUBSCRIPTIONS, null, MediaType.APPLICATION_JSON,
                             uriBuilder.clone().path(getClass(), "getSubscriptions").build(organization.getId())
                                       .toString()));
        links.add(createLink("GET", Constants.LINK_REL_GET_MEMBERS, null, MediaType.APPLICATION_JSON,
                             uriBuilder.clone().path(getClass(), "getMembers").build(organization.getId())
                                       .toString()));
        if (securityContext.isUserInRole("system/admin") || securityContext.isUserInRole("system/manager")) {
            links.add(createLink("GET", Constants.LINK_REL_GET_ORGANIZATION_BY_ID, null, MediaType.APPLICATION_JSON,
                                 uriBuilder.clone().path(getClass(), "getById").build(organization.getId()).toString()));
            links.add(createLink("GET", Constants.LINK_REL_GET_ORGANIZATION_BY_NAME, null, MediaType.APPLICATION_JSON,
                                 uriBuilder.clone().path(getClass(), "getByName").queryParam("name", organization.getName()).build()
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