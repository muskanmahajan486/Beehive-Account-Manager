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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import java.io.StringReader;
import java.util.HashSet;
import java.util.Set;

import flexjson.JSONTokener;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.openremote.model.Controller;


/**
 * Unit tests for {@link org.openremote.beehive.account.model.CustomerFulfillment} class.
 *
 * @author Juha Lindfors
 */
public class CustomerFulfillmentTest
{
  // Test Lifecycle -------------------------------------------------------------------------------

  private String fulfillmentJSON;

  /**
   * Set up tests with loading sample JSON documents to compare to.
   *
   * @throws java.io.IOException  if setup fails
   */
  @BeforeClass public void loadJsonTestFiles() throws IOException
  {
    try
    {
      fulfillmentJSON = loadFulfillmentJSONFile("fulfillment.json");
    }

    catch (Throwable t)
    {
      System.err.println(String.format("%n%n !!! TEST SETUP FAILURE !!! %n%n "));

      t.printStackTrace();
    }
  }



  // Tests ----------------------------------------------------------------------------------------


  @Test public void testCustomerFulfillmentJSON() throws Exception
  {
    byte[] credentials = new byte[] { 's', 'e', 'c', 'r', 'e', 't', 's', 'e', 'c', 'r', 'e', 't' };

    Set<String> macs = new HashSet<String>();
    macs.add("FF:FF:FF:FF:FF:FF");

    Controller controller = new Controller("test", macs, null, null);

    CustomerFulfillment fulfillment = new CustomerFulfillment(
        "testCustomerFulfillmentJSON",
        "email@somewhere.com",
        credentials,
        controller
    );

    String json = fulfillment.toJSONString();

    Assert.assertTrue(compare(json, fulfillmentJSON), json + "\n\n" + fulfillmentJSON);
  }


  @Test public void testCustomerFulfillmentTransformer() throws Exception
  {
    CustomerFulfillment.FulfillmentTransformer transformer =
                new CustomerFulfillment.FulfillmentTransformer();

    StringReader reader = new StringReader(fulfillmentJSON);

    CustomerFulfillment f = (CustomerFulfillment)transformer.read(new BufferedReader(reader));

    Assert.assertTrue(f != null);

    // TODO : complete assertions
  }


  // Helper Methods -------------------------------------------------------------------------------


  private String loadFulfillmentJSONFile(String name) throws IOException
  {
    File testResourceDirs = new File(
        System.getProperty("openremote.project.resources.dir"), "test"
    );

    File testUserRegistrationDir = new File(testResourceDirs, "fulfillment");
    File userRegistrationJSONFile = new File(testUserRegistrationDir, name);

    FileInputStream io = new FileInputStream(userRegistrationJSONFile);
    InputStreamReader in = new InputStreamReader(io, UserRegistration.UTF8);
    BufferedReader reader = new BufferedReader(in);

    StringBuilder builder = new StringBuilder();

    while (true)
    {
      String line = reader.readLine();

      if (line == null)
      {
        break;
      }

      builder.append(line);
      builder.append(String.format("%n"));
    }

    return builder.toString().trim();
  }


  private boolean compare(String json1, String json2)
  {
    JSONTokener tok1 = new JSONTokener(json1.trim());
    JSONTokener tok2 = new JSONTokener(json2.trim());

    while (tok1.more())
    {
      Character c1 = tok1.nextClean();
      Character c2 = tok2.nextClean();

      if (!c1.equals(c2))
      {
        return false;
      }
    }

    return tok1.more() == tok2.more();
  }

}

