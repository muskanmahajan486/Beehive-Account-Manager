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

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.text.MessageFormat;

/**
 * @author Juha Lindfors
 */
public class HttpInternalError extends WebApplicationException
{

  public static String format(String msg, Object... params)
  {
    try
    {
      return MessageFormat.format(msg, params);
    }

    catch (Throwable cause)
    {
      return msg + "  [EXCEPTION MESSAGE FORMATTING ERROR: " + cause.getMessage().toUpperCase() + "]";
    }
  }

  public HttpInternalError(String message)
  {
    this(null, message);
  }

  public HttpInternalError(String message, Object... params)
  {
    this(format(message, params));
  }

  public HttpInternalError(Throwable rootCause, final String message)
  {
    super(Response.noContent().status(

        // TODO : add debug mode that includes the stack trace as a response document

        new Response.StatusType()
        {
          @Override public int getStatusCode()
          {
            return Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
          }

          @Override public String getReasonPhrase()
          {
            return "Internal Server Error - " + message;
          }

          @Override public Response.Status.Family getFamily()
          {
            return Response.Status.Family.SERVER_ERROR;
          }
        }

    ).build());
  }

}
