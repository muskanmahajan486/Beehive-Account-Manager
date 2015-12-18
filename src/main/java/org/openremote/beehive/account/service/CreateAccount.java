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

import org.openremote.base.Defaults;
import org.openremote.base.exception.IncorrectImplementationException;
import org.openremote.beehive.account.model.CustomerFulfillment;
import org.openremote.beehive.account.model.UserRegistration;
import org.openremote.model.Controller;
import org.openremote.model.Model;
import org.openremote.model.User;
import org.openremote.model.data.json.DeserializationException;
import org.openremote.model.persistence.jpa.RelationalAccount;
import org.openremote.model.persistence.jpa.RelationalController;
import org.openremote.model.persistence.jpa.RelationalUser;
import org.openremote.model.persistence.jpa.beehive.BeehiveController;
import org.openremote.model.persistence.jpa.beehive.BeehiveUser;
import org.openremote.model.persistence.jpa.beehive.MinimalBeehiveRole;
import org.openremote.model.persistence.jpa.beehive.MinimalBeehiveUserRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.io.UnsupportedEncodingException;
import java.util.Locale;


/**
 * Beehive Account Manager REST API for creating new account instances.
 *
 * @author Juha Lindfors
 */

@Path("users")

public class CreateAccount
{

  // Constants ------------------------------------------------------------------------------------


  public static final String WEBAPP_PARAM_SERVICE_DB_SCHEMA = "ServiceSchema";

  /**
   * Log category for the account registration requests.
   */
  private static final AccountManager.Log LOG_CATEGORY = AccountManager.Log.REGISTRATION;



  // Class Members --------------------------------------------------------------------------------

  /**
   * Logger for this account registration implementation.
   */
  private static Logger log = LoggerFactory.getLogger(LOG_CATEGORY.getCanonicalLogHierarchyName());



  // Instance Fields ------------------------------------------------------------------------------

  /**
   * Security context associated with the incoming HTTP request provided by the host HTTP servlet
   * service.
   */
  @Context private SecurityContext security;

  @Context private HttpServletRequest request;

  @Context private ServletContext webapp;


  // REST API Implementation ----------------------------------------------------------------------

  @Consumes({ MediaType.APPLICATION_JSON, UserRegistration.JSON_HTTP_CONTENT_TYPE })

  @POST public Response create(UserRegistration registration)
  {
    RelationalAccount acct = createPersistentAccount();
    User user = createUserAccount(acct, registration);

    log.info(
        "CREATE ACCOUNT: [Service admin: ''{}''] created new account for user ''{}''.",
        security.getUserPrincipal().getName(), user.getName()
    );

    return Response.ok(user).build();
  }


  @Consumes(CustomerFulfillment.JSON_HTTP_CONTENT_TYPE)

  @POST public Response create(CustomerFulfillment fulfillment) throws DeserializationException
  {
    RelationalAccount acct = createPersistentAccount();
    RelationalUser user = createUserAccount(acct, fulfillment);

    ControllerData ctrlData = new ControllerData(fulfillment);

    Controller addedController = addController(Schema.resolveDBSchema(webapp), acct, ctrlData.controller);

    log.info(
        "CREATE ACCOUNT: [Service admin: ''{}''] created new account for user ''{}''.",
        security.getUserPrincipal().getName(), user.getName()
    );

    CustomerFulfillment createdFulfillment = null;
    try
    {
      UserRegistration reg = new UserRegistration(user,
        new User.Authentication("<not provided>".getBytes("UTF-8"), User.CredentialsEncoding.UNSPECIFIED));
      createdFulfillment = new CustomerFulfillment(reg, addedController);
      createdFulfillment.getAccounts().add(acct);
    } catch (UnsupportedEncodingException exception)
    {
      throw new HttpInternalError(
              security.getUserPrincipal(), LOG_CATEGORY.getCanonicalLogHierarchyName(), exception,
              "Account creation FAILED: {0}",
              exception.getMessage()
      );
    }

    return Response.ok(createdFulfillment).build();
  }


  // TODO: @POST @Consumes({MediaType.TEXT_XML, MediaType.APPLICATION_XML})



  // Private Instance Methods ---------------------------------------------------------------------


  private RelationalAccount createPersistentAccount()
  {
    try
    {
      RelationalAccount acct = new RelationalAccount();

      getEntityManager().persist(acct);

      return acct;
    }

    catch (PersistenceException exception)
    {
      // throw HTTP 500 - Internal Error in case the database save fails...

      throw new HttpInternalError(
          security.getUserPrincipal(), LOG_CATEGORY.getCanonicalLogHierarchyName(), exception,
          "Account creation FAILED: {0}",
          exception.getMessage()
      );
    }
  }



  private RelationalUser createUserAccount(RelationalAccount acct, UserRegistration registration)
  {
    // throw HTTP 400 - Bad Request if the incoming user registration document didn't
    // deserialize correctly...

    if (registration == null)
    {
      throw new HttpBadRequest(
          security.getUserPrincipal(), LOG_CATEGORY.getCanonicalLogHierarchyName(),
          "User registration JSON representation was not correctly deserialized."
      );
    }

    Schema schema = Schema.resolveDBSchema(webapp);

    // throw HTTP 409 - Conflict if given username already exists (covers the underlying
    // persistence constraint exception that would be thrown otherwise instead)...

    if (exists(schema, registration.getName()))
    {
      throw new HttpConflict(
          security.getUserPrincipal(), LOG_CATEGORY.getCanonicalLogHierarchyName(),
          "User ''{0}'' already exists.",
          registration.getName()
      );
    }

    try
    {
      return  createPersistentUserAccount(schema, acct, registration);
    }

    catch (Model.ValidationException exception)
    {
      // throw HTTP 400 - Bad Request if the user data in the incoming document cannot be
      // validated...

      throw new HttpBadRequest(
          security.getUserPrincipal(), LOG_CATEGORY.getCanonicalLogHierarchyName(), exception,
          "Incorrect user data: {0}",
          exception.getMessage()
      );
    }
  }



  private Controller addController(Schema schema, RelationalAccount acct, final Controller controller)
  {
    if (controller == null)
    {
      // TODO :
      //        the ControllerData object where this controller instance is derived from
      //        does not yet know how to deal with an empty collection of controllers
      //        in the fullfillment (see the to-do there). So using a null guard here for
      //        for now, allowing the request to pass but without a controller data in
      //        the database...

      return null;
    }

    Controller dbController = new RelationalController(acct, controller);

    if (schema == Schema.LEGACY_BEEHIVE)
    {
      dbController = new BeehiveController(acct, controller);
    }

    getEntityManager().persist(dbController);

    return dbController;
  }


  private boolean exists(Schema schema, String username)
  {
    try
    {
      String entityName = "User";

      if (schema == Schema.LEGACY_BEEHIVE)
      {
        entityName = "BeehiveUser";
      }

      return getEntityManager().createQuery(
          "SELECT u FROM " + entityName + " u WHERE u.username = :name", User.class)
          .setParameter("name", username)
          .getResultList().size() != 0;
    }

    catch (PersistenceException exception)
    {
      // throw HTTP 500 - Internal Error in case the database operation fails...

      throw new HttpInternalError(
          security.getUserPrincipal(), LOG_CATEGORY.getCanonicalLogHierarchyName(), exception,
          "Error in checking duplicate usernames: {0}",
          exception.getMessage()
      );
    }
  }

  private RelationalUser createPersistentUserAccount(Schema schema, RelationalAccount acct,
                                                     UserRegistration registration)
      throws Model.ValidationException
  {
    try
    {
      EntityManager em = getEntityManager();

      switch (schema)
      {
        case LEGACY_BEEHIVE:

          BeehiveUser beehiveUser = new BeehiveUser(
              acct, registration,
              registration.getAttribute(User.CREDENTIALS_ATTRIBUTE_NAME).getBytes(Defaults.UTF8)
          );

          beehiveUser.link(acct);

          // TODO : eliminate the code duplication on persist (and linking?)...

          em.persist(acct);   // TODO : not needed?
          em.persist(beehiveUser);

          MinimalBeehiveRole accountOwnerRole = getAccountOwnerRole();
          if (accountOwnerRole != null)
          {
            MinimalBeehiveUserRole userRoleJoin = new MinimalBeehiveUserRole(beehiveUser.getId(), accountOwnerRole.getId());
            em.persist(userRoleJoin);
          }

          return beehiveUser;

        case ACCOUNT_MANAGER_2_0:

          RelationalUser user = new RelationalUser(registration);

          user.link(acct);

          em.persist(acct);   // TODO : not needed?
          em.persist(user);

          return user;

        default:

          throw new IncorrectImplementationException("Incorrect schema identifier: {0}", schema);
      }
    }

    catch (PersistenceException exception)
    {
      // throw HTTP 500 - Internal Error in case the database save fails...

      throw new HttpInternalError(
          security.getUserPrincipal(), LOG_CATEGORY.getCanonicalLogHierarchyName(), exception,
              "Account creation FAILED: {0}", exception.getMessage()
      );
    }
  }

  private MinimalBeehiveRole getAccountOwnerRole()
  {
    try
    {
      return (MinimalBeehiveRole) getEntityManager()
              .createNamedQuery("findRoleByName")
              .setParameter("roleName", AccountManager.Role.ACCOUNT_OWNER_ROLE.toString()).getSingleResult();
    }
    catch (Exception e)
    {
      log.warn("Could not find account-owner role", e);
    }
    return null;
  }


  private EntityManager getEntityManager()
  {
    return (EntityManager)request.getAttribute(AccountManager.ENTITY_MANAGER_LOOKUP);
  }


  // Enums ----------------------------------------------------------------------------------------

  public enum Schema
  {
    LEGACY_BEEHIVE,
    ACCOUNT_MANAGER_2_0;

    // TODO : resolve schema in a central location once at startup...

    public static Schema resolveDBSchema(ServletContext webapp)
    {
      String dbSchemaParameter = webapp.getInitParameter(WEBAPP_PARAM_SERVICE_DB_SCHEMA);

      if (dbSchemaParameter == null)
      {
        // TODO Log

        return Schema.ACCOUNT_MANAGER_2_0;
      }

      dbSchemaParameter = dbSchemaParameter.toUpperCase(Locale.ENGLISH);

      try
      {
        return Schema.valueOf(dbSchemaParameter);
      }

      catch (IllegalArgumentException e)
      {
        // TODO : adjust exception type + log on the server

        throw new HttpInternalError(
            "Invalid " + WEBAPP_PARAM_SERVICE_DB_SCHEMA + "value: " + dbSchemaParameter
        );
      }
    }

  }


  // Nested Classes -------------------------------------------------------------------------------

  private static class ControllerData extends CustomerFulfillment
  {
    // TODO:
    //    set an upper limit on number of allowed controllers and number of allowed
    //    mac addresses within the controller....

    private Controller controller = null;

    ControllerData(CustomerFulfillment copy)
    {
      super(copy);


      // TODO : what to do if controllers are empty - deal with the null ref here?


      if (!super.controllers.isEmpty())
      {
        controller = controllers.iterator().next();
      }
    }
  }

}

