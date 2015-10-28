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
package org.openremote.beehive;

import org.apache.catalina.Engine;
import org.apache.catalina.Host;
import org.apache.catalina.Realm;
import org.apache.catalina.UserDatabase;
import org.apache.catalina.authenticator.BasicAuthenticator;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.NamingContextListener;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.realm.MemoryRealm;
import org.apache.catalina.realm.UserDatabaseRealm;
import org.apache.catalina.startup.Embedded;
import org.apache.catalina.users.MemoryUserDatabaseFactory;
import org.apache.coyote.http11.Http11Protocol;
import org.openremote.security.KeyManager;
import org.openremote.security.PrivateKeyManager;

import java.io.File;
import java.security.cert.Certificate;

/**
 * This implementation configures an embedded Tomcat instance suitable for testing.  <p>
 *
 * For now, the embedded Tomcat depends on file based server configuration
 * (via /resources/tomcat/web.xml) and file based web archive (WAR) deployment via build/webapps
 * directory. Embedded Tomcat can be further modified to support programmatic configuration
 * and deployments.
 *
 * @author <a href = "mailto:juha@openremote.org">Juha Lindfors</a>
 */
public class Tomcat
{

  // Constants ------------------------------------------------------------------------------------

  /**
   * The default connector port used for secure HTTPS connector: {@value} <p>
   *
   * TODO :
   *   currently port configuration is fixed, parameterize if necessary
   */
  public static final int DEFAULT_SECURE_CONNECTOR_PORT = 12123;

  /**
   * The default webapp context URI where the web archive being tested is deployed : {@value}  <p>
   *
   * Typically you'd access this webapp by constructing an URL :
   * scheme://ip-address:port/webapp-context, e.g.
   * https://localhost:{@link #DEFAULT_SECURE_CONNECTOR_PORT} + DEFAULT_WEBAPP_CONTEXT  <p>
   *
   * TODO :
   *   currently web app deployment context URI is fixed, parameterize if necessary
   */
  public final static String DEFAULT_WEBAPP_CONTEXT = "/test";




  /**
   * Relative path to Tomcat's "appBase" directory (where all WAR archives are deployed) under
   * this project's build directory. <p>
   *
   * To create a full path, use {@link #APPBASE}
   *
   * TODO :
   *   currently Tomcat's appBase path is fixed, parameterize if necessary
   */
  private static final String TEST_WEBAPPS_PATH = "webapps";

  /**
   * Relative path to the file-based user database under this project's /resources directory. This
   * file can be used with Tomcat's in-memory realm. <p>
   *
   * To set a full path, combine with {@link #PROJECT_RESOURCES_DIR}, e.g.
   * new File(PROJECT_RESOURCES_DIR, USER_DATABASE_RELATIVE_PATH).
   */
  private static final String USER_DATABASE_RELATIVE_PATH = "/tomcat/users.xml";


  /**
   * File path to this project's /resources directory. This property is set by the Ant build
   * script as part of the unit test JVM set up.
   */
  private static final File PROJECT_RESOURCES_DIR =
      new File(System.getProperty("openremote.project.resources.dir"));

  /**
   * File path to this project's /build directory. This property is set by the Ant build
   * script as part of the unit test JVM set up.
   */
  private static final File PROJECT_BUILD_DIR =
      new File(System.getProperty("openremote.project.build.dir"));


  private static final File APPBASE = new File(PROJECT_BUILD_DIR, TEST_WEBAPPS_PATH);

  private static final File PRIVATE_KEYSTORE_LOCATION = new File(APPBASE, "tomcat-private.keystore");

  private final static String PRIVATE_TEST_KEYSTORE_CREDENTIALS = "ChanGeTh!s";

  private final static String DEFAULT_PRIVATE_TEST_KEYSTORE_KEYALIAS = "tomcat";

  private final static KeyManager.Storage DEFAULT_PRIVATE_TEST_KEYSTORE_FORMAT =
      KeyManager.Storage.JCEKS;



  // Instance Fields ------------------------------------------------------------------------------

  private Embedded server;

  private Certificate httpsCertificate;


  // Constructors ---------------------------------------------------------------------------------

  public Tomcat() throws Exception
  {
    server = new Embedded(buildInMemoryFileRealm());
    server.setName("TomcatTestServer");
    server.setUseNaming(true);

//    server.addAuthenticator(new BasicAuthenticator(), "BASIC");

    Host localhost = server.createHost("localhost", APPBASE.getAbsolutePath());
    localhost.setAutoDeploy(true);

    StandardContext context = (StandardContext) server.createContext(
        DEFAULT_WEBAPP_CONTEXT, createDocBase()
    );

    context.setDefaultWebXml(getTomcatConfig());

    localhost.addChild(context);

    Engine engine = server.createEngine();
    engine.setDefaultHost(localhost.getName());
    engine.setName("TestTomcat");
    engine.addChild(localhost);

    Connector connector = createHTTPSConnector(localhost);

    server.addEngine(engine);
    server.addConnector(connector);
    server.setAwait(true);

    System.out.println(
        "\n\n Deploying to web context " +
        "https://localhost:" + connector.getPort() +
        DEFAULT_WEBAPP_CONTEXT + " from " + context.getDocBase() + "\n\n"
    );
  }

  // Public Instance Methods ----------------------------------------------------------------------

  /**
   * Starts the embedded Tomcat server.
   *
   * @throws Exception    if things fail
   */
  public void start() throws Exception
  {
    if (server != null)
    {
      server.start();
    }
  }

  /**
   * Stops the embedded Tomcat server.
   *
   * @throws Exception  if things fail
   */
  public void stop() throws Exception
  {
    if (server != null)
    {
      server.stop();
    }
  }

  /**
   * Returns the public certificate of the self-signed key used with HTTPS transport
   * encryption.
   *
   * @return  HTTPS connector certificate
   */
  public Certificate getHttpsCertificate()
  {
    return httpsCertificate;
  }


  // Private Instance Methods ---------------------------------------------------------------------

  private Connector createHTTPSConnector(Host tomcatHost) throws Exception
  {
    boolean SECURE = true;

    Connector secureConnector = server.createConnector(
        tomcatHost.getName(), DEFAULT_SECURE_CONNECTOR_PORT, SECURE
    );

    secureConnector.setProtocolHandlerClassName(Http11Protocol.class.getName());
    secureConnector.setAttribute("SSLEnabled", "true");
    secureConnector.setScheme("https");
    secureConnector.setAttribute("clientAuth", "false");
    secureConnector.setAttribute("sslProtocol", "TLS");

    httpsCertificate = createHTTPSKeyStore();

    secureConnector.setAttribute("keystoreFile", PRIVATE_KEYSTORE_LOCATION.getAbsolutePath());
    secureConnector.setAttribute("keystorePass", PRIVATE_TEST_KEYSTORE_CREDENTIALS);
    secureConnector.setAttribute("keystoreType", DEFAULT_PRIVATE_TEST_KEYSTORE_FORMAT.getStorageName());

    return secureConnector;
  }


  private Certificate createHTTPSKeyStore() throws Exception
  {
    PrivateKeyManager privatekey = PrivateKeyManager.create(
        PRIVATE_KEYSTORE_LOCATION.toURI(),
        PRIVATE_TEST_KEYSTORE_CREDENTIALS.toCharArray(),
        DEFAULT_PRIVATE_TEST_KEYSTORE_FORMAT
    );

    // TODO : switch to EC keys...

    return privatekey.addKey(
        DEFAULT_PRIVATE_TEST_KEYSTORE_KEYALIAS,
        PRIVATE_TEST_KEYSTORE_CREDENTIALS.toCharArray(),
        KeyManager.AsymmetricKeyAlgorithm.RSA
    );

  }


  /**
   * Builds a Tomcat in-memory authentication realm from a user database defined in
   * new File({@link #PROJECT_RESOURCES_DIR}, {@link #USER_DATABASE_RELATIVE_PATH}).
   *
   * @return  Tomcat realm
   */
  private Realm buildInMemoryFileRealm()
  {
    File users = new File(PROJECT_RESOURCES_DIR, USER_DATABASE_RELATIVE_PATH);

    MemoryRealm realm = new MemoryRealm();
    realm.setPathname(users.getAbsolutePath());

    return realm;
  }


  private String createDocBase()
  {
    File docBase = new File(APPBASE, "service");

    return docBase.getAbsolutePath();
  }

  private String getTomcatConfig()
  {
    File tomcat = new File(PROJECT_RESOURCES_DIR, "tomcat");
    File web = new File(tomcat, "web.xml");

    return web.getAbsolutePath();
  }

}

