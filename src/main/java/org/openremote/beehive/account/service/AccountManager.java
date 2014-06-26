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

import javax.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;

/**
 * A JAX-RS 2.0 application of OpenRemote Beehive Account Manager Service. <p>
 *
 * This JAX-RS application aggregates the relevant REST resources, providers and
 * features that compose the account manager service. <p>
 *
 * This implementation uses the explicit resource registration via {@link #getClasses()} method
 * to support pre-Servlet 3.0 containers.
 *
 * @author <a href = "mailto:juha@openremote.org">Juha Lindfors</a>
 */
public class AccountManager extends Application
{

  // Application Overrides ------------------------------------------------------------------------

  @Override public Set<Class<?>> getClasses()
  {
    Set<Class<?>> classes = new HashSet<Class<?>>();

    classes.add(CreateAccount.class);
    classes.add(DeleteAccount.class);
    classes.add(UserAccount.class);

    return classes;
  }
}

