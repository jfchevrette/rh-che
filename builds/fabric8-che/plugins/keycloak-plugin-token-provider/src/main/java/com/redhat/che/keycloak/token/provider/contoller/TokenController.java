/**
 * ***************************************************************************** Copyright (c) 2017
 * Red Hat inc.
 *
 * <p>All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * <p>Contributors: Red Hat - Initial Contribution
 * *****************************************************************************
 */
package com.redhat.che.keycloak.token.provider.contoller;

import com.redhat.che.keycloak.token.provider.exception.KeycloakException;
import com.redhat.che.keycloak.token.provider.oauth.OpenShiftGitHubOAuthAuthenticator;
import com.redhat.che.keycloak.token.provider.service.KeycloakTokenProvider;
import com.redhat.che.keycloak.token.provider.util.KeycloakUserValidator;
import com.redhat.che.keycloak.token.provider.validator.KeycloakTokenValidator;
import io.swagger.annotations.ApiOperation;
import java.io.IOException;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.eclipse.che.api.auth.shared.dto.OAuthToken;
import org.eclipse.che.api.core.BadRequestException;
import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.UnauthorizedException;
import org.eclipse.che.commons.env.EnvironmentContext;
import org.eclipse.che.security.oauth.OAuthAuthenticator;
import org.eclipse.che.security.oauth.OAuthAuthenticatorProvider;

@Path("/token")
@Singleton
public class TokenController {
  private static final String GIT_HUB_OAUTH_PROVIDER = "github";

  @Inject private KeycloakTokenProvider tokenProvider;

  @Inject private KeycloakTokenValidator validator;

  @Inject private KeycloakUserValidator userValidator;

  @Inject protected OAuthAuthenticatorProvider providers;

  @POST
  @Path("/github")
  @Consumes(MediaType.APPLICATION_JSON)
  public void setGitHubToken(OAuthToken token) throws ServerException {

    if (token == null) {
      throw new ServerException("No token provided");
    }

    OAuthAuthenticator provider = providers.getAuthenticator(GIT_HUB_OAUTH_PROVIDER);

    if (provider == null) {
      throw new ServerException("\"" + GIT_HUB_OAUTH_PROVIDER + "\" oauth provider not registered");
    } else if (!(provider instanceof OpenShiftGitHubOAuthAuthenticator)) {
      throw new ServerException(
          "'setToken' API is not supported by the original 'GitHubOAuthAuthenticator', 'OpenShiftGitHubOAuthAuthenticator' should be configured instead");
    }

    String userId = EnvironmentContext.getCurrent().getSubject().getUserId();

    try {
      ((OpenShiftGitHubOAuthAuthenticator) provider).setToken(userId, token);
    } catch (IOException e) {
      throw new ServerException(e.getMessage());
    }
  }

  @GET
  @Path("/github")
  @ApiOperation(value = "Get GitHub token from Keycloak token")
  public Response getGitHubToken(@HeaderParam(HttpHeaders.AUTHORIZATION) String keycloakToken)
      throws ForbiddenException, NotFoundException, ConflictException, BadRequestException,
          ServerException, UnauthorizedException, IOException {
    String token = null;
    try {
      validator.validate(keycloakToken);
      token = tokenProvider.obtainGitHubToken(keycloakToken);
    } catch (KeycloakException e) {
      return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
    }
    return Response.ok(token).build();
  }

  @GET
  @Path("/oso")
  @ApiOperation(value = "Get OpenShift token from Keycloak Token")
  public Response getOpenShiftToken(@HeaderParam(HttpHeaders.AUTHORIZATION) String keycloakToken)
      throws ForbiddenException, NotFoundException, ConflictException, BadRequestException,
          ServerException, UnauthorizedException, IOException {
    String token = null;
    try {
      validator.validate(keycloakToken);
      token = tokenProvider.obtainOsoToken(keycloakToken);
    } catch (Exception e) {
      return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
    }
    return Response.ok(token).build();
  }

  @GET
  @Path("/user")
  @ApiOperation(
    value =
        "Return true if the token provided in the authorization header corresponds to the user who owns the namespace."
  )
  public Response getUserMatches(@HeaderParam(HttpHeaders.AUTHORIZATION) String keycloakToken) {
    if (userValidator.matchesUsername(keycloakToken)) {
      return Response.ok("true").build();
    } else {
      return Response.ok("false").build();
    }
  }
}
