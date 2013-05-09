/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.core.data;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public abstract class AbstractDataStore implements DataStore {

    private static final String ALGORITHM = "HmacSHA1";

    /**
     * Array of hexadecimal digits.
     */
    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private String secret;

    public void setSecret(String secret) {
        this.secret = secret;
    }

    /**
     * Returns the hex encoding of the given bytes.
     *
     * @param value value to be encoded
     * @return encoded value
     */
    protected static String encodeHexString(byte[] value) {
        char[] buffer = new char[value.length * 2];
        for (int i = 0; i < value.length; i++) {
            buffer[2 * i] = HEX[(value[i] >> 4) & 0x0f];
            buffer[2 * i + 1] = HEX[value[i] & 0x0f];
        }
        return new String(buffer);
    }


    @Override
    public DataIdentifier getIdentifierFromReference(String reference) {
        if (secret != null) {
            int colon = reference.indexOf(':');
            if (colon != -1) {
                String identifier = reference.substring(0, colon);
                String signature = reference.substring(colon + 1);
                if (signature.equals(sign(identifier))) {
                    return new DataIdentifier(identifier, reference);
                }
            }
        }
        return null;
    }

    //---------------------------------------------------------< protected >--

    protected DataIdentifier createIdentifier(final String identifier) {
        if (secret != null) {
            return new DataIdentifier(identifier) {
                public String getReference() {
                    return identifier + ':' + sign(identifier);
                }
            };
        } else {
            return new DataIdentifier(identifier);
        }
    }

    private String sign(String identifier) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes("UTF-8"), ALGORITHM));
            return encodeHexString(mac.doFinal(identifier.getBytes("UTF-8")));
        } catch (Exception e) {
            // TODO: log a warning about this exception
            return null;
        }
    }

}
