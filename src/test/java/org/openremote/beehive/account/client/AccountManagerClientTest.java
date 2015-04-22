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


  // Admin User Registration Tests ----------------------------------------------------------------


  /**
   * Tests a client with admin credentials creating a new user account using the account manager
   * user registration object.
   *
   * @throws Exception  if test fails
   */
  @Test public void testAdminCreateUserRegistration() throws Exception
  {
    // create registration...

    UserRegistration registration = new UserRegistration(
        "testAdminCreateUserRegistration-" + UUID.randomUUID().toString(),
        "email@some.com",
        new byte[] { 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j' }
    );

    // use admin to create a new user account...

    Response response = defaultAdminClient.create(registration);

    // should get HTTP OK...

    Assert.assertTrue(
        response.getStatus() == Response.Status.OK.getStatusCode(),
        "Got " + response.getStatus() + " : " + response.getStatusInfo().getReasonPhrase()
    );
  }

  /**
   * Tests a client with admin credentials creating a new user account using object model
   * user object with additional credentials attribute.
   *
   * @throws Exception  if test fails
   */
  @Test public void testAdminCreateUserWithExplicitAttributes() throws Exception
  {
    // create new user instance...

    User user = new User(
        "testAdminCreateUserWithExplicitAttributes-" + UUID.randomUUID().toString(),
        "email@some.email.com"
    );

    // add credentials attribute... while this API usage is allowed, should use
    // the UserRegistration object instead...

    user.addAttribute(User.CREDENTIALS_ATTRIBUTE_NAME, "newcredentials1");

    // use admin to create a new user account (with user object)...

    Response response = defaultAdminClient.create(user);

    // should get HTTP OK...

    Assert.assertTrue(
        response.getStatus() == Response.Status.OK.getStatusCode(),
        "Got " + response.getStatus() + " : " + response.getStatusInfo().getReasonPhrase()
    );
  }


  /**
   * Tests a client with admin credentials creating multiple unique user registrations.
   *
   * @throws Exception  if test fails
   */
  @Test public void testAdminCreateMultipleUsers() throws Exception
  {
    // execute individual registration test multiple times -- will use unique usernames to avoid
    // name conflicts...

    testAdminCreateUserRegistration();
    testAdminCreateUserRegistration();
    testAdminCreateUserRegistration();
    testAdminCreateUserRegistration();
    testAdminCreateUserRegistration();
  }

  /**
   * Tests a client with admin credentials attempting to create duplicate user accounts.
   *
   * @throws Exception  if test fails
   */
  @Test public void testAdminCreateDuplicateUser() throws Exception
  {
    // new user registration instance...

    UserRegistration registration = new UserRegistration(
        "testAdminCreateDuplicateUser",
        "email@some.com",
        new byte[] { 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j' }
    );

    // register...

    Response response = defaultAdminClient.create(registration);

    // should get HTTP OK...

    Assert.assertTrue(
        response.getStatus() == Response.Status.OK.getStatusCode(),
        "Got " + response.getStatus() + " : " + response.getStatusInfo().getReasonPhrase()
    );

    // try again with same registration data...

    response = defaultAdminClient.create(registration);

    // Should get HTTP Conflict response...

    Assert.assertTrue(
        response.getStatus() == Response.Status.CONFLICT.getStatusCode(),
        "Got " + response.getStatus() + " : " + response.getStatusInfo().getReasonPhrase()
    );
  }


  /**
   * Test creating user accounts when admin user is provided with incorrect credentials.
   *
   * @throws Exception  if test fails
   */
  @Test public void testAdminCreateUserWrongAuthenticationCredentials() throws Exception
  {
    // set up client instance with incorrect credentials...

    AccountManagerClient client = new AccountManagerClient(
        DEFAULT_SERVICE_ROOT, ADMIN_USERNAME,
        new byte[] { 'i', 'n', 'c', 'o', 'r', 'r', 'e', 'c', 't', '_', 'p', 'a', 's', 's' }
    );

    client.createTemporaryCertificateTrustStore(tomcat.getHttpsCertificate());

    // Attempt to register...

    Response response = client.create(
        new UserRegistration(
            "newuser3", null,
            new byte[] { 'n', 'e', 'w', 'c', 'r', 'e', 'd', 'e', 'n', 't', 'i', 'a', 'l', 's', '3' }
        )
    );

    // should get HTTP Not Authorized...

    Assert.assertTrue(
        response.getStatus() == Response.Status.UNAUTHORIZED.getStatusCode(),
        "Got " + response.getStatus()
    );
  }

  /**
   * Test creating user account with missing credentials attribute.
   *
   * @throws Exception  if test fails
   */
  @Test public void testAdminCreateUserNoCredentials() throws Exception
  {
    Response response = defaultAdminClient.create(
        new User(UUID.randomUUID().toString(), "foo@bar.com")
    );

    // Should get HTTP 400 - Bad request...

    Assert.assertTrue(
        response.getStatus() == Response.Status.BAD_REQUEST.getStatusCode(),
        "Got " + response.getStatus() + " : " + response.getStatusInfo().getReasonPhrase()
    );
  }


  /**
   * Test creating user account with missing email attribute. This API
   * use is possible but should generally be avoided, use UserRegistration instead.
   *
   * @throws Exception  if test fails
   */
  @Test public void testAdminCreateUserNullEmail() throws Exception
  {
    User user = new User(UUID.randomUUID().toString(), null);
    user.addAttribute(User.CREDENTIALS_ATTRIBUTE_NAME, "secretsecret");

    Response response = defaultAdminClient.create(user);

    // will receive HTTP 400 Bad Request unless server side has been configured
    // to accept null emails in User domain object validation...

    Assert.assertTrue(
        response.getStatus() == Response.Status.BAD_REQUEST.getStatusCode(),
        "Got " + response.getStatus() + " : " + response.getStatusInfo().getReasonPhrase()
    );
  }

  // TODO : add test with server configured to disable email validation (allow null emails)...



  // User Create Registration Tests ---------------------------------------------------------------


  /**
   * Tests creating user account with authentication credentials that do not map to
   * authorization role that allows new account creation.
   *
   * @throws Exception  if test fails
   */
  @Test public void testCreateUser() throws Exception
  {
    Response response = defaultUserClient.create(
        new UserRegistration(
            "newuser2", null,
            new byte[] { 'n', 'e', 'w', 'c', 'r', 'e', 'd', 'e', 'n', 't', 'i', 'a', 'l', 's', '2' }
        )
    );

    // should get HTTP forbidden... the user does not map to admin role with account
    // creation authorization...

    Assert.assertTrue(
        response.getStatus() == Response.Status.FORBIDDEN.getStatusCode(),
        "Got " + response.getStatus()
    );
  }

  /**
   * Tests creating user account with unrecognized authorization credentials.
   *
   * @throws Exception  if test fails
   */
  @Test public void testCreateUser2() throws Exception
  {

    AccountManagerClient client = new AccountManagerClient(
        DEFAULT_SERVICE_ROOT, "user2", USER_CREDENTIALS
    );

    client.createTemporaryCertificateTrustStore(tomcat.getHttpsCertificate());


    Response response = client.create(
        new UserRegistration(
            "newuser2", null,
            new byte[] { 'n', 'e', 'w', 'c', 'r', 'e', 'd', 'e', 'n', 't', 'i', 'a', 'l', 's', '2' }
        )
    );

    // should get HTTP not authorized... the user does not exist...

    Assert.assertTrue(
        response.getStatus() == Response.Status.UNAUTHORIZED.getStatusCode(),
        "Got " + response.getStatus()
    );
  }



  // Admin Fulfillment Tests ----------------------------------------------------------------------


  /**
   * Tests creating user account with fulfillment using admin credentials.
   *
   * @throws Exception  if test fails
   */
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
        response.getStatus() == Response.Status.OK.getStatusCode(),
        "Got " + response.getStatus() + " : " + response.getStatusInfo().getReasonPhrase()
    );
  }

  // TODO : test an upper limit on number of allowed controller mac addresses...


  /**
   * Tests creating user account with fulfillment using admin credentials.
   * Uses byte array for MAC address generation instead of strings.
   *
   * @throws Exception  if test fails
   */
  @Test public void testAdminCreateUserFulfillment() throws Exception
  {
    Controller controller = new Controller();
    controller.addMacAddress(new int[] { 0x01, 0x02, 0x03, 0x04, 0x05, 0x06 });

    CustomerFulfillment customer = new CustomerFulfillment(
        "newuser2" + UUID.randomUUID(), "email" +
        "@some2.email.test",
        new byte[] { 't', 'e', 's', 't', 'p', 'a', 's', 's', 'w', 'o', 'r', 'd' },
        controller
    );

    Response response = defaultAdminClient.create(customer);

    Assert.assertTrue(
        response.getStatus() == Response.Status.OK.getStatusCode(),
        "Got " + response.getStatus() + " : " + response.getStatusInfo().getReasonPhrase()
    );
  }


  /**
   * Tests creating user account with fulfillment using admin credentials.
   * Uses byte array for MAC address generation instead of strings.
   *
   * @throws Exception  if test fails
   */
  @Test public void testAdminCreateUserFulfillmentNoMacs() throws Exception
  {
    Controller controller = new Controller();

    CustomerFulfillment customer = new CustomerFulfillment(
        "newuser2" + UUID.randomUUID(), "email" +
        "@some2.email.test",
        new byte[] { 't', 'e', 's', 't', 'p', 'a', 's', 's', 'w', 'o', 'r', 'd' },
        controller
    );

    Response response = defaultAdminClient.create(customer);

    // TODO : this needs a fix in the object model's ControllerTransformer.deserialize() impl...

    Assert.assertTrue(
        response.getStatus() == Response.Status.OK.getStatusCode(),
        "Got " + response.getStatus() + " : " + response.getStatusInfo().getReasonPhrase()
    );
  }

  /**
   * Tests creating user account with fulfillment using admin credentials.
   * Uses byte array for MAC address generation instead of strings.
   *
   * @throws Exception  if test fails
   */
  @Test public void testAdminCreateUserFulfillmentNullController() throws Exception
  {
    CustomerFulfillment customer = new CustomerFulfillment(
        "newuser2-" + UUID.randomUUID(), "email" +
        "@some2.email.test",
        new byte[] { 't', 'e', 's', 't', 'p', 'a', 's', 's', 'w', 'o', 'r', 'd' },
        null
    );

    Response response = defaultAdminClient.create(customer);

    // TODO:
    //   needs a fix in the controller object model -- how to deal with null reference
    //   the current behavior may still change

    Assert.assertTrue(
        response.getStatus() == Response.Status.OK.getStatusCode(),
        "Got " + response.getStatus() + " : " + response.getStatusInfo().getReasonPhrase()
    );
  }



  // User Fulfillment Tests -----------------------------------------------------------------------

  /**
   * Tests creating user account with fulfillment using user credentials
   * (not authorized to create accounts)...
   *
   * @throws Exception  if test fails
   */
  @Test public void testUserCreateCustomerFulfillment() throws Exception
  {
    Controller controller = new Controller();
    controller.addMacAddress(new int[] { 0x01, 0x02, 0x03, 0x04, 0x05, 0x06 });

    CustomerFulfillment customer = new CustomerFulfillment(
        "newuser2" + UUID.randomUUID(), "email" +
        "@some2.email.test",
        new byte[] { 't', 'e', 's', 't', 'p', 'a', 's', 's', 'w', 'o', 'r', 'd' },
        controller
    );

    Response response = defaultUserClient.create(customer);

    // should get HTTP forbidden...

    Assert.assertTrue(
        response.getStatus() == Response.Status.FORBIDDEN.getStatusCode(),
        "Got " + response.getStatus() + " : " + response.getStatusInfo().getReasonPhrase()
    );
  }

  /**
   * Tests creating account + fulfillment with unrecognized authorization credentials.
   *
   * @throws Exception  if test fails
   */
  @Test public void testUserCreateCustomerFulfillment2() throws Exception
  {
    // create new client with unknown user...

    AccountManagerClient client = new AccountManagerClient(
        DEFAULT_SERVICE_ROOT, "user2", USER_CREDENTIALS
    );

    // we need to trust the self-signed certificate of our test servlet container instance...

    client.createTemporaryCertificateTrustStore(tomcat.getHttpsCertificate());

    Controller controller = new Controller();
    controller.addMacAddress(new int[] { 0x01, 0x02, 0x03, 0x04, 0x05, 0x06 });

    CustomerFulfillment customer = new CustomerFulfillment(
        "newuser2" + UUID.randomUUID(), "email" +
        "@some2.email.test",
        new byte[] { 't', 'e', 's', 't', 'p', 'a', 's', 's', 'w', 'o', 'r', 'd' },
        controller
    );

    Response response = client.create(customer);

    // should get HTTP not authorized... the user does not exist...

    Assert.assertTrue(
        response.getStatus() == Response.Status.UNAUTHORIZED.getStatusCode(),
        "Got " + response.getStatus()
    );
  }




  // Admin Delete Tests ---------------------------------------------------------------------------


  @Test public void testAdminDeleteUser() throws Exception
  {
    Response response = defaultAdminClient.delete("user");

    Assert.assertTrue(
        response.getStatus() == Response.Status.NOT_FOUND.getStatusCode(),
        "Got " + response.getStatus()
    );

    String uname = UUID.randomUUID().toString();

    response = defaultAdminClient.create(
        new UserRegistration(
            uname, "my@email.com",
            new byte[] { 'n', 'e', 'w', 'c', 'r', 'e', 'd', 'e', 'n', 't', 'i', 'a', 'l', 's', '2' }
        )
    );

    Assert.assertTrue(
        response.getStatus() == Response.Status.OK.getStatusCode(),
        "Got " + response.getStatus()
    );

    defaultAdminClient.delete(uname);

    Assert.assertTrue(
        response.getStatus() == Response.Status.OK.getStatusCode(),
        "Got " + response.getStatus()
    );
  }

  @Test public void testAdminDeleteUserWrongAuthenticationCredentials() throws Exception
  {
    // create new admin client with incorrect credentials...

    AccountManagerClient client = new AccountManagerClient(
        DEFAULT_SERVICE_ROOT, ADMIN_USERNAME,
        new byte[] { 'i', 'n', 'c', 'o', 'r', 'r', 'e', 'c', 't', '_', 'p', 'a', 's', 's' }
    );

    // we need to trust the self-signed certificate of our test servlet container instance...

    client.createTemporaryCertificateTrustStore(tomcat.getHttpsCertificate());

    Response response = client.delete("newuser");

    Assert.assertTrue(
        response.getStatus() == Response.Status.UNAUTHORIZED.getStatusCode(),
        "Got " + response.getStatus()
    );
  }


  // User Delete Tests ----------------------------------------------------------------------------

  @Test public void testDeleteUser() throws Exception
  {
    Response response = defaultUserClient.delete("user");

    Assert.assertTrue(
        response.getStatus() == Response.Status.FORBIDDEN.getStatusCode(),
        "Got " + response.getStatus()
    );
  }





  // SetHTTPSProtocol Tests -----------------------------------------------------------------------


  /**
   * Tests setting vulnerable SSL protocols, which should fail.
   *
   * @throws Exception  expected
   */
  @Test(expectedExceptions = ClientConfigurationException.class)

  public void testHttpsConfigurationDisabledSSL() throws Exception
  {
    AccountManagerClient client = new AccountManagerClient(
        DEFAULT_SERVICE_ROOT,
        ADMIN_USERNAME,
        ADMIN_CREDENTIALS
    ).setHttpsProtocol("SSLv2");

    client.create(new User(UUID.randomUUID().toString(), "test@email.com"));
  }


  // TODO : add tests for setting TLSv1.1 and TLSv1.2 (these will be JVM specific tests)...


  /**
   * Tests setting unknown protocol, which should raise an exception.
   *
   * @throws Exception  expected
   */
  @Test(expectedExceptions = ClientConfigurationException.class)

  public void testHttpsConfigurationEmptyString() throws Exception
  {
    AccountManagerClient client = new AccountManagerClient(
        DEFAULT_SERVICE_ROOT,
        ADMIN_USERNAME,
        ADMIN_CREDENTIALS
    ).setHttpsProtocol("");

    client.create(new User(UUID.randomUUID().toString(), "test@email.com"));
  }

  /**
   * Test setting a null reference which should fall back to defaults (TLS fallback cascade).
   *
   * @throws Exception  if test fails
   */
  @Test public void testHttpsConfigurationNullString() throws Exception
  {
    AccountManagerClient client = new AccountManagerClient(
        DEFAULT_SERVICE_ROOT,
        ADMIN_USERNAME,
        ADMIN_CREDENTIALS
    ).setHttpsProtocol((String)null);


    // we need to trust the self-signed certificate of our test servlet container instance...

    client.createTemporaryCertificateTrustStore(tomcat.getHttpsCertificate());

    client.create(new User(UUID.randomUUID().toString(), "test@email.com"));
  }


}

