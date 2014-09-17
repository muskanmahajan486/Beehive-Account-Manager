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

import javax.servlet.ServletConfig;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.HashSet;
import java.util.Set;

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



  /**
   * The security role name defined for account owner in web deployment descriptor: {@value}
   */
  public static final String ACCOUNT_OWNER_ROLE = "account-owner";




  // Constructors ---------------------------------------------------------------------------------

  public AccountManager()
  {
    //System.setProperty("jersey.config.server.tracing.type", "ALL");
  }


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

