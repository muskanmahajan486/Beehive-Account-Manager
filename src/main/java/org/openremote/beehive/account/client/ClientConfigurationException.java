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
package org.openremote.beehive.account.client;

import org.openremote.base.exception.OpenRemoteException;
import org.openremote.base.exception.OpenRemoteRuntimeException;

/**
 * TODO
 *
 * @author Juha Lindfors
 */
public class ClientConfigurationException extends OpenRemoteRuntimeException
{
  public ClientConfigurationException(String msg)
  {
    super(msg);
  }

  public ClientConfigurationException(String msg, Object... args)
  {
    super(msg, args);
  }

  public ClientConfigurationException(String msg, Throwable cause)
  {
    super(msg, cause);
  }

  public ClientConfigurationException(String msg, Throwable cause, Object... args)
  {
    super(msg, cause, args);
  }
}
