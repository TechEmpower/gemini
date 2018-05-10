/*******************************************************************************
 * Copyright (c) 2018, TechEmpower, Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name TechEmpower, Inc. nor the names of its
 *       contributors may be used to endorse or promote products derived from
 *       this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL TECHEMPOWER, INC. BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY
 * OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *******************************************************************************/
package com.techempower.gemini.pyxis.crypto;

import java.security.*;
import java.security.spec.*;
import java.util.*;

import javax.crypto.*;
import javax.crypto.spec.*;

import com.techempower.gemini.*;
import com.techempower.gemini.configuration.*;
import com.techempower.helper.*;
import com.techempower.log.*;
import com.techempower.util.*;
import com.techempower.util.EnhancedProperties.*;

/**
 * AesGcmNoPaddingCryptograph provides a Cryptograph implementation using the
 * AES cipher suite; specifically, the AES/GCM/NoPadding cipher. What is 
 * provided is the ability to encrypt and decrypt via AES, and message
 * authentication provided by GCM. 
 *   <p>
 * AesGcmNoPaddingCryptograph provides the following configurable variables:
 * <ul>
 * <li>Security.Cryptograph.AesGcmNoPadding.Enabled</li>
 * <li>Security.Cryptograph.AesGcmNoPadding.Base64Key</li>
 * <li>Security.Cryptograph.AesGcmNoPadding.KeyBits</li>
 * <li>Security.Cryptograph.AesGcmNoPadding.IvBits</li>
 * <li>Security.Cryptograph.AesGcmNoPadding.TagBits</li>
 * </ul>
 *   <p>
 * <strong>Important</strong>: AesGcmNoPaddingCryptograph requires that the 
 * <a href="http://www.oracle.com/technetwork/java/javase/downloads/jce8-download-2133166.html"
 * >JCE Unlimited Strength Jurisdiction Policy Files</a> are installed.
 *   <p>
 * The spec for GCM can be found 
 * <a href="http://csrc.nist.gov/groups/ST/toolkit/BCM/documents/proposedmodes/gcm/gcm-spec.pdf"
 * >here</a>.
 */
public final class AesGcmNoPaddingCryptograph
  implements Cryptograph
{
  private static final IntRange      IV_BITS_RANGE =
      new IntRange(1, Integer.MAX_VALUE);
  
  // According to the GCM spec, any value between 1 and 2^64 is valid, but for
  // simplicity, this is limited to an int value. Generally, 96 bits is 
  // appropriate, and TLS is commonly implemented 96 bits.
  private static final int           DEFAULT_IV_BIT_SIZS = 96;

  // The key bit size specifies how large the secret key is. Broadly speaking,
  // larger keys yield stronger encryption. The default is 256 bits.
  private static final List<Integer> KEY_BIT_SIZES = 
      new ArrayList<>(Arrays.asList(256, 196, 128));

  // The authentication tag bit size specifies how large (strong) the tag is.
  // Valid tag bit sizes are 128, 120, 112, 104, and 96. The default is
  // 128 bits.
  private static final List<Integer> TAG_BIT_SIZES =
      new ArrayList<>(Arrays.asList(128, 120, 112, 104, 96));
  
  private static final String PROPS_PREFIX      = "AesGcmNoPadding.";
  private static final String COMPONENT_CODE    = "gcme";
  private static final String CIPHER_SUITE_NAME = "AES";
  private static final String CIPHER_NAME       = "AES/GCM/NoPadding";
  
  private final SecureRandom      random = new SecureRandom();
  private final ComponentLog      log;

  private int     keyBitSize  = KEY_BIT_SIZES.get(0);
  private int     ivBitSize   = DEFAULT_IV_BIT_SIZS;
  private int     tagBitSize  = TAG_BIT_SIZES.get(0);
  private int     keyByteSize = keyBitSize / Byte.SIZE;
  private int     ivByteSize  = ivBitSize / Byte.SIZE;
  private int     tagByteSize = tagBitSize / Byte.SIZE;
  private boolean enabled     = false;
  
  // The key is the most sensitive attribute of the AesGcmNoPaddingCryptograph.
  private Key key;

  /**
   * Constructs an AesGcmNoPaddingCryptograph for the provided application.
   * 
   * @throws IllegalArgumentException if the Base64Key has the wrong length
   */
  public AesGcmNoPaddingCryptograph(GeminiApplication app)
  {
    log = app.getLog(COMPONENT_CODE);
    app.getConfigurator().addConfigurable(this);
  }
  
  /**
   * Protected constructor for unit testing.
   */
  protected AesGcmNoPaddingCryptograph()
  {
    this.log = null;
  }

  @Override
  public void configure(EnhancedProperties props)
  {
    final Focus focus = props
        .focus(Cryptograph.PROPS_PREFIX)
        .focus(PROPS_PREFIX);
    
    enabled = focus.getBoolean("Enabled", enabled);
    
    if (enabled)
    {
      keyBitSize = focus.getInt("KeyBits", keyBitSize);
      ivBitSize = focus.getInt("IvBits", ivBitSize);
      tagBitSize = focus.getInt("TagBits", tagBitSize);
      
      keyByteSize = keyBitSize / Byte.SIZE;
      ivByteSize = ivBitSize / Byte.SIZE;
      tagByteSize = tagBitSize / Byte.SIZE;
      
      final String base64Key = focus.get("Base64Key");
      if (base64Key != null)
      {
        final byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        setKey(new SecretKeySpec(keyBytes, CIPHER_SUITE_NAME));
      }
      else
      {
        // This will ensure that as long as the keyBitSize, ivBitSize, and 
        // tagBitSize configuration parameters are valid, a key will always be
        // on created for this Encoder. In practice, a key should be specified
        // in the configuration to avoid using an execution-time generated
        // key.
        setKey(generateKey());
      }
      
      if (log != null)
      {
        log.log("Configured " + toString());
      }
    }
  }
  
  @Override
  public String toString()
  {
    return "AesGcmNoPaddingCryptograph ["
        // Uncomment for debugging purposes; while it is generally safe to 
        // assume that the logs will be stored on the same machine as the
        // key, it cannot be guaranteed. As such, keeping this value out of
        // the logs is strongly encouraged.
        // + "key:" 
        // + new String(Base64.getEncoder().encode(key.getEncoded()))
        // + ";"
        + "enabled:" + enabled
        + ";keyBitSize:" + keyBitSize
        + ";ivBitSize:" + ivBitSize
        + ";tagBitSize:" + tagBitSize
        + "]";
  }

  /**
   * Returns a cipher text encryption of the provided plaintext. To decrypt 
   * the returned cipher text, see {@link #decrypt(byte[])}.
   *
   * <p>Note that encrypting the same plaintext separately multiple times
   * will <b>not</b> produce the same cipher text each time.
   * <p>GCM supports plaintext between 0 and roughly 2^32 bytes (just shy of
   * 68GB). 
   *
   * @param plaintext The bytes to be encrypted.
   */
  @Override
  public byte[] encrypt(byte[] plaintext)
  {
    if (!enabled)
    {
      throw new IllegalStateException(
          "AesGcmNoPaddingCryptograph is not enabled.");
    }
    
    final Cipher cipher = constructCipherInstance();
    final byte[] initializationVector = new byte[ivByteSize];
    random.nextBytes(initializationVector);
    final AlgorithmParameterSpec parameters = new GCMParameterSpec(tagBitSize,
        initializationVector);
    try
    {
      cipher.init(Cipher.ENCRYPT_MODE, key, parameters, random);
    }
    catch (InvalidKeyException | InvalidAlgorithmParameterException exc)
    {
      throw new EncryptionError(
          "Cannot encrypt; the secret key and/or GCM parameters are not valid.", 
          exc);
    }

    // Because GCM does not use padding, and because our IV and authentication
    // tag lengths are fixed, we know the size of our output in advance.
    final byte[] ciphertext = 
        new byte[ivByteSize + plaintext.length + tagByteSize];
    System.arraycopy(initializationVector, 0, ciphertext, 0, ivByteSize);
    try
    {
      cipher.doFinal(plaintext, 0, plaintext.length, ciphertext, ivByteSize);
    }
    catch (IllegalBlockSizeException
        | BadPaddingException
        | ShortBufferException exc)
    {
      // We do not expect to see these exceptions in any environment because
      // configuration parameters have already been validated.
      throw new EncryptionError(
          "Cannot encrypt; unexpected problem with cipher.", exc);
    }
    return ciphertext;
  }

  /**
   * Decrypts a cipher text that was produced by the {@link #encrypt(byte[])}
   * method. Returns {@code null} if the cipher text could not have been
   * produced by {@link #encrypt(byte[])}.
   *
   * @param ciphertext a message to be decrypted.
   */
  @Override
  public byte[] decrypt(byte[] ciphertext)
  {
    if (!enabled)
    {
      throw new IllegalStateException(
          "AesGcmNoPaddingCryptograph is not enabled");
    }
    
    final Cipher cipher = constructCipherInstance();
    final AlgorithmParameterSpec params = new GCMParameterSpec(
        tagBitSize, ciphertext, 0, ivByteSize);
    try
    {
      cipher.init(Cipher.DECRYPT_MODE, key, params, random);
    }
    catch (InvalidKeyException | InvalidAlgorithmParameterException exc)
    {
      throw new EncryptionError(
          "Cannot decrypt; the secret key and/or GCM parameters are not valid.", 
          exc);
    }
    
    final byte[] plaintext;
    try
    {
      plaintext = cipher.doFinal(
          ciphertext, ivByteSize, ciphertext.length - ivByteSize);
    }
    catch (IllegalBlockSizeException | BadPaddingException exc)
    {
      // Truncate the ciphertext to 500 characters for logging, to avoid
      // writing very large character arrays to the log.
      final int logLengthLimit = 500;
      String logCipherText = new String(Base64.getEncoder().encode(ciphertext));
      if (logCipherText.length() > logLengthLimit)
      {
        logCipherText = StringHelper.truncate(logCipherText, logLengthLimit) 
            + " (truncated to " + logLengthLimit + " characters)";
      }
      
      // This is what we generally expect to see when we're asked to decrypt
      // invalid ciphertext.
      throw new EncryptionError(
          "Cannot decrypt; probably invalid ciphertext: " + logCipherText,
          exc);
    }
    return plaintext;
  }
  
  @Override
  public SecretKeySpec generateKey()
  {
    final byte[] bytes = new byte[keyByteSize];
    random.nextBytes(bytes);
    
    return new SecretKeySpec(bytes, CIPHER_SUITE_NAME);
  }
  
  @Override
  public void setKey(Key key)
  {
    this.key = key;
    
    verifyConfiguration();
  }

  /**
   * Returns a new AES/GCM/NoPadding cipher instance.
   */
  private final Cipher constructCipherInstance()
  {
    try
    {
      return Cipher.getInstance(CIPHER_NAME);
    }
    catch (NoSuchAlgorithmException | NoSuchPaddingException exc)
    {
      throw new EncryptionError(
          "Unexpected problem instantiating cipher for " + CIPHER_NAME + ".",
          exc);
    }
  }

  /**
   * Verifies that the JCE Unlimited Strength Jurisdiction Policy Files are
   * installed and that the configuration is valid.
   *
   * @throws IllegalStateException if policy files are not installed or the
   * configuration is invalid.
   */
  private final void verifyConfiguration()
  {
    if (!KEY_BIT_SIZES.contains(keyBitSize))
    {
      throw new ConfigurationError(
          "Key bit size " + keyBitSize 
          + " is not valid; must be one of " + KEY_BIT_SIZES + ".");
    }
    if (!IV_BITS_RANGE.contains(ivBitSize))
    {
      throw new ConfigurationError(
          "Initialization vector size " + ivBitSize
          + " is not valid; must be between " + IV_BITS_RANGE.min 
          + " and " + IV_BITS_RANGE.max + ".");
    }
    if (!TAG_BIT_SIZES.contains(this.tagBitSize))
    {
      throw new ConfigurationError(
          "Tag bit size " + tagBitSize 
          + " is not valid; must be one of " + TAG_BIT_SIZES + ".");
    }

    final Cipher cipher = constructCipherInstance();
    try
    {
      // Try to instantiate and initialize a cipher using our configuration.
      // In the event of a configuration problem, this will immediately raise
      // exceptions that we should percolate up as configuration errors.
      cipher.init(Cipher.ENCRYPT_MODE, key);
    }
    catch (InvalidKeyException exc)
    {
      exc.printStackTrace();
      throw new ConfigurationError(
          "JCE Unlimited Strength Jurisdiction Policy Files are not installed."
              + " Download from"
              + " http://www.oracle.com/technetwork/java/javase/downloads/jce8-download-2133166.html"
              + " and copy the files `local_policy.jar` and `US_export_policy.jar`"
              + " into $JDK_HOME/jre/lib/security, then restart the application.",
          exc);
    }
    
    if (key.getEncoded().length != keyByteSize)
    {
      throw new ConfigurationError(
          "A " + keyBitSize + " bit (" + keyByteSize
              + " byte) key is required, but the provided argument was "
              + (Byte.SIZE * key.getEncoded().length) 
              + " bits (" + key.getEncoded().length
              + " bytes).");
    }
  }
}
