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
                 java.text.SimpleDateFormat,
                 java.util.*,
                 org.apache.jackrabbit.tck.WebAppTestConfig,
                 org.apache.jackrabbit.test.JNDIRepositoryStub,
                 org.apache.jackrabbit.tck.WebAppTestConfig,
                 org.apache.jackrabbit.tck.j2ee.RepositoryServlet,
                 org.apache.jackrabbit.tck.TestFinder,
                 junit.framework.TestSuite"
%><%@page session="false" %><%

// get path from jar where the test sources are stored
String TEST_JCR_PATH = "/WEB-INF/lib/tck-webapp-0.1.jar";

Session repSession = RepositoryServlet.getSession();
if (repSession == null) {
    return;
}

String mode = request.getParameter("mode");

%><html>
    <head>
        <link rel="stylesheet" href="docroot/ui/default.css" type="text/css" title="style" />
    </head>
    <body style="margin-top:0px;border-width:0px">
        <table width="100%">
            <tr>
                <td colspan="3" id="technavcell">
                    <div id="technav">
                    <%
                    if (mode == null || !mode.equals("view")) {
                        %><span class="technavat">New Test</span><a href="config.jsp?mode=view">View Results</a><%
                    } else {
                        %><a href="config.jsp?mode=">New Test</a><span class="technavat">View Results</span></a><%
                    }
                        %>
                    </div>
                </td>
            </tr>
            <tr>
                <td colspan="3">
                    <%
                    if (mode == null || !mode.equals("view")) {
                        // check for property additions
                        String newid = request.getParameter("newid");
                        String newvalue = request.getParameter("newvalue");
                        if (newvalue != null && !"".equals(newvalue) && newid != null && !"".equals(newid)) {
                            WebAppTestConfig.saveProperty(newid, newvalue, repSession);
                        }

                        // reset to default configuration (from properties file) if requested
                        String resetConfig = request.getParameter("resetconfig");
                        if (resetConfig != null && "yes".equals(resetConfig)) {
                            WebAppTestConfig.resetConfiguration();
                        }

                        // load current configuration
                        Map props = WebAppTestConfig.getCurrentConfig();
                        %>
                        <form name="test" action="graph.jsp" target="graph" method="post">
                            <table width="100%">
                                <tr><th class="content" width="40%" >Default Configuration</td><td width="60%" class="content" align="right"><input type="submit" value="start" class="submit"><input type="hidden" name="mode" value="testnow"></td></tr>
                                <%
                                // display default config
                                for (int i = 0; i < WebAppTestConfig.propNames.length; i++) {
                                    String name = WebAppTestConfig.propNames[i];
                                    String value = (String) props.get(name);
                                    %><tr><td class="graph"><%= name %></td><td class="graph"><input class="input" name="<%= name %>" value="<%= value %>"></td></tr><%
                                }

                                // display test suite specific configs
                                TestFinder tf = new TestFinder();
                                tf.find(getServletConfig().getServletContext().getResource(TEST_JCR_PATH).openStream(),
                                        "TestAll.java");
                                Iterator  tests = tf.getSuites().keySet().iterator();
                                tests = tf.getSuites().keySet().iterator();

                                while (tests.hasNext()) {
                                    String key = (String) tests.next();
                                    TestSuite t = (TestSuite) tf.getSuites().get(key);
                                    Map configs = WebAppTestConfig.getTestCaseSpecificConfigs(t);
                                    if (configs.size() > 0) {
                                        %><tr><th class="content" colspan="2"><a id="<%= key  %>"></a><%= key  %></td></tr><%

                                        Iterator citr = configs.keySet().iterator();
                                        while (citr.hasNext()) {
                                            String ckey = (String) citr.next();
                                            // split title if too long
                                            String title = (ckey.length() > 80) ? ckey.substring(0, 80) + " " + ckey.substring(81) : ckey;
                                            %><tr><td class="graph"><%= title %></td><td class="graph"><input class="input" name="<%= ckey %>" value="<%= configs.get(ckey) %>"></td><%
                                        }
                                        %><tr><td class="graph" valign="top"><input class="input" id="newid<%= key  %>"></td><td class="graph"><input class="input" id="newvalue<%= key  %>"></td></tr>
                                          <tr><td class="content" colspan="2" align="right"><input type="button" value="add" class="submit" onclick="window.location.href='config.jsp?newid='+document.getElementById('newid<%= key  %>').value+'&newvalue='+document.getElementById('newvalue<%= key  %>').value;"></td></tr><%
                                    }
                                }
                                %><tr><td class="content">Set default configuration</td></td><td class="content" align="right"><input type="button" value="reset" class="submit" onclick="window.location.href='config.jsp?resetconfig=yes';"></td></tr>
                            </table>
                        </form>
                    <%
                    } else {
                        %>
                        <form name="view" action="graph.jsp" target="graph">
                        <input type="hidden" name="mode" value="view">
                        <table>
                        <%
                        Node rootNode = repSession.getRootNode();

                        if (rootNode.hasNode("testing")) {
                            %>
                            <tr><td class="graph" valign="top">Select test to be viewed</td><td>
                            <select name="test" size="10" onchange="document.view.submit();">
                            <%
                            NodeIterator tests = rootNode.getNode("testing").getNodes();

                            ArrayList al = new ArrayList();
                            //hack : todo??
                            while (tests.hasNext()) {
                                al.add(tests.nextNode());
                            }

                            Collections.reverse(al);
                            Iterator itr = al.iterator();
                            // eoh

                            while (itr.hasNext()) {
                                Node n = (Node) itr.next();
                                String sdate = n.getName();
                                Calendar cal = Calendar.getInstance();
                                cal.setTimeInMillis(Long.parseLong(sdate));
                                SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy hh:mm:ss");
                                sdate = formatter.format(cal.getTime());
                                %><option value="<%= n.getName() %>"><%= sdate %><%
                            }
                            %>
                            </select>

                            </td></tr>
                        <%
                        }
                        %>
                        </table>
                        </form>
                        <%
                    }
                    %>
                </td>
            </tr>
        </table>
    </body>
</html>