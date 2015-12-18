/*
 * OpenRemote, the Home of the Digital Home.
 * Copyright 2008-2015, OpenRemote Inc.
 *
 * See the contributors.txt file in the distribution for a
 * full listing of individual contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.openremote.beehive.account.service;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.openremote.beehive.account.model.rest.CustomerFulfillmentReader;
import org.openremote.beehive.account.model.rest.CustomerFulfillmentWriter;
import org.openremote.beehive.account.model.rest.UserRegistrationReader;
import org.openremote.beehive.account.model.rest.UserWriter;


/**
 * A JAX-RS 2.0 application of OpenRemote Beehive Account Manager Service. <p>
 *
 * This JAX-RS application aggregates the relevant REST resources, providers and
 * features that compose the account manager service. <p>
 *
 * This implementation uses the explicit resource registration via {@link #getClasses()} method
 * to support pre-Servlet 3.0 containers. It currently assumes a servlet container-based
 * deployment.
 *
 * @author Juha Lindfors
 */
public class AccountManager extends Application
{

  // Constants ------------------------------------------------------------------------------------

  public static final String ENTITY_MANAGER_LOOKUP = "EntityManager";



  // Class Members --------------------------------------------------------------------------------

  private static final Set<Class<?>> resourceClasses = new HashSet<Class<?>>(10);

  static
  {
    resourceClasses.add(CreateAccount.class);
    resourceClasses.add(DeleteAccount.class);
    resourceClasses.add(UserAccount.class);
  }

  private static final Set<Class<?>> providerClasses = new HashSet<Class<?>>();

  static
  {
    providerClasses.add(UserAuthorization.class);
    providerClasses.add(UserRegistrationReader.class);
    providerClasses.add(CustomerFulfillmentReader.class);
    providerClasses.add(UserWriter.class);
    providerClasses.add(CustomerFulfillmentWriter.class);
  }



  // Constructors ---------------------------------------------------------------------------------

  public AccountManager()
  {
    //System.setProperty("jersey.config.server.tracing.type", "ALL");
  }



  // Application Overrides ------------------------------------------------------------------------

  @Override public Set<Class<?>> getClasses()
  {
    Set<Class<?>> classes = new HashSet<Class<?>>();

    classes.addAll(resourceClasses);
    classes.addAll(providerClasses);

    return classes;
  }



  /**
   * Implements user authorization as a dynamic feature. This allows authorization configuration
   * to be made available through servlet's deployment descriptor.
   *
   * TODO :
   *   implement user role to resource mapping
   */
  private static class UserAuthorization implements DynamicFeature
  {
    @Override public void configure(ResourceInfo info, FeatureContext ctx)
    {
      if (info.getResourceClass().equals(UserAccount.class))
      {
        ctx.register(new AuthorizationRole(Role.ACCOUNT_OWNER_ROLE, Role.SERVICE_ADMINISTRATOR_ROLE));
      }
      else if (info.getResourceClass().equals(CreateAccount.class))
      {
        ctx.register(new AuthorizationRole(Role.SERVICE_ADMINISTRATOR_ROLE));
      }

      else if (info.getResourceClass().equals(DeleteAccount.class))
      {
        ctx.register(new AuthorizationRole(Role.SERVICE_ADMINISTRATOR_ROLE));
      }
    }
  }

  /**
   * A basic request authorization filter for incoming requests.
   */
  private static class AuthorizationRole implements ContainerRequestFilter
  {

    private Role[] roles;

    private AuthorizationRole(Role... roles)
    {
      this.roles = roles;
    }

    @Override public void filter(ContainerRequestContext ctx)
    {
      SecurityContext security = ctx.getSecurityContext();

      for (Role role : roles)
      {
        if (security.isUserInRole(role.getWebDescriptorRoleName()))
        {
          return;
        }
      }

      ctx.abortWith(Response.status(Response.Status.FORBIDDEN).build());
    }
  }

  public enum Role
  {
    SERVICE_ADMINISTRATOR_ROLE("service-admin"),

    ACCOUNT_OWNER_ROLE("account-owner");


    private String rolename;

    private Role(String rolename)
    {
      this.rolename = rolename;
    }

    public String getWebDescriptorRoleName()
    {
      return rolename;
    }

    @Override public String toString()
    {
      return getWebDescriptorRoleName();
    }
  }

  public enum Log
  {
    TRANSACTION               ("Transaction"),

    REGISTRATION              ("Rest.Registration"),

    REGISTRATION_DESERIALIZE  ("Rest.Registration.Deserialize"),

    FULFILLMENT_DESERIALIZE   ("Rest.Fulfillment.Deserialize");


    private String name;

    private Log(String name)
    {
      this.name = name;
    }

    public String getCanonicalLogHierarchyName()
    {
      return "OpenRemote.AccountManager." + name;
    }
  }
}

