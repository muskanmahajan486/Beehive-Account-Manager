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
import java.io.FileInputStream;

import java.io.FileNotFoundException;
import java.net.URI;
import java.net.URL;

import java.nio.charset.Charset;

import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.cert.Certificate;

import javax.net.ssl.HostnameVerifier;
//import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;

import javax.net.ssl.TrustManagerFactory;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.cloudseance.foundation.JavaVM;
import org.bouncycastle.util.encoders.Base64;

import org.openremote.base.Version;
import org.openremote.base.exception.InitializationException;

import org.openremote.logging.Hierarchy;
import org.openremote.logging.Logger;
import org.openremote.security.KeyManager;
import org.openremote.security.PrivateKeyManager;
import org.openremote.security.SecurityProvider;
import org.openremote.security.TrustStore;

import org.openremote.model.User;

import org.openremote.beehive.account.model.CustomerFulfillment;
import org.openremote.beehive.account.model.UserRegistration;



/**
 * TODO
 *
 * @author Juha Lindfors
 */
public class AccountManagerClient
{

  // Constants ------------------------------------------------------------------------------------


  public static final URI REST_ROOT_PATH = URI.create("rest");

  public static final URI RPC_REST_PATH = URI.create("rpc");

  public static final URI SERVICE_PATH = URI.create("accountmanager");

  public static final KeyManager.Storage DEFAULT_TRUST_STORE_FORMAT = KeyManager.Storage.JCEKS;

  public static final String DEFAULT_PRIVATE_KEYSTORE_FILENAME = "tomcat-private.keystore";

  public static final String DEFAULT_TRUSTSTORE_FILENAME = "client.truststore";

  public static final Charset UTF8 = Charset.forName("UTF-8");


  // Class Members --------------------------------------------------------------------------------


  private static final Logger log = Logger.getInstance(Log.CLIENT);


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
        password.getBytes(UTF8)
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
        new UserRegistration(user, null, "Smb9324$#@#@$".getBytes(UTF8))
    );

    System.out.println(response.getStatus() + ": " + response.getStatusInfo());

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

    TrustStore trustStore = TrustStore.create(trustStoreLocation, DEFAULT_TRUST_STORE_FORMAT);

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


  // Instance Fields ------------------------------------------------------------------------------

  private URL serviceRootURL;

  private byte[] credentials;

  private String username;

  private URI trustStoreLocation = null;

  //private WebTarget serviceEndpoint;


  // Constructors ---------------------------------------------------------------------------------


  /**
   * TODO
   *
   * @param serviceRootURL
   * @param username
   * @param credentials
   */
  public AccountManagerClient(URL serviceRootURL, String username, byte[] credentials)
  {
    this.serviceRootURL = serviceRootURL;

    this.username = username;
    this.credentials = credentials;
  }



  // Public Instance Methods ----------------------------------------------------------------------


  public Response create(User user)
  {
    WebTarget target = constructTargetBase(createClient()).path("users");

    Entity<String> jsonEntity = Entity.entity(user.toJSONString(), MediaType.APPLICATION_JSON);

    return sendPost(target, jsonEntity);
  }

  public Response create(UserRegistration user)
  {
    WebTarget target = constructTargetBase(createClient()).path("users");

    Entity<String> jsonEntity = Entity.entity(user.toJSONString(), MediaType.APPLICATION_JSON);

    return sendPost(target, jsonEntity);
  }

  public Response create(CustomerFulfillment fulfillment)
  {
    WebTarget target = constructTargetBase(createClient()).path("users");

    Entity<String> jsonEntity = Entity.entity(
        fulfillment.toJSONString(), CustomerFulfillment.JSON_HTTP_CONTENT_TYPE
    );

    return sendPost(target, jsonEntity);
  }

  public Response retrieveAccountInfo(String username)
  {
    WebTarget target = constructTargetBase(createClient()).path("users/" + username + "/accounts");

    return sendGet(target);
  }


  public Response delete(String username)
  {
    WebTarget target = constructTargetBase(createClient()).path("users/" + username);

    return sendDelete(target);
  }



  public AccountManagerClient setCertificateTrustStore(URI storeLocation)
  {
    this.trustStoreLocation = storeLocation;

    return this;
  }

  public AccountManagerClient createCertificateTrustStore(URI storeLocation, Certificate cert)
      throws InitializationException
  {
    // TODO

    try
    {
      TrustStore trustStore = TrustStore.create(storeLocation, KeyManager.Storage.JCEKS);
      trustStore.addTrustedCertificate("single-certificate-store", cert);

      setCertificateTrustStore(storeLocation);
    }

    catch (Exception exception)
    {
      throw new InitializationException(
          "Creating client''s certificate trust store failed : {0}",
          exception, exception.getMessage()
      );
    }

    return this;
  }



  public AccountManagerClient createTemporaryCertificateTrustStore(Certificate cert)
      throws InitializationException
  {
    File workDir = new File(System.getProperty("user.dir"));
    File temp = new File(workDir, "cert.truststore");

    temp.deleteOnExit();

    return createCertificateTrustStore(temp.toURI(), cert);
  }




  // Private Instance Methods ---------------------------------------------------------------------

  private Response sendGet(WebTarget target)
  {
    Invocation.Builder invocationBuilder = target.request();

    authenticate(invocationBuilder);

    return invocationBuilder.get();
  }

  private Response sendPost(WebTarget target, Entity content)
  {
    Invocation.Builder invocationBuilder = target.request();

    authenticate(invocationBuilder);

    return invocationBuilder.post(content);
  }

  private void authenticate(Invocation.Builder invocation)
  {
    invocation.header(
        "Authorization",
        "Basic " + new String(Base64.encode((username + ":" + new String(credentials)).getBytes(UTF8)))
    );
  }

  private Response sendDelete(WebTarget target)
  {
    Invocation.Builder invocationBuilder = target.request();

    authenticate(invocationBuilder);

    return invocationBuilder.delete();
  }

//
//  private Client createClient(File trustStore) throws InitializationException
//  {
//    ClientBuilder builder = ClientBuilder.newBuilder().hostnameVerifier(new HostnameVerifier()
//    {
//      public boolean verify(String s, SSLSession sslSession)
//      {
//        // TODO
//
//        return true;
//      }
//    });
//
//    try
//    {
//      KeyStore keystore = KeyStore.getInstance(DEFAULT_TRUST_STORE_FORMAT.getStorageName());
//      BufferedInputStream bin = new BufferedInputStream(new FileInputStream(trustStore));
//
//      keystore.load(bin, null);
//
//      builder = builder.trustStore(keystore);
//    }
//
//    catch (Exception exception)
//    {
//      throw new InitializationException("Error loading trust store: {0}", exception.getMessage());
//    }
//
//    return builder.build();
//  }
//

  private WebTarget constructTargetBase(Client client)
  {
    WebTarget target = client
        .target(serviceRootURL.toString())
        .path(REST_ROOT_PATH.toString())
        .path(RPC_REST_PATH.toString())
        .path(SERVICE_PATH.toString())
        .path(new VersionPath().toString());

    return target;
  }






  private Client createClient()
  {
    //System.setProperty("javax.net.ssl.trustStore", "/Users/juha/testTrustStore");

    ClientBuilder builder = ClientBuilder.newBuilder().hostnameVerifier(new HostnameVerifier()
    {
      public boolean verify(String s, SSLSession sslSession)
      {
        // TODO

        return true;  //To change body of implemented methods use File | Settings | File Templates.
      }
    });


    // set up a null trust manager factory -- if a trust store is not configured in this client
    // the null reference will return default trust factories for SSL context...

    TrustManagerFactory tmf = null;

    if (trustStoreLocation != null)
    {
      // If a trust store has been configured for this client, attempt to load the keystore and
      // initialize it for the SSL context...

      try
      {
        KeyStore trustedKeyCertificates = KeyStore.getInstance(DEFAULT_TRUST_STORE_FORMAT.getStorageName());
        FileInputStream fis = new FileInputStream(new File(trustStoreLocation));

        trustedKeyCertificates.load(fis, KeyManager.EMPTY_KEY_PASSWORD);

        tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustedKeyCertificates);
      }

      catch (FileNotFoundException exception)
      {
        throw new ClientConfigurationException(
            "Configured trusted certificate store was not found at ''{0}'': {1}",
            exception, trustStoreLocation, exception.getMessage()
        );
      }

      catch (Exception exception)
      {
        throw new ClientConfigurationException(
            "Error establishing a trust store: {0}", exception.getMessage()
        );
      }
    }

    try
    {
      SSLContext ssl = selectHttpsProtocol();

      ssl.init(null, (tmf == null) ? null : tmf.getTrustManagers(), null /* default secure random */);

      builder.sslContext(ssl);
    }

    catch (KeyManagementException exception)
    {
      throw new ClientConfigurationException(
          "Failed to initialize HTTPS SSL/TLS context : {0}", exception, exception.getMessage()
      );
    }

    return builder.build();
  }

  private SSLContext selectHttpsProtocol()
  {
    try
    {
      return SSLContext.getInstance(HttpsProtocol.TLS_V1_2.getJCAName());
    }

    catch (NoSuchAlgorithmException exception)
    {
      log.debug("TLS 1.2 not available, trying TLS v1.1...");

      try
      {
        return SSLContext.getInstance(HttpsProtocol.TLS_V1_1.getJCAName());
      }

      catch (NoSuchAlgorithmException exception2)
      {
        log.debug("TLS 1.1 not available, trying TLS v1.0...");

        try
        {
          // For some reason at least Oracle JDK 6 seems to require this property to be set
          // before TLS v1.0 is working purely without SSLv2Hello in handshake, not sure why.
          // OpenJDK 6 includes TLS v1.1 so avoids this execution branch...

          System.setProperty("https.protocols", "TLSv1");

          return SSLContext.getInstance(HttpsProtocol.TLS_V1_0.getJCAName());
        }

        catch (NoSuchAlgorithmException exception3)
        {
          throw new ClientConfigurationException(
              "HTTPS TLS protocols not available in the current runtime " +
              "(tried TLS 1.0, TLS 1.1, TLS 1.2)"
          );
        }
      }
    }
  }


  // Nested Enums ---------------------------------------------------------------------------------

  private enum HttpsProtocol
  {
    TLS_V1_0("TLSv1"),
    TLS_V1_1("TLSv1.1"),
    TLS_V1_2("TLSv1.2");


    private String jcaName;

    private HttpsProtocol(String jcaStandardName)
    {
      this.jcaName = jcaStandardName;
    }

    private String getJCAName()
    {
      return jcaName;
    }
  }


  public enum Log implements Hierarchy
  {

    CLIENT("Client");


    private String name;

    private Log(String name)
    {
      this.name = name;
    }

    @Override public String getCanonicalLogHierarchyName()
    {
      return "OpenRemote.AccountManager." + name;
    }
  }


  // Nested Classes -------------------------------------------------------------------------------

  private static class VersionPath extends Version
  {

    private VersionPath()
    {
      super(2, 0, 0);
    }

    @Override public String toString()
    {
      return super.majorVersion + "/" + super.minorVersion + "/" + super.bugfixVersion;
    }
  }


}

