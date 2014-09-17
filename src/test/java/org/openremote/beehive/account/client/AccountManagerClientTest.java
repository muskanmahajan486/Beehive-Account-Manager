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
package org.openremote.beehive.account.client;

import org.openremote.beehive.Tomcat;
import org.openremote.security.KeyManager;
import org.openremote.security.PrivateKeyManager;
import org.openremote.security.SecurityProvider;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.ws.rs.core.Response;
import java.net.URL;
import java.security.Security;
import java.security.cert.Certificate;

/**
 * TODO
 *
 * @author <a href="mailto:juha@openremote.org">Juha Lindfors</a>
 */
public class AccountManagerClientTest
{


  // Constants ------------------------------------------------------------------------------------

  private static final String ADMIN_USERNAME = "admin";

  private static final byte[] ADMIN_CREDENTIALS = "admin".getBytes();

  private static final URL DEFAULT_SERVICE_ROOT;


  // Class Initializers ---------------------------------------------------------------------------

  static
  {
    try
    {
      DEFAULT_SERVICE_ROOT = new URL("https://localhost:12123/test");
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

  @BeforeClass public void installBouncyCastleProvider()
  {
    Security.addProvider(SecurityProvider.BC.getProviderInstance());
  }

  @BeforeClass public void startTomcat()
  {
    try
    {
      tomcat = new Tomcat();
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

  @Test public void testCreateUser() throws Exception
  {
    PrivateKeyManager privatekey = PrivateKeyManager.create(KeyManager.Storage.JCEKS);
    Certificate keyCertificate = privatekey.addKey("tomcat", "tomcat".toCharArray());

    AccountManagerClient client = new AccountManagerClient(
        DEFAULT_SERVICE_ROOT, ADMIN_USERNAME, ADMIN_CREDENTIALS
    ).createTemporaryCertificateTrustStore(keyCertificate);

    Response response = client.createUserAccount("newuser", "newcredentials".getBytes());

    Assert.assertTrue(
        response.getStatus() == Response.Status.OK.getStatusCode(),
        "Got " + response.getStatus()
    );
  }


  @Test public void testCreateUserWrongAuthenticationCredentials() throws Exception
  {
    PrivateKeyManager privatekey = PrivateKeyManager.create(KeyManager.Storage.JCEKS);
    Certificate keyCertificate = privatekey.addKey("tomcat", "tomcat".toCharArray());

    AccountManagerClient client = new AccountManagerClient(
        DEFAULT_SERVICE_ROOT, ADMIN_USERNAME, "INCORRECT_ADMIN_PASSWORD".getBytes()
    ).createTemporaryCertificateTrustStore(keyCertificate);

    Response response = client.createUserAccount("newuser", "newcredentials".getBytes());

    Assert.assertTrue(
        response.getStatus() == Response.Status.UNAUTHORIZED.getStatusCode(),
        "Got " + response.getStatus()
    );
  }
}

