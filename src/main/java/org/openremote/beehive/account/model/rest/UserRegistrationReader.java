/*
 *  Copyright 2013-2015, Juha Lindfors.
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
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyReader;

import org.openremote.base.Defaults;

import org.openremote.logging.Logger;

import org.openremote.model.User;
import org.openremote.model.data.json.DeserializationException;
import org.openremote.model.data.json.UserTransformer;

import org.openremote.beehive.account.model.UserRegistration;
import org.openremote.beehive.account.service.AccountManager;


/**
 * Deserializes new user registrations from JSON document. This implementation takes a regular
 * user instance in JSON format with additional user attributes required for registering a new
 * user account (see {@link org.openremote.model.User#CREDENTIALS_ATTRIBUTE_NAME} and
 * {@link org.openremote.model.User#AUTHMODE_ATTRIBUTE_NAME}.  <p>
 *
 * If the incoming JSON document cannot be interpreted, will return a HTTP error status 400 -
 * Bad Request. In case of any other errors will return status 500 -- Internal Server Error. <p>
 *
 * If successful, will return a new instance of {@link UserRegistration}.
 *
 * @author Juha Lindfors
 */

@Consumes ( { MediaType.APPLICATION_JSON, UserRegistration.JSON_HTTP_CONTENT_TYPE } )

public class UserRegistrationReader implements MessageBodyReader<UserRegistration>
{

  // Class Members --------------------------------------------------------------------------------

  private static Logger log = Logger.getInstance(AccountManager.Log.CREATE_USER_DESERIALIZE);



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

      return registration;
    }

    catch (DeserializationException exception)
    {
      log.error(
          "Deserializing new user registration failed: {0}",
          exception, exception.getMessage()
      );

      throw new BadRequest(
          exception, "Unable to parse user registration from JSON: " + exception.getMessage()
      );
    }

    catch (Exception e)
    {
      log.error("Unknown error: " + e.getMessage());

      throw new InternalError(e, e.getMessage());
    }
  }


  // Private Instance Methods ---------------------------------------------------------------------

  private byte[] extractMandatoryCredentials(User user) throws WebApplicationException
  {
    String credentials = user.getAttribute(User.CREDENTIALS_ATTRIBUTE_NAME);

    if (credentials == null || credentials.equals(""))
    {
      throw new BadRequest("User registration credentials are missing.");
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


  // Nested Classes -------------------------------------------------------------------------------

  public static class BadRequest extends WebApplicationException
  {
    public BadRequest(String message)
    {
      this(null, message);
    }

    public BadRequest(Throwable rootCause, final String message)
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


  public static class InternalError extends WebApplicationException
  {
    public InternalError(String message)
    {
      this(null, message);
    }

    public InternalError(Throwable rootCause, final String message)
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


}

