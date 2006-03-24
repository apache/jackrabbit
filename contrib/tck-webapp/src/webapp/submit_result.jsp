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
                     org.apache.jackrabbit.tck.j2ee.RepositoryServlet,
                     javax.jcr.Node,
                     javax.jcr.NodeIterator,
                     java.text.SimpleDateFormat,
                     java.util.*,
                     org.apache.jackrabbit.test.RepositoryHelper,
                     org.apache.jackrabbit.tck.WebAppTestConfig,
                     java.net.URLEncoder,
                     java.net.URL,
                     java.net.URLConnection,
                     java.io.*,
                     org.apache.jackrabbit.tck.TestResultParser"
%><%@page session="false" %><%
Session repSession = RepositoryServlet.getSession();
if (repSession == null) {
    return;
}
String lookupURL = "resultlookup.jsp?sampledate=";

Node rootNode = repSession.getRootNode();
%>
<html>
    <head>
        <link rel="stylesheet" href="docroot/ui/default.css" type="text/css" title="style" />
        <script>
            function getTestResult(url, inputfield) {
                var httpcon = document.all ? new ActiveXObject("Microsoft.XMLHTTP") : new XMLHttpRequest();
                if (httpcon) {
                	httpcon.open('GET', url, false);
                	httpcon.send(null);
                	inputfield.value = httpcon.responseText;
                } else {
                	inputfield.value = "";
                    window.alert("Select a valid sample date");
                }
            }
        </script>
    </head>
    <body style="margin-top:0px;border-width:0px">
        <form name="submit" action="<%= RepositoryServlet.getSubmitUrl() %>" method="post">
        <%
        // build submit form
        if (rootNode.hasNode("testing")) {
            // create properties "file" containing system (java) and repository information
            String properties = "#system properties\n";
            String[] propertyNames = {"java.class.version", "java.vendor", "java.vendor.url",
                                      "java.version", "os.name", "os.arch", "os.version"};
            for (int i = 0; i < propertyNames.length; i++) {
                String pval = System.getProperty(propertyNames[i]);
                properties += propertyNames[i] + "=" + pval + "\n";
            }

            properties += "#repository properties\n";
            RepositoryHelper helper = new RepositoryHelper(WebAppTestConfig.getCurrentConfig());
            String dkeys[] = helper.getRepository().getDescriptorKeys();
            for (int i = 0; i < dkeys.length; i++) {
                String dval = helper.getRepository().getDescriptor(dkeys[i]);
                properties += dkeys[i] + "=" + dval + "\n";
            }
            
            // license info
            Node lk = repSession.getRootNode().getNode("licNode");
            String did = lk.getProperty("key").getString();
            String installid = lk.getUUID();
            %>
            <table width="100%">
                <tr>
                    <td colspan="2">
                         <iframe name="userinfo" style="margin-top: 0px;border-top: 1px solid #000000;" src="<%= RepositoryServlet.getSubmitUrl() %>?downloadidinfo=<%= did %>" height="200" width="450" frameborder="0"></iframe>
                    </td>
                </tr>
                <tr>
                    <td class="content" valign="top" width="40%">Test Date</td>
                    <td class="content" width="60%" align="right">
                    <select name="test" onchange="getTestResult('<%= lookupURL %>'+this.options[this.selectedIndex].value, document.submit.resultxml);">
                        <option value="">Sample date</option>
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
                            if ("props".equals(sdate)) {
                                continue;
                            }
                            Calendar cal = Calendar.getInstance();
                            cal.setTimeInMillis(Long.parseLong(sdate));
                            SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy hh:mm:ss");
                            sdate = formatter.format(cal.getTime());
                            %><option value="<%= n.getName() %>"><%= sdate %></option><%
                        }
                        %>
                        </select></td>
                </tr>
                <tr>
                    <td colspan="2" class="content">
                        &nbsp;<input type="hidden" name="publishresults" id="publishresults" value="yes"><input type="hidden" name="properties" value="<%= properties %>"><input type="hidden" name="resultxml"><input type="hidden" name="downloadid" value="<%= did %>"><input type="hidden" name="installid" value="<%= installid %>">
                    </td>
                </tr>
                <tr>
                    <td colspan="2" class="content">
                        <input type="checkbox" name="publishresultscb" checked onclick="if (this.checked) {document.getElementById('publishresults').value='yes';}else{document.getElementById('publishresults').value='';}">Publishing test results allowed
                    </td>
                </tr>
                <tr>
                    <td class="content"><input type="submit" value="Send" class="submit"></td><td align="right" class="content"><input type="button" value="Cancel" class="submit" onclick="window.close();"></td>
                </tr>
            </table>
            <%
        } else {
            %>No test(s) executed so far.<%
        }
        %>
        </form>
    </body>
</html>
