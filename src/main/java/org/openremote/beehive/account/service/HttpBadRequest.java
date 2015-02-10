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

/**
 * @author Juha Lindfors
 */
public class HttpBadRequest extends WebApplicationException
{
  public HttpBadRequest(String message)
  {
    this(null, message);
  }

  public HttpBadRequest(Throwable rootCause, final String message)
  {
    super(Response.noContent().status(

        // TODO : add debug mode that includes the stack trace as a response document

        new Response.StatusType()
        {
          @Override public int getStatusCode()
          {
            return Response.Status.BAD_REQUEST.getStatusCode();
          }

          @Override public String getReasonPhrase()
          {
            return "Bad Request - " + message;
          }

          @Override public Response.Status.Family getFamily()
          {
            return Response.Status.Family.CLIENT_ERROR;
          }
        }

    ).build());
  }
}
