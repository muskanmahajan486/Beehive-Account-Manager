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

import flexjson.transformer.Transformer;
import org.openremote.base.Version;
import org.openremote.beehive.account.model.CustomerFulfillment;
import org.openremote.model.Account;
import org.openremote.model.Controller;
import org.openremote.model.data.json.ControllerTransformer;
import org.openremote.model.data.json.JSONHeader;
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
import java.util.HashMap;
import java.util.Map;

/**
 * Writes a CustomerFulfillment instance to a JSON payload.
 * Account id is provided as an attribute of the returned payload.
 *
 * @author <a href="mailto:eric@openremote.org">Eric Bariaux</a>
 */
@Produces({ MediaType.APPLICATION_JSON })
public class CustomerFulfillmentWriter implements MessageBodyWriter<CustomerFulfillment>
{
  @Override
  public boolean isWriteable(Class<?> aClass, Type type, Annotation[] annotations, MediaType mediaType)
  {
    return CustomerFulfillment.class.isAssignableFrom(aClass);
  }

  @Override
  public long getSize(CustomerFulfillment customerFulfillment, Class<?> aClass, Type type, Annotation[] annotations,
                      MediaType mediaType)
  {
    return 0;
  }

  @Override
  public void writeTo(CustomerFulfillment customerFulfillment, Class<?> aClass, Type type, Annotation[] annotations,
                      MediaType mediaType,  MultivaluedMap<String, Object> multivaluedMap, OutputStream outputStream
                      ) throws IOException, WebApplicationException
  {
    if (!customerFulfillment.getAccounts().isEmpty())
    {
      for (Account acct : customerFulfillment.getAccounts())
      {
        if (acct instanceof RelationalAccount)
        {
          customerFulfillment.addAttribute("accountId", Long.toString(((RelationalAccount) acct).getId()));
          break;
        }
      }

    }

    PrintWriter pw = new PrintWriter(outputStream);
    Map<Class<?>, Transformer> transformers = new HashMap<Class<?>, Transformer>();
    transformers.put(CustomerFulfillment.class, new CustomerFulfillment.FulfillmentTransformer());
    transformers.put(Controller.class, new ControllerTransformer());
    pw.write(JSONHeader.toJSON(customerFulfillment, customerFulfillment.getClass().toString(), new Version(4, 0, 0), transformers));
    pw.flush();
  }
}
