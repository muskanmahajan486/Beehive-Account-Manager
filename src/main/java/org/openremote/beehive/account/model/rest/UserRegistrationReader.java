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
package org.openremote.beehive.account.model.rest;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;

import org.openremote.base.Defaults;

import org.openremote.logging.Logger;

import org.openremote.model.User;
import org.openremote.model.data.json.DeserializationException;
import org.openremote.model.data.json.UserTransformer;

import org.openremote.beehive.account.model.UserRegistration;
import org.openremote.beehive.account.service.AccountManager;
import org.openremote.beehive.account.service.HttpBadRequest;
import org.openremote.beehive.account.service.HttpInternalError;


/**
 * Deserializes new user registrations from JSON document. This implementation takes a regular
 * user instance in JSON format with additional user attributes required for registering a new
 * user account (see {@link org.openremote.model.User#CREDENTIALS_ATTRIBUTE_NAME} and
 * {@link org.openremote.model.User#AUTHMODE_ATTRIBUTE_NAME}.  <p>
 *
 * If the incoming JSON document cannot be interpreted, will return a HTTP error status 400 -
 * Bad Request. In case of any other errors will return status 500 -- Internal Server Error. <p>
 *
 * Assumes the incoming request has a HTTP Content-Type of either "application/json"
 * ({@link MediaType#APPLICATION_JSON}) or "application/vnd.openremote.user-registration+json"
 * ({@link UserRegistration#JSON_HTTP_CONTENT_TYPE}).
 *
 * If successful, will return a new instance of {@link UserRegistration}.
 *
 * @author Juha Lindfors
 */

@Consumes ({ MediaType.APPLICATION_JSON, UserRegistration.JSON_HTTP_CONTENT_TYPE })

public class UserRegistrationReader implements MessageBodyReader<UserRegistration>
{

  // Class Members --------------------------------------------------------------------------------

  private static Logger log = Logger.getInstance(AccountManager.Log.REGISTRATION_DESERIALIZE);



  // Implements MessageBodyReader -----------------------------------------------------------------

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
      // TODO : set upper limit to request document size.
      // TODO : enforce a request timeout

      log.info("Deserializing user registration JSON document...");

      // Deserialize default user from JSON stream...

      User user = new UserTransformer().read(new BufferedReader(new InputStreamReader(entityStream)));


      // For user registration, it must have mandatory registration attributes to continue...

      byte[] credentials = extractMandatoryCredentials(user);


      // Check for optional credentials encoding property, if present...

      User.CredentialsEncoding credsEncoding = getAuthMode(user);


      // Build a new registration instance...

      UserRegistration registration = new UserRegistration(
          user, new User.Authentication(credentials, credsEncoding)
      );

      log.info("Deserialized registration for ''{0}''...", registration.toString());

      // Done...

      return registration;
    }

    catch (DeserializationException exception)
    {
      log.error(
          "Deserializing new user registration failed: {0}",
          exception, exception.getMessage()
      );

      throw new HttpBadRequest(
          exception, "Unable to parse user registration from JSON: " + exception.getMessage()
      );
    }

    catch (Exception e)
    {
      log.error("Unknown error: " + e.getMessage());

      throw new HttpInternalError(e, e.getMessage());
    }
  }


  // Private Instance Methods ---------------------------------------------------------------------

  private byte[] extractMandatoryCredentials(User user) throws WebApplicationException
  {
    String credentials = user.getAttribute(User.CREDENTIALS_ATTRIBUTE_NAME);

    if (credentials == null || credentials.equals(""))
    {
      throw new HttpBadRequest("User registration credentials are missing.");
    }

    return credentials.getBytes(Defaults.DEFAULT_CHARSET);
  }

  private User.CredentialsEncoding getAuthMode(User user)
  {
    User.CredentialsEncoding result = User.CredentialsEncoding.DEFAULT;

    String authModeProperty = user.getAttribute(User.AUTHMODE_ATTRIBUTE_NAME);

    if (authModeProperty != null && !authModeProperty.equals(""))
    {
      try
      {
        result = UserRegistration.CredentialsEncoding.valueOf(authModeProperty);
      }

      catch (Exception e)
      {
        log.error(
            "Unrecognized ''{0}'' value ''{1}'' -- falling back to default encoding type: {2}",
            User.AUTHMODE_ATTRIBUTE_NAME, authModeProperty, User.CredentialsEncoding.DEFAULT
        );

        result = User.CredentialsEncoding.DEFAULT;
      }
    }

    return result;
  }

}

