package org.dojotoolkit.optimizer.tests;

import org.dojotoolkit.optimizer.rhino.RhinoJSOptimizerFactory;
import org.osgi.framework.BundleContext;

public class RhinoSyncLoaderOptimizerTest extends SyncLoaderOptimizerTest {

	public RhinoSyncLoaderOptimizerTest(BundleContext bundleContext, String[] ids) {
		super(bundleContext, ids);
	}
	
	protected void setUp() throws Exception {
		super.setUp();
		factory = new RhinoJSOptimizerFactory();
	}
}
