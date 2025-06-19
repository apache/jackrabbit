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

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.List;

import junit.framework.TestCase;

import org.apache.catalina.startup.Tomcat;
import org.apache.commons.io.FileUtils;
import org.apache.tomcat.util.scan.StandardJarScanner;
import org.slf4j.bridge.SLF4JBridgeHandler;

import org.htmlunit.WebClient;
import org.htmlunit.html.HtmlElement;
import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlInput;
import org.htmlunit.html.HtmlPage;

public class TomcatIT extends TestCase {

    static {
        SLF4JBridgeHandler.install();
    }

    private URL url;

    private Tomcat tomcat;

    private WebClient client;

    protected void setUp() throws Exception {
        File war = null;
        for (File f : new File("target").listFiles()) {
            if (f.isDirectory() && new File(f, "WEB-INF/web.xml").isFile()) {
                war = f;
                break;
            }
        }
        assertNotNull(war);
        rewriteWebXml(war);

        File bootstrap = new File("target", "bootstrap.properties");
        bootstrap.delete();

        File baseDir = new File("target", "tomcat");
        FileUtils.deleteQuietly(baseDir);

        File repoDir = new File("target", "repository");
        FileUtils.deleteQuietly(repoDir);

        url = new URL("http://localhost:12856/");

        tomcat = new Tomcat();
        tomcat.setSilent(true);
        tomcat.setBaseDir(baseDir.getPath());
        tomcat.setHostname(url.getHost());
        tomcat.setPort(url.getPort());
        tomcat.getConnector();

        StandardJarScanner jarScanner = new StandardJarScanner();
        jarScanner.setScanManifest(false);
        tomcat.addWebapp("", war.getAbsolutePath()).setJarScanner(jarScanner);

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
                        home.setValueAttribute("target/repository");
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

    private void rewriteWebXml(File war) throws IOException {
        File webXml = new File(war, new File("WEB-INF", "web.xml").getPath());
        assertTrue(webXml.exists());
        List<String> lines = Files.readAllLines(webXml.toPath());
        BufferedWriter writer = Files.newBufferedWriter(webXml.toPath());
        try {
            for (String line : lines) {
                line = line.replace("<param-value>jackrabbit/bootstrap.properties</param-value>",
                        "<param-value>target/bootstrap.properties</param-value>");
                writer.write(line);
                writer.write(System.lineSeparator());
            }
        } finally {
            writer.close();
        }
    }

    protected void tearDown() throws Exception {
        client.close();

        tomcat.stop();
    }

}
