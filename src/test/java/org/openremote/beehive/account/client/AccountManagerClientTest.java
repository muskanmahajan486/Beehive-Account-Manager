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
package org.openremote.beehive.account.client;

import java.net.URL;

import java.security.Security;

import java.util.UUID;

import javax.ws.rs.core.Response;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.openremote.base.Defaults;

import org.openremote.security.SecurityProvider;

import org.openremote.model.Controller;
import org.openremote.model.User;

import org.openremote.beehive.Tomcat;
import org.openremote.beehive.account.model.CustomerFulfillment;
import org.openremote.beehive.account.model.UserRegistration;



/**
 * Unit tests for {@link org.openremote.beehive.account.client.AccountManagerClient} implementation.
 *
 * @author Juha Lindfors
 */
public class AccountManagerClientTest
{


  // Constants ------------------------------------------------------------------------------------


  private static final String ADMIN_USERNAME = "admin";

  private static final String DEFAULT_USERNAME = "user";

  private static final byte[] ADMIN_CREDENTIALS = "admin".getBytes(Defaults.UTF8);

  private static final byte[] USER_CREDENTIALS = "user".getBytes(Defaults.UTF8);

  private static final URL DEFAULT_SERVICE_ROOT;




  // Class Initializers ---------------------------------------------------------------------------

  static
  {
    try
    {
      DEFAULT_SERVICE_ROOT = new URL(
          "https://localhost:" +
          Tomcat.DEFAULT_SECURE_CONNECTOR_PORT +
          Tomcat.DEFAULT_WEBAPP_CONTEXT
      );
    }

    catch (Throwable throwable)
    {
      String msg =
          AccountManagerClientTest.class.getSimpleName() + " tests failed : " +
          throwable.getMessage();

      System.err.println(msg);

      throw new Error(msg);
    }

  }



  // Test Lifecycle -------------------------------------------------------------------------------

  private Tomcat tomcat;

  private AccountManagerClient defaultAdminClient = new AccountManagerClient(
      DEFAULT_SERVICE_ROOT,
      ADMIN_USERNAME,
      ADMIN_CREDENTIALS
  );

  private AccountManagerClient defaultUserClient = new AccountManagerClient(
      DEFAULT_SERVICE_ROOT,
      DEFAULT_USERNAME,
      USER_CREDENTIALS
  );


  @BeforeClass public void installBouncyCastleProvider()
  {
    Security.addProvider(SecurityProvider.BC.getProviderInstance());
  }

  @BeforeClass public void startTomcatAndInitializeClients()
  {
    try
    {
      tomcat = new Tomcat();

      defaultAdminClient.createTemporaryCertificateTrustStore(tomcat.getHttpsCertificate());
      defaultUserClient.createTemporaryCertificateTrustStore(tomcat.getHttpsCertificate());

      tomcat.start();
    }

    catch (Throwable t)
    {
      t.printStackTrace();

      throw new Error(
          "Tests in class " + this.getClass().getSimpleName() + " cannot run : " + t.getMessage()
      );
    }
  }

  @AfterClass public void removeBouncyCastleProvider()
  {
    Security.removeProvider(SecurityProvider.BC.getProviderInstance().getName());
  }

  @AfterClass public void stopTomcat()
  {
    try
    {
      tomcat.stop();
    }

    catch (Throwable throwable)
    {
      System.err.println("Failed to stop embedded Tomcat instance: " + throwable.getMessage());
    }
  }


  // Tests ----------------------------------------------------------------------------------------

  @Test public void testAdminCreateUserRegistration() throws Exception
  {
    UserRegistration registration = new UserRegistration(
        "testAdminCreateUserRegistration-" + UUID.randomUUID().toString(),
        "email@some.com",
        new byte[] { 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j' }
    );

    Response response = defaultAdminClient.create(registration);

    Assert.assertTrue(
        response.getStatus() == Response.Status.NO_CONTENT.getStatusCode(),
        "Got " + response.getStatus() + " : " + response.getStatusInfo().getReasonPhrase()
    );
  }

  @Test public void testAdminCreateUserWithExplicitAttributes() throws Exception
  {
    User user = new User(
        "testAdminCreateUserWithExplicitAttributes-" + UUID.randomUUID().toString(),
        "email@some.email.com"
    );

    user.addAttribute("credentials", "newcredentials1");

    Response response = defaultAdminClient.create(user);

    Assert.assertTrue(
        response.getStatus() == Response.Status.NO_CONTENT.getStatusCode(),
        "Got " + response.getStatus() + " : " + response.getStatusInfo().getReasonPhrase()
    );
  }


  @Test public void testAdminCreateCustomerFulfillment() throws Exception
  {
    Controller ctrl = new Controller();
    ctrl.addMacAddress("FF:FF:FF:FF:FF:00");
    ctrl.addMacAddress("01:01:01:01:01:01");

    CustomerFulfillment fulfillment = new CustomerFulfillment(
        "testAdminCreateCUSTOMERFULFILLMENT-" + UUID.randomUUID().toString(),
        "email@some.email.com",
        new byte[] { 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k' },
        ctrl
    );

    Response response = defaultAdminClient.create(fulfillment);

    Assert.assertTrue(
        response.getStatus() == Response.Status.NO_CONTENT.getStatusCode(),
        "Got " + response.getStatus() + " : " + response.getStatusInfo().getReasonPhrase()
    );
  }

  @Test public void testAdminCreateMultipleUsers() throws Exception
  {
    testAdminCreateUserRegistration();
    testAdminCreateUserRegistration();
    testAdminCreateUserRegistration();
    testAdminCreateUserRegistration();
    testAdminCreateUserRegistration();
  }

  @Test public void testAdminCreateUserFulfillment() throws Exception
  {
    Controller controller = new Controller();
    controller.addMacAddress(new int[] { 0x01, 0x02, 0x03, 0x04, 0x05, 0x06 });

    CustomerFulfillment customer = new CustomerFulfillment(
        "newuser2", "email@some2.email.test", "testpassword".getBytes(Defaults.UTF8),
        controller
    );

    Response response = defaultAdminClient.create(customer);

    Assert.assertTrue(
        response.getStatus() == Response.Status.NO_CONTENT.getStatusCode(),
        "Got " + response.getStatus() + " : " + response.getStatusInfo().getReasonPhrase()
    );
  }




  @Test public void testCreateUser() throws Exception
  {
    Response response = defaultUserClient.create(
        new UserRegistration("newuser2", null, "newcredentials2".getBytes(Defaults.UTF8))
    );

    Assert.assertTrue(
        response.getStatus() == Response.Status.FORBIDDEN.getStatusCode(),
        "Got " + response.getStatus()
    );
  }


  @Test public void testCreateUserWrongAuthenticationCredentials() throws Exception
  {
    AccountManagerClient client = new AccountManagerClient(
        DEFAULT_SERVICE_ROOT, ADMIN_USERNAME, "INCORRECT_ADMIN_PASSWORD".getBytes(Defaults.UTF8)
    );

    client.createTemporaryCertificateTrustStore(tomcat.getHttpsCertificate());

    Response response = client.create(
        new UserRegistration("newuser3", null, "newcredentials3".getBytes(Defaults.UTF8))
    );

    Assert.assertTrue(
        response.getStatus() == Response.Status.UNAUTHORIZED.getStatusCode(),
        "Got " + response.getStatus()
    );
  }



  @Test public void testAdminDeleteUser() throws Exception
  {
    Response response = defaultAdminClient.delete("user");

    Assert.assertTrue(
        response.getStatus() == Response.Status.OK.getStatusCode(),
        "Got " + response.getStatus()
    );
  }


  @Test public void testDeleteUser() throws Exception
  {
    Response response = defaultUserClient.delete("user");

    Assert.assertTrue(
        response.getStatus() == Response.Status.FORBIDDEN.getStatusCode(),
        "Got " + response.getStatus()
    );
  }

  @Test public void testDeleteUserWrongAuthenticationCredentials() throws Exception
  {
    AccountManagerClient client = new AccountManagerClient(
        DEFAULT_SERVICE_ROOT, ADMIN_USERNAME, "INCORRECT_ADMIN_PASSWORD".getBytes(Defaults.UTF8)
    );

    client.createTemporaryCertificateTrustStore(tomcat.getHttpsCertificate());

    Response response = client.delete("newuser");

    Assert.assertTrue(
        response.getStatus() == Response.Status.UNAUTHORIZED.getStatusCode(),
        "Got " + response.getStatus()
    );
  }


}

