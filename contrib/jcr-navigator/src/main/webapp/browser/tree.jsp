<%@taglib uri="http://java.sun.com/jstl/core" prefix="c" %>
<%@taglib uri="http://jackrabbit.apache.org/jcr-taglib" prefix="jcr" %>
<% 
  pageContext.setAttribute("action",request.getParameter("action")) ;
  String data = request.getParameter("data").trim() ;
  // Prefix
  String prefix = request.getParameter("prefix");
  if (prefix==null) {
	  prefix = "";
  }
  pageContext.setAttribute("prefix", prefix) ;  
  //{"node":{"widgetId":"tree/","objectId":"","index":0}
  System.out.println(Calendar.getInstance().getTime() + ". ip = " + request.getRemoteAddr());
  System.out.println(Calendar.getInstance().getTime() + ". data = " + data);
  int start = data.indexOf("widgetId\":\"");
  String path = data.substring(start+11,data.indexOf("\",",start)) ;
  pageContext.setAttribute("path", path.substring(prefix.length())) ;
  pageContext.setAttribute("jcrsession",session.getAttribute("jcr.session"));
  int x = 0;
%>
<%@page import="java.util.Calendar"%>
<c:if test="${action eq 'getChildren'}">
(
[
<jcr:set item="${path}" var="node"/>
<c:forEach var="child" items="${node.nodes}">
	<% if (x!=0) {%>,<%}else {x++;}%>
	{
	title:"<c:out value="${child.name}"/>",
	isFolder:true,
	widgetId:"<c:out value="${prefix}"/><c:out value="${child.path}"/>"
<% 		if (prefix.length()>0) { %>,
	objectId:"<c:out value="${child.path}"/>"
<% 		} %>
	}
</c:forEach>
]
)
</c:if>
