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
package org.apache.jackrabbit.api.jsr283;

import javax.jcr.Credentials;

/**
 * <code>GuestCredentials</code> implements the <code>Credentials</code>
 * interface and is used to obtain a "guest", "public" or "anonymous" session.
 * Note that the characteristics of the session created from the
 * <code>GuestCredentials</code> remain implementation specific.
 *
 * @since JCR 2.0
 */
public final class GuestCredentials implements Credentials {

    /**
     * The constructor creates a new <code>GuestCredentials</code> object.
     */
    public GuestCredentials() {
    }
}
