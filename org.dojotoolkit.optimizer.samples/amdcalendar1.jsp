<%@ page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="ISO-8859-1"%>
<%@ page import="org.dojotoolkit.optimizer.JSOptimizer" %>
<%@ page import="org.dojotoolkit.optimizer.JSAnalysisData" %>
<!DOCTYPE html>
<html>
<head>
	<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
	<style type="text/css">
		@import "dojo/resources/dojo.css";
		@import "dijit/themes/claro/claro.css";
	</style>
	<script type="text/javascript">
		require = {
			async: 1,
			baseUrl: './',
            packages: [
                {
                    name: 'dojo',
                    location: 'dojo',
                    main:'main',
                    lib: '.'
                },
                {
                    name: 'dijit',
                    location: 'dijit',
                    main:'main',
                    lib: '.'
                },
                {
                    name: 'dojox',
                    location: 'dojox',
                    main:'main',
                    lib: '.'
                }
            ],
            has: {
                "config-tlmSiblingOfDojo":0,
                "dojo-sync-loader":0
            },
			locale : "<%=request.getLocale().toString().toLowerCase().replace('_', '-')%>"
		};
	</script>
	<%
		boolean debug = (request.getParameter("debug") == null) ? false : Boolean.valueOf(request.getParameter("debug"));
		String url = null;
		if (debug) {
			url = request.getContextPath() +"/_javascript?debug=true";
		} else {
		    JSOptimizer jsOptimizer = (JSOptimizer)pageContext.getServletContext().getAttribute("org.dojotoolkit.optimizer.JSOptimizer");
		    if (jsOptimizer == null) {
		    	throw new JspException("A JSOptimizer  has not been loaded into the servlet context");
		    }
			JSAnalysisData analysisData = jsOptimizer.getAnalysisData(new String[] {"amdtest/Calendar"});
			url = request.getContextPath() +"/_javascript?modules=amdtest/Calendar&version="+analysisData.getChecksum()+"&locale="+request.getLocale();
		}
	%>
	<script type="text/javascript" src="<%=url%>"></script>
	<script type="text/javascript">
		require(["dojo/domReady!", 'amdtest/Calendar']);
	</script>

</head>
<body class="claro">
<div id="calendarNode"></div>
</body>
</html>