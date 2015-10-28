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
package org.openremote.beehive.account.model;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import flexjson.JSONTokener;

import org.openremote.base.exception.IncorrectImplementationException;

import org.openremote.model.Account;
import org.openremote.model.Model;
import org.openremote.model.User;

import static org.openremote.beehive.account.model.UserRegistration.convertToUTF8Bytes;
import static org.openremote.beehive.account.model.UserRegistration.clear;

/**
 * Unit tests for {@link org.openremote.beehive.account.model.UserRegistration} class.
 *
 * @author Juha Lindfors
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
  @BeforeMethod @AfterMethod public void resetDefaultValidators()
  {
    User.setNameValidator(User.DEFAULT_NAME_VALIDATOR);
    User.setEmailValidator(User.DEFAULT_EMAIL_VALIDATOR);
//    UserRegistration.setCredentialsSizeConstraint(
//        UserRegistration.DEFAULT_CREDENTIALS_MIN_LEN,
//        UserRegistration.DEFAULT_CREDENTIALS_MAX_LEN
//    );
  }


  // Base Constructor Tests -----------------------------------------------------------------------

  /**
   * Basic constructor tests.
   *
   * @throws Exception
   *            if tests fail
   */
/*  @Test public void testCtor() throws Exception
  {
    char[] password = new char[] { 'p', 'a', 's', 's', 'p', 'h', 'r', 'a', 's', 'e' };
    byte[] credentials = convertToUTF8Bytes(password);

    try
    {
      // check the instance data after creation...

      RegistrationData reg = new RegistrationData(new UserRegistration(
          "abc", "some@email.at.somewhere", credentials
      ));

      Assert.assertTrue(reg.username.equals("abc"));
      Assert.assertTrue(reg.email.equals("some@email.at.somewhere"));
      Assert.assertTrue(reg.accounts.isEmpty());


      // check the JSON data presentation after creation...

      UserRegistration.setCredentialsSizeConstraint(6, 6);

      password = new char[] { 's', 'e', 'c', 'r', 'e', 't' };
      credentials = convertToUTF8Bytes(password);

      UserRegistration ur = new UserRegistration("foo", "email@host.domain", credentials);

      Assert.assertTrue(
          compare(ur.toJSONString(), userRegistrationJSON),
          ur.toJSONString() + "\n\n" + userRegistrationJSON
      );
    }

    finally
    {
      clear(credentials);

      UserRegistration.setCredentialsSizeConstraint(
          UserRegistration.DEFAULT_CREDENTIALS_MIN_LEN,
          UserRegistration.DEFAULT_CREDENTIALS_MAX_LEN
      );
    }
  }
*/

  /**
   * Basic constructor test with UTF8 charset.
   *
   * @throws Exception  if tests fail
   */
  @Test public void testCtorUTF8() throws Exception
  {
    // check the instance data after creation...

    char[] password = new char[] { '是', '啊', '!', '!', '三', '隻', '肥', '腸', '圓', '滾', '滾', '!', '!' };
    byte[] credentials = convertToUTF8Bytes(password);

    try
    {
      UserRegistration ur = new UserRegistration("每日一懶", "email@host.domain", credentials);

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

    finally
    {
      clear(credentials);
    }
  }


  // Base Constructor Email Validation Tests ------------------------------------------------------

  /**
   * Test email validation in constructor, invalid host.
   */
  @Test (expectedExceptions = Model.ValidationException.class)

  public void testCtorNotValidEmail1() throws Exception
  {
    char[] password = new char[] { 'p', 'a', 's', 's', 'p', 'h', 'r', 'a', 's', 'e' };
    byte[] credentials = convertToUTF8Bytes(password);

    try
    {
      // should not be valid with default validator, domain should have minimum 2 chars...

      new UserRegistration("abc", "some@email.d", credentials);
    }

    finally
    {
      clear(credentials);
    }
  }


  /**
   * Test email validation in constructor, malformed email format.
   */
  @Test (expectedExceptions = Model.ValidationException.class)

  public void testCtorNotValidEmail2() throws Exception
  {
    char[] password = new char[] { 'p', 'a', 's', 's', 'p', 'o', 'r', 't', '1', '2', '3' };
    byte[] credentials = convertToUTF8Bytes(password);

    // should not be valid with default validator, email and host is missing...

    try
    {
      new UserRegistration("abc", "@.de", credentials);
    }

    finally
    {
      clear(credentials);
    }
  }

  /**
   * Test email validation in constructor, no domain.
   */
  @Test (expectedExceptions = Model.ValidationException.class)

  public void testCtorNotValidEmail3() throws Exception
  {
    char[] password = new char[] { 'p', 'a', 's', 's', 'w', 'o', 'r', 'd', '3', '2', '1' };
    byte[] credentials = convertToUTF8Bytes(password);

    try
    {
      // should not be valid with default validator, domain is missing...

      new UserRegistration("abc", "some@email", credentials);
    }

    finally
    {
      clear(credentials);
    }
  }


  /**
   * Test email validation in constructor, not an email format.
   */
  @Test (expectedExceptions = Model.ValidationException.class)

  public void testCtorNotValidEmail4() throws Exception
  {
    char[] password = new char[] { 'p', '4', 's', 's', 'w', '0', 'r', 'd', '4', '5', '6' };
    byte[] credentials = convertToUTF8Bytes(password);

    try
    {
      // 'Some' should not be a valid email with default email validator...

      new UserRegistration("abc", "some", credentials);
    }

    finally
    {
      clear(credentials);
    }
  }


  /**
   * Test email validation in constructor, empty string is not valid.
   */
  @Test (expectedExceptions = Model.ValidationException.class)

  public void testCtorNotValidEmail5() throws Exception
  {
    char[] password = new char[] { 'P', '4', 'S', 's', 'W', '0', 'r', 'D', '6', '5', '4' };
    byte[] credentials = convertToUTF8Bytes(password);

    try
    {
      // Empty email is not allowed by default validator, use null instead for no email...

      new UserRegistration("abc", "", credentials);
    }

    finally
    {
      clear(credentials);
    }
  }


  /**
   * Test email validation in constructor, nulls are allowed.
   */
  @Test public void testCtorNullEmail() throws Exception
  {
    char[] password = new char[] { 'P', '4', 'S', 's', 'P', '0', 'R', '7', '8', '9' };
    byte[] credentials = convertToUTF8Bytes(password);

    try
    {
      // Default validator allows null references for email...

      UserRegistration ur = new UserRegistration("abc", null, credentials);

      Assert.assertTrue(new RegistrationData(ur).email.equals(""));
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

      char[] password = new char[] { 'p', 'a', 's', 's', 'w', 'o', 'r', 'd', '1', '2' };
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

      password = new char[] { 'p', 'a', 's', 's', 'w', 'o', 'r', 'd', '1', '2' };
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

      password = new char[] { 'p', 'a', 's', 's', 'w', 'o', 'r', 'd', '1', '2' };
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
   * Test email field values that exceed the serialization and database schema length limit.
   */
  @Test (expectedExceptions = Model.ValidationException.class)

  public void testOverlongEmailInConstructor() throws Exception
  {
    char[] password = new char[] { 'p', 'a', 's', 's', 'w', 'o', 'r', 'd', '1', '2' };
    byte[] credentials = convertToUTF8Bytes(password);

    StringBuilder b = new StringBuilder();
    b.append("a@b.");

    int size = b.length();

    for (int i = 0; i <= Model.DEFAULT_STRING_ATTRIBUTE_LENGTH_CONSTRAINT - size; ++i)
    {
      b.append('c');
    }

    String email = b.toString();

    try
    {
      new UserRegistration("abc", email, credentials);
    }

    finally
    {
      clear(credentials);
    }
  }


  /**
   * Test email field values that exceed the serialization and database schema length limit
   * (with custom validator)
   */
  @Test (expectedExceptions = Model.ValidationException.class)

  public void testOverlongEmailCustomValidatorInConstructor() throws Exception
  {
    char[] password = new char[] { 'p', 'a', 's', 's', 'w', 'o', 'r', 'd', '1', '2' };
    byte[] credentials = convertToUTF8Bytes(password);

    User.setEmailValidator(new Model.Validator<String>()
    {
      @Override public void validate(String s) throws Model.ValidationException
      {
        // accept everything...
      }
    });

    StringBuilder b = new StringBuilder();
    b.append("a@b.");

    int size = b.length();

    for (int i = 0; i <= Model.DEFAULT_STRING_ATTRIBUTE_LENGTH_CONSTRAINT - size; ++i)
    {
      b.append('c');
    }

    String email = b.toString();

    try
    {
      new UserRegistration("abc", email, credentials);
    }

    finally
    {
      clear(credentials);

      User.setEmailValidator(User.DEFAULT_EMAIL_VALIDATOR);
    }
  }


  // Base Constructor Name Validation Tests -------------------------------------------------------

  /**
   * Tests constructor with default username validator.
   */
  @Test public void testValidUsername() throws Exception
  {
    char[] password = new char[] { 'p', 'a', 's', 's', 'w', 'o', 'r', 'd', '1', '2' };
    byte[] credentials = convertToUTF8Bytes(password);

    try
    {
      // basic valid user name....

      UserRegistration ur = new UserRegistration("abc", "some@email.de", credentials);

      Assert.assertTrue(new RegistrationData(ur).username.equals("abc"));
      Assert.assertTrue(new RegistrationData(ur).email.equals("some@email.de"));
    }

    finally
    {
      clear(credentials);
    }
  }


  /**
   * Tests constructor with default username validator and empty string.
   */
  @Test (expectedExceptions = Model.ValidationException.class)

  public void testInvalidUsername1() throws Exception
  {
    char[] password = new char[] { 'p', 'a', 's', 's', 'w', 'o', 'r', 'd', '1', '2', '3' };
    byte[] credentials = convertToUTF8Bytes(password);

    try
    {
      // empty usernames are not allowed...

      new UserRegistration("", "some@email.de", credentials);
    }

    finally
    {
      clear(credentials);
    }
  }

  /**
   * Tests constructor with default username validator and null username.
   */
  @Test (expectedExceptions = Model.ValidationException.class)

  public void testInvalidUsername2() throws Exception
  {
    char[] password = new char[] { 'p', 'a', 's', 's', 'w', 'o', 'r', 'd', '1', '2', '3' };
    byte[] credentials = convertToUTF8Bytes(password);

    try
    {
      // Null usernames are not allowed...

      new UserRegistration(null, "some@email.de", credentials);
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
      char[] password = new char[] { 'p', 'a', 's', 's', 'w', 'o', 'r', 'D', '1', '2' };
      byte[] credentials = convertToUTF8Bytes(password);

      try
      {
        // regular name should be accepted...

        UserRegistration ur = new UserRegistration("somename", "some@email.de", credentials);

        Assert.assertTrue(new RegistrationData(ur).username.equals("somename"));
        Assert.assertTrue(new RegistrationData(ur).email.equals("some@email.de"));
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

      password = new char[] { 'p', 'a', 's', 's', 'w', 'o', 'r', 'd', '3', '4' };
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

      password = new char[] { 'p', 'a', 's', 'S', 'w', 'o', 'r', 'd', '5', '5' };
      credentials = convertToUTF8Bytes(password);

      try
      {
        RegistrationData data = new RegistrationData(
            new UserRegistration("ACME-somename", "some@email.de", credentials)
        );

        Assert.assertTrue(data.username.equals("ACME-somename"));
        Assert.assertTrue(data.email.equals("some@email.de"));
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
      // Accept any username (nulls and empties are still rejected)...

      User.setNameValidator(new Model.Validator<String>()
      {
        @Override public void validate(String s) throws Model.ValidationException
        {
          // accept everything
        }
      });


      // Null usernames should always fail, despite what the validator says...

      char[] password = new char[] { 'p', 'a', 's', 's', 'w', 'o', 'r', 'd', '3', '2', '1' };
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

      password = new char[] { 'p', 'a', 's', 's', 'w', 'o', 'r', 'd', '9', '8', '7' };
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

      password = new char[] { 'p', 'a', 's', 's', 'w', 'o', 'r', 'd', '3', '3', '3' };
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

      password = new char[] { 'p', 'a', 's', 's', 'w', 'o', 'r', 'd', '5', '5' };
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

      password = new char[] { 'p', 'a', 's', 's', 'w', 'o', 'r', 'd', '1', '2', '3', '4' };
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

      password = new char[] { 'p', 'a', 's', 's', 'w', 'o', 'r', 'd', 'd', 'r', 'o', 'w' };
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



  // Base Constructor Credentials Validation Tests ------------------------------------------------


  /**
   * Test constructor credentials mapping to JSON serialization document.
   *
   * @throws Exception
   *            if test fails
   */
/*  @Test public void testCtorCreds() throws Exception
  {
    UserRegistration.setCredentialsSizeConstraint(6, 6);

    try
    {
      char[] password = new char[] { 's', 'e', 'c', 'r', 'e', 't' };
      byte[] credentials = convertToUTF8Bytes(password);

      UserRegistration ur1 = new UserRegistration("foo", "email@host.domain", credentials);

      Assert.assertTrue(
          compare(ur1.toJSONString(), userRegistrationJSON),
          ur1.toJSONString() + "\n\n" + userRegistrationJSON
      );
    }

    finally
    {
      UserRegistration.setCredentialsSizeConstraint(
          UserRegistration.DEFAULT_CREDENTIALS_MIN_LEN,
          UserRegistration.DEFAULT_CREDENTIALS_MAX_LEN
      );
    }
  }
*/

  /**
   * Test credentials not null constraint on constructor call
   *
   * @throws Exception
   *            if test fails
   */
  @Test (expectedExceptions = Model.ValidationException.class)

  public void testCtorNullCreds() throws Exception
  {
    new UserRegistration("abc", "me@somewhere.country", null);
  }



  // TODO : need tests for upper limit hard constraints (db schema caused)



  // Copy Constructor Tests -----------------------------------------------------------------------
/*
  @Test public void testCopyCtor() throws Exception
  {
    UserRegistration.setCredentialsSizeConstraint(4, 4);

    try
    {
      UserRegistration ur1 = new UserRegistration(
          "abc", "me@somewhere.country", "pass".getBytes(UserRegistration.UTF8)
      );

      RegistrationData data = new RegistrationData(new UserRegistration(ur1));

      Assert.assertTrue(data.username.equals("abc"));
      Assert.assertTrue(data.email.equals("me@somewhere.country"));
      Assert.assertTrue(data.accounts.isEmpty());
      Assert.assertTrue(data.getAttribute("credentials").equals("pass"));
      Assert.assertTrue(data.getAttribute("authMode").equals("scrypt"));
    }

    finally
    {
      UserRegistration.setCredentialsSizeConstraint(
          UserRegistration.DEFAULT_CREDENTIALS_MIN_LEN,
          UserRegistration.DEFAULT_CREDENTIALS_MAX_LEN
      );
    }
  }
*/

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





  // ToJSONString Tests ---------------------------------------------------------------------------
/*
  @Test public void testUserRegistrationJSON() throws Exception
  {
    UserRegistration.setCredentialsSizeConstraint(6, 6);

    char[] password = new char[] { 's', 'e', 'c', 'r', 'e', 't' };
    byte[] credentials = convertToUTF8Bytes(password);

    try
    {
      UserRegistration reg = new UserRegistration(
          "foo", "email@host.domain", credentials
      );

      String json = reg.toJSONString();

      Assert.assertTrue(compare(json, userRegistrationJSON), json + "\n\n" + userRegistrationJSON);
    }

    finally
    {
      UserRegistration.setCredentialsSizeConstraint(
          UserRegistration.DEFAULT_CREDENTIALS_MIN_LEN,
          UserRegistration.DEFAULT_CREDENTIALS_MAX_LEN
      );
    }
  }
*/

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

    return builder.toString().trim();
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

