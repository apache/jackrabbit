<%@taglib uri="http://java.sun.com/jstl/core" prefix="c" %>
<%@taglib uri="http://jackrabbit.apache.org/jcr-taglib" prefix="jcr" %>
<% 
  pageContext.setAttribute("action",request.getParameter("action")) ;
  String data = request.getParameter("data").trim() ;
  //{"node":{"widgetId":"tree/","objectId":"","index":0}
  System.out.println(Calendar.getInstance().getTime() + ". data = " + data);
  int start = data.indexOf("objectId\":\"");
  String path = data.substring(start+11,data.indexOf("\",",start)) ;
  pageContext.setAttribute("path", path) ;
  int x = 0;
%>
<%@page import="java.util.Calendar"%>
<jcr:session>
<c:if test="${action eq 'getChildren'}">
(
[
<jcr:set item="${path}" var="node"/>
<c:forEach var="child" items="${node.nodes}">
	<% if (x!=0) {%>,<%}else {x++;}%>
	{
	title:"<c:out value="${child.name}"/>",
	isFolder:true,
	objectId:"<c:out value="${child.path}"/>"
	}
</c:forEach>
]
)
</c:if>
</jcr:session>
