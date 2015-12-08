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
package org.openremote.beehive.account.model.rest;

import org.openremote.base.Version;
import org.openremote.beehive.account.model.UserRegistration;
import org.openremote.model.Account;
import org.openremote.model.User;
import org.openremote.model.data.json.JSONHeader;
import org.openremote.model.data.json.UserTransformer;
import org.openremote.model.persistence.jpa.RelationalAccount;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

/**
 * Writes a User instance to a JSON payload.
 * Credentials are not output in the payload but replaced with a "<not provided>" placeholder.
 * Account id is provided as an attribute of the returned user.
 *
 * @author <a href="mailto:eric@openremote.org">Eric Bariaux</a>
 */

@Produces({ MediaType.APPLICATION_JSON })
public class UserWriter implements MessageBodyWriter<User>
{
  @Override
  public boolean isWriteable(Class<?> aClass, Type type, Annotation[] annotations, MediaType mediaType)
  {
    return User.class.isAssignableFrom(aClass);
  }

  @Override
  public long getSize(User user, Class<?> aClass, Type type, Annotation[] annotations, MediaType mediaType)
  {
    return 0;
  }

  @Override
  public void writeTo(User user, Class<?> aClass, Type type, Annotation[] annotations, MediaType mediaType,
                      MultivaluedMap<String, Object> multivaluedMap, OutputStream outputStream
                      ) throws IOException, WebApplicationException
  {
    UserRegistration reg = new UserRegistration(user,
            new User.Authentication("<not provided>".getBytes("UTF-8"), User.CredentialsEncoding.UNSPECIFIED));

    if (!user.getAccounts().isEmpty())
    {
      for (Account acct : user.getAccounts())
      {
        if (acct instanceof RelationalAccount)
        {
          reg.addAttribute("accountId", Long.toString(((RelationalAccount) acct).getId()));
          break;
        }
      }
    }

    PrintWriter pw = new PrintWriter(outputStream);
    pw.write(JSONHeader.toJSON(reg, new Version(4, 0, 0), new UserTransformer()));
    pw.flush();
  }

}
