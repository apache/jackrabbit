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
package org.apache.jackrabbit.core.persistence.ext;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.derby.drda.NetworkServerControl;
import org.apache.derby.jdbc.EmbeddedDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>ExternalDerbyProcess</code> is a helper class for starting
 * an external derby server on localhost, port 1527 (standard settings).
 *
 */
public class ExternalDerbyProcess {
	
	public static Logger logger = LoggerFactory.getLogger(ExternalDerbyProcess.class);
	
	public static void setLogger(Logger aLogger) {
		logger = aLogger;
	}
	
	/**
	 * Returns the path to the JAR file that a certain class is located in. This only works
	 * if the classloader loaded this class from a JAR file.
	 */
	public static final String getJarFileForClass(Class clazz) {
		// eg. /org/apache/derby/drda/NetworkServerControl.class
		String classResource = "/" + clazz.getCanonicalName().replace(".", "/") + ".class";
		// eg. jar:file:/Users/alex/.m2/repository/org/apache/derby/derbynet/10.2.1.6/derbynet-10.2.1.6.jar!/org/apache/derby/drda/NetworkServerControl.class
		String fullResourceURL = clazz.getResource(classResource).toString();
		// eg. /Users/alex/.m2/repository/org/apache/derby/derbynet/10.2.1.6/derbynet-10.2.1.6.jar
		return fullResourceURL.replaceFirst("jar:file:([^!]+).*", "$1");
	}
	
	public static final String EXTERNAL_DERBY_JDBC_DRIVER = "org.apache.derby.jdbc.ClientDriver";
	
	public static final String EXTERNAL_DERBY_USER = "cloud";
	
	public static final String EXTERNAL_DERBY_PASSWORD = "scape";
	
	private static final String DERBY_JAVA_CMD =
		"-Dderby.drda.logConnections=true org.apache.derby.drda.NetworkServerControl start";
	
	private static final int DERBY_STARTUP_TIME = 1000; // ms

	private static List processes = new ArrayList();
	
	public static Process start() throws IOException, InterruptedException {
		// Let's create a hand-made pid
		final String prefix = "[derby server " + (processes.size() + 1) + "]: ";
		
		// derby server needs classpath with:
		// org.apache.derby:derby
		// org.apache.derby:derbynet
		String derbyJar = getJarFileForClass(EmbeddedDriver.class);
		String derbyNetJar = getJarFileForClass(NetworkServerControl.class);
		
		String classPath = System.getProperty("java.class.path");
		classPath += File.pathSeparatorChar + derbyJar;
		classPath += File.pathSeparatorChar + derbyNetJar;
		
		String cmd = "java -cp " + classPath + " " + DERBY_JAVA_CMD;
		logger.info(prefix + "Starting " + cmd);
		
		final Process p = Runtime.getRuntime().exec(cmd);
		processes.add(p);
		
		// log stdout and stderr to our console
		Thread stdoutThread = new Thread(new InputStream2ConsolePrinter(p.getInputStream(), prefix, false));
		Thread stderrThread = new Thread(new InputStream2ConsolePrinter(p.getErrorStream(), prefix, true));
		stdoutThread.start();
		stderrThread.start();

		// write the exit code on the console when the process ends
		Thread exitCodeThread = new Thread(new Runnable() {
			public void run() {
				try {
					logger.debug(prefix + "Exit code: " + p.waitFor());
				} catch (InterruptedException e) {
					logger.error("Interrupted while waiting for external derby process", e);
				}
			}
		});
		exitCodeThread.start();
		
		// wait for the server to start
		Thread.sleep(DERBY_STARTUP_TIME);
		
		return p;
	}
	
	public static void killAll() {
		for (int i=0; i < processes.size(); i++) {
			logger.debug("Killing derby server " + i);
			((Process) processes.get(i)).destroy();
		}
	}

	private static class InputStream2ConsolePrinter implements Runnable {
		
		private BufferedReader input;
		private String prefix;
		private boolean toStdErr;
		
		public InputStream2ConsolePrinter(InputStream is, String prefix, boolean toStdErr) {
			this.input = new BufferedReader(new InputStreamReader(is));
			this.prefix = prefix;
			this.toStdErr = toStdErr;
		}

		public void run() {
			String line;
			try {
				while ((line = input.readLine()) != null) {
					if (toStdErr) {
						logger.warn(prefix + line);
					} else {
						logger.debug(prefix + line);
					}
				}
				input.close();
			} catch (IOException e) {
			}
		}
		
	}
	
}
