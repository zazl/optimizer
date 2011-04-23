/*
    Copyright (c) 2004-2010, The Dojo Foundation All Rights Reserved.
    Available via Academic Free License >= 2.1 OR the modified BSD license.
    see: http://dojotoolkit.org/license for details
*/
package org.dojotoolkit.optimizer;

import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.dojotoolkit.server.util.resource.ResourceLoader;

public class ChecksumCreator {
	private static Logger logger = Logger.getLogger("org.dojotoolkit.optimizer");
	public static String createChecksum(String[] dependencies, ResourceLoader resourceLoader) throws IOException {
		long start = System.currentTimeMillis();
		String checksum = null;
		StringBuffer content = new StringBuffer();
		for (String dependency : dependencies) {
			String contentElement = resourceLoader.readResource(Util.normalizePath(dependency));
			if (contentElement != null) {
				content.append(contentElement);
			}
		}
		try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(content.toString().getBytes());
            BigInteger number = new BigInteger(1,messageDigest);
            checksum = number.toString(16);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
		
		long end = System.currentTimeMillis();
		logger.logp(Level.FINE, ChecksumCreator.class.getName(), "createChecksum", "time : "+(end-start)+" ms");
		return checksum;
	}
}
