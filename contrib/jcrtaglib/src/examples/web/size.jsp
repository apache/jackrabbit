<%@taglib uri="/taglib/jcr" prefix="jcr" %>
<%@taglib uri="/taglib/c" prefix="c" %>
<h1>Size Tag with default traverser</h1>

<jcr:session>

	<h2>Size of root node: </h2>
	<jcr:size node="/" traverserDepth="0" /> bytes <br/>
	<jcr:properties node="/" var="prop">
		<jcr:out item="${prop}"/> <br>
	</jcr:properties>

	<h2>Size of testA node</h2>
	<jcr:size node="TestA" traverserDepth="0" /> bytes <br/>
	<jcr:properties node="/TestA" var="prop">
		<jcr:out item="${prop}"/><br>
	</jcr:properties>

	<h2>Size of A-L2 node</h2>
	<jcr:size node="TestA/A-L2" traverserDepth="0" /> bytes <br/>
	<jcr:properties node="/TestA/A-L2" var="prop">
		<jcr:out item="${prop}"/><br>
	</jcr:properties>

	<h2>Size of testA node with depth=1</h2>
	<jcr:size node="TestA" traverserDepth="1" /> bytes <br/>

	<h2>Size of testB node</h2>
	<jcr:size node="/TestB" traverserDepth="0" /> bytes <br/>
	<jcr:properties node="TestB" var="prop">
		<jcr:out item="${prop}"/><br>
	</jcr:properties>

</jcr:session>
