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
package org.openremote.beehive.account.model;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.Arrays;

import org.openremote.base.Defaults;

import org.openremote.model.User;


/**
 * This domain object extends the class {@link org.openremote.model.User} from object
 * model with user details that are transferred between services as part of the registration
 * process. In particular, it adds fields that would normally not traverse between systems
 * after the registration process is complete. <p>
 *
 * This implementation is specific to this Beehive Account Manager implementation, and therefore
 * not part of the shared OpenRemote object model. Account and user registrations should occur
 * through this Account Manager service. The account service can then delegate relevant
 * registration information and manage registration processes further in the back-end systems.
 *
 * @author Juha Lindfors
 */
public class UserRegistration extends User
{

  // Constants ------------------------------------------------------------------------------------


  public static final String JSON_HTTP_CONTENT_TYPE =
      "application/vnd.openremote.user-registration+json";


//
//  /**
//   * Default credentials minimum length constraint for user registrations: {@value}
//   */
//  public static final int DEFAULT_CREDENTIALS_MIN_LEN = 10;
//
//  /**
//   * Default credentials maximum length, equaling to the database schema and serialization
//   * schema maximums defined in {@link Model#DEFAULT_STRING_ATTRIBUTE_LENGTH_CONSTRAINT}: {@value}
//   */
//  public static final int DEFAULT_CREDENTIALS_MAX_LEN =
//      Model.DEFAULT_STRING_ATTRIBUTE_LENGTH_CONSTRAINT;
//



  // Class Initializers ---------------------------------------------------------------------------
//
//  static
//  {
//    try
//    {
//      setCredentialsSizeConstraint(DEFAULT_CREDENTIALS_MIN_LEN, DEFAULT_CREDENTIALS_MAX_LEN);
//    }
//
//    catch (Throwable t)
//    {
//      t.printStackTrace(); // todo log
//    }
//  }


  // Class Members --------------------------------------------------------------------------------

//
// TODO :
//        the validation needs to be implemented in Object Model User class' addAttribute
//        and then in relational entity's authentication object.
//
//  private static javax.validation.Validator validator;
//
//
//  public static void setCredentialsSizeConstraint(int min, int max)
//  {
//    if (min < 0 || max > Model.DEFAULT_STRING_ATTRIBUTE_LENGTH_CONSTRAINT)
//    {
//      // TODO : log
//      return;
//    }
//
//    setCredentialsConstraint(new SizeDef().min(min).max(max));
//  }
//
//  public static void setCredentialsConstraint(ConstraintDef... constraints)
//  {
//    HibernateValidatorConfiguration config =
//        Validation.byProvider(HibernateValidator.class).configure();
//
//    ConstraintMapping credentialsMapping = config.createConstraintMapping();
//
//    PropertyConstraintMappingContext property = credentialsMapping
//        .type(UserRegistration.class)
//        .property("credentials", ElementType.FIELD);
//
//    for (ConstraintDef c : constraints)
//    {
//      property.constraint(c);
//    }
//
//    config.addMapping(credentialsMapping);
//
//    validator = config.buildValidatorFactory().getValidator();
//  }


  /**
   * Converts character array to UTF8 bytes without relying on String.getBytes(). Array is
   * cleared when this method is done.
   */
  public static byte[] convertToUTF8Bytes(char[] array)
  {
    CharBuffer chars = CharBuffer.wrap(array);
    ByteBuffer bytes = UserRegistration.UTF8.encode(chars);

    byte[] buffer = Arrays.copyOf(bytes.array(), bytes.limit());

    chars.clear();
    bytes.clear();

    clear(array);

    return buffer;
  }

  public static void clear(char[] array)
  {
    for (char c : array)
    {
      c = 0;
    }
  }

  public static void clear(byte[] array)
  {
    for (byte b : array)
    {
      b = 0;
    }
  }


  private static byte[] extractAuthCredentials(Authentication auth)
  {
    return new UserAuthentication(auth).credentials;
  }



  // Constructors ---------------------------------------------------------------------------------

  /**
   * Creates a new user registration with a given user name, email and login credentials.
   *
   * @param username
   *          A unique username in this account manager. The username must match the validation
   *          constraints defined in {@link User#DEFAULT_NAME_VALIDATOR} or set in a custom
   *          validator via {@link User#setNameValidator}. A username cannot be null or empty
   *          string. The maximum
   *
   * @param email
   *          An email address for the user (for registration confirmation, and such). Must be
   *          a valid email as defined in {@link User#DEFAULT_EMAIL_VALIDATOR} or as set in a
   *          custom validator via {@link User#DEFAULT_EMAIL_VALIDATOR}. The email validator
   *          may be implemented to accept empty or null emails.
   *
   * @param credentials TODO
   *
   * @throws ValidationException
   */
  public UserRegistration(String username, String email, byte[] credentials)
      throws ValidationException
  {
    super(username, email);

    addAttribute(
        User.CREDENTIALS_ATTRIBUTE_NAME,
        (credentials == null) ? "" : new String(credentials)
    );

    addAttribute(
        User.AUTHMODE_ATTRIBUTE_NAME,
        User.CredentialsEncoding.SCRYPT.getEncodingName()
    );

    validate();
  }


  // TODO : review

  protected UserRegistration(UserRegistration copy)
  {
    this(
        copy,
        (copy == null)
            ? null
            : copy.getAttribute(User.CREDENTIALS_ATTRIBUTE_NAME).getBytes(Defaults.DEFAULT_CHARSET)
    );
  }

  // TODO : review

  private UserRegistration(User user, byte[] credentials)
  {
    super(user);

    addAttribute(
        User.CREDENTIALS_ATTRIBUTE_NAME,
        (credentials == null) ? "" : new String(credentials)
    );

    addAttribute(
        User.AUTHMODE_ATTRIBUTE_NAME,
        User.CredentialsEncoding.SCRYPT.getEncodingName()
    );
  }

  // TODO : review

  public UserRegistration(User user, Authentication authentication)
  {
    this(user, extractAuthCredentials(authentication));

    UserAuthentication auth = new UserAuthentication(authentication);

    addAttribute(User.AUTHMODE_ATTRIBUTE_NAME, auth.encoding.getEncodingName());

  }


  // Object Overrides -----------------------------------------------------------------------------

  @Override public String toString()
  {
    return "User: " + username + ", " + email;
  }


  // Protected Instance Methods -------------------------------------------------------------------

  protected void validate() throws ValidationException
  {
//    Set<ConstraintViolation<UserRegistration>> errors = validator.validate(this);
//
//    if (!errors.isEmpty())
//    {
//      String messages = "";
//
//      for (ConstraintViolation violation : errors)
//      {
//        messages = messages + violation.getPropertyPath() + " ";
//        messages = messages + violation.getMessage();
//      }
//
//      throw new ValidationException(messages);
//    }
  }


  // Nested Classes -------------------------------------------------------------------------------


  private static class UserAuthentication extends Authentication
  {
    private byte[] credentials = super.getCredentials();
    private CredentialsEncoding encoding = super.encoding;

    private UserAuthentication(Authentication copy)
    {
      super(copy);
    }
  }

}

