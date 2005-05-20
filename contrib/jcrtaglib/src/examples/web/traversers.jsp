<%@taglib uri="/taglib/jcr" prefix="jcr" %>
<%@taglib uri="/taglib/c" prefix="c" %>
<h1>Traversers</h1>

<jcr:session>

<h2>Preorder traverser</h2>
<jcr:nodes 
	node="/" 
	var="node" 
	traverserID="org.apache.jackrabbit.taglib.traverser.PreorderTraverser" 
	traverserDepth="5">
	<c:out value="${node.path}"/><br>
</jcr:nodes>

<h2>Postorder traverser</h2>
<jcr:nodes 
	node="/" 
	var="node" 
	traverserID="org.apache.jackrabbit.taglib.traverser.PostorderTraverser" 
	traverserDepth="5">
	<c:out value="${node.path}"/><br>
</jcr:nodes>

<h2>AncestorTraverser (breadcrumb) from node "Apples/Apple1/Nice apple1"</h2>
<jcr:nodes 
	node="Apples/Apple1/Nice apple1" 
	var="node" 
	traverserID="org.apache.jackrabbit.taglib.traverser.AncestorsTraverser" 
	traverserDepth="5">
	<c:out value="${node.name}"/> /
</jcr:nodes>
<br>

<h2>ExpandedNodeTraverser (navigator) to node "Apples/Apple1"</h2>
<p>Depth is ignored in this Traverser implementation</p>
<jcr:nodes 
	node="/" 
	var="node" 
	traverserID="org.apache.jackrabbit.taglib.traverser.ExpandedNodeTraverser" 
	traverserParam="Apples/Apple1"
	traverserDepth="5">

	<c:forEach begin="0" end="${node.depth}">
		&nbsp;&nbsp;
	</c:forEach>
	/ <c:out value="${node.name}"/><br/>
</jcr:nodes>
<br>

<h2>
	ExpandedNodesTraverser (navigator with multiple expanded nodes)<br> 
	to node "/Apples/Apple1" and <br>
	to node "/Oranges" 
	</h2>
<p>Depth is ignored in this Traverser implementation</p>

<jcr:set item="Apples/Apple1" var="apple1" />
<jcr:set item="Oranges" var="oranges" />

<%
 	java.util.Collection c = new java.util.ArrayList() ;
 	c.add(pageContext.getAttribute("apple1"));
 	c.add(pageContext.getAttribute("oranges")); 	
 	pageContext.setAttribute("target", c);
%>
<jcr:nodes 
	node="/" 
	var="node" 
	traverserID="org.apache.jackrabbit.taglib.traverser.ExpandedNodesTraverser" 
	traverserParam="${target}"
	traverserDepth="5">
	<c:forEach begin="0" end="${node.depth}">
		&nbsp;&nbsp;
	</c:forEach>
	/ <c:out value="${node.name}"/><br/>
</jcr:nodes>


</jcr:session>
