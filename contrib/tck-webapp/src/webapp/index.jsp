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

// checker intervall
long CHECKER_INTERVALL = 24 * 60 * 60 * 1000;

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
// quick and dirty check
if (repSession.getRootNode().getNode("licNode").canAddMixin("mix:referenceable")) {
    repSession.getRootNode().getNode("licNode").addMixin("mix:referenceable");
    repSession.getRootNode().save();
}

// last version checker time
long lastChecked = (repSession.getRootNode().hasNode("lastChecked")) ? repSession.getRootNode().getNode("lastChecked").getProperty("time").getLong() : 0;
long currentTime = System.currentTimeMillis();
boolean checkIt = ((lastChecked + CHECKER_INTERVALL) < currentTime) || lastChecked == 0;
boolean isUpToDate = (repSession.getRootNode().hasNode("lastChecked")) ? repSession.getRootNode().getNode("lastChecked").getProperty("uptodate").getBoolean() : true;

// load version
String cVersion;
is = getServletConfig().getServletContext().getResource("/version.id").openStream();

if (is != null) {
    try {
        props.load(is);
        cVersion = props.getProperty("version.id", "undefined");
    } catch (IOException e) {
        cVersion = "undefined";
    }
} else {
    cVersion = "undefined";
}

// build check version url
StringBuffer snippet = new StringBuffer(256);
snippet.append(RepositoryServlet.getTckVersionCheckerPath());
// install id
snippet.append("?s=" + repSession.getRootNode().getNode("licNode").getUUID());
// version
snippet.append("&v=" + cVersion);
// download id
snippet.append("&d=" + repSession.getRootNode().getNode("licNode").getProperty("key").getString());
// java version
snippet.append("&j");
String vendor = System.getProperty("java.vendor");
if (vendor != null) {
    int end = vendor.indexOf(' ');
    if (end != -1) {
        vendor = vendor.substring(0, end);
    }
    snippet.append(vendor);
    snippet.append(' ');
}
snippet.append(System.getProperty("java.version"));
// os
snippet.append("&o=" + System.getProperty("os.name") +
        " " + System.getProperty("os.version"));

String checkVersionUrl = snippet.toString();

// get parent handle
String parent = request.getRequestURI();
if (parent.length() > 1) {
    parent = parent.substring(0,parent.lastIndexOf('/'));
}
String mode = request.getParameter("mode");
mode = (mode == null || mode.equals("")) ? "test" : mode;

String exludeListTestMethod = "";

if (mode.equals("test")) {
    // get exclude list version currently stored in the tck repository
    String excludeListVersion = "";
    if (repSession.getRootNode().hasNode("excludelist")) {
        Node excludeListNode = repSession.getRootNode().getNode("excludelist");
        excludeListVersion = excludeListNode.getProperty("version").getString();
    }
    exludeListTestMethod = (mode.equals("test")) ? "excludeListIsUpToDate('" + RepositoryServlet.getExcludeListCheckerPath() + "?v=" + excludeListVersion + "');" : "";
}
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

        function setGreen(doNotTell) {
            var img = document.getElementById('vcheckpic');
            img.src = "docroot/imgs/green.png";
            img.setAttribute("title", "The Tck web application is up to date.");
            // tell server that a check got performed (perform in 24h again)
            if (doNotTell != true) {
                tellChecked(<%= currentTime %>, true);
            }
        }

        function setRed(doNotTell) {
            var img = document.getElementById('vcheckpic');
            img.src = "docroot/imgs/red.png";
            img.setAttribute("title", "A new Tck version is available.");
            var link = document.getElementById('vcheckpic_href');
            link.setAttribute("href", "<%= RepositoryServlet.getTckUpdateUrl() %>");
            link.setAttribute("target", "_new");
            // tell server that a check got performed (perform in 24h again)
            if (doNotTell != true) {
                tellChecked(<%= currentTime %>, false);
            }
        }

        function tellChecked(currTime, upToDate) {
            var httpcon = document.all ? new ActiveXObject("Microsoft.XMLHTTP") : new XMLHttpRequest();
            if (httpcon) {
                var url = "set_checktime.jsp?time=" + currTime + "&upToDate=" + upToDate;
                httpcon.open('GET', url, false);
                httpcon.send(null);
            }
        }

        function checkVersion(checkerurl) {
            <%
            if (checkIt) {%>
                var tester = new Image();
                tester.onload = setRed;
                tester.onerror = setGreen;
                tester.src = checkerurl;
                <%
            } else {
                if (isUpToDate) {
                    %>setGreen(true);<%
                } else {
                    %>setRed(true);<%
                }
            }%>
        }

    </script>
    </head>
    <body onload="setImage('logo', 'http://jsr170tools.day.com/crx/crx_main_files/banner_right.gif');checkVersion('<%= checkVersionUrl %>');<%= exludeListTestMethod %>">
        <center>
            <table cellpadding="0" cellspacing="0" border="0" id="maintable">
                <!-- banner -->
                <tr>
                    <td class="leadcell"><span class="leadcelltext">TCK for JSR 170<br>Content Repository for Java Technology API</span></td><td class="logocell"><a target="_blank" href="http://www.day.com" title="www.day.com"><img id="logo" border="0"></td>
                </tr>
                <tr>
                    <td id="technavcell">
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
                    <td align="right" id="technavcell">
                        <a href="javascript:void(0);" id="vcheckpic_href"><img src="docroot/imgs/green.png" id="vcheckpic" border="0"></a>
                    </td>
                </tr>
                <%
                if (mode.equals("test")) {
                    %><tr>
                        <td colspan="2">
                            <iframe name="graph" src="graph.jsp" height="600" width="960" frameborder="0"></iframe>
                        </td>
                    </tr>
                    <tr>
                        <td id="technavcell" colspan="2">
                            <table width="100%">
                                <tr>
                                    <td width="10%"></script><input type="button" value="Start" class="submit" onclick="startTest('<%= RepositoryServlet.getExcludeListUrl() %>', document.getElementById('excudelist').checked)"></td>
                                    <td width="20%">Start Test</td>
                                    <td width="40%" align="center"><input type="checkbox" id="excudelist" checked>Exclude List&nbsp;</td>
                                    <td width="20%" align="right">Submit Test Data</td>
                                    <td width="10%" align="right"><input type="button" value="Submit" class="submit" onclick="var strwin = window.open('submit_result.jsp','SubmitTestResult', 'width=470,height=350');strwin.focus()"></td>
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
                                                while (tests.hasNext()) {
                                                    al.add(tests.nextNode());
                                                }

                                                Collections.reverse(al);
                                                Iterator itr = al.iterator();

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
