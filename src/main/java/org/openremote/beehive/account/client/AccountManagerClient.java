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
import java.security.cert.Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.bouncycastle.util.encoders.Base64;

import org.openremote.base.Version;
import org.openremote.base.exception.InitializationException;

import org.openremote.security.KeyManager;
import org.openremote.security.TrustStore;

import org.openremote.model.User;

import org.openremote.beehive.account.model.CustomerFulfillment;
import org.openremote.beehive.account.model.UserRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This is a convenience client Java API for executing the REST RPC operations on Beehive account
 * manager service. It helps in abstracting away the details of the HTTP layer; the JSON transfer
 * documents are handled by the Java domain objects and the mapping of operations to HTTP URLs
 * and use of correct media types are handled by this API. <p>
 *
 * For examples on how to directly invoke corresponding operations against the service's HTTP REST
 * API with 'curl', refer to the examples in the documentation at http://www.openremote.org/x/vBJoAQ
 *
 * @see #create(org.openremote.beehive.account.model.UserRegistration)
 * @see #create(org.openremote.beehive.account.model.CustomerFulfillment)
 * @see #delete(String)
 *
 * @author Juha Lindfors
 *
 *
 * TODO : document/link to client configuration API above.
 *
 */
public class AccountManagerClient
{

  // Constants ------------------------------------------------------------------------------------


  /**
   * The default root path for the REST API if the remote service has been configured in its
   * default REST API path '/rest/rpc/accountmanager/[major]/[minor]/[bugfix-version]/' path
   * (see the remote service's web.xml for servlet path mapping for details).
   *
   * TODO : make externally configurable
   */
  public static final URI REST_ROOT_PATH = URI.create("rest");

  /**
   * The default REST API path identifying RPC style REST invocations to the remote service
   * when it has been configured with its default REST API path
   * '/rest/rpc/accountmanager/[major]/[minor]/[bugfix-version]/'
   * (see the remote service's web.xml for servlet path mapping for details).
   *
   * TODO : make externally configurable
   */
  public static final URI RPC_REST_PATH = URI.create("rpc");

  /**
   * The default REST API path identifying the service name of the remote service
   * when it has been configured with its default REST API path
   * '/rest/rpc/accountmanager/[major]/[minor]/[bugfix-version]/'
   * (see the remote service's web.xml for servlet path mapping for details).
   *
   * TODO : make externally configurable
   */
  public static final URI SERVICE_PATH = URI.create("accountmanager");

  /**
   * Default storage format used for trust store when key certificates not signed by global
   * certificate authorities are used.
   */
  public static final KeyManager.Storage DEFAULT_TRUST_STORE_FORMAT = KeyManager.Storage.JCEKS;

  /**
   * UTF-8 charset used for byte-to-string conversions.
   */
  public static final Charset UTF8 = Charset.forName("UTF-8");



  // Class Members --------------------------------------------------------------------------------


  /**
   * Account manager client logging.
   */
  private static final Logger log = LoggerFactory.getLogger(Log.CLIENT.getCanonicalLogHierarchyName());


  public static void main(String... args) throws Exception
  {
    Tools.main(args);
  }



  // Instance Fields ------------------------------------------------------------------------------

  private URL serviceRootURL;

  private byte[] credentials;

  private String username;

  private URI trustStoreLocation = null;

  private String httpsProtocolJcaName = null;


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



  public AccountManagerClient setHttpsProtocol(String jcaProtocolName)
  {
    this.httpsProtocolJcaName = jcaProtocolName;

    return this;
  }

  public AccountManagerClient setHttpsProtocol(HttpsProtocol protocol)
  {
    return setHttpsProtocol(protocol.getJCAName());
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
    // TODO : catch javax.ws.rs.ProcessingException

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






  private Client createClient() //throws ClientConfigurationException
  {
    //System.setProperty("javax.net.ssl.trustStore", "/Users/juha/testTrustStore");

    ClientBuilder builder = ClientBuilder.newBuilder();


    // set up a null trust manager factory -- if a trust store is not configured in this client
    // the null reference will return default trust factories for SSL context...

    TrustManagerFactory tmf = null;

    if (trustStoreLocation != null)
    {
      // If a trust store has been configured for this client, attempt to load the keystore and
      // initialize it for the SSL context...

      try
      {
        KeyStore trustedKeyCertificates =
            KeyStore.getInstance(DEFAULT_TRUST_STORE_FORMAT.getStorageName());

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
    if (httpsProtocolJcaName != null)
    {
      try
      {
        return SSLContext.getInstance(httpsProtocolJcaName);
      }

      catch (NoSuchAlgorithmException exception)
      {
        throw new ClientConfigurationException(
            "Configured HTTPS algorithm ''{0}'' is not available: {1}",
            httpsProtocolJcaName, exception.getMessage()
        );
      }
    }


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


  public enum Log
  {

    CLIENT("Client");


    private String name;

    private Log(String name)
    {
      this.name = name;
    }

    public String getCanonicalLogHierarchyName()
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

