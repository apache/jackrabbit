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
package org.apache.jackrabbit.spi2dav;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assume.assumeTrue;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.jcr.RepositoryException;
import javax.net.ssl.SSLException;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class RepositoryServiceImplIT {

    @Rule
    public TemporaryFolder tmpDirectory = new TemporaryFolder();

    static boolean canConnectTo(String urlSpec) throws MalformedURLException {
        URL url = new URL(urlSpec);
        try {
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.connect();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    @Test
    public void testGetAgainstTrustedCertServer() throws RepositoryException, ClientProtocolException, IOException {
        assumeTrue("Cannot connect to http://www.apache.org", canConnectTo("http://www.apache.org"));
        RepositoryServiceImpl repositoryServiceImpl = RepositoryServiceImplTest.getRepositoryService("https://jackrabbit.apache.org/jcr", ConnectionOptions.builder().build());
        HttpClient client = repositoryServiceImpl.getClient(null);
        HttpGet get = new HttpGet("https://jackrabbit.apache.org/jcr/index.html");
        String content = client.execute(get, new BasicResponseHandler());
        assertFalse(content.isEmpty());
    }

    @Test
    public void testGetAgainstTrustedCertServerWithSystemProperties() throws RepositoryException, ClientProtocolException, IOException {
        assumeTrue("Cannot connect to http://www.apache.org", canConnectTo("http://www.apache.org"));
        // use dedicated trust store
        Path keyStorePath = tmpDirectory.getRoot().toPath().resolve("emptyPKCS12.keystore");
        try (InputStream is = this.getClass().getResourceAsStream("emptyPKCS12.keystore")) {
            Files.copy(is, keyStorePath);
        }
        String oldTrustStore = System.setProperty("javax.net.ssl.trustStore", keyStorePath.toString());
        String oldTrustStorePassword = System.setProperty("javax.net.ssl.trustStorePassword", "storePassword");
        String oldDebug = System.setProperty("javax.net.debug", "ssl");
        try {
            ConnectionOptions connectionOptions = ConnectionOptions.builder().useSystemProperties(true).build();
            RepositoryServiceImpl repositoryServiceImpl = RepositoryServiceImplTest.getRepositoryService("https://jackrabbit.apache.org/jcr", connectionOptions);
            HttpClient client = repositoryServiceImpl.getClient(null);
            HttpGet get = new HttpGet("https://jackrabbit.apache.org/jcr/index.html");
            // connection must fail as cert is not trusted due to used trust store being empty
            assertThrows(SSLException.class, () -> client.execute(get, new BasicResponseHandler()));
        } finally {
            setOrClearSystemProperty("javax.net.ssl.trustStore", oldTrustStore);
            setOrClearSystemProperty("javax.net.ssl.trustStorePassword", oldTrustStorePassword);
            setOrClearSystemProperty("javax.net.debug", oldDebug);
        }
    }

    private static void setOrClearSystemProperty(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
    }
}
