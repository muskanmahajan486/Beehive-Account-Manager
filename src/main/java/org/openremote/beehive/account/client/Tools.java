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


import java.io.File;
import java.net.URI;
import java.net.URL;
import java.security.Security;
import java.security.cert.Certificate;

import javax.ws.rs.core.Response;

import org.openremote.beehive.account.model.UserRegistration;
import org.openremote.security.KeyManager;
import org.openremote.security.PrivateKeyManager;
import org.openremote.security.SecurityProvider;
import org.openremote.security.TrustStore;

/**
 *
 * @author Juha Lindfors
 */
public class Tools
{

  // Constants ------------------------------------------------------------------------------------

  /**
   * Default key store name used when a private keystore is generated for Tomcat with the tooling
   * provided by this class.
   */
  public static final String DEFAULT_PRIVATE_KEYSTORE_FILENAME = "tomcat-private.keystore";

  /**
   * Default trust store name used when a public key certificate trust store is generated for
   * the client with the tooling provided by this class.
   */
  public static final String DEFAULT_TRUSTSTORE_FILENAME = "client.truststore";



  // Class Members --------------------------------------------------------------------------------

  public static void main(String... args) throws Exception
  {
    Security.addProvider(SecurityProvider.BC.getProviderInstance());

    if (args.length == 0)
    {
      System.err.println("");
      System.err.println("Use --help argument to list possible arguments for this command.");
      System.err.println("");
    }

    if (args[0].equalsIgnoreCase("--generate-keys"))
    {
      generateKeys(args);
    }

    if (args[0].equalsIgnoreCase("--deployment-test"))
    {
      executeTestRequests(args);
    }

    else
    {
      System.err.println();
      System.err.println("Unknown option '" + args[0] + "', use --help for valid options.");
      System.err.println();
    }

  }



  private static void executeTestRequests(String... args) throws Exception
  {
    if (args.length < 3)
    {
      System.err.println("");
      System.err.println("Attribute --deployment-test requires additional arguments:");
      System.err.println("  [--deployment-test <url> <username:password>]");
      System.err.println("");

      return;
    }

    String username;
    String password;

    try
    {
      username = args[2].substring(0, args[2].indexOf(":"));
      password = args[2].substring(args[2].indexOf(":") + 1, args[2].length());
    }

    catch (IndexOutOfBoundsException e)
    {
      System.err.println();
      System.err.println("Unable to parse <username:password> from '" + args[2] + "'.");
      System.err.println();

      return;
    }

    AccountManagerClient client = new AccountManagerClient(
        new URL(args[1]),
        username,
        password.getBytes(AccountManagerClient.UTF8)
    );

    File userHomeDir = new File(System.getProperty("user.home"));
    File trustStoreLocation = new File(userHomeDir, "client.truststore");

    client.setCertificateTrustStore(trustStoreLocation.toURI());

    System.out.println("");
    System.out.println("");

    System.out.println("Executing Deployment Test to " + args[1] + " with user " + username);

    String user = "johndoe";

    System.out.println();
    System.out.println("Creating new user account for " + user);
    System.out.println();

    Response response = client.create(
        new UserRegistration(
            user,
            "email@host.domain",
            "Smb9324$#@#@$".getBytes(AccountManagerClient.UTF8))
    );

    System.out.println(response.getStatus() + ": - " + response.getStatusInfo());

    System.out.println();
    System.out.println("Retrieving account info for user " + user);
    System.out.println();

    response = client.retrieveAccountInfo(user);

    System.out.println(response.getStatus() + ": " + response.getStatusInfo());

    System.out.println();
    System.out.println("Deleting " + user);
    System.out.println();

    response = client.delete(user);

    System.out.println(response.getStatus() + ": " + response.getStatusInfo());

  }


  private static void generateKeys(String... args) throws KeyManager.KeyManagerException
  {
    if (args.length < 2)
    {
      System.err.println("");
      System.err.println("Attribute --generate-keys requires password as second argument.");
      System.err.println("  [--generate-keys <password> [--alias <alias>]]");
      System.err.println("");

      return;
    }

    String alias = "tomcat";

    if (args.length > 2)
    {
      if (args[2].equalsIgnoreCase("--alias"))
      {
        if (args.length < 4)
        {
          System.err.println("");
          System.err.println("Attribute --alias requires a second argument:");
          System.err.println("  [--generate-keys <password> [--alias <alias>]]");
          System.err.println("");

          return;
        }

        alias = args[3];
      }
    }


    // Store private key store in current work directory...

    Certificate cert = createPrivateKeyStore(alias, args[1]);


    // Store the certificate to a client trust store...

    createClientTrustStore(alias, cert);

    System.out.println();
    System.out.println("Trusted Tomcat Client Certificate: ");
    System.out.println();

    System.out.println(cert);

    System.out.println();
    System.out.println("Tomcat private key store saved to " + DEFAULT_PRIVATE_KEYSTORE_FILENAME);
    System.out.println("Client trust store saved to " + DEFAULT_TRUSTSTORE_FILENAME);

    System.out.println();
  }


  private static void createClientTrustStore(String alias, Certificate cert)
      throws KeyManager.KeyManagerException
  {
    File workingDirectory = new File(System.getProperty("user.dir"));

    URI trustStoreLocation = new File(workingDirectory, DEFAULT_TRUSTSTORE_FILENAME).toURI();

    TrustStore trustStore = TrustStore.create(
        trustStoreLocation,
        AccountManagerClient.DEFAULT_TRUST_STORE_FORMAT
    );

    trustStore.addTrustedCertificate(alias, cert);
  }


  private static Certificate createPrivateKeyStore(String alias, String password)
      throws KeyManager.KeyManagerException
  {
    File workingDirectory = new File(System.getProperty("user.dir"));
    URI privateKeyStoreLocation = new File(
        workingDirectory, DEFAULT_PRIVATE_KEYSTORE_FILENAME
    ).toURI();

    char[] storePassword = password.toCharArray();

    // Create store and add a new private key...

    PrivateKeyManager privatekey = PrivateKeyManager.create(
        privateKeyStoreLocation, storePassword, KeyManager.Storage.JCEKS
    );

    System.out.println();
    System.out.println("Generating new key for alias '" + alias + "'...");
    System.out.println();

    // TODO : switch to EC keys...

    return privatekey.addKey(
        alias, storePassword, KeyManager.AsymmetricKeyAlgorithm.RSA
    );
  }


  // Constructors ---------------------------------------------------------------------------------


  private Tools()
  {

  }
}
