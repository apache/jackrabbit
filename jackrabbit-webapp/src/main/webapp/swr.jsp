<%--
  Licensed to the Apache Software Foundation (ASF) under one or more
  contributor license agreements.  See the NOTICE file distributed with
  this work for additional information regarding copyright ownership.
  The ASF licenses this file to You under the Apache License, Version 2.0
  (the "License"); you may not use this file except in compliance with
  the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
--%><%@ page import="javax.jcr.Repository,
                 javax.jcr.Session,
                 org.apache.jackrabbit.j2ee.RepositoryAccessServlet,
                 javax.jcr.SimpleCredentials,
                   java.util.Calendar,
                   java.text.NumberFormat"
%><%@ page contentType="text/html;charset=UTF-8" %><%
    Repository rep;
    Session jcrSession;
    try {
        rep = RepositoryAccessServlet.getRepository(pageContext.getServletContext());
        jcrSession = rep.login(new SimpleCredentials("anonymous", "".toCharArray()));
    } catch (Throwable e) {
        %>Error while accessing the repository: <font color="red"><%= e.getMessage() %></font><br><%
        %>Check the configuration or use the <a href="admin/">easy setup</a> wizard.<%
        return;
    }
    try {
        String q = request.getParameter("q");
        String swrnum = request.getParameter("swrnum");
        String numResults = null;
        try {
            numResults = NumberFormat.getNumberInstance().format(Long.parseLong(swrnum));
        } catch (NumberFormatException e) {
            // ignore
        }
        if (q == null || numResults == null) {
            return;
        }

        request.setAttribute("title", "Search within results");
        %><jsp:include page="header.jsp"/>
    <form name=f action="search.jsp">
      <table border=0 cellpadding=0 cellspacing=0 width=100%>
        <tr><table border=0 width=100%><tr><td><br>There were about <b><%= numResults %></b> results for <b><%= q %></b>.<br>
        Use the search box below to search within these results.<br><br></td></tr></table>
        </td></tr>
        <tr><td valign=middle>
        <table border=0 width=100%><tr><td>
        <INPUT type=hidden name=q value="<%= q %>">
        <INPUT type=text name=as_q size=31 maxlength=256 value="">
        <INPUT type=submit VALUE="Search&nbsp;within&nbsp;results">
        </td></tr></table>
        </td></tr>
      </table>
    </form>
<jsp:include page="footer.jsp"/>
<%
    } finally {
        if (jcrSession != null) {
            jcrSession.logout();
        }
    }
%>