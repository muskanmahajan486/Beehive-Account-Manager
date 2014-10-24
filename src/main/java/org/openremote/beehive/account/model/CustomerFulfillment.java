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
package org.openremote.beehive.account.model;


import org.openremote.model.Controller;
import org.openremote.model.User;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * TODO
 *
 * @author <a href="mailto:juha@openremote.org">Juha Lindfors</a>
 */
public class CustomerFulfillment extends UserRegistration
{

  private Set<Controller> controllers = new CopyOnWriteArraySet<Controller>();

  public CustomerFulfillment(String username, String email, byte[] credentials, Controller controller)
      throws ValidationException
  {
    super(username, email, credentials);

    controllers.add(controller);
  }

  public void add(Controller controller)
  {
    controllers.add(controller);
  }


  private class FulfillmentTransformer extends RegistrationTransformer
  {
    @Override public void extendedProperties(User user)
    {
      startObject("controllers");

      for (Controller controller : controllers)
      {
        writeProperty("mac-addresses", controller.getMacAddresses());
      }

      endObject();
    }
  }

}

