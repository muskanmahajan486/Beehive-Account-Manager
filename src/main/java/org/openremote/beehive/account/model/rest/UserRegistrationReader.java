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
package org.openremote.beehive.account.model.rest;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;

import org.openremote.beehive.account.model.UserRegistration;
import org.openremote.model.data.json.JSONTransformer;


/**
 * TODO
 *
 * @author <a href = "mailto:juha@openremote.org">Juha Lindfors</a>
 */

@Consumes (MediaType.APPLICATION_JSON)

public class UserRegistrationReader implements MessageBodyReader<UserRegistration>
{
  @Override public boolean isReadable(Class<?> type, Type genericType,
                                      Annotation[] annotations, MediaType mediaType)
  {
    return type == UserRegistration.class;
  }

  @Override public UserRegistration readFrom(Class<UserRegistration> type, Type genericType,
                                             Annotation[] annotations, MediaType mediaType,
                                             MultivaluedMap<String, String> httpHeaders,
                                             InputStream entityStream)
  {
    try
    {
      UserRegistration.RegistrationTransformer jsonTransformer =
          new UserRegistration.RegistrationTransformer();

      return jsonTransformer.read(new BufferedReader(new InputStreamReader(entityStream)));
    }

    catch (JSONTransformer.DeserializationException exception)
    {
      throw new WebApplicationException(
          "Unable to parse user registration from JSON: " + exception.getMessage(), exception
      );
    }
  }

}

