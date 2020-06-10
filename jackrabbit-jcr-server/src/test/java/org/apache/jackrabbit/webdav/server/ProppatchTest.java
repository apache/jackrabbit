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
package org.apache.jackrabbit.webdav.server;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.MultiStatusResponse;
import org.apache.jackrabbit.webdav.client.methods.HttpPropfind;
import org.apache.jackrabbit.webdav.client.methods.HttpProppatch;
import org.apache.jackrabbit.webdav.property.DavProperty;
import org.apache.jackrabbit.webdav.property.DavPropertyName;
import org.apache.jackrabbit.webdav.property.DavPropertyNameSet;
import org.apache.jackrabbit.webdav.property.DavPropertySet;
import org.apache.jackrabbit.webdav.property.DefaultDavProperty;
import org.apache.jackrabbit.webdav.xml.Namespace;

public class ProppatchTest extends WebDAVTestBase {

    public void testPropPatchSurrogate() throws IOException, DavException {

        String testuri = this.root + "ppsurrogate";

        try {
            int status;

            HttpPut put = new HttpPut(testuri);
            put.setEntity(new StringEntity("1"));
            status = this.client.execute(put, this.context).getStatusLine().getStatusCode();
            assertEquals("status: " + status, 201, status);

            DavPropertyName name = DavPropertyName.create("foobar", Namespace.EMPTY_NAMESPACE);
            DavPropertySet props = new DavPropertySet();
            DavProperty<String> foobar = new DefaultDavProperty<>(name, "\uD83D\uDCA9");
            props.add(foobar);
            HttpProppatch proppatch = new HttpProppatch(testuri, props, new DavPropertyNameSet());
            HttpResponse resp = this.client.execute(proppatch, this.context);
            status = resp.getStatusLine().getStatusCode();
            assertEquals(207, status);
            EntityUtils.consume(resp.getEntity());

            DavPropertyNameSet names = new DavPropertyNameSet();
            names.add(name);
            HttpPropfind propfind = new HttpPropfind(testuri, names, 0);
            resp = this.client.execute(propfind, this.context);
            status = resp.getStatusLine().getStatusCode();
            assertEquals(207, status);
            MultiStatusResponse[] responses = propfind.getResponseBodyAsMultiStatus(resp).getResponses();
            assertEquals(1, responses.length);

            MultiStatusResponse response = responses[0];
            DavPropertySet found = response.getProperties(200);

            DavProperty<?> f = found.get(name);
            assertEquals("\uD83D\uDCA9", f.getValue());
        } finally {
            delete(testuri);
        }
    }

}
