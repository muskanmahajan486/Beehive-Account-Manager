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

import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;

/**
 * TODO
 *
 * @author <a href="mailto:juha@openremote.org">Juha Lindfors</a>
 */

@Path ("/users/{username}/accounts")

public class UserAccount
{

  // Instance Fields ------------------------------------------------------------------------------

  /**
   * Inject the username value from this resource path's URI template.
   */
  @NotNull @PathParam("username")
  private String username;

  /**
   * Inject the current established JAX-RS security context.
   */
  @Context private SecurityContext security;


  // HTTP Methods ---------------------------------------------------------------------------------

  @GET @Produces (MediaType.TEXT_PLAIN)

  public String listUserAccounts()
  {
    return "[SEC: " + security.getUserPrincipal().getName() + "] Retrieve accounts for User " + username;
  }

}

