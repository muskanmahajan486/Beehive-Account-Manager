/*
 * OpenRemote, the Home of the Digital Home.
 * Copyright 2008-2014, OpenRemote Inc.
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
import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.openremote.beehive.account.model.rest.UserRegistrationReader;
import org.openremote.logging.Hierarchy;
import org.openremote.logging.Logger;


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
 * @author <a href = "mailto:juha@openremote.org">Juha Lindfors</a>
 */
public class AccountManager extends Application
{

  // Constants ------------------------------------------------------------------------------------

  public static final String ENTITY_MANAGER_LOOKUP = "EntityManager";

  public static final String ENTITY_TX_LOOKUP = "EntityTransaction";


  // Class Members --------------------------------------------------------------------------------

  private static final Set<Class<?>> resourceClasses = new HashSet<Class<?>>(5);

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
    providerClasses.add(EntityPersistence.class);
    providerClasses.add(UserRegistrationReader.class);
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


  // Nested Classes -------------------------------------------------------------------------------

  /**
   * This container filter implements a managed persistence context for JPA entities used
   * in JAX-RS resources.
   */
  private static class EntityPersistence implements ContainerRequestFilter, ContainerResponseFilter
  {
    // Class Members ------------------------------------------------------------------------------

    /**
     * Initialize the persistence context factory.
     */
    private static EntityManagerFactory emFactory = Persistence.createEntityManagerFactory("H2");

    private static Logger log = Logger.getInstance(Log.TRANSACTION);



    // Instance Fields ----------------------------------------------------------------------------

    /**
     * This implements a managed, extended persistence context (see JPA 2.1 specification
     * section 3.3). For a per transaction context, open and close entity manager per each request
     * in filter methods of this class.
     */
    private EntityManager entityManager = emFactory.createEntityManager();


    // ContainerRequestFilter Implementation ------------------------------------------------------

    /**
     * Passes the entity manager reference as a request property to the resource classes to use.
     * Also begins the transaction boundary for JPA entities here.
     */
    @Override public void filter(ContainerRequestContext request)
    {
      EntityTransaction tx = null;
      String user = "<no name>";

      try
      {
        user = request.getSecurityContext().getUserPrincipal().getName();

        request.setProperty(ENTITY_MANAGER_LOOKUP, entityManager);

        tx = entityManager.getTransaction();

        tx.begin();

        request.setProperty(ENTITY_TX_LOOKUP, tx);

        log.info(
            "Started transaction for user ''{0}'', request ''{1} {2}''...",
            user, request.getMethod(), request.getUriInfo().getAbsolutePath()
        );
      }

      catch (Throwable throwable)
      {
        log.error(
            "Failed to start entity persistence transaction for user ''{0}'', request {1} : {2}",
            throwable, user, request.getUriInfo().getAbsolutePath(), throwable.getMessage()
        );

        if (tx != null && tx.isActive())
        {
          try
          {
            tx.rollback();
          }

          catch (Throwable t)
          {
            log.info("Transaction rollback for user ''{0}'' failed : {1}", t, user, t.getMessage());
          }
        }
      }
    }

    /**
     * Manages the entity transaction boundary on return request. If entity transaction has
     * been marked for rollback, or we are returning an HTTP error code 400 or above, roll back
     * the entity transaction.
     */
    @Override public void filter(ContainerRequestContext request, ContainerResponseContext response)
    {
      String user = "<no name>";
      EntityTransaction tx = null;

      try
      {
        user = request.getSecurityContext().getUserPrincipal().getName();
        tx = (EntityTransaction)request.getProperty(ENTITY_TX_LOOKUP);

        if (tx != null && tx.isActive())
        {
          if (tx.getRollbackOnly())
          {
            tx.rollback();

            log.info(
                "ROLLBACK: tx for user '{0}' was marked for roll back. Request : ''{1} {2}''",
                user, request.getMethod(), request.getUriInfo().getAbsolutePath()
            );
          }

          else if (response.getStatus() >= 400)
          {
            tx.rollback();

            log.info(
                "ROLLBACK: error response ''{0} : {1}'' to user ''{2}'' request ''{3} {4}''.",
                response.getStatus(), response.getStatusInfo().getReasonPhrase(),
                user, request.getMethod(), request.getUriInfo().getAbsolutePath()
            );
          }

          else
          {
            tx.commit();

            log.info(
                "COMMIT: user ''{0}'' request ''{1} {2}''",
                user, request.getMethod(), request.getUriInfo().getAbsolutePath()
            );
          }
        }
      }

      catch (Throwable throwable)
      {
        log.error("Implementation error: {0}", throwable, throwable.getMessage());

        if (tx != null && tx.isActive())
        {
          try
          {
            tx.rollback();

            log.info(
                "ROLLBACK: user ''{0}'', request ''{1} {2}''.",
                user, request.getMethod(), request.getUriInfo().getAbsolutePath()
            );
          }

          catch (Throwable t)
          {
            log.info("Transaction rollback for user ''{0}'' failed : {1}", t, user, t.getMessage());
          }
        }
      }
    }
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

  private enum Role
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

  public enum Log implements Hierarchy
  {
    TRANSACTION("Transaction"),

    CREATE_USER("Rest.CreateUser");


    private String name;

    private Log(String name)
    {
      this.name = name;
    }

    @Override public String getCanonicalLogHierarchyName()
    {
      return "OpenRemote.AccountManager." + name;
    }
  }
}

