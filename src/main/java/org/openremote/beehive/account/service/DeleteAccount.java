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

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotNull;
import javax.ws.rs.DELETE;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;
import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * TODO
 *
 * @author <a href="mailto:juha@openremote.org">Juha Lindfors</a>
 */
@Path ("/users/{username}")

public class DeleteAccount
{
  @Context
  private SecurityContext security;

  @Context
  private HttpServletRequest request;

  /**
   * Inject the username value from this resource path's URI template.
   */
  @NotNull @PathParam ("username")
  private String username;


  @DELETE @Produces (MediaType.TEXT_PLAIN)

  public String delete() throws Exception
  {
    BufferedReader reader= new BufferedReader(new InputStreamReader(request.getInputStream()));

    System.err.println("DELETE ACCOUNT");

    while(true)
    {
      String str = reader.readLine();

      if (str == null)
      {
        break;
      }

      System.err.print(str);
    }

    System.err.println("");

    return "[SEC: " + security.getUserPrincipal().getName() + "] Delete account for user " +
        username + "\n";
  }
}
