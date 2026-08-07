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
package org.apache.jackrabbit.osgi;

import static org.ops4j.pax.exam.CoreOptions.bundle;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.options.DefaultCompositeOption;

/**
 * Provisions the JARs assembled into {@code target/test-bundles} for the OSGi tests.
 * <p>
 * Anything that is not already a bundle is repacked as one. HttpComponents publishes no
 * OSGi bundles for the 5.x line: {@code httpclient5-osgi} and {@code httpcore5-osgi} stop
 * at 5.0-beta, and the plain 5.x JARs carry no {@code Bundle-SymbolicName}, so without
 * this the bundles depending on them cannot resolve. This is what the {@code wrap:} URL
 * handler does; provisioning {@code wrap:} URLs was tried first and left the Pax Exam
 * native container unable to start, so the repack is done here instead.
 */
public final class TestBundles {

    private TestBundles() {
    }

    public static Option jarBundles() throws IOException {
        DefaultCompositeOption composite = new DefaultCompositeOption();
        for (File jar : new File("target", "test-bundles").listFiles()) {
            if (jar.getName().endsWith(".jar") && jar.isFile()) {
                File installable = isBundle(jar) ? jar : asBundle(jar);
                composite.add(bundle(installable.toURI().toURL().toString()));
            }
        }
        return composite;
    }

    private static boolean isBundle(File jar) throws IOException {
        try (JarFile jarFile = new JarFile(jar)) {
            Manifest manifest = jarFile.getManifest();
            return manifest != null
                    && manifest.getMainAttributes().getValue("Bundle-SymbolicName") != null;
        }
    }

    /**
     * Repacks a plain JAR as an OSGi bundle exporting every package it contains and
     * resolving its own dependencies dynamically.
     */
    private static File asBundle(File jar) throws IOException {
        File dir = new File("target", "wrapped-bundles");
        dir.mkdirs();
        File wrapped = new File(dir, jar.getName());

        try (JarFile in = new JarFile(jar)) {
            Set<String> packages = new TreeSet<String>();
            for (Enumeration<JarEntry> e = in.entries(); e.hasMoreElements();) {
                String name = e.nextElement().getName();
                int slash = name.lastIndexOf('/');
                if (name.endsWith(".class") && slash > 0) {
                    packages.add(name.substring(0, slash).replace('/', '.'));
                }
            }

            String symbolicName = jar.getName().substring(0, jar.getName().length() - ".jar".length());
            Manifest manifest = new Manifest();
            Attributes main = manifest.getMainAttributes();
            main.put(Attributes.Name.MANIFEST_VERSION, "1.0");
            main.putValue("Bundle-ManifestVersion", "2");
            main.putValue("Bundle-SymbolicName", symbolicName);
            main.putValue("Bundle-Version", bundleVersion(in));
            main.putValue("Export-Package", String.join(",", packages));
            main.putValue("DynamicImport-Package", "*");

            byte[] buffer = new byte[8192];
            try (JarOutputStream out = new JarOutputStream(new FileOutputStream(wrapped), manifest)) {
                for (Enumeration<JarEntry> e = in.entries(); e.hasMoreElements();) {
                    JarEntry entry = e.nextElement();
                    if (entry.isDirectory() || entry.getName().equals(JarFile.MANIFEST_NAME)) {
                        continue;
                    }
                    out.putNextEntry(new JarEntry(entry.getName()));
                    try (InputStream content = in.getInputStream(entry)) {
                        copy(content, out, buffer);
                    }
                    out.closeEntry();
                }
            }
        }
        return wrapped;
    }

    /**
     * Derives an OSGi-legal Bundle-Version from the JAR's Implementation-Version. The
     * assembly renames JARs to {@code <artifactId>.jar}, so the file name carries no
     * version.
     */
    private static String bundleVersion(JarFile jar) throws IOException {
        Manifest manifest = jar.getManifest();
        String version = manifest == null
                ? null : manifest.getMainAttributes().getValue("Implementation-Version");
        if (version == null) {
            return "0.0.0";
        }
        // OSGi wants major.minor.micro; anything after the third segment is a qualifier
        String[] parts = version.split("[.-]");
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < 3; i++) {
            result.append(i < parts.length && parts[i].matches("\\d+") ? parts[i] : "0");
            if (i < 2) {
                result.append('.');
            }
        }
        return result.toString();
    }

    private static void copy(InputStream in, OutputStream out, byte[] buffer) throws IOException {
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }
}
