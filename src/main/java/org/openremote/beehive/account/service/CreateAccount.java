/*
 * Copyright 2013-2015, Juha Lindfors. All rights reserved.
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

import java.util.Locale;

import javax.persistence.EntityManager;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;

import org.openremote.base.Defaults;
import org.openremote.base.exception.IncorrectImplementationException;

import org.openremote.logging.Logger;

import org.openremote.model.Controller;
import org.openremote.model.Model;
import org.openremote.model.User;
import org.openremote.model.data.json.DeserializationException;
import org.openremote.model.persistence.jpa.RelationalAccount;
import org.openremote.model.persistence.jpa.RelationalController;
import org.openremote.model.persistence.jpa.RelationalUser;
import org.openremote.model.persistence.jpa.beehive.BeehiveController;
import org.openremote.model.persistence.jpa.beehive.BeehiveUser;

import org.openremote.beehive.account.model.UserRegistration;
import org.openremote.beehive.account.model.CustomerFulfillment;
import org.openremote.beehive.account.model.rest.UserRegistrationReader;



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


  // Class Members --------------------------------------------------------------------------------

  private static Logger log = Logger.getInstance(AccountManager.Log.REGISTRATION);


  // Instance Fields ------------------------------------------------------------------------------

  @Context private SecurityContext security;

  @Context private HttpServletRequest request;

  @Context private ServletContext webapp;


  // REST API Implementation ----------------------------------------------------------------------

  @Consumes( { MediaType.APPLICATION_JSON, UserRegistration.JSON_HTTP_CONTENT_TYPE } )

  @POST public void create(UserRegistration registration) throws DeserializationException
  {
    RelationalAccount acct = createPersistentAccount();
    User user = createUserAccount(acct, registration);

    log.info(
        "CREATE ACCOUNT: [Service admin: ''{0}''] created new account for user ''{1}''.",
        security.getUserPrincipal().getName(), user.getName()
    );
  }


  @Consumes(CustomerFulfillment.JSON_HTTP_CONTENT_TYPE)

  @POST public void create(CustomerFulfillment fulfillment) throws DeserializationException
  {
    RelationalAccount acct = createPersistentAccount();
    RelationalUser user = createUserAccount(acct, fulfillment);

    ControllerData ctrlData = new ControllerData(fulfillment);

    addController(resolveDBSchema(), acct, ctrlData.controller);

    log.info(
        "CREATE ACCOUNT: [Service admin: ''{0}''] created new account for user ''{1}''.",
        security.getUserPrincipal().getName(), user.getName()
    );
  }


  // TODO: @POST @Consumes({MediaType.TEXT_XML, MediaType.APPLICATION_XML})



  // Private Instance Methods ---------------------------------------------------------------------


  private RelationalAccount createPersistentAccount()
  {
    RelationalAccount acct = new RelationalAccount();

    getEntityManager().persist(acct);

    return acct;
  }


  // TODO : resolve schema in a central location once at startup...

  private Schema resolveDBSchema()
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
      throw new UserRegistrationReader.InternalError(
          "Invalid " + WEBAPP_PARAM_SERVICE_DB_SCHEMA + "value: " + dbSchemaParameter
      );
    }
  }

  private RelationalUser createUserAccount(RelationalAccount acct, UserRegistration registration)
      throws DeserializationException
  {
    if (registration == null)
    {
      throw new DeserializationException(
          "User registration JSON representation was not correctly deserialized."
      );
    }

    // TODO check/test for duplicate user names

    try
    {
      return  createPersistentUserAccount(resolveDBSchema(), acct, registration);
    }

    catch (Model.ValidationException exception)
    {
      throw new DeserializationException(
          "Incorrect user data: {0}", exception, exception.getMessage()
      );
    }
  }


  private EntityManager getEntityManager()
  {
    return (EntityManager)request.getAttribute(AccountManager.ENTITY_MANAGER_LOOKUP);
  }


  private void addController(Schema schema, RelationalAccount acct, Controller controller)
  {
    EntityManager em = getEntityManager();

    switch (schema)
    {
      case LEGACY_BEEHIVE:

        BeehiveController beehiveCtrl = new BeehiveController(acct, controller);

        em.persist(beehiveCtrl);

        break;

      case ACCOUNT_MANAGER_2_0:

        RelationalController relController = new RelationalController(acct, controller);

        em.persist(relController);

        break;

      default:

        throw new IncorrectImplementationException("Incorrect schema identifier: {0}", schema);
    }
  }

  private RelationalUser createPersistentUserAccount(Schema schema, RelationalAccount acct,
                                                     UserRegistration registration)
      throws Model.ValidationException
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

        em.persist(acct);
        em.persist(beehiveUser);

        return beehiveUser;

      case ACCOUNT_MANAGER_2_0:

        RelationalUser user = new RelationalUser(registration);

        user.link(acct);

        em.persist(acct);
        em.persist(user);

        return user;


      default:

        throw new IncorrectImplementationException("Incorrect schema identifier: {0}", schema);
    }
  }


  // Enums ----------------------------------------------------------------------------------------

  private enum Schema
  {
    LEGACY_BEEHIVE,
    ACCOUNT_MANAGER_2_0
  }


  // Nested Classes -------------------------------------------------------------------------------

  private static class ControllerData extends CustomerFulfillment
  {
    private Controller controller = null;

    ControllerData(CustomerFulfillment copy)
    {
      super(copy);

      if (!super.controllers.isEmpty())
      {
        controller = controllers.iterator().next();
      }
    }
  }

}

