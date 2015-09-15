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
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;

import org.openremote.model.data.json.DeserializationException;

import org.openremote.beehive.account.model.CustomerFulfillment;
import org.openremote.beehive.account.service.AccountManager;
import org.openremote.beehive.account.service.HttpBadRequest;
import org.openremote.beehive.account.service.HttpInternalError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Deserializes new customer fulfillments from JSON document.  <p>
 *
 * If the incoming JSON document cannot be interpreted, will return a HTTP error status 400 -
 * Bad Request. In case of any other errors will return status 500 -- Internal Server Error. <p>
 *
 * Assumes the incoming request has a HTTP Content-Type of
 * "application/vnd.openremote.customer-fulfillment+json"
 * ({@link CustomerFulfillment#JSON_HTTP_CONTENT_TYPE}). <p>
 *
 * If successful, will return a new instance of
 * {@link org.openremote.beehive.account.model.CustomerFulfillment}.
 *
 * @author Juha Lindfors
 */
@Consumes (CustomerFulfillment.JSON_HTTP_CONTENT_TYPE)

public class CustomerFulfillmentReader implements MessageBodyReader<CustomerFulfillment>
{

  // Class Members --------------------------------------------------------------------------------

  private static Logger log = LoggerFactory.getLogger(
          AccountManager.Log.FULFILLMENT_DESERIALIZE.getCanonicalLogHierarchyName());



  // Implements MessageBodyReader -----------------------------------------------------------------

  @Override public boolean isReadable(Class<?> type, Type genericType,
                                      Annotation[] annotations, MediaType mediaType)
  {
    return type == CustomerFulfillment.class;
  }

  @Override public CustomerFulfillment readFrom(Class<CustomerFulfillment> type, Type genericType,
                                                Annotation[] annotations, MediaType mediaType,
                                                MultivaluedMap<String, String> httpHeaders,
                                                InputStream entityStream)
  {
    try
    {
      // TODO : set upper limit to request document size.
      // TODO : enforce a request timeout

      log.info("Deserializing customer fulfillment JSON document...");

      CustomerFulfillment.FulfillmentTransformer transformer =
          new CustomerFulfillment.FulfillmentTransformer();

      return (CustomerFulfillment)transformer.read(
          new BufferedReader(new InputStreamReader(entityStream))
      );
    }

    catch (DeserializationException exception)
    {
      log.error(
          "Deserializing new customer fulfillment failed: {}",
          exception, exception.getMessage()
      );

      throw new HttpBadRequest(
          exception, "Unable to parse customer fulfillment from JSON: " + exception.getMessage()
      );
    }

    catch (ClassCastException exception)
    {
      String msg =
          "Type mismatch. Was expecting type ''{}'' but the resulting object from a " +
          "JSON transformer could not be converted to this type.";

      log.error(msg, CustomerFulfillment.class.getSimpleName());

      throw new HttpInternalError(msg, CustomerFulfillment.class.getSimpleName());
    }

    catch (Exception exception)
    {
      log.error("Unknown error: " + exception.getMessage(), exception);

      throw new HttpInternalError(exception, exception.getMessage());
    }
  }
}


