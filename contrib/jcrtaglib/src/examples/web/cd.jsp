<%@taglib uri="/taglib/jcr" prefix="jcr" %>
<%@taglib uri="/taglib/c" prefix="c" %>
<h1>Nodes Tag with default traverser</h1>

<jcr:session>

<h3>Current directory stored in the page Context</h3>
<h2>Depth = 1</h2>
<jcr:cd node="/TestA">
	<jcr:nodes node="A-L2" var="node" traverserDepth="1">
		<c:out value="${node.path}"/><br>
	</jcr:nodes>
</jcr:cd>

<h2>Depth = 2</h2>
<jcr:cd node="/TestA">
	<jcr:nodes node="A-L2" var="node" traverserDepth="2">
		<c:out value="${node.path}"/><br>
	</jcr:nodes>
</jcr:cd>

<h3>Current directory stored in the request</h3>
<jcr:cd node="/TestA" scope="request">
	<jcr:out item="A-L2"/><br>
</jcr:cd>

</jcr:session>

