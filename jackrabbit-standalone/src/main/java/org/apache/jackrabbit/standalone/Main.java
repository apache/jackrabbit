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
package org.apache.jackrabbit.standalone;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.servlet.jackrabbit.JackrabbitRepositoryServlet;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.NCSARequestLog;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.bio.SocketConnector;
import org.mortbay.jetty.handler.RequestLogHandler;
import org.mortbay.jetty.servlet.ServletHolder;
import org.mortbay.jetty.webapp.WebAppContext;

/**
 *
 */
public class Main {

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {
        new Main(args).run();
    }

    private final Options options = new Options();

    private final CommandLine command;

    private final RequestLogHandler accessLog = new RequestLogHandler();

    private final WebAppContext webapp = new WebAppContext();

    private final Connector connector = new SocketConnector();

    private final Server server = new Server();

    private Main(String[] args) throws ParseException {
        options.addOption("?", "help", false, "print this message");
        options.addOption("n", "notice", false, "print copyright notices");
        options.addOption("l", "license", false, "print license information");

        options.addOption("q", "quiet", false, "disable console output");
        options.addOption("d", "debug", false, "enable debug logging");

        options.addOption("h", "host", true, "IP address of the HTTP server");
        options.addOption("p", "port", true, "TCP port of the HTTP server (8080)");
        options.addOption("f", "file", true, "location of this jar file");
        options.addOption("r", "repo", true, "repository directory (jackrabbit)");
        options.addOption("c", "conf", true, "repository configuration file");

        command = new GnuParser().parse(options, args);
    }

    public void run() throws Exception {
        String defaultFile = "jackrabbit-standalone.jar";
        URL location =
            Main.class.getProtectionDomain().getCodeSource().getLocation();
        if (location != null && "file".equals(location.getProtocol())) {
            File file = new File(location.getPath());
            if (file.isFile()) {
                defaultFile = location.getPath();
            }
        }
        File file = new File(command.getOptionValue("file", defaultFile));

        if (command.hasOption("help")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("java -jar " + file.getName(), options, true);
        } else if (command.hasOption("notice")) {
            copyToOutput("/META-INF/NOTICE.txt");
        } else if (command.hasOption("license")) {
            copyToOutput("/META-INF/LICENSE.txt");
        } else {
            message("Welcome to Apache Jackrabbit!");
            message("-------------------------------");

            File repository =
                new File(command.getOptionValue("repo", "jackrabbit"));
            message("Using repository directory " + repository);
            repository.mkdirs();
            File tmp = new File(repository, "tmp");
            tmp.mkdir();
            File log = new File(repository, "log");
            log.mkdir();

            message("Writing log messages to " + log);
            prepareServerLog(log);

            message("Starting the server...");
            prepareWebapp(file, repository, tmp);
            accessLog.setHandler(webapp);
            prepareAccessLog(log);
            server.setHandler(accessLog);
            prepareConnector();
            server.addConnector(connector);
            prepareShutdown();
            server.start();

            String host = connector.getHost();
            if (host == null) {
                host = "localhost";
            }
            message("Apache Jackrabbit is now running at "
                    +"http://" + host + ":" + connector.getPort() + "/");
        }
    }

    private void prepareServerLog(File log)
            throws IOException {
        Layout layout =
            new PatternLayout("%d{dd.MM.yyyy HH:mm:ss} *%-5p* %c{1}: %m%n");

        Logger jackrabbitLog = Logger.getRootLogger();
        jackrabbitLog.addAppender(new FileAppender(
                layout, new File(log, "jackrabbit.log").getPath()));

        Logger jettyLog = Logger.getLogger("org.mortbay.log");
        jettyLog.addAppender(new FileAppender(
                layout, new File(log, "jetty.log").getPath()));
        jettyLog.setAdditivity(false);

        if (command.hasOption("debug")) {
            jackrabbitLog.setLevel(Level.DEBUG);
            jettyLog.setLevel(Level.DEBUG);
        } else {
            jackrabbitLog.setLevel(Level.INFO);
            jettyLog.setLevel(Level.INFO);
        }

        System.setProperty(
                "derby.stream.error.file",
                new File(log, "derby.log").getPath());
    }

    private void prepareAccessLog(File log) {
        NCSARequestLog ncsa = new NCSARequestLog(
                new File(log, "access.log.yyyy_mm_dd").getPath());
        ncsa.setFilenameDateFormat("yyyy-MM-dd");
        accessLog.setRequestLog(ncsa);
    }

    private void prepareWebapp(File file, File repository, File tmp) {
        webapp.setContextPath("/");
        webapp.setWar(file.getPath());
        webapp.setExtractWAR(false);
        webapp.setTempDirectory(tmp);

        ServletHolder servlet =
            new ServletHolder(JackrabbitRepositoryServlet.class);
        servlet.setInitOrder(1);
        servlet.setInitParameter("repository.home", repository.getPath());
        String conf = command.getOptionValue("conf");
        if (conf != null) {
            servlet.setInitParameter("repository.config", conf);
        }
        webapp.addServlet(servlet, "/repository.properties");
    }

    private void prepareConnector() {
        String port = command.getOptionValue("port", "8080");
        connector.setPort(Integer.parseInt(port));
        String host = command.getOptionValue("host");
        if (host != null) {
            connector.setHost(host);
        }
    }

    private void prepareShutdown() {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                try {
                    message("Shutting down the server...");
                    server.stop();
                    server.join();
                    message("-------------------------------");
                    message("Goodbye from Apache Jackrabbit!");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void message(String message) {
        if (!command.hasOption("quiet")) {
            System.out.println(message);
        }
    }

    private void copyToOutput(String resource) throws IOException {
        InputStream stream = Main.class.getResourceAsStream(resource);
        try {
            IOUtils.copy(stream, System.out);
        } finally {
            stream.close();
        }
    }

}
