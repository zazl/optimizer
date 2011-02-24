/*
    Copyright (c) 2004-2010, The Dojo Foundation All Rights Reserved.
    Available via Academic Free License >= 2.1 OR the modified BSD license.
    see: http://dojotoolkit.org/license for details
*/
package org.dojotoolkit.optimizer;

/**
 * Simple structure for packaging localization information
 *
 */
public class Localization {
	public String bundlePackage = null;
	public String modulePath = null;
	public String bundleName = null;
	
	public Localization(String bundlePackage, String modulePath, String bundleName) {
		this.bundlePackage = bundlePackage;
		this.modulePath = modulePath;
		this.bundleName = bundleName;
	}
}
