/*
    Copyright (c) 2004-2011, The Dojo Foundation All Rights Reserved.
    Available via Academic Free License >= 2.1 OR the modified BSD license.
    see: http://dojotoolkit.org/license for details
*/
package org.dojotoolkit.optimizer.tests;

import java.io.IOException;

import org.dojotoolkit.optimizer.JSAnalysisData;
import org.dojotoolkit.optimizer.JSOptimizer;
import org.dojotoolkit.optimizer.Localization;
import org.dojotoolkit.server.util.rhino.RhinoClassLoader;
import org.osgi.framework.BundleContext;

import junit.framework.TestCase;

public abstract class SyncLoaderOptimizerTest extends OptimizerTest {
	private static String depCompare = "/dojo/window.js /dijit/_base/manager.js /dijit/_base/focus.js /dojo/AdapterRegistry.js /dijit/_base/place.js /dijit/_base/window.js /dijit/_base/popup.js /dijit/_base/scroll.js /dojo/uacss.js /dijit/_base/sniff.js /dijit/_base/typematic.js /dijit/_base/wai.js /dijit/_base.js /dijit/_Widget.js /dojo/string.js /dojo/date/stamp.js /dojo/parser.js /dojo/cache.js /dijit/_Templated.js /dijit/_Container.js /dijit/_Contained.js /dijit/layout/_LayoutWidget.js /dojo/regexp.js /dojo/cookie.js /dijit/_CssStateMixin.js /dijit/form/_FormWidget.js /dijit/_HasDropDown.js /dijit/form/Button.js /dijit/form/ToggleButton.js /dijit/layout/StackController.js /dijit/layout/StackContainer.js /dijit/layout/_TabContainerBase.js /dijit/_KeyNavContainer.js /dijit/MenuItem.js /dijit/PopupMenuItem.js /dijit/CheckedMenuItem.js /dijit/MenuSeparator.js /dijit/Menu.js /dijit/layout/TabController.js /dijit/layout/ScrollingTabController.js /dijit/layout/TabContainer.js "; 
	private static String locCompare = "dijit.nls.common:/dojo/../dijit/nls:common ";
	
	public SyncLoaderOptimizerTest(BundleContext bundleContext, String[] ids) {
		super(bundleContext, ids);
	}

	protected void runTest() throws Throwable {
		RhinoClassLoader rhinoClassLoader = new RhinoClassLoader(resourceLoader);
		JSOptimizer optimizer = factory.createJSOptimizer(resourceLoader, rhinoClassLoader, true, null);
		JSAnalysisData analysisData;
		try {
			analysisData = optimizer.getAnalysisData(new String[] {"dijit.layout.TabContainer"});
			StringBuffer sb = new StringBuffer();
			for (String dependency: analysisData.getDependencies()) {
				sb.append(dependency);
				sb.append(' ');
			}
			assertEquals(depCompare, sb.toString());
			sb = new StringBuffer();
			for (Localization l : analysisData.getLocalizations()) {
				sb.append(l.bundlePackage+":"+l.modulePath+":"+l.bundleName);
				sb.append(' ');
			}
			assertEquals(locCompare, sb.toString());
		} catch (IOException e) {
			TestCase.fail(e.getMessage());
			e.printStackTrace();
		}
	}
}
