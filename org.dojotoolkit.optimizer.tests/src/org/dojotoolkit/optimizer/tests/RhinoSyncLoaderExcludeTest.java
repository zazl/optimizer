/*
    Copyright (c) 2004-2011, The Dojo Foundation All Rights Reserved.
    Available via Academic Free License >= 2.1 OR the modified BSD license.
    see: http://dojotoolkit.org/license for details
*/
package org.dojotoolkit.optimizer.tests;

import org.dojotoolkit.optimizer.rhino.RhinoJSOptimizerFactory;
import org.osgi.framework.BundleContext;

public class RhinoSyncLoaderExcludeTest extends SyncLoaderExcludeTest {
	public RhinoSyncLoaderExcludeTest(BundleContext bundleContext, String[] ids) {
		super(bundleContext, ids);
	}
	
	protected void setUp() throws Exception {
		super.setUp();
		factory = new RhinoJSOptimizerFactory();
	}
}
