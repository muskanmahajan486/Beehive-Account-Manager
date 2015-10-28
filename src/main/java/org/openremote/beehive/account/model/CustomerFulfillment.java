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
package org.openremote.beehive.account.model;

import java.io.StringReader;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import flexjson.transformer.Transformer;

import org.openremote.base.Defaults;
import org.openremote.base.exception.IncorrectImplementationException;

import org.openremote.beehive.account.service.AccountManager;

import org.openremote.model.Controller;
import org.openremote.model.User;
import org.openremote.model.data.json.ControllerTransformer;
import org.openremote.model.data.json.DeserializationException;
import org.openremote.model.data.json.JSONHeader;
import org.openremote.model.data.json.JSONModel;
import org.openremote.model.data.json.ModelObject;
import org.openremote.model.data.json.UserTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This domain object extends class {@link org.openremote.beehive.account.model.UserRegistration}
 * from object with additional customer details that can be transferred as part of the registration
 * process. Typically, additional configuration defaults, such as controller configuration can be
 * included if they are known at account registration time as part of the fulfillment process. <p>
 *
 * @author Juha Lindfors
 */
public class CustomerFulfillment extends UserRegistration
{

  // Constants ------------------------------------------------------------------------------------

  public static final String JSON_HTTP_CONTENT_TYPE =
      "application/vnd.openremote.customer-fulfillment+json";


  // Class Members --------------------------------------------------------------------------------

  private static final Logger log = LoggerFactory.getLogger(
          AccountManager.Log.REGISTRATION_DESERIALIZE.getCanonicalLogHierarchyName());


  // Instance Fields ------------------------------------------------------------------------------

  protected Set<Controller> controllers = new CopyOnWriteArraySet<Controller>();



  // Constructors ---------------------------------------------------------------------------------

  public CustomerFulfillment(String username, String email,
                             byte[] credentials, Controller controller)
      throws ValidationException
  {
    super(username, email, credentials);

    super.jsonTransformer = new FulfillmentTransformer();

    add(controller);
  }

  protected CustomerFulfillment(CustomerFulfillment copy)
  {
    super(copy, new UserAuthentication(copy));

    if (!copy.controllers.isEmpty())
    {
      this.controllers = new CopyOnWriteArraySet<Controller>(copy.controllers);
    }
  }


  private CustomerFulfillment(UserRegistration registration, Controller controller)
  {
    super(registration);

    super.jsonTransformer = new FulfillmentTransformer();

    if (controller != null)
    {
      controllers.add(controller);
    }
  }



  // UserRegistration Overrides -------------------------------------------------------------------

  @Override public String toJSONString()
  {
    Map<Class<?>, Transformer> transformers = new HashMap<Class<?>, Transformer>();
    transformers.put(Controller.class, new ControllerTransformer());

    return JSONHeader.toJSON(this, JSON_SCHEMA_VERSION, jsonTransformer, transformers);
  }


  // Public Instance Methods ----------------------------------------------------------------------

  public void add(Controller controller)
  {
    // don't allow nulls into the set...

    if (controller == null)
    {
      return;
    }

    controllers.add(controller);
  }


  // Nested Classes -------------------------------------------------------------------------------

  public static class FulfillmentTransformer extends UserTransformer
  {

    // Constants ----------------------------------------------------------------------------------

    /**
     * The JSON property name value used in customer fulfillment JSON document for
     * included controller instances: {@value}
     */
    public static final String CONTROLLERS_JSON_PROPERTY_NAME = "controllers";



    // UserTransformer Overrides ------------------------------------------------------------------

    @Override public void writeExtendedProperties(User user)
    {
      try
      {
        CustomerFulfillment fulfillment = (CustomerFulfillment)user;

        if (fulfillment.controllers.isEmpty())
        {
          return;
        }

        Set<JSONHeader<Controller>> jsonControllers = new HashSet<JSONHeader<Controller>>();

        for (Controller ctrl : fulfillment.controllers)
        {
          JSONHeader<Controller> json = new JSONHeader<Controller>(
              ctrl, Controller.JSON_SCHEMA_VERSION
          );

          json.excludeLibraryNameHeader(true);

          jsonControllers.add(json);
        }

        writeArray(CONTROLLERS_JSON_PROPERTY_NAME, jsonControllers);
      }

      catch (ClassCastException exception)
      {
        throw new IncorrectImplementationException(
            "Required user registration type, got {0}", user.getClass().getName()
        );
      }
    }


    @Override protected CustomerFulfillment deserialize(JSONModel model)
        throws DeserializationException
    {
      // TODO : replace repeated code from UserRegistrationReader

      // Let superclass deserialize itself first...

      User user = super.deserialize(model);

      // For user registration, it must have mandatory registration attributes to continue...

      byte[] credentials = extractMandatoryCredentials(user);

      // Check for optional credentials encoding property, if present...

      User.CredentialsEncoding credsEncoding = getAuthMode(user);

      UserRegistration registration = new UserRegistration(
          user, new Authentication(credentials, credsEncoding)
      );


      // Check if controller objects are included...

      Controller ctrl = null;

      List<ModelObject> controllerArray = model.getModel()
          .getObjectArray(CONTROLLERS_JSON_PROPERTY_NAME);

      if (controllerArray != null && !controllerArray.isEmpty())
      {
        // TODO : this only allows a single controller to be associated per fulfillment, for now

        // TODO :
        //        we need to convert incoming ModelObject back to raw JSON string for the
        //        transformer to deserialize, find a better solution for this

        StringReader reader = new StringReader(controllerArray.get(0).toString());

        ctrl = new ControllerTransformer().read(reader);
      }

      return new CustomerFulfillment(registration, ctrl);
    }



    // Private Instance Methods -------------------------------------------------------------------


    // TODO : replace repeated code from UserRegistrationReader
    private byte[] extractMandatoryCredentials(User user) throws DeserializationException
    {
      String credentials = user.getAttribute(User.CREDENTIALS_ATTRIBUTE_NAME);

      if (credentials == null || credentials.equals(""))
      {
        throw new DeserializationException("User registration credentials are missing.");
      }

      return credentials.getBytes(Defaults.DEFAULT_CHARSET);
    }

    // TODO : replace repeated code from UserRegistrationReader
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
              "Unrecognized ''{}'' value ''{}'' -- falling back to default encoding type: {}",
              User.AUTHMODE_ATTRIBUTE_NAME, authModeProperty, User.CredentialsEncoding.DEFAULT
          );

          result = User.CredentialsEncoding.DEFAULT;
        }
      }

      return result;
    }
  }



  private static class UserAuthentication extends Authentication
  {
    private static byte[] getCredentials(User copy)
    {
      return copy.getAttribute(User.CREDENTIALS_ATTRIBUTE_NAME).getBytes(Defaults.UTF8);
    }

    private static CredentialsEncoding getAuthMode(User copy)
    {
      return CredentialsEncoding.valueOf(
          copy.getAttribute(User.AUTHMODE_ATTRIBUTE_NAME).toUpperCase(Locale.ENGLISH)
      );
    }

    private UserAuthentication(CustomerFulfillment copy)
    {
      super(getCredentials(copy), getAuthMode(copy));
    }

  }
}

