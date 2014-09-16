#!/bin/sh
#  --------------------------------------------------------------------
#  OpenRemote, the Home of the Digital Home.
#  Copyright 2008-2014, OpenRemote Inc.
#
#  See the contributors.txt file in the distribution for a full listing
#  of individual contributors.
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
#  Author: Juha Lindfors (juha@openremote.org)
#
##

function printHelp()
{
  echo "";
  echo "-------------------------------------------------------------";
  echo "";
  echo "  OpenRemote, the Home of the Digital Home.";
  echo "  Copyright 2008-2014, OpenRemote Inc.";
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

if [ "$1" == "help" -o "$1" == "-h" -o "$1" == "--help" -o "$1" == "" ]; then
  printHelp;
fi


# -----------------------------------------------------------------------------
#
#   Set up classpath.
#
# -----------------------------------------------------------------------------

# OpenRemote libs.
CLASSPATH=BeehiveAccountClient-2.0.0.jar
CLASSPATH="$CLASSPATH:lib/openremote-object-model-0.2.0_WIP.jar"
CLASSPATH="$CLASSPATH:lib/openremote-security-0.3.0.jar"

# BouncyCastle libs for OpenRemote security.
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


java \
  -classpath "$CLASSPATH" \
  org.openremote.beehive.account.client.AccountManagerClient $@

