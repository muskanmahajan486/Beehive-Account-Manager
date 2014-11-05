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

import org.openremote.base.Version;
import org.openremote.base.exception.IncorrectImplementationException;
import org.openremote.model.User;
import org.openremote.model.data.json.UserTransformer;

import java.io.Reader;
import java.nio.charset.Charset;
import java.util.Map;

/**
 * This domain object extends the class {@link org.openremote.model.User} from OpenRemote object
 * model with user details that are transferred between processes as part of the registration
 * process. In particular, it adds fields that would normally not traverse between systems
 * after the registration process is complete. <p>
 *
 * This implementation is specific to this Beehive Account Manager implementation, and therefore
 * not part of the shared OpenRemote object model. Account and user registrations should occur
 * through the Account Manager service. The account service can then delegate relevant
 * registration information and manage registration processes further in the back-end systems.
 *
 * @author <a href = "mailto:juha@openremote.org">Juha Lindfors</a>
 */
public class UserRegistration extends User
{

  // Constants ------------------------------------------------------------------------------------

  public static final Charset UTF8 = Charset.forName("UTF-8");


  // Instance Fields ------------------------------------------------------------------------------

  private transient byte[] credentials;


  // Constructors ---------------------------------------------------------------------------------

  public UserRegistration(String username, String email, byte[] credentials)
      throws ValidationException
  {
    super(username, email);

    super.jsonTransformer = new RegistrationTransformer(credentials);

    this.credentials = credentials;
  }

  private UserRegistration(User user, byte[] credentials)
  {
    super(user);

    super.jsonTransformer = new RegistrationTransformer(credentials);

    this.credentials = credentials;
  }


  // Nested Classes -------------------------------------------------------------------------------


  /**
   * Extends the {@link org.openremote.model.User} object's JSON transformer implementation to
   * manage the serialization of additional data fields that are only present at the registration
   * phase.
   */
  public static class RegistrationTransformer extends UserTransformer
  {

    // Instance Fields ----------------------------------------------------------------------------

    private transient byte[] credentials;


    // Constructors -------------------------------------------------------------------------------

    public RegistrationTransformer()
    {

    }

    private RegistrationTransformer(byte[] credentials)
    {
      this.credentials = credentials;
    }


    // Overrides UserTransformer ------------------------------------------------------------------

    /**
     * This is an implementation of the property field extension mechanism provided by the
     * super class in {@link org.openremote.model.data.json.UserTransformer}. It adds a
     * 'credentials' field to the serialized JSON document sent to a service end-point. <p>
     *
     * For the purpose of the serialization, credentials bytes are converted to an
     * {@link UserRegistration#UTF8} string.
     *
     * @param user
     */
    @Override protected void writeExtendedProperties(User user)
    {
      writeProperty("credentials", new String(credentials, UTF8));
    }


    /**
     * Deserializes this instance as an extension of {@link org.openremote.model.User} object.
     *
     * @param schemaVersion
     * @param className
     * @param jsonProperties
     *
     * @return
     *
     * @throws DeserializationException
     */
    @Override protected UserRegistration deserialize(Version schemaVersion,
                                                     String className,
                                                     Map<String, String> jsonProperties)
        throws DeserializationException
    {
      // Let superclass deserialize itself first...

      User user = super.deserialize(schemaVersion, User.class.getName(), jsonProperties);

      // Create an instance of this with additional properties...

      return new UserRegistration(
          user, jsonProperties.get("credentials").getBytes(UTF8)
      );
    }

    /**
     * Overridden to return instances of this class, as returned by the
     * {@link #deserialize(org.openremote.base.Version, String, java.util.Map)} implementation.
     *
     * @param reader
     *          input stream to deserialize this instance form
     *
     * @return  a new instance of this class
     *
     * @throws  DeserializationException
     *            if read deserialization fails
     */
    @Override public UserRegistration read(Reader reader) throws DeserializationException
    {
      // We know it is an instance of this class since we implement it as such in the
      // deserialize method above. Just need to remember to make changes on both methods
      // if the type ever changes...

      try
      {
        return (UserRegistration) super.read(reader);
      }

      catch (ClassCastException e)
      {
        throw new IncorrectImplementationException(
            "Deserialization does not return expected ''UserRegistration'' type."
        );
      }
    }
  }
}

