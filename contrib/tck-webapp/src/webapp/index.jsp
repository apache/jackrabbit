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
                     java.util.*,
                     java.text.SimpleDateFormat,
                     org.apache.jackrabbit.tck.j2ee.RepositoryServlet,
                     javax.jcr.RepositoryException,
                     java.io.InputStream,
                     org.apache.jackrabbit.test.RepositoryStub,
                     java.io.IOException,
                     org.apache.jackrabbit.test.RepositoryStubException"
%><%@page session="false" %><%

Session repSession = RepositoryServlet.getSession();
if (repSession == null) {
    return;
}

// load download id from file
Properties props = new Properties();
InputStream is = getServletConfig().getServletContext().getResource("/download.id").openStream();
String did;

if (is != null) {
    try {
        props.load(is);
        did = props.getProperty("download.id", "undefined");
    } catch (IOException e) {
        did = "undefined";
    }
} else {
    did = "undefined";
}

// copy download id into repo
if (!repSession.getRootNode().hasNode("licNode")) {
    Node licNode = repSession.getRootNode().addNode("licNode", "nt:unstructured");
    licNode.setProperty("key", did);
    repSession.getRootNode().save();
} else if (!repSession.getRootNode().getNode("licNode").getProperty("key").equals(did)) {
    repSession.getRootNode().getNode("licNode").setProperty("key", did);
    repSession.getRootNode().save();
}

String parent = request.getRequestURI();
if (parent.length() > 1) {
    parent = parent.substring(0,parent.lastIndexOf('/'));
}
String mode = request.getParameter("mode");
mode = (mode == null || mode.equals("")) ? "test" : mode;

%><html>
    <head><title>TCK for JSR170</title>
    <link rel="stylesheet" href="docroot/ui/default.css" type="text/css" title="style" />
    <script type="text/javascript" src="docroot/js/server_call.js"></script>
    <script>
        function setImage(id, url) {
            var logoImg = new Image();
            logoImg.src = url;
            var img = document.getElementById(id);

            if (logoImg.width > 0) {
                img.src= logoImg.src;
            } else {
                img.src = "docroot/imgs/banner_right.jpg";
            }
        }

    </script>
    </head>
    <body onload="setImage('logo', 'http://jsr170tools.day.com/crx/crx_main_files/banner_right.gif');">
        <center>
            <table cellpadding="0" cellspacing="0" border="0" id="maintable">
                <!-- banner -->
                <tr>
                    <td class="leadcell"><span class="leadcelltext">TCK for JSR 170<br>Content Repository for Java Technology API</span></td><td class="logocell"><a target="_blank" href="http://www.day.com" title="www.day.com"><img id="logo" border="0"></td>
                </tr>
                <tr>
                    <td colspan="2" id="technavcell">
                    <div id="technav">
                    <%
                    if (mode.equals("test")) {
                        %><span class="technavat">Test</span><a href="index.jsp?mode=view">View Results</a>
                        <a href="index.jsp?mode=config">Test Config</a><a href="index.jsp?mode=preferences">Preferences</a><%
                    } else if (mode.equals("view")){
                        %><a href="index.jsp?mode=">Test</a><span class="technavat">View Results</span></a>
                        <a href="index.jsp?mode=config">Test Config</a><a href="index.jsp?mode=preferences">Preferences</a><%
                    } else if (mode.equals("config")){
                        %><a href="index.jsp?mode=">Test</a><a href="index.jsp?mode=view">View Results</a>
                        <span class="technavat">Test Config</span><a href="index.jsp?mode=preferences">Preferences</a><%
                    } else {
                        %><a href="index.jsp?mode=">Test</a><a href="index.jsp?mode=view">View Results</a>
                        <a href="index.jsp?mode=config">Test Config</a><span class="technavat">Preferences</span><%
                    }
                    %>
                    </div>
                </td>
                </tr>
                <%
                if (mode.equals("test")) {
                    // get exclude list version currently stored in the tck repository
                    String excludeListVersion = "";
                    if (repSession.getRootNode().hasNode("excludelist")) {
                        Node excludeListNode = repSession.getRootNode().getNode("excludelist");
                        excludeListVersion = excludeListNode.getProperty("version").getString();
                    }
                    %><tr>
                        <td colspan="2">
                            <iframe name="graph" src="graph.jsp" height="600" width="960" frameborder="0"></iframe>
                        </td>
                    </tr>
                    <tr>
                        <td id="technavcell" colspan="2">
                            <table width="100%">
                                <tr>
                                    <td width="10%"><input type="button" value="Start" class="submit" onclick="startTest('<%= RepositoryServlet.getExcludeListUrl() %>','<%= excludeListVersion %>', document.getElementById('excudelist').checked, document.getElementById('autoupdate').checked)"></td>
                                    <td width="20%">Start Test</td>
                                    <td width="40%" align="center"><input type="checkbox" id="excudelist" checked>Exclude List&nbsp;<input type="checkbox" id="autoupdate" checked>Auto Update</td>
                                    <td width="20%" align="right">Submit Test Data</td>
                                    <td width="10%" align="right"><input type="button" value="Submit" class="submit" onclick="var strwin = window.open('submit_result.jsp','SubmitTestResult', 'width=500,height=400');strwin.focus()"></td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                    <tr>
                        <td colspan="2">
                            <iframe name="statuswin" style="margin-top: 0px;border-top: 1px solid #000000;" src="status.jsp" height="80" width="960" frameborder="0"></iframe>
                        </td>
                    </tr><%
                } else if (mode.equals("view")){
                    %><tr>
                        <td colspan="2">
                            <iframe name="graph" src="graph.jsp" height="590" width="960" frameborder="0"></iframe>
                        </td>
                    </tr>
                    <tr>
                        <td id="technavcell" colspan="2">
                            <form name="view" action="graph.jsp" target="graph">
                                <input type="hidden" name="mode" value="view">
                                <%
                                Node rootNode = repSession.getRootNode();

                                if (rootNode.hasNode("testing")) {
                                    %>
                                    <table width="100%">
                                        <tr>
                                            <td valign="top" width="90%">Select test to be viewed</td>
                                            <td width="10%" align="right">
                                            <select name="test" onchange="document.view.submit();">
                                                <option value="">Select date</option>
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
                                    </table>
                                <%
                                }
                                %>
                            </form>
                        </td>
                    </tr>
                    <tr>
                        <td colspan="2">
                            <iframe name="statuswin" style="margin-top: 0px;border-top: 1px solid #000000;" src="status.jsp" height="80" width="960" frameborder="0"></iframe>
                        </td>
                    </tr><%
                } else if (mode.equals("config")){
                    %><tr>
                        <td colspan="2">
                            <iframe name="config" src="config.jsp" height="680" width="960" frameborder="0"></iframe>
                        </td>
                    </tr>
                    <tr>
                        <td id="technavcell" colspan="2">
                            <table width="100%">
                                <tr>
                                    <td width="40%" align="left">Save Configuration</td>
                                    <td width="10%" align="right"><input type="button" value="Save" class="submit" onclick="window.config.document.configform.submit();"></td>
                                </tr>
                            </table>
                        </td>
                    </tr><%
                } else if (mode.equals("preferences")){
                %><tr>
                    <td colspan="2">
                        <iframe name="preferences" src="preferences.jsp" height="680" width="960" frameborder="0"></iframe>
                    </td>
                </tr><%
                }
                %>
            </table>
        </center>
    </body>
</html>
