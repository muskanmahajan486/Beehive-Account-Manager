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
import org.openremote.model.User;
import org.openremote.model.data.json.UserTransformer;

import java.io.Reader;
import java.nio.charset.Charset;
import java.util.Map;

/**
 * TODO
 *
 * @author <a href = "mailto:juha@openremote.org">Juha Lindfors</a>
 */
public class UserRegistration extends User
{

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

  public static class RegistrationTransformer extends UserTransformer
  {
    private byte[] credentials;

    public RegistrationTransformer()
    {

    }

    private RegistrationTransformer(byte[] credentials)
    {
      this.credentials = credentials;
    }

    @Override protected void extendedProperties(User user)
    {
      writeProperty("credentials", new String(credentials, Charset.forName("UTF-8")));
    }

    @Override protected UserRegistration deserialize(Version schemaVersion,
                                                     String className,
                                                     Map<String, String> jsonProperties)
        throws DeserializationException
    {
      User user = super.deserialize(schemaVersion, className, jsonProperties);

      return new UserRegistration(
          user, jsonProperties.get("credentials").getBytes(Charset.forName("UTF-8"))
      );
    }

    @Override public UserRegistration read(Reader reader) throws DeserializationException
    {
      User user = super.read(reader);

      return new UserRegistration(user, credentials);
    }
  }
}

