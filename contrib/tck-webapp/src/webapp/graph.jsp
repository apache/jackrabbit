<%--
Copyright 2004-2005 The Apache Software Foundation or its licensors,
                    as applicable.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
--%><%@ page import="javax.jcr.Session,
         javax.jcr.Node,
         javax.jcr.NodeIterator,
         junit.framework.Test,
         junit.framework.TestSuite,
         java.util.*,
         junit.framework.TestCase,
         javax.jcr.Property,
         java.text.SimpleDateFormat,
         org.apache.jackrabbit.tck.*,
         org.apache.jackrabbit.tck.j2ee.RepositoryServlet"
%><%@page session="false" %><%

// get the repository session for read(config and test results) and write (config and test results) access
Session repSession = RepositoryServlet.getSession(request);
if (repSession == null) {
    return;
}

// get path from jar where the test sources are stored
String testJarPath = "/WEB-INF/lib/tck-webapp-0.1.jar";

// display mode:
// - testnow : new test
// - view: view results
// - null: blank view
String mode = request.getParameter("mode");

%><html>
    <head>
        <link rel="stylesheet" href="docroot/ui/default.css" type="text/css" title="style" />
    </head>
    <body style="margin-top:0px;border-width:0px">
    <%

    if (mode != null && mode.equals("testnow")) {
        // read and save test configuration
        WebAppTestConfig.save(request, repSession);

        // prepare test
        TestFinder tf = new TestFinder();
        tf.find(getServletConfig().getServletContext().getResource(testJarPath).openStream(),
                "TestAll.java");
        Iterator  tests = tf.getSuites().keySet().iterator();
        int maxlen = 0;

        // get max number of tests per test unit
        while (tests.hasNext()) {
            String key = (String) tests.next();
            TestSuite suite = (TestSuite) tf.getSuites().get(key);
            int count = suite.countTestCases();
            maxlen = (count > maxlen) ? count : maxlen;
        }

        out.write("<table width=\"100%\">");
        tests = tf.getSuites().keySet().iterator();

        while (tests.hasNext()) {
            String key = (String) tests.next();
            TestSuite t = (TestSuite) tf.getSuites().get(key);
            Enumeration members = t.tests();
            out.write("<tr><th colspan=\"100\" class=\"content\">" + t.toString() + "</th></tr>");
            while (members.hasMoreElements()) {
                TestSuite aTest = (TestSuite) members.nextElement();

                out.write("<tr><td class=\"graph\" width=\"35%\">" + aTest.toString() + "</td>");

                Enumeration testMethods = aTest.tests();
                while (testMethods.hasMoreElements()) {
                    TestCase tc = (TestCase) testMethods.nextElement();
                    String methodname = tc.getName();

                    String id = methodname + "(" + aTest.getName() + ")";
                    out.write("<td align=\"center\" title=\"" + methodname + "\" id=\"" + id + "\"><img border=\"0\" id=\"" + id + "img" + "\" src=\"docroot/imgs/clear.png\"></td>");
                }
                out.write("</tr>");
            }
            if (tests.hasNext()) {
                out.write("<tr><th colspan=\"100\" class=\"content\">&nbsp;</th></tr>");
            }
        }

        out.write("</table>");

        // start testing
        Node rootNode = repSession.getRootNode();
        Node testResNode = (rootNode.hasNode("testing")) ? rootNode.getNode("testing") : rootNode.addNode("testing", "nt:unstructured");
        rootNode.save();

        out.write("<script>parent.statuswin.document.write(\"<html><head><title></title>\");");
        out.write("parent.statuswin.document.write('<link rel=\"stylesheet\" href=\"docroot/ui/default.css\" type=\"text/css\" title=\"style\" /></head>');");
        out.write("parent.statuswin.document.write('<body style=\"margin-top:5px;margin-left:10px;border-width:0px;font-size:11px;\">');");
        out.write("parent.statuswin.document.write(\"starting\");</script>");
        TestFinder testfinder = new TestFinder();
        testfinder.find(getServletConfig().getServletContext().getResource(testJarPath).openStream(),
                "TestAll.java");
        TckTestRunner runner = new TckTestRunner(out);
        String logStr = "<script>" +
                "parent.statuswin.document.write(\"{0} : {1}<br>\");" +
                "parent.statuswin.scrollBy(0,20);" +
                "</script>";
        runner.setLogString(logStr);
        String interAStr = "<script>" +
                "var cell=document.getElementById(\"{0}img\");" +
                "cell.src=\"docroot/imgs/{1}.png\";" +
                "window.scrollBy(0,20);" +
                "cell=document.getElementById(\"{0}\");" +
                "cell.setAttribute(\"title\",cell.getAttribute(\"title\")+\" time:{2}ms\");" +
                "</script>";
        runner.setInteractionString(interAStr);
        Tester t = new Tester(testfinder, runner);
        long startMillies = System.currentTimeMillis();
        t.run();
        Node results = testResNode.addNode(String.valueOf(startMillies));
        testResNode.save();
        t.storeResults(results);
        out.write("<script>parent.statuswin.document.write(\"...finished. Test took " + String.valueOf(System.currentTimeMillis() - startMillies) + "ms<br>\");</script>");
        out.write("<script>parent.statuswin.scrollBy(0,20);</script>");
        out.write("<script>parent.statuswin.document.write(\"</body></html>\");</script>");
    } else if (mode != null && mode.equals("view")) {
        out.write("<script>parent.statuswin.document.write(\"<html><head><title></title>\");");
        out.write("parent.statuswin.document.write('<link rel=\"stylesheet\" href=\"docroot/ui/default.css\" type=\"text/css\" title=\"style\" /></head>');");
        out.write("parent.statuswin.document.write('<body style=\"margin-top:5px;margin-left:10px;border-width:0px;0px;font-size:11px;\">');");
        out.write("parent.statuswin.document.write(\"view test result<br>\");</script>");

        TestFinder tf = new TestFinder();
        tf.find(getServletConfig().getServletContext().getResource(testJarPath).openStream(), "TestAll.java");

        // the test to be viewed is defined by the timestamp
        String testTimeInMs = request.getParameter("test");

        // load "test root"
        Node testroot = repSession.getRootNode().getNode("testing/" + testTimeInMs);

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(Long.parseLong(testTimeInMs));
        SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy hh:mm:ss");
        String formatedTestTime = formatter.format(cal.getTime());

        out.write("<table width=\"100%\">");
        out.write("<tr><th class=\"content\" width=<\"100%\">Test: " + formatedTestTime + "</th></tr>");
        out.write("</table>");

        out.write("<table width=\"100%\">");
        out.write("<tr><th colspan=\"100\" class=\"content\">&nbsp;</th></tr>");

        // display the test result categorized by the test suites:
        // - level1
        // - level2
        // - .....
        Iterator tests = tf.getSuites().keySet().iterator();

        while (tests.hasNext()) {
            String key = (String) tests.next();
            TestSuite t = (TestSuite) tf.getSuites().get(key);
            Enumeration members = t.tests();
            out.write("<tr><th class=\"content\" colspan=\"100\">" + t.toString() + "</th></tr>");
            while (members.hasMoreElements()) {
                TestSuite aTest = (TestSuite) members.nextElement();

                out.write("<tr><td class=\"graph\" width=\"35%\">" + aTest.toString() + "</td>");

                Enumeration testMethods = aTest.tests();
                while (testMethods.hasMoreElements()) {
                    TestCase tc = (TestCase) testMethods.nextElement();
                    String methodname = tc.getName();

                    // test identifier
                    String keyname = methodname + "(" + aTest.getName() + ")";

                    // load node containig the test result for one test
                    Node testResultNode = testroot.getNode(key + "/" + keyname);

                    int status = new Long(testResultNode.getProperty("status").getLong()).intValue();
                    String color = "clear";
                    switch (status) {
                        case TestResult.SUCCESS:
                            color = "pass";
                            break;
                        case TestResult.ERROR:
                            color = "error";
                            break;
                        case TestResult.FAILURE:
                            color = "failure";
                    }

                    String testTime = (testResultNode.hasProperty("testtime")) ?
                            String.valueOf(testResultNode.getProperty("testtime").getLong()) : "0";

                    String errorMsg = (testResultNode.hasProperty("errrormsg")) ? "Error: " + testResultNode.getProperty("errrormsg").getString() : "";
                    errorMsg = errorMsg.replaceAll("'"," ");
                    errorMsg = errorMsg.replaceAll("\""," ");
                    errorMsg = errorMsg.replaceAll("\n"," ");
                    errorMsg = errorMsg.replaceAll("\r"," ");

                    String testInfo = "- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -" +
                            "- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -<br>" +
                            "Test name: " + methodname + "(" + aTest.getName() + ")<br>" +
                            "Time: " + testTime + "ms<br>" + errorMsg + "<br>";

                    out.write("<td  title=\"" + methodname + " time: " +
                            testTime +"ms\" id=\"" + methodname + "(" + aTest.getName() + ")\" " +
                            "onclick=\"parent.statuswin.document.write('" + testInfo + "');parent.statuswin.scrollBy(0,70);\">" +
                            "<img src=\"docroot/imgs/" + color + ".png\" border=\"0\"></td>");
                }
                out.write("</tr>");
            }
            if (tests.hasNext()) {
                out.write("<tr><th colspan=\"100\" class=\"content\">&nbsp;</th></tr>");
            }
        }

        out.write("</table>");
        out.write("<script>parent.statuswin.scrollBy(0,20);</script>");
        out.write("<script>parent.statuswin.document.write(\"</body></html>\");</script>");
    } else {
        out.write("<table>");
        out.write("<tr>");
        out.write("<td class=\"graph\">");
        out.write("Welcome to the JSR 170 Technology Compatibility Kit");
        out.write("</td>");
        out.write("<tr>");
        out.write("<table>");
    }

    %>
    </body>
</html>