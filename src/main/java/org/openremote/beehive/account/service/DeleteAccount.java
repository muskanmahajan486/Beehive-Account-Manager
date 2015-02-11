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

import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotNull;
import javax.ws.rs.DELETE;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Locale;

/**
 * TODO
 *
 * @author Juha Lindfors
 */
@Path ("/users/{username}")

public class DeleteAccount
{
  @Context private SecurityContext security;

  @Context private HttpServletRequest request;

  @Context private ServletContext webapp;

  /**
   * Inject the username value from this resource path's URI template.
   */
  @NotNull @PathParam ("username")
  private String username;

  @DELETE @Produces (MediaType.TEXT_PLAIN)

  public void delete() throws Exception
  {
    try
    {
      CreateAccount.Schema schema = CreateAccount.Schema.resolveDBSchema(webapp);

      String entityName = "User";

      if (schema == CreateAccount.Schema.LEGACY_BEEHIVE)
      {
        entityName = "BeehiveUser";
      }

      EntityManager entityManager = getEntityManager();

      List results = entityManager
          .createQuery("SELECT u FROM " + entityName + " u WHERE u.username = :name")
          .setParameter("name", username)
          .getResultList();

      if (results.isEmpty())
      {
        // TODO : align with other exception types

        throw new NotFoundException("Username was not found.");
      }

      entityManager.remove(results.get(0));
    }

    catch (PersistenceException exception)
    {
      throw new HttpInternalError(exception.getMessage());
    }
  }

  private EntityManager getEntityManager()
  {
    return (EntityManager)request.getAttribute(AccountManager.ENTITY_MANAGER_LOOKUP);
  }
}

