#!/bin/sh
#  --------------------------------------------------------------------
#  Copyright 2013-2015, Juha Lindfors. All rights reserved.
#
#  This program is free software; you can redistribute it and/or modify
#  it under the terms of the GNU Affero General Public License as
#  published by the Free Software Foundation; either version 3 of the
#  License, or (at your option) any later version.
#
#  This program is distributed in the hope that it will be useful, but
#  WITHOUT ANY WARRANTY; without even the implied warranty of
#  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
#  Affero General Public License for more details.
#
#  You should have received a copy of the GNU Affero General Public
#  License along with this program; if not, see
#  http://www.gnu.org/licenses/.
#  --------------------------------------------------------------------
#
##
#  This is a convenience script to run client-side utilities
#  for Beehive Account service.
#
#    > sh beehive-account-client.sh --help
#
#  to view usage details.
#
#  Author: Juha Lindfors
#
##

printHelp()
{
  echo "";
  echo "-------------------------------------------------------------";
  echo "";
  echo "  Beehive Account Manager Client Utilities.";
  echo "";
  echo "-------------------------------------------------------------";
  echo "";
  echo "  Usage: ";
  echo "";
  echo "    sh beehive-account-client.sh --deployment-test <URL>";
  echo "";
  echo "         - Executes some test requests against live service to";
  echo "           ensure deployment works correctly.";
  echo "";
  echo "";
  echo "    sh beehive-account-client.sh --generate-keys <passwd> [-alias <alias>]";
  echo "";
  echo "         - Creates a new self-signed key pair and appropriate";
  echo "           Tomcat private key store and client trusted";
  echo "           certificate store files.";
  echo "";
  echo "";

  exit 0;
}

if [ "$1" = "help" -o "$1" = "-h" -o "$1" = "--help" -o "$1" = "" ]; then
  printHelp;
fi


# -----------------------------------------------------------------------------
#
#   Set up classpath.
#
# -----------------------------------------------------------------------------

# OpenRemote libs.
CLASSPATH=BeehiveAccountClient-2.0.0.jar
CLASSPATH="$CLASSPATH:lib/object-model-0.2.0_WIP.jar"
CLASSPATH="$CLASSPATH:lib/openremote-security-0.3.1.jar"

# Logging
CLASSPATH="$CLASSPATH:lib/slf4j-api-1.7.12.jar"

# FlexJSON lib for JSON processing
CLASSPATH="$CLASSPATH:lib/flexjson-3.2.jar"

# BouncyCastle libs for security.
CLASSPATH="$CLASSPATH:lib/bcprov-jdk15on-150.jar"
CLASSPATH="$CLASSPATH:lib/bcpkix-jdk15on-150.jar"

# JAX-RS API and Jersey client implementation + Jersey repackaged
# Google Guava implementation.
CLASSPATH="$CLASSPATH:lib/javax.ws.rs-api-2.0.jar"
CLASSPATH="$CLASSPATH:lib/jersey-client-2.6.jar"
CLASSPATH="$CLASSPATH:lib/jersey-common-2.6.jar"
CLASSPATH="$CLASSPATH:lib/jersey-guava-2.6.jar"

# Java Inject API and Glassfish HK2 dependency injection framework.
CLASSPATH="$CLASSPATH:lib/javax.inject.jar"
CLASSPATH="$CLASSPATH:lib/hk2-api-2.2.0.jar"
CLASSPATH="$CLASSPATH:lib/hk2-locator-2.2.0.jar"
CLASSPATH="$CLASSPATH:lib/hk2-utils-2.2.0.jar"

# Javassist bytecode lib dependency.
CLASSPATH="$CLASSPATH:lib/javassist-3.18.1-GA.jar"

# Add main class as JVM argument
JVM_ARGS="org.openremote.beehive.account.client.AccountManagerClient"


# -----------------------------------------------------------------------------
#
#  Debug options:
#
#   To debug SSL handshake from the client, add the Java system property
#   '-Djavax.net.debug=ssl:handshake' to the JVM execution below. See
#   JVM documentation for additional SSL/TLS debug options.
#
# -----------------------------------------------------------------------------

# Uncomment the line below to enable SSL handshake debugging. See JVM
# documentation for additional SSL/TLS debug options

##DEBUG_SSL_CLIENT="-Djavax.net.debug=ssl:handshake

if [ -n "$DEBUG_SSL_CLIENT" ]; then
  JVM_ARGS="$DEBUG_SSL_CLIENT $JVM_ARGS"
fi


# -----------------------------------------------------------------------------
#
#  HTTPS Transport Layer Security:
#
#   The implementation strictly enforces TLS v1.1 and thus disables SSL
#   protocols due to POODLE (CVE-2014-3566) SSLv3 vulnerability. To override
#   the HTTPS security protocols being used, uncomment the line below.
#
#   Note that the availability of TLS protocol versions depends on the JVM
#   version and vendor:
#
#                        TLSv1      TLSv1.1     TLSv1.2
#
#   Oracle JDK 6           x
#   OpenJDK 6              x           x
#   Oracle JDK 7           x           x           x
#   OpenJDK 7              x           x           x
#
# -----------------------------------------------------------------------------

# Uncomment the line below to override the HTTPS security protocol used
# in the implementation.

##HTTPS_PROTOCOLS="-Dhttps.protocols=TLSv1.1,TLSv1.2"

if [ -n "$HTTPS_PROTOCOLS" ]; then
  JVM_ARGS="$HTTPS_PROTOCOLS $JVM_ARGS"
fi


# Run...

java -classpath "$CLASSPATH" $JVM_ARGS $@

