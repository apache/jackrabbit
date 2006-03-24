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
                     javax.jcr.RepositoryException,
                     java.util.HashMap,
                     java.io.IOException"
%><%@page session="false" %><%
Session repSession = RepositoryServlet.getSession();
if (repSession == null) {
    return;
}

String isUpToDate = request.getParameter("upToDate");

Node lastChecked = (repSession.getRootNode().hasNode("lastChecked")) ?
        repSession.getRootNode().getNode("lastChecked") :
        repSession.getRootNode().addNode("lastChecked", "nt:unstructured");

    lastChecked.setProperty("time", System.currentTimeMillis());
    lastChecked.setProperty("uptodate", (isUpToDate != null && isUpToDate.equals("true")) ? true : false);
    repSession.save();
%>