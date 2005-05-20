<%@taglib uri="/taglib/jcr" prefix="jcr" %>
<%@taglib uri="/taglib/c" prefix="c" %>
<h1>Count Tag with default traverser</h1>

<jcr:session>
Child nodes of "/": 

<h2>Depth = 0</h2>
<jcr:count node="/" traverserDepth="0" /><br/>

<h2>Depth = 1</h2>
<jcr:count node="/" traverserDepth="1"/><br/>

<h2>Depth = 2</h2>
<jcr:count node="/" traverserDepth="2"/><br/>

</jcr:session>

