/*
    Copyright (c) 2004-2011, The Dojo Foundation All Rights Reserved.
    Available via Academic Free License >= 2.1 OR the modified BSD license.
    see: http://dojotoolkit.org/license for details
*/
package org.dojotoolkit.optimizer.tests;

import java.io.IOException;

import junit.framework.TestCase;

import org.dojotoolkit.optimizer.JSAnalysisData;
import org.dojotoolkit.optimizer.JSOptimizer;
import org.dojotoolkit.server.util.rhino.RhinoClassLoader;
import org.osgi.framework.BundleContext;

public class SyncLoaderExcludeTest extends OptimizerTest {
	private static String depCompare = "/dojo/fx/Toggler.js /dojo/fx.js /dojo/html.js /dijit/layout/ContentPane.js /dijit/layout/AccordionPane.js /dijit/layout/AccordionContainer.js "; 
	
	public SyncLoaderExcludeTest(BundleContext bundleContext, String[] ids) {
		super(bundleContext, ids);
	}

	protected void runTest() throws Throwable {
		RhinoClassLoader rhinoClassLoader = new RhinoClassLoader(resourceLoader);
		JSOptimizer optimizer = factory.createJSOptimizer(resourceLoader, rhinoClassLoader, true, null);
		JSAnalysisData analysisData;
		try {
			analysisData = optimizer.getAnalysisData(new String[] {"dijit.layout.TabContainer"});
			StringBuffer sb = new StringBuffer();
			
			analysisData = optimizer.getAnalysisData(new String[] {"dijit.layout.AccordionContainer"}, new JSAnalysisData[] {analysisData});
			for (String dependency: analysisData.getDependencies()) {
				sb.append(dependency);
				sb.append(' ');
			}
			assertEquals(depCompare, sb.toString());
		} catch (IOException e) {
			TestCase.fail(e.getMessage());
			e.printStackTrace();
		}
	}
}
