/*******************************************************************************
 * Copyright (c) 2012-2015 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package com.codenvy.api.account.server;

import com.codenvy.api.account.server.dao.Account;
import com.codenvy.api.account.server.dao.AccountDao;
import com.codenvy.api.account.server.subscription.CreditCardDao;
import com.codenvy.api.account.shared.dto.CreditCard;
import com.codenvy.api.account.shared.dto.Nonce;
import com.codenvy.api.core.ForbiddenException;
import com.codenvy.api.core.NotFoundException;
import com.codenvy.api.core.ServerException;
import com.codenvy.api.core.rest.Service;
import com.codenvy.api.core.rest.annotations.Required;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;

/**
 * @author Max Shaposhnik (mshaposhnik@codenvy.com) on 1/28/15.
 * @version $Id: $
 */
@Path("/creditcard")
public class CreditCardService extends Service {

    private final CreditCardDao creditCardDao;
    private final AccountDao    accountDao;


    @Inject
    public CreditCardService(AccountDao accountDao,
                             CreditCardDao creditCardDao) {
        this.accountDao = accountDao;
        this.creditCardDao = creditCardDao;
    }

    @ApiOperation(value = "Client token",
            notes = "Get client token. Roles: account/owner, system/admin, system/manager.",
            position = 14)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 403, message = "Access denied"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @POST
    @Path("/{id}/token")
    @RolesAllowed({"account/owner", "system/admin", "system/manager"})
    public String getClientToken(@ApiParam(value = "Account ID", required = true)
                                 @PathParam("id") String accountId,
                                 @ApiParam(value = "Client nonce", required = true)
                                 @Required Nonce nonce)
            throws NotFoundException, ServerException, ForbiddenException {

        return creditCardDao.clientToken(accountId);
    }


    @ApiOperation(value = "Add credit card",
            notes = "Add credit card to account. Roles: account/owner, system/admin, system/manager.",
            position = 14)
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Created"),
            @ApiResponse(code = 403, message = "Access denied"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @POST
    @Path("/{id}/")
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed({"account/owner", "system/admin", "system/manager"})
    public void addCreditCardToAccount(@ApiParam(value = "Account ID", required = true)
                                       @PathParam("id") String accountId,
                                       @ApiParam(value = "Client nonce", required = true)
                                       @Required Nonce nonce)
            throws NotFoundException, ServerException, ForbiddenException {

        creditCardDao.registerCard(accountId, nonce.getNonce());

        final Account account = accountDao.getById(accountId);
        account.getAttributes().put("codenvy:paid", "true");
        accountDao.update(account);
    }

    @ApiOperation(value = "Get credit cards",
            notes = "Get all credit cards registered to account. Roles: account/owner, system/admin, system/manager.",
            position = 14)
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Created"),
            @ApiResponse(code = 403, message = "Access denied"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @GET
    @Path("/{id}/")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"account/owner", "system/admin", "system/manager"})
    public List<CreditCard> getCreditCardsOfAccount(@ApiParam(value = "Account ID", required = true)
                                                    @PathParam("id") String accountId)
            throws NotFoundException, ServerException, ForbiddenException {

        List<CreditCard> storedCards = creditCardDao.getCards(accountId);
        for (CreditCard card : storedCards) {
            card.setToken("");
        }
        return storedCards;
    }


    @ApiOperation(value = "Remove credit card",
            notes = "Remove credit card and make account free. Roles: account/owner, system/admin, system/manager.",
            position = 15)
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Created"),
            @ApiResponse(code = 403, message = "Access denied"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @DELETE
    @Path("/{id}/")
    @RolesAllowed({"account/owner", "system/admin", "system/manager"})
    public void removeCreditCardFromAccount(@ApiParam(value = "Account ID", required = true)
                                            @PathParam("id") String accountId,
                                            @Required CreditCard card) throws NotFoundException, ServerException,
                                                                              ForbiddenException {

        for (CreditCard storedCard : creditCardDao.getCards(accountId)) {
            if (storedCard.getNumber().equals(card.getNumber())) {
                creditCardDao.deleteCard(accountId, storedCard.getToken());
            }
        }

        // TODO charge if user has consumed paid resources -- это еще нужно ?
        // TODO send email -- а это ?
        if (creditCardDao.getCards(accountId).isEmpty()) {
            final Account account = accountDao.getById(accountId);
            account.getAttributes().remove("codenvy:paid");
            accountDao.update(account);
        }
    }

}
