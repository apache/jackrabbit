/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.tck;

import junit.framework.TestSuite;
import junit.framework.Test;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.File;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;

/**
 * The <code>TestFinder</code> class is responsible to find all <code>TestCase</code>s which are
 * in a jar. The information which jar has to be searched is passed in the constructor.
 */
public class TestFinder {
    /** Jar file to search the test classes */
    private JarFile jarFile;

    /** all tests */
    private Map allTests;

    /** all test suites */
    private Map suites;

    /**
     * The path where the jar containing the test classes and its sources is residing is passed here.
     */
    public TestFinder() {
        allTests = new HashMap();
        suites = new HashMap();
    }

    /**
     * This method searches all tests.
     *
     * @param exclude file(class) name (e.g. allTests) of tests which should be excluded.
     * @param in <code>InputStream</code> of jar file containing the test class sources
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public void find(InputStream in, String exclude) throws IOException, ClassNotFoundException {
        File tmpFile = null;
        try {
            // save to a temp file
            tmpFile = File.createTempFile("tck-tests", "jar");
            OutputStream out = null;
            try {
                out = new FileOutputStream(tmpFile);
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            } finally {
                in.close();
                out.close();
            }

            jarFile = new JarFile(tmpFile);

            // go through all jar file entries and take the one we are interessted in.
            Enumeration en = jarFile.entries();

            while (en.hasMoreElements()) {
                JarEntry entry = (JarEntry) en.nextElement();
                String name = entry.getName();

                if (!entry.isDirectory() && name.endsWith(".java")) {
                    // only source files are from interesst
                    String classname = name.replace('/','.').substring(0, name.lastIndexOf(".java"));

                    Class testclass = Class.forName(classname);

                    // check if class is really a testsuite
                    if (!isTestSuite(testclass)) {
                        continue;
                    }

                    // check for files to be excluded
                    if (name.endsWith(exclude)) {
                        continue;
                    }

                    // retrieve keyword from source file
                    String keyword = getKeyword(entry);

                    // classify testsuite (level1, level2,...)
                    if (suites.containsKey(keyword)) {
                        TestSuite suite = (TestSuite) suites.get(keyword);
                        suite.addTestSuite(testclass);
                    } else {
                        TestSuite suite = new TestSuite(keyword);
                        suite.addTestSuite(testclass);
                        suites.put(keyword, suite);
                    }

                    // memorize tests
                    allTests.put(classname, keyword);
                }
            }
        } finally {
            if (tmpFile != null) {
                tmpFile.delete();
            }
        }
    }

    /**
     * Check if the passed test class is a "real" test suite
     *
     * @param testclass class to check
     * @return true if a test suite
     */
    private boolean isTestSuite(Class testclass) {
        TestSuite ts = new TestSuite(testclass);
        if (ts.countTestCases() > 0) {
            Test t = (Test) ts.tests().nextElement();
            if (t.toString().startsWith("warning")) {
                return false;
            }
        } else {
            return false;
        }
        return true;
    }

    /**
     * Reads the keyword from the java source.
     *
     * @param entry <code>JarEntry</code> to parse
     * @return returns the existing keyword or "unspecified"
     * @throws IOException
     */
    private String getKeyword(JarEntry entry) throws IOException {
        InputStream input = jarFile.getInputStream(entry);
        InputStreamReader isr = new InputStreamReader(input);
        BufferedReader reader = new BufferedReader(isr);
        String line;
        while ((line = reader.readLine()) != null) {
            String keyword;
            if ((keyword = parseLine(line)) != null) {
                return keyword;
            }
        }
        reader.close();
        return "unspecified";
    }

    /**
     * Parses a line and checks for the "keywords" keyword
     *
     * @param line line to parse
     * @return the kewyword string or null
     */
    private String parseLine(String line) {
        int pos = line.indexOf("keywords");
        int len = "keywords".length();
        String word = "";

        if ( pos >= 0) {
            char l[] = line.toCharArray();
            pos += len;

            while (pos < l.length) {
                char c = l[pos];

                switch (c) {
                    case 9: // '\t'
                    case 10: // '\n'
                    case 12: // '\f'
                    case 13: // '\r'
                    case 32: // ' '
                        if (!"".equals(word)) {
                            return word;
                        }
                        break;
                    default:
                        word += c;
                }
                pos++;
            }
        }
        return ("".equals(word)) ? null : word;
    }

    /**
     * Returns all tests categorized by it's keyword
     * @return all tests
     */
    public Map getTests() {
        return allTests;
    }

    /**
     * Returns all built test suites. the suites are categorized:
     * <li> - level1
     * <li> - level 2
     * <li> - optional...
     *
     * @return test suites
     */
    public Map getSuites() {
        return suites;
    }
}
