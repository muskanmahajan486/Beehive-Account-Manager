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

/**
 * TODO
 *
 * @author <a href = "mailto:juha@openremote.org">Juha Lindfors</a>
 */

@Path("users")

public class CreateAccount
{

  // Constants ------------------------------------------------------------------------------------


  public static final String WEBAPP_PARAM_SERVICE_DB_SCHEMA = "ServiceSchema";


  // Class Members --------------------------------------------------------------------------------

  private static Logger log = Logger.getInstance(AccountManager.Log.CREATE_USER);


  // Instance Fields ------------------------------------------------------------------------------

  @Context private SecurityContext security;

  @Context private HttpServletRequest request;



  // REST API Implementation ----------------------------------------------------------------------

  @Consumes({ MediaType.APPLICATION_JSON })
  @POST public void create(UserRegistration registration) throws Exception
  {
    if (registration == null)
    {
      throw new JSONTransformer.DeserializationException(
          "User registration JSON representation was not correctly deserialized."
      );
    }

    EntityManager em = getEntityManager();

    RelationalAccount account = new RelationalAccount();
    em.persist(account);

    RelationalUser user = new RelationalUser(registration);
    user.link(account);

    em.persist(user);

    log.info(
        "CREATE ACCOUNT: [Service admin: ''{0}''] created new account for user ''{1}''.",
        security.getUserPrincipal().getName(), user.getName()
    );
  }

  @POST public void create(CustomerFulfillment fulfillment)
  {
    throw new RuntimeException("Not implemented yet.");
  }


  /*
  @POST @Consumes({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
  public void create(UserRegistration reg, boolean xml) throws Exception
  {
    throw new RuntimeException("Not implemented yet.");
  }
  */


  // Private Instance Methods ---------------------------------------------------------------------

  private EntityManager getEntityManager()
  {
    return (EntityManager)request.getAttribute(AccountManager.ENTITY_MANAGER_LOOKUP);
  }

}

