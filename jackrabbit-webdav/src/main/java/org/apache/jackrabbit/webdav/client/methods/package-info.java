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

/**
 * Provides classes for use with the Apache HttpClient, supporting WebDAV
 * request methods.
 * <p>
 * As of package version 3.0.0 these classes are built on Apache HttpClient 5. This is a
 * breaking change: {@link org.apache.jackrabbit.webdav.client.methods.BaseDavRequest}
 * now extends
 * {@code org.apache.hc.client5.http.classic.methods.HttpUriRequestBase} and takes
 * the request method as its first constructor argument, and the response
 * accessors take {@code org.apache.hc.core5.http.ClassicHttpResponse} in place of
 * the HttpClient 4 {@code HttpResponse}.
 *
 * @see <a href="https://issues.apache.org/jira/browse/JCR-2406">JCR-2406</a>
 * @see <a href=
 *      "https://hc.apache.org/httpcomponents-client-5.6.x/">https://hc.apache.org/httpcomponents-client-5.6.x/</a>
 */
@org.osgi.annotation.versioning.Version("3.0.0")
package org.apache.jackrabbit.webdav.client.methods;
