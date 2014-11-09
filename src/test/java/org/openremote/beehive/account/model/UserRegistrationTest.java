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
package org.openremote.beehive.account.model;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import flexjson.JSONTokener;

import org.openremote.base.exception.IncorrectImplementationException;
import org.openremote.model.Account;
import org.openremote.model.Model;
import org.openremote.model.User;


/**
 * Unit tests for {@link org.openremote.beehive.account.model.UserRegistration} class.
 *
 * @author <a href="mailto:juha@openremote.org">Juha Lindfors</a>
 */
public class UserRegistrationTest
{

  // Test Lifecycle -------------------------------------------------------------------------------

  private String userRegistrationJSON;
  private String userRegistrationCharsJSON;

  /**
   * Set up tests with loading sample JSON documents to compare to.
   *
   * @throws IOException  if setup fails
   */
  @BeforeClass public void loadJsonTestFiles() throws IOException
  {
    try
    {
      userRegistrationJSON = loadUserRegistrationJSONFile("user-registration.json");
      userRegistrationCharsJSON = loadUserRegistrationJSONFile("user-registration-characters.json");
    }

    catch (Throwable t)
    {
      System.err.println(String.format("%n%n !!! TEST SETUP FAILURE !!! %n%n "));

      t.printStackTrace();
    }
  }

  /**
   * The tests may be setting their own validators. While tests cleaning up after themselves
   * is nice, not trusting that they always do. Therefore reset to default validators before
   * and after each test.
   */
  @BeforeTest @AfterTest public void resetDefaultValidators()
  {
    User.setNameValidator(User.DEFAULT_NAME_VALIDATOR);
    User.setEmailValidator(User.DEFAULT_EMAIL_VALIDATOR);
  }


  // Constructor Tests ----------------------------------------------------------------------------

  /**
   * Basic constructor tests.
   *
   * @throws Exception  if tests fail
   */
  @Test public void testCtor() throws Exception
  {
    // check the instance data after creation...

    RegistrationData reg = new RegistrationData(new UserRegistration(
        "abc", "some@email.at.somewhere", "passphare".getBytes(UserRegistration.UTF8)
    ));

    Assert.assertTrue(reg.username.equals("abc"));
    Assert.assertTrue(reg.email.equals("some@email.at.somewhere"));
    Assert.assertTrue(reg.accounts.isEmpty());


    // check the JSON data presentation after creation...

    UserRegistration ur = new UserRegistration(
        "foo", "email@host.domain", "secret".getBytes(UserRegistration.UTF8)
    );

    Assert.assertTrue(
        compare(ur.toJSONString(), userRegistrationJSON),
        ur.toJSONString() + "\n\n" + userRegistrationJSON
    );
  }

  /**
   * Basic constructor test with UTF8 charset.
   *
   * @throws Exception  if tests fail
   */
  @Test public void testCtorUTF8() throws Exception
  {
    // check the instance data after creation...

    UserRegistration ur = new UserRegistration(
        "每日一懶", "email@host.domain", "是啊!!三隻肥腸圓滾滾!!".getBytes(UserRegistration.UTF8)
    );

    RegistrationData reg = new RegistrationData(ur);

    Assert.assertTrue(reg.username.equals("每日一懶"));
    Assert.assertTrue(reg.email.equals("email@host.domain"));
    Assert.assertTrue(reg.accounts.isEmpty());

    // check the JSON data presentation...

    Assert.assertTrue(
        compare(ur.toJSONString(), userRegistrationCharsJSON),
        ur.toJSONString() + String.format("%n%n") + userRegistrationCharsJSON
    );
  }


  private byte[] convertToUTF8Bytes(char[] array)
  {
    CharBuffer chars = CharBuffer.wrap(array);
    ByteBuffer bytes = UserRegistration.UTF8.encode(chars);

    byte[] buffer = bytes.array();

    chars.clear();
    bytes.clear();

    clear(array);

    return buffer;
  }

  private void clear(char[] array)
  {
    for (char c : array)
    {
      c = 0;
    }
  }

  private void clear(byte[] array)
  {
    for (byte b : array)
    {
      b = 0;
    }
  }


  /**
   * Test email validation in constructor.
   */
  @Test public void testCtorNotValidEmail()
  {

    // should not be valid with default validator, domain should have minimum 2 chars...

    char[] password = new char[] { 'p', 'a', 's', 's', 'p', 'h', 'r', 'a', 's', 'e' };
    byte[] credentials = convertToUTF8Bytes(password);

    try
    {
      new UserRegistration("abc", "some@email.d", credentials);

      Assert.fail("should not get here...");
    }

    catch (Model.ValidationException e)
    {
      // expected...
    }

    finally
    {
      clear(credentials);
    }


    // should not be valid with default validator, email and host is missing...

    password = new char[] { 'p', 'a', 's', 's', 'p', 'o', 'r', 't' };
    credentials = convertToUTF8Bytes(password);

    try
    {
      new UserRegistration("abc", "@.de", credentials);

      Assert.fail("should not get here...");
    }

    catch (Model.ValidationException e)
    {
      // expected...
    }

    finally
    {
      clear(credentials);
    }


    // should not be valid with default validator, domain is missing...

    password = new char[] { 'p', 'a', 's', 's', 'w', 'o', 'r', 'd' };
    credentials = convertToUTF8Bytes(password);

    try
    {
      new UserRegistration("abc", "some@email", credentials);

      Assert.fail("should not get here...");
    }

    catch (Model.ValidationException e)
    {
      // expected...
    }

    finally
    {
      clear(credentials);
    }


    // 'Some' should not be a valid email with default email validator...

    password = new char[] { 'p', '4', 's', 's', 'w', '0', 'r', 'd' };
    credentials = convertToUTF8Bytes(password);

    try
    {
      new UserRegistration("abc", "some", credentials);

      Assert.fail("should not get here...");
    }

    catch (Model.ValidationException e)
    {
      // expected...
    }

    finally
    {
      clear(credentials);
    }


    // Empty email is not allowed by default validator, use null instead for no email...

    password = new char[] { 'P', '4', 'S', 's', 'W', '0', 'r', 'D' };
    credentials = convertToUTF8Bytes(password);

    try
    {
      new UserRegistration("abc", "", credentials);

      Assert.fail("should not get here...");
    }

    catch (Model.ValidationException e)
    {
      // expected...
    }

    finally
    {
      clear(credentials);
    }


    // Default validator allows null references for email...

    password = new char[] { 'P', '4', 'S', 's', 'P', '0', 'R', '7' };
    credentials = convertToUTF8Bytes(password);

    try
    {
      new UserRegistration("abc", null, credentials);
    }

    catch (Model.ValidationException e)
    {
      Assert.fail("should not get here...");
    }

    finally
    {
      clear(credentials);
    }
  }


  /**
   * Tests constructor when custom email validator has been configured...
   */
  @Test public void testCtorCustomEmailValidator()
  {
    try
    {
      // .de emails should be valid in default email validator...

      char[] password = new char[] { 'p', 'a', 's', 's', 'w', 'o', 'r', 'd' };
      byte[] credentials = convertToUTF8Bytes(password);

      try
      {
        new UserRegistration("abc", "some@email.de", credentials);
      }

      catch (Model.ValidationException e)
      {
        Assert.fail("should not get here...");
      }

      finally
      {
        clear(credentials);
      }


      // change validator to only accept .fi emails...

      UserRegistration.setEmailValidator(new Model.Validator<String>()
      {
        private Pattern regexp = Pattern.compile(".+@.+\\.fi");

        @Override public void validate(String email) throws Model.ValidationException
        {
          Matcher m = regexp.matcher(email);

          if (!m.matches())
          {
            throw new Model.ValidationException("No match '" + email + "'.");
          }
        }
      });


      // .de email should now fail...

      password = new char[] { 'p', 'a', 's', 's', 'w', 'o', 'r', 'd' };
      credentials = convertToUTF8Bytes(password);

      try
      {
        new UserRegistration("abc", "some@email.de", credentials);

        Assert.fail("should not get here...");
      }

      catch (Model.ValidationException e)
      {
        // expected...
      }

      finally
      {
        clear(credentials);
      }


      // .fi email should now pass...

      password = new char[] { 'p', 'a', 's', 's', 'w', 'o', 'r', 'd' };
      credentials = convertToUTF8Bytes(password);

      try
      {
        new UserRegistration("abc", "some@email.fi", credentials);
      }

      catch (Model.ValidationException e)
      {
        Assert.fail("should not get here...");
      }

      finally
      {
        clear(credentials);
      }
    }

    finally
    {
      UserRegistration.setEmailValidator(UserRegistration.DEFAULT_EMAIL_VALIDATOR);
    }
  }


  /**
   * Tests constructor with default username validator.
   */
  @Test public void testValidUsername()
  {
    // basic valid user name....

    char[] password = new char[] { 'p', 'a', 's', 's', 'w', 'o', 'r', 'd' };
    byte[] credentials = convertToUTF8Bytes(password);

    try
    {
      new UserRegistration("abc", "some@email.de", credentials);
    }

    catch (Model.ValidationException e)
    {
      Assert.fail("should not get here...");
    }

    finally
    {
      clear(credentials);
    }



    // empty usernames are not allowed...

    password = new char[] { 'p', 'a', 's', 's', 'w', 'o', 'r', 'd' };
    credentials = convertToUTF8Bytes(password);

    try
    {
      new UserRegistration("", "some@email.de", credentials);

      Assert.fail("should not get here...");
    }

    catch (Model.ValidationException e)
    {
      // expected...
    }

    finally
    {
      clear(credentials);
    }


    // Null usernames are not allowed...

    password = new char[] { 'p', 'a', 's', 's', 'w', 'o', 'r', 'd' };
    credentials = convertToUTF8Bytes(password);

    try
    {
      new UserRegistration(null, "some@email.de", credentials);

      Assert.fail("should not get here...");
    }

    catch (Model.ValidationException e)
    {
      // expected...
    }

    finally
    {
      clear(credentials);
    }
  }


  /**
   * Test constructor user name validation with custom validator.
   */
  @Test public void testCustomUsername()
  {
    try
    {
      // regular name should be accepted...

      char[] password = new char[] { 'p', 'a', 's', 's', 'w', 'o', 'r', 'd' };
      byte[] credentials = convertToUTF8Bytes(password);

      try
      {
        new UserRegistration("somename", "some@email.de", credentials);
      }

      catch (Model.ValidationException e)
      {
        Assert.fail("should not get here...");
      }

      finally
      {
        clear(credentials);
      }


      // Only accept usernames that begin with 'ACME'...

      User.setNameValidator(new Model.Validator<String>()
      {
        @Override public void validate(String s) throws Model.ValidationException
        {
          if (!s.startsWith("ACME"))
          {
            throw new Model.ValidationException("No match in '" + s + "'");
          }
        }
      });


      // Previous regular name should now fail...

      password = new char[] { 'p', 'a', 's', 's', 'w', 'o', 'r', 'd' };
      credentials = convertToUTF8Bytes(password);

      try
      {
        new UserRegistration("somename", "some@email.de", credentials);

        Assert.fail("should not get here...");
      }

      catch (Model.ValidationException e)
      {
        // expected...
      }

      finally
      {
        clear(credentials);
      }


      // ACME username should still be accepted...

      password = new char[] { 'p', 'a', 's', 's', 'w', 'o', 'r', 'd' };
      credentials = convertToUTF8Bytes(password);

      try
      {
        new UserRegistration("ACME-somename", "some@email.de", credentials);
      }

      catch (Model.ValidationException e)
      {
        Assert.fail("should not get here...");
      }

      finally
      {
        clear(credentials);
      }
    }

    finally
    {
      User.setNameValidator(User.DEFAULT_NAME_VALIDATOR);
    }
  }

  /**
   * Test constructor user name validation with custom validator with empty and null user names.
   */
  @Test public void testCustomUsernameEmptyAndNull()
  {
    try
    {
      // Only any username (nulls and empties are still rejected)...

      User.setNameValidator(new Model.Validator<String>()
      {
        @Override public void validate(String s) throws Model.ValidationException
        {
          // accept everything
        }
      });


      // Null usernames should always fail, despite what the validator says...

      char[] password = new char[] { 'p', 'a', 's', 's', 'w', 'o', 'r', 'd' };
      byte[] credentials = convertToUTF8Bytes(password);

      try
      {
        new UserRegistration(null, "some@email.de", credentials);

        Assert.fail("should not get here...");
      }

      catch (Model.ValidationException e)
      {
        // expected...
      }

      finally
      {
        clear(credentials);
      }

      // Empty usernames should always fail, despite what the validator says...

      password = new char[] { 'p', 'a', 's', 's', 'w', 'o', 'r', 'd' };
      credentials = convertToUTF8Bytes(password);

      try
      {
        new UserRegistration("   ", "some@email.de", credentials);

        Assert.fail("should not get here...");
      }

      catch (Model.ValidationException e)
      {
        // expected...
      }

      finally
      {
        clear(credentials);
      }


      // Empty usernames should always fail, despite what the validator says...

      password = new char[] { 'p', 'a', 's', 's', 'w', 'o', 'r', 'd' };
      credentials = convertToUTF8Bytes(password);

      try
      {
        new UserRegistration("\r", "some@email.de", credentials);

        Assert.fail("should not get here...");
      }

      catch (Model.ValidationException e)
      {
        // expected...
      }

      finally
      {
        clear(credentials);
      }


      // Empty usernames should always fail, despite what the validator says...

      password = new char[] { 'p', 'a', 's', 's', 'w', 'o', 'r', 'd' };
      credentials = convertToUTF8Bytes(password);

      try
      {
        new UserRegistration("\n", "some@email.de", credentials);

        Assert.fail("should not get here...");
      }

      catch (Model.ValidationException e)
      {
        // expected...
      }

      finally
      {
        clear(credentials);
      }


      // Empty usernames should always fail, despite what the validator says...

      password = new char[] { 'p', 'a', 's', 's', 'w', 'o', 'r', 'd' };
      credentials = convertToUTF8Bytes(password);

      try
      {
        new UserRegistration("\t", "some@email.de", credentials);

        Assert.fail("should not get here...");
      }

      catch (Model.ValidationException e)
      {
        // expected...
      }

      finally
      {
        clear(credentials);
      }


      // Regular user names should still be accepted...

      password = new char[] { 'p', 'a', 's', 's', 'w', 'o', 'r', 'd' };
      credentials = convertToUTF8Bytes(password);

      try
      {
        new UserRegistration("somename", "some@email.de", credentials);
      }

      catch (Model.ValidationException e)
      {
        Assert.fail("should not get here...");
      }

      finally
      {
        clear(credentials);
      }
    }

    finally
    {
      User.setNameValidator(User.DEFAULT_NAME_VALIDATOR);
    }
  }



  @Test public void testCopyCtor() throws Exception
  {
    UserRegistration ur1 = new UserRegistration(
        "abc", "me@somewhere.country", "pass".getBytes(UserRegistration.UTF8)
    );

    RegistrationData data = new RegistrationData(new UserRegistration(ur1));

    Assert.assertTrue(data.username.equals("abc"));
    Assert.assertTrue(data.email.equals("me@somewhere.country"));
    Assert.assertTrue(data.accounts.isEmpty());
  }


  @Test public void testCopyCtorNull()
  {
    try
    {
      new UserRegistration(null);

      Assert.fail("should not get here...");
    }

    catch (IncorrectImplementationException e)
    {
      // expected...
    }
  }



  @Test public void testCreds() throws Exception
  {
    UserRegistration ur1 = new UserRegistration(
        "foo", "email@host.domain", "secret".getBytes(UserRegistration.UTF8)
    );

    Assert.assertTrue(
        compare(ur1.toJSONString(), userRegistrationJSON),
        ur1.toJSONString() + "\n\n" + userRegistrationJSON
    );
  }


  @Test public void testValidCreds() throws Exception
  {
    try
    {
      new UserRegistration("abc", "me@somewhere.country", null);

      Assert.fail("should not get here...");
    }

    catch (Model.ValidationException e)
    {
      // expected
    }
  }



  // ToJSONString Tests ---------------------------------------------------------------------------

  @Test public void testUserRegistrationJSON() throws Exception
  {
    UserRegistration reg = new UserRegistration(
        "foo", "email@host.domain", "secret".getBytes(UserRegistration.UTF8)
    );

    String json = reg.toJSONString();

    Assert.assertTrue(compare(json, userRegistrationJSON), json + "\n\n" + userRegistrationJSON);
  }


  // Helper Methods -------------------------------------------------------------------------------

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


  private String loadUserRegistrationJSONFile(String name) throws IOException
  {
    File testResourceDirs = new File(
        System.getProperty("openremote.project.resources.dir"), "test"
    );

    File testUserRegistrationDir = new File(testResourceDirs, "user-registration");
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

    //System.err.println("\n\n\n == LOAD FILE ====  \n" + builder.toString());

    return builder.toString();
  }



  // Nested Classes -------------------------------------------------------------------------------

  private static class RegistrationData extends UserRegistration
  {
    private String username;
    private String email;
    private Set<Account> accounts;

    private RegistrationData(UserRegistration reg)
    {
      super(reg);

      this.username = super.username;
      this.email = super.email;
      this.accounts = new HashSet<Account>(super.accounts);
    }
  }
}

