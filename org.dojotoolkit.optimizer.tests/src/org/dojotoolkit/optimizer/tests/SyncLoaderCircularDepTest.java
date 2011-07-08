/*
    Copyright (c) 2004-2010, The Dojo Foundation All Rights Reserved.
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

public class SyncLoaderCircularDepTest extends OptimizerTest {
	private static String depCompare = "/test/syncloader/circular/F.js /test/syncloader/circular/E.js /test/syncloader/circular/D.js /test/syncloader/circular/C.js /test/syncloader/circular/B.js /test/syncloader/circular/A.js ";
	
	public SyncLoaderCircularDepTest(BundleContext bundleContext, String[] ids) {
		super(bundleContext, ids);
	}

	protected void runTest() throws Throwable {
		RhinoClassLoader rhinoClassLoader = new RhinoClassLoader(resourceLoader);
		JSOptimizer optimizer = factory.createJSOptimizer(resourceLoader, rhinoClassLoader, true, null);
		try {
			JSAnalysisData analysisData = optimizer.getAnalysisData(new String[] {"test.syncloader.circular.A"});
			StringBuffer sb = new StringBuffer();
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

