/*
The MIT License (MIT)
Copyright (c) 2020 Sentaroh

Permission is hereby granted, free of charge, to any person obtaining a copy of
this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights to use,
copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
and to permit persons to whom the Software is furnished to do so, subject to
the following conditions:

The above copyright notice and this permission notice shall be included in all copies or
substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
OTHER DEALINGS IN THE SOFTWARE.

*/
package com.ildar_aim.smbsync4;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.security.KeyStore;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.security.auth.x500.X500Principal;

class KeyStoreUtils {
    private static Logger log= LoggerFactory.getLogger(KeyStoreUtils.class);

    public static final String KEY_STORE_ALIAS = "SMBSync4";

    static final private String PROVIDER = "AndroidKeyStore";

    public static boolean isStoredKeyExists(Context c, String alias) {
        boolean result=false;
        try {
            KeyStore keyStore = KeyStore.getInstance(PROVIDER);
            keyStore.load(null);
            result=keyStore.containsAlias(alias);
        } catch(Exception e) {}
        if (log.isDebugEnabled()) log.debug("isStoredKeyExists result="+result);
        return result;
    }

    public static SecretKey getStoredKey(Context c, String alias) throws Exception {
        if (log.isDebugEnabled()) log.debug("getStoredKey entered");
        KeyStore keyStore = KeyStore.getInstance(PROVIDER);
        keyStore.load(null);
        SecretKey privateKey =null;
        // B1 GUARD: distinguish "alias genuinely absent" (true first run -> generate) from a
        // TRANSIENT KeyStore error. The previous code asked isStoredKeyExists(), which swallows
        // every exception and returns false, so a transient failure (keystore2 migration after an
        // OS/OEM update, keystore daemon not ready at early boot, StrongBox hiccup) looked
        // IDENTICAL to "absent" and REGENERATED the key here -- permanently orphaning every value
        // that was encrypted with the old key (all SMB/ZIP credentials). Now we query the
        // already-loaded store directly so a KeyStore error PROPAGATES (the caller treats it as a
        // load/save failure and keeps the on-disk data) instead of silently replacing the key.
        boolean alias_exists = keyStore.containsAlias(alias);
        if (!alias_exists) {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVIDER);
            keyGenerator.init(new KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT| KeyProperties.PURPOSE_DECRYPT)
                    .setCertificateSubject(new X500Principal("CN="+alias))
                    .setCertificateSerialNumber(BigInteger.ONE)
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                    .setRandomizedEncryptionRequired(false)
                    .build());
            privateKey =keyGenerator.generateKey();
        } else {
            privateKey = (SecretKey) keyStore.getKey(alias, null);
            if (privateKey == null) {
                // Alias is present but the key could not be retrieved. Do NOT fall through to
                // regeneration -- that would orphan the data this key protects. Fail loudly so
                // the caller keeps the existing on-disk config and retries later.
                throw new IllegalStateException("KeyStore alias '"+alias+"' exists but its key could not be retrieved; refusing to regenerate to avoid orphaning encrypted data");
            }
        }
        if (log.isDebugEnabled()) log.debug("getStoredKey exit");

        return privateKey;
    }

}
