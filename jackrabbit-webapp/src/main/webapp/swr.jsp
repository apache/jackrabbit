<%
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
%><%@ page import="javax.jcr.Repository,
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
        %>
<html>
  <head><title>Search within results</title>
    <link rel="shortcut icon" href="<%= request.getContextPath() %>/images/favicon.ico" type="image/vnd.microsoft.icon">
    <style><!--
div,td{color:#000}
.f{color:#666}
.flc,.fl:link{color:#77c}
a:link,.w,a.w:link,.w a:link,.q:visited,.q:link,.q:active,.q{color:#00c}
a:visited,.fl:visited{color:#551a8b}
a:active,.fl:active{color:red}
.t{background:#e5ecf9;color:#000}
.bb{border-bottom:1px solid #36c}
.bt{border-top:1px solid #36c}
.j{width:34em}
.h{color:#36c}
.i,.i:link{color:#a90a08}
.a,.a:link{color:green}
.z{display:none}
div.n{margin-top:1ex}
.n a{font-size:10pt;color:#000}
.n .i{font-size:10pt;font-weight:bold}
.b a{font-size:12pt;color:#00c;font-weight:bold}
#np,#nn,.nr,#logo span,.ch{cursor:pointer;cursor:hand}
.tas{padding:3px 3px 3px 5px}
.taf{padding:3px 3px 6px 5px}
.tam{padding:6px 3px 6px 5px}
.tal{padding:6px 3px 3px 5px}
.sl,.r{font-weight:normal;margin:0;display:inline}
.sl{font-size:84%}
.r{font-size:1em}
.e{margin:.75em 0}
.bl{display:none}
.fl2,.fl2:link,.fl2:visited{color:#77c}
.fl2:active{color:red}
#navbar div,#logo span{background:url(<%= request.getContextPath() %>/images/jackrabbitlogo.gif) no-repeat;overflow:hidden;height:26px}
#logo{display:block;width:150px;height:52px;position:relative;overflow:hidden;margin:15px 0 12px}
#logo span{background-position:0 -26px;position:absolute;top:0;left:0;width:100%;height:100%}
body,td,div,.p,a{font-family:arial,sans-serif}
.g{margin:1em 0}
#sd{font-size:113%;font-weight:bold}
#ap{font-size:64%}
--></style></head>
  <body bgcolor=#ffffff text=#000000 onload=document.f.as_q.focus() link=#0000cc vlink=#551A8B alink=#ff0000>
    <form name=f action="search.jsp">
      <table border=0 cellpadding=0 cellspacing=0 width=100%>
        <tr><td valign=top><a href=/><img border=0 src="<%= request.getContextPath() %>/images/jackrabbitlogo.gif" width=320 height=83 alt=Jackrabbit></a><br><center><font face=arial,sans-serif color=green><b>Search&nbsp;within&nbsp;results</b></font></center></td><td><table border=0 width=100%><tr><td><font face=arial,sans-serif><br>There were about <b><%= numResults %></b> results for <b><%= q %></b>.<br>Use the search box below to search within these results.<br><br></font></td></tr></table></td></tr><tr><td width=205 align=right>&nbsp;</td><td valign=middle><INPUT type=hidden name=q value="<%= q %>"><INPUT type=text name=as_q size=31 maxlength=256 value=""><INPUT type=submit VALUE="Search&nbsp;within&nbsp;results"></font></td></tr>
      </table>
    </form>
    <br>
    <center>
      <p><hr class=z>
      <table border=0 cellpadding=2 cellspacing=0 width=100% class="t n bt">
        <tr><td align=center><font size=-1><a href="http://jackrabbit.apache.org">Jackrabbit&nbsp;Home</a></td></tr>
      </table>
      <br><font size=-1 class=p>&copy;<%= Calendar.getInstance().get(Calendar.YEAR) %> Apache Jackrabbit</font>
    </center>

  </body>

</html><%
    } finally {
        if (jcrSession != null) {
            jcrSession.logout();
        }
    }
%>