<%@ page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="ISO-8859-1"%>
<%@ page import="org.dojotoolkit.optimizer.JSOptimizer" %>
<%@ page import="org.dojotoolkit.optimizer.servlet.JSURLGenerator" %>
<%@ page import="org.dojotoolkit.json.JSONParser" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.io.StringReader" %>
<%@ page import="java.io.IOException" %>
<!DOCTYPE html>
<html>
<head>
    <title>AMD Calendar Example via JSP/Zazl Optimizer</title>
	<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
	<style type="text/css">
		@import "dojo/resources/dojo.css";
		@import "dijit/themes/claro/claro.css";
	</style>
	<script type="text/javascript">
        var dojoConfig = {
            locale : "<%=request.getLocale().toString().toLowerCase().replace('_', '-')%>"
		};
        var zazlConfig = {
        	packages: [{name: 'dojo'}, {name: 'dijit'}, {name: 'dojox'}],
            config: { "amdtest/Calendar": {myconfigval: "My Config Value"}}
        };
	</script>
	<%
		JSOptimizer jsOptimizer = (JSOptimizer)pageContext.getServletContext().getAttribute("org.dojotoolkit.optimizer.JSOptimizer");
		if (jsOptimizer == null) {
			throw new JspException("A JSOptimizer  has not been loaded into the servlet context");
		}
		JSURLGenerator urlGenerator = new JSURLGenerator(jsOptimizer, request.getLocale(), request.getContextPath());
		String configString = "{'packages': ["+
		                      "{'name': 'dojo', 'location': 'dojo', 'main':'main'},"+
		                      "{'name': 'dijit', 'location': 'dijit', 'main':'main'},"+
		                      "{'name': 'dojox', 'location': 'dojox', 'main':'main'}"+
		                      "]}";
		Map<String, Object> cfg = null;        
		try {        
			cfg = (Map<String, Object>)JSONParser.parse(new StringReader(configString));
		} catch (IOException e) {
			throw new JspException(e);
		}
	%>
	<script type="text/javascript" src="<%=urlGenerator.generateURL("amdtest/Calendar", cfg)%>"></script>
	<script type="text/javascript">
        require(["amdtest/Calendar"], 
        function(calendar) {
            console.log("done");
        });
	</script>
</head>
<body class="claro">
<div id="calendarNode"></div>
</body>
</html>