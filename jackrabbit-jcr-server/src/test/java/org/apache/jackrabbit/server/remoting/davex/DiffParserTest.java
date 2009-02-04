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
package org.apache.jackrabbit.server.remoting.davex;

import junit.framework.TestCase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** <code>DiffParserTest</code>... */
public class DiffParserTest extends TestCase {

    public void testSetProperty() throws IOException, DiffException {
        ArrayList l = new ArrayList();
        l.add("\"simple string\"");
        l.add("2345");
        l.add("true");
        l.add("false");
        l.add("234.3455");
        l.add("null");

        for (int i = 0; i < l.size(); i++) {
            final String value = l.get(i).toString();
            String diff = "^/a/prop : " + value;

            DummyDiffHandler handler = new DummyDiffHandler() {
                public void setProperty(String targetPath, String diffValue) {
                    assertEquals(targetPath, "/a/prop");
                    assertEquals(value, diffValue);
                }
            };
            DiffParser parser = new DiffParser(handler);
            parser.parse(diff);
        }
    }

    public void testSetPropertyMissing() throws IOException,
            DiffException {
        ArrayList l = new ArrayList();
        l.add("");
        l.add(null);

        for (int i = 0; i < l.size(); i++) {
            Object obj = l.get(i);
            String value = (obj == null) ? "" : obj.toString();
            String diff = "^/a/prop : " + value;

            DummyDiffHandler handler = new DummyDiffHandler() {
                public void setProperty(String targetPath, String diffValue) {
                    assertEquals(targetPath, "/a/prop");
                    assertTrue(diffValue == null || "".equals(diffValue));
                }
            };
            DiffParser parser = new DiffParser(handler);
            parser.parse(diff);
        }
    }

    public void testSetPropertyWithUnicodeChars() throws IOException,
            DiffException {
        final String value = "\"String value containing \u2355\u8723 unicode chars.\"";
        String diff = "^/a/prop : " + value;

        DiffHandler handler = new DummyDiffHandler() {
            public void setProperty(String targetPath, String diffValue) {
                assertEquals(targetPath, "/a/prop");
                assertEquals(value, diffValue);
            }
        };

        DiffParser parser = new DiffParser(handler);
        parser.parse(diff);
    }

    public void testSetPropertyWithTrailingLineSep() throws IOException,
            DiffException {
        final String value = "\"String value ending with \r\r\n\n\r\n.\"";
        String diff = "^/a/prop : " + value;

        DiffHandler handler = new DummyDiffHandler() {
            public void setProperty(String targetPath, String diffValue) {
                assertEquals(targetPath, "/a/prop");
                assertEquals(value, diffValue);
            }
        };

        DiffParser parser = new DiffParser(handler);
        parser.parse(diff);
    }

    public void testSetPropertyWithSpecialChar() throws IOException, DiffException {
        final String value = "+abc \\r+ \\n-ab >c \r\\r\\n+";
        String diff = "^/a/prop : " + value;

        DiffHandler handler = new DummyDiffHandler() {
            public void setProperty(String targetPath, String diffValue) {
                assertEquals(targetPath, "/a/prop");
                assertEquals(value, diffValue);
            }
        };

        DiffParser parser = new DiffParser(handler);
        parser.parse(diff);
    }

    public void testSetPropertyUnterminatedString() throws IOException,
            DiffException {
        final String value = "\"String value ending with \r\r\n\n\r\n.";
        String diff = "^/a/prop : " + value;

        DiffHandler handler = new DummyDiffHandler() {
            public void setProperty(String targetPath, String diffValue) {
                assertEquals(targetPath, "/a/prop");
                assertEquals(value, diffValue);
            }
        };
        DiffParser parser = new DiffParser(handler);
        parser.parse(diff);
    }

    public void testSetPropertyWithUnescapedAction() throws IOException,
            DiffException {
        String diff = "^abc : \r+def : \n-ghi : \r\n^jkl : \n\r>mno : \n";

        DiffHandler handler = new DummyDiffHandler() {
            public void addNode(String targetPath, String diffValue)
                    throws DiffException {
                assertEquals("def", targetPath);
                assertEquals("", diffValue);
            }
            public void setProperty(String targetPath, String diffValue) {
                assertTrue("abc".equals(targetPath) || "jkl".equals(targetPath));
                assertEquals("", diffValue);
            }
            public void remove(String targetPath, String diffValue)
                    throws DiffException {
                assertEquals("ghi", targetPath);
                assertEquals("", diffValue);
            }

            public void move(String targetPath, String diffValue) throws DiffException {
                assertEquals("mno", targetPath);
                assertEquals("\n", diffValue);
            }
        };

        DiffParser parser = new DiffParser(handler);
        parser.parse(diff);
    }

    public void testValidDiffs() throws IOException, DiffException {
        List l = new ArrayList();
        // unquoted string value
        l.add(new String[] {"+/a/b : 134", "/a/b","134"});
        l.add(new String[] {"+/a/b : 2.3", "/a/b","2.3"});
        l.add(new String[] {"+/a/b : true", "/a/b","true"});
        // quoted string value
        l.add(new String[] {"+/a/b : \"true\"", "/a/b","\"true\""});
        l.add(new String[] {"+/a/b : \"string value containing \u3456 unicode char.\"", "/a/b","\"string value containing \u3456unicode char.\""});
        // value consisting of quotes
        l.add(new String[] {"+/a/b : \"", "/a/b","\""});
        l.add(new String[] {"+/a/b : \"\"", "/a/b","\"\""});
        // value consisting of single
        l.add(new String[] {"+/a/b : '", "/a/b","'"});
        l.add(new String[] {"+/a/b : ''''", "/a/b","''''"});
        // value consisting of space(s) only
        l.add(new String[] {"+/a/b :  ", "/a/b"," "});
        l.add(new String[] {"+/a/b :     ", "/a/b","   "});
        // value consisting of line separators only
        l.add(new String[] {"+/a/b : \n", "/a/b","\n"});
        l.add(new String[] {"+/a/b : \r", "/a/b","\r"});
        l.add(new String[] {"+/a/b : \r\n", "/a/b","\r\n"});
        l.add(new String[] {"+/a/b : \r\n\n\r", "/a/b","\r\n\n\r"});
        // path containing whilespace
        l.add(new String[] {"+/a   /b : 123", "/a   /b","123"});
        l.add(new String[] {"+/a\r\t/b : 123", "/a\r\t/b","123"});
        // path having trailing whitespace
        l.add(new String[] {"+/a/b  : 123", "/a/b","123"});
        l.add(new String[] {"+/a/b\r : 123", "/a/b\r","123"});
        l.add(new String[] {"+/a/b\r\n\n\r\n: 123", "/a/b\r\n\n\r\n","123"});
        // path containing reserved characters
        l.add(new String[] {"++abc+ : val", "+abc+","val"});
        l.add(new String[] {"++++++ : val", "+++++","val"});
        // value containing reserver characters
        l.add(new String[] {"+/a/b : +", "/a/b","+"});
        l.add(new String[] {"+/a/b : +->+-", "/a/b","+->+-"});
        l.add(new String[] {"+/a/b : \"+->+-\"", "/a/b","\"+->+-\""});
        // other whitespace than ' ' used as key-value separator
        l.add(new String[] {"+/a/b :\r123", "/a/b","123"});
        l.add(new String[] {"+/a/b\r: 123", "/a/b","123"});
        l.add(new String[] {"+/a/b\r:\r123", "/a/b","123"});
        l.add(new String[] {"+/a/b\r:\n123", "/a/b","123"});
        l.add(new String[] {"+/a/b\t:\r123", "/a/b","123"});
        l.add(new String[] {"+/a/b\t:\t123", "/a/b","123"});
        // path containing colon
        l.add(new String[] {"+/a:b/c:d : 123", "/a:b/c:d","123"});
        // value starting with colon -> ok
        l.add(new String[] {"+/a/b : : val", "/a/b",": val"});
        // missing value
        l.add(new String[] {"+/a/b : ", "/a/b", ""});
        l.add(new String[] {"+/a/b :\n", "/a/b", ""});

        for (Iterator it = l.iterator(); it.hasNext();) {
            final String[] strs = (String[]) it.next();
            DiffHandler hndl = new DummyDiffHandler() {
                public void setProperty(String targetPath, String diffValue)
                        throws DiffException {
                    assertEquals(strs[1], targetPath);
                    assertEquals(strs[2], diffValue);
                }
            };
            DiffParser parser = new DiffParser(hndl);
            parser.parse(strs[0]);
        }

        l = new ArrayList();
        // multiple commands
        l.add("+abc :\n\n+def : val");
        l.add("+abc :\n\n+def : val\n");
        l.add("+abc : \r+def : val");
        l.add("+/a/b : val\r+abc : \r ");
        l.add("+/a/b : val\r+abc :\n\n ");
        // missing value in the last action.
        l.add("+/a/b : \r+abc :\n");
        l.add("+/a/b : \\r+abc : abc\r\r+abc :\r");
        l.add("+abc :\n\n+def : val\r\r>abc : ");

        for (Iterator it = l.iterator(); it.hasNext();) {
            final List li = new ArrayList();
            DiffHandler dh = new DummyDiffHandler() {
                public void addNode(String targetPath, String diffValue) throws DiffException {
                    li.add(diffValue);
                }
            };

            String diff = it.next().toString();
            DiffParser parser = new DiffParser(dh);
            parser.parse(diff);
            assertEquals(2, li.size());
        }
    }

    public void testSeparatorLines() throws IOException, DiffException {
        String diff = "+abc :\n\n+val : val";
        DiffHandler dh = new DummyDiffHandler() {
            public void addNode(String targetPath, String diffValue) throws DiffException {
                if ("abc".equals(targetPath)) {
                    assertEquals("", diffValue);
                } else {
                    assertEquals("val", diffValue);
                }
            }
        };
        new DiffParser(dh).parse(diff);

        diff = "+abc :\n+val : val";
        dh = new DummyDiffHandler() {
            public void addNode(String targetPath, String diffValue) throws DiffException {
                assertEquals("+val : val", diffValue);
            }
        };
        new DiffParser(dh).parse(diff);

        // TODO: check again: currently all line-sep. chars before an diff-char are ignored unless they are escaped in way the handler understands (e.g. JSON does: \\r for \r).
        diff = "+abc :\r\r\r+def : val";
        dh = new DummyDiffHandler() {
            public void addNode(String targetPath, String diffValue) throws DiffException {
                if ("abc".equals(targetPath)) {
                    assertEquals("", diffValue);
                } else {
                    assertEquals("val", diffValue);
                }
            }
        };
        new DiffParser(dh).parse(diff);

        diff = "+abc : val\r+def :\n\n ";
        dh = new DummyDiffHandler() {
            public void addNode(String targetPath, String diffValue) throws DiffException {
                if ("abc".equals(targetPath)) {
                    assertEquals("val", diffValue);
                } else {
                    assertEquals("\n ", diffValue);
                }
            }
        };
        new DiffParser(dh).parse(diff);
    }

    public void testUnicodeLineSep() throws IOException, DiffException {
        String diff = "+abc : val" + new String(new byte[] {Character.LINE_SEPARATOR}, "utf-8") + "+abc : val";
        DiffHandler dh = new DummyDiffHandler() {
            public void addNode(String targetPath, String diffValue) throws DiffException {
                assertEquals("abc", targetPath);
                assertEquals("val", diffValue);
            }
        };
        new DiffParser(dh).parse(diff);
    }

    public void testInvalidDiff() throws IOException, DiffException {
        List l = new ArrayList();
        l.add("");
        // path, separator and value missing
        l.add("+");
        l.add("+/a/b : val\r+");
        // path starting with whitespace, separator and value missing
        l.add("+\n");
        // separator and value missing
        l.add("+/a/b");
        l.add("+/a/b : val\r+abc\n");
        l.add("+/a/b :");
        // invalid for separator and value are missing (all : and whitespace
        // is interpreted as part of the path.
        l.add("+/a/b:");
        l.add("+/a/b:val");
        l.add("+/a/b: val");
        l.add("+/a/b:\rval");
        l.add("+/a/b :: val");
        // diff starting with whitespace
        l.add(" +/a/b: val");
        l.add("\r\r\r\r\r\r+/a/b: val");
        // key starting with whitespace
        l.add("+\r/a/b : 123");
        l.add("+ /a/b : 123");
        // key starting with colon
        l.add("+:/a/b : 123");

        for (Iterator it = l.iterator(); it.hasNext();) {
            String diff = it.next().toString();
            try {
                DiffParser parser = new DiffParser(new DummyDiffHandler());
                parser.parse(diff);
                fail(diff + " is not a valid diff string -> should throw DiffException.");
            } catch (DiffException e) {
                // ok
            }
        }
    }

    private class DummyDiffHandler implements DiffHandler {

        public void addNode(String targetPath, String diffValue)
                throws DiffException {
            // does nothing
        }

        public void setProperty(String targetPath, String diffValue)
                throws DiffException {
            // does nothing
        }

        public void remove(String targetPath, String diffValue)
                throws DiffException {
            // does nothing
        }

        public void move(String targetPath, String diffValue) throws DiffException {
            // does nothing
        }
    }
}
