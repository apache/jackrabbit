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
		 java.util.List,
		 java.util.ArrayList"
%><%@page session="false" %><%

    String parent = request.getRequestURI();
    if (parent.length() > 1) {
        parent = parent.substring(0,parent.lastIndexOf('/'));
    }

%><html>
    <head><title>TCK for JSR170</title>
    <link rel="stylesheet" href="docroot/ui/default.css" type="text/css" title="style" />
    </head>
    <body>
        <center>
            <table cellpadding="0" cellspacing="0" border="0" id="maintable">
                <!-- banner -->
                <tr>
                    <td class="leadcell"><span class="leadcelltext">TCK for JSR 170<br>Content Repository Standard</span></td><td class="logocell"><a target="_blank" href="http://www.day.com" title="www.day.com"><img src="docroot/imgs/logo.png" width="238" height="100" border="0"></td>
                </tr>
                <tr>
                <td colspan="2">
                    <iframe name="graph" src="graph.jsp" height="500" width="960" frameborder="0"></iframe>
                </td>
                </tr>
                <tr>
                <td colspan="2">
                    <iframe name="config" style="margin-top: 20px;border-top: 1px solid #000000;" src="config.jsp" height="200" width="960" frameborder="0"></iframe>
                </td>
                </tr>
                <tr>
                <td colspan="2">
                    <iframe name="statuswin" style="margin-top: 20px;border-top: 1px solid #000000;" src="status.jsp" height="100" width="960" frameborder="0"></iframe>
                </td>
                </tr>
            </table>
        </center>
    </body>
</html>