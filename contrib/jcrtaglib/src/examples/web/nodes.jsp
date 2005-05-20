<%@taglib uri="/taglib/jcr" prefix="jcr" %>
<%@taglib uri="/taglib/c" prefix="c" %>
<h1>Nodes Tag with default traverser</h1>

<h2>Depth = 0</h2>
<jcr:session>

<jcr:nodes node="/" var="node" traverserDepth="0">
	<c:out value="${node.path}"/><br>
</jcr:nodes>

<h2>Depth = 1</h2>
<jcr:nodes node="/" var="node" traverserDepth="1">
	<c:out value="${node.path}"/><br>
</jcr:nodes>

<h2>Depth = 2</h2>
<jcr:nodes node="/" var="node" traverserDepth="2">
	<c:out value="${node.path}"/><br>
</jcr:nodes>

</jcr:session>

