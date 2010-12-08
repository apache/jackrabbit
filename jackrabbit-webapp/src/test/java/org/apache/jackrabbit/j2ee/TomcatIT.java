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
package org.apache.jackrabbit.j2ee;

import java.io.IOException;
import java.net.URL;

import junit.framework.TestCase;

import org.apache.catalina.Engine;
import org.apache.catalina.Host;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.startup.Embedded;
import org.slf4j.bridge.SLF4JBridgeHandler;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

public class TomcatIT extends TestCase {

    private URL url;

    private Embedded tomcat;

    private WebClient client;

    protected void setUp() throws Exception {
        SLF4JBridgeHandler.install();

        url = new URL("http://localhost:12856/");

        tomcat = new Embedded();
        tomcat.setCatalinaBase("tomcat");
        tomcat.setCatalinaHome("tomcat");

        Engine engine = tomcat.createEngine();
        engine.setName("localengine");
        engine.setDefaultHost(url.getHost());

        Host host = tomcat.createHost(url.getHost(), "webapps");
        host.setAutoDeploy(false);
        engine.addChild(host);

        String webapp = System.getProperty("webapp.directory");
        StandardContext context =
            (StandardContext) tomcat.createContext("", webapp);
        context.setDefaultWebXml(System.getProperty("default.webxml"));
        host.addChild(context);

        tomcat.addEngine(engine);

        Connector connector =
            tomcat.createConnector(url.getHost(), url.getPort(), false);
        tomcat.addConnector(connector);

        tomcat.setAwait(true);
        tomcat.start();

        client = new WebClient();
    }

    public void testTomcat() throws Exception {
        HtmlPage page = client.getPage(url);
        assertEquals("Content Repository Setup", page.getTitleText());

        page = submitNewRepositoryForm(page);
        assertEquals("Content Repository Ready", page.getTitleText());

        page = page.getAnchorByText("home").click();
        assertEquals("Apache Jackrabbit JCR Server", page.getTitleText());
    }

    private HtmlPage submitNewRepositoryForm(HtmlPage page) throws IOException {
        for (HtmlForm form : page.getForms()) {
            for (HtmlInput mode : form.getInputsByName("mode")) {
                if ("new".equals(mode.getValueAttribute())) {
                    for (HtmlInput home : form.getInputsByName("repository_home")) {
                        home.setValueAttribute("repository");
                        for (HtmlElement submit : form.getElementsByAttribute("input", "type", "submit")) {
                            return submit.click();
                        }
                    }
                }
            }
        }
        fail();
        return null;
    }

    protected void tearDown() throws Exception {
        client.closeAllWindows();

        tomcat.stop();
    }

}
