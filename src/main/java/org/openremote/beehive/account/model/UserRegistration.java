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

import org.openremote.model.Model;
import org.openremote.model.User;
import org.openremote.model.data.json.UserTransformer;
import org.openremote.model.persistence.jpa.RelationalUser;


/**
 * This domain object extends the class {@link org.openremote.model.User} from OpenRemote object
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

  /**
   * Default character set used in string-to-byte conversions.
   */
  public static final Charset UTF8 = Charset.forName("UTF-8");

  /**
   * Default credentials minimum length constraint for user registrations: {@value}
   */
  public static final int DEFAULT_CREDENTIALS_MIN_LEN = 10;

  /**
   * Default credentials maximum length, equaling to the database schema and serialization
   * schema maximums defined in {@link Model#DEFAULT_STRING_ATTRIBUTE_LENGTH_CONSTRAINT}: {@value}
   */
  public static final int DEFAULT_CREDENTIALS_MAX_LEN =
      Model.DEFAULT_STRING_ATTRIBUTE_LENGTH_CONSTRAINT;




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
   * @param credentials
   *
   * @throws ValidationException
   */
  public UserRegistration(String username, String email, byte[] credentials)
      throws ValidationException
  {
    super(username, email);

    super.jsonTransformer = new RegistrationTransformer(credentials);

    this.credentials = credentials;

    validate();
  }


  protected UserRegistration(UserRegistration copy)
  {
    this(
        copy,
        (copy == null) ? new byte[] {} : Arrays.copyOf(copy.credentials, copy.credentials.length)
    );
  }

  private UserRegistration(User user, byte[] credentials)
  {
    super(user);

    super.jsonTransformer = new RegistrationTransformer(credentials);

    this.credentials = credentials;
  }




  // Protected Instance Methods -------------------------------------------------------------------

  protected void validate() throws ValidationException
  {
    Set<ConstraintViolation<UserRegistration>> errors = validator.validate(this);

    if (!errors.isEmpty())
    {
      String messages = "";

      for (ConstraintViolation violation : errors)
      {
        messages = messages + violation.getPropertyPath() + " ";
        messages = messages + violation.getMessage();
      }

      throw new ValidationException(messages);
    }
  }


  // Nested Classes -------------------------------------------------------------------------------


  /**
   * Extends the {@link org.openremote.model.User} object's JSON transformer implementation to
   * manage the serialization of additional data fields that are only present at the registration
   * phase.
   */
  public static class RegistrationTransformer extends UserTransformer
  {

    // Instance Fields ----------------------------------------------------------------------------

    private transient byte[] credentials;


    // Constructors -------------------------------------------------------------------------------

    public RegistrationTransformer()
    {

    }

    private RegistrationTransformer(byte[] credentials)
    {
      this.credentials = credentials;
    }


    // Overrides UserTransformer ------------------------------------------------------------------

    /**
     * This is an implementation of the property field extension mechanism provided by the
     * super class in {@link org.openremote.model.data.json.UserTransformer}. It adds a
     * 'credentials' field to the serialized JSON document sent to a service end-point. <p>
     *
     * For the purpose of the serialization, credentials bytes are converted to an
     * {@link UserRegistration#UTF8} string.
     *
     * @param user
     */
    @Override protected void writeExtendedProperties(User user)
    {
      writeProperty("credentials", new String(credentials, UTF8));
    }


    /**
     * Deserializes this instance as an extension of {@link org.openremote.model.User} object.
     *
     * @param schemaVersion
     * @param className
     * @param jsonProperties
     *
     * @return
     *
     * @throws DeserializationException
     */
    @Override protected UserRegistration deserialize(Version schemaVersion,
                                                     String className,
                                                     Map<String, String> jsonProperties)
        throws DeserializationException
    {
      // Let superclass deserialize itself first...

      User user = super.deserialize(schemaVersion, User.class.getName(), jsonProperties);

      // Create an instance of this with additional properties...

      return new UserRegistration(
          user, scrypt(user, jsonProperties.get("credentials").getBytes(UTF8))
      );
    }

    /**
     * Overridden to return instances of this class, as returned by the
     * {@link #deserialize(org.openremote.base.Version, String, java.util.Map)} implementation.
     *
     * @param reader
     *          input stream to deserialize this instance form
     *
     * @return  a new instance of this class
     *
     * @throws  DeserializationException
     *            if read deserialization fails
     */
    @Override public UserRegistration read(Reader reader) throws DeserializationException
    {
      // We know it is an instance of this class since we implement it as such in the
      // deserialize method above. Just need to remember to make changes on both methods
      // if the type ever changes...

      try
      {
        return (UserRegistration) super.read(reader);
      }

      catch (ClassCastException e)
      {
        throw new IncorrectImplementationException(
            "Deserialization does not return expected ''UserRegistration'' type."
        );
      }
    }


    // Private Instance Methods -------------------------------------------------------------------

    // http://www.tarsnap.com/scrypt/scrypt.pdf
    private byte[] scrypt(User user, byte[] passphrase)
    {
      byte[] salt = (user.getName() + SALT_PADDING).getBytes(UTF8);

      int iterationCountN = 16384;    // 2^14 (< 100 ms), general work factor (2^20 for ~5s)
      int hashBlockSize = 8;          // relative memory cost
      int parallelization = 1;        // CPU cost
      int dkLen = 127;                // length of the result key

      return SCrypt.generate(
          passphrase, salt, iterationCountN, hashBlockSize, parallelization, dkLen);
    }
  }


  // Enums ----------------------------------------------------------------------------------------

  public enum CredentialsEncoding
  {
    SCRYPT
  }
}

