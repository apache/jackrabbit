<%--
   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  The ASF licenses this file to You
   under the Apache License, Version 2.0 (the "License"); you may not
   use this file except in compliance with the License.
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

Session repSession = RepositoryServlet.getSession();
if (repSession == null) {
    return;
}
%>
<html>
    <head>
        <link rel="stylesheet" href="docroot/ui/default.css" type="text/css" title="style" />
        <script>
            function showConfig(id) {
                parent.config.document.location.href="config.jsp#" + id;
            }
        </script>
    </head>
    <body style="margin-top:0px;border-width:0px">
        <table width="100%">
            <tr>
                <td colspan="3">
                    <%
                    // save test configuration
                    WebAppTestConfig.save(request, repSession);

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
                    <form name="configform" action="config.jsp" method="post">
                        <table width="100%">
                            <tr><th class="content" width="40%" colspan="2">Default Configuration</td></tr>
                            <%
                            // display default config
                            for (int i = 0; i < WebAppTestConfig.propNames.length; i++) {
                                String name = WebAppTestConfig.propNames[i];
                                String value = (String) props.get(name);
                                %><tr><td class="graph"><%= name %></td><td class="graph"><input class="input" name="<%= name %>" value="<%= value %>"></td></tr><%
                            }

                            // display test suite specific configs
                            TestFinder tf = new TestFinder();
                            tf.find(getServletConfig().getServletContext().getResource(RepositoryServlet.getTckWebappJarPath()).openStream(),
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
                                        String title = (ckey.length() > 70) ? ckey.substring(0, 70) + " " + ckey.substring(71) : ckey;
                                        %><tr><td class="graph"><%= title %></td><td class="graph"><input class="input" name="<%= ckey %>" value="<%= configs.get(ckey) %>"></td><%
                                    }
                                    %><tr><td class="graph" valign="top"><input class="input" id="newid<%= key  %>"></td><td class="graph"><input class="input" id="newvalue<%= key  %>"></td></tr>
                                      <tr><td class="content" colspan="2" align="right"><input type="button" value="add" class="submit" onclick="window.location.href='config.jsp?newid='+document.getElementById('newid<%= key  %>').value+'&newvalue='+document.getElementById('newvalue<%= key  %>').value;"></td></tr><%
                                }
                            }
                            %><tr><td class="content">Set default configuration</td></td><td class="content" align="right"><input type="button" value="reset" class="submit" onclick="window.location.href='config.jsp?resetconfig=yes';"></td></tr>
                        </table>
                    </form>
                </td>
            </tr>
        </table>
    </body>
</html>