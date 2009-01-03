/*******************************************************************************
 * Copyright (c) 2009 AGETO Service GmbH and others.
 * All rights reserved.
 *  
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 * 
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.cloudfree.examples.bugsearch.internal.setup;

import org.eclipse.cloudfree.configuration.PlatformConfiguration;
import org.eclipse.cloudfree.configuration.preferences.PlatformScope;
import org.eclipse.cloudfree.configuration.service.IConfigurationService;
import org.osgi.service.prefs.BackingStoreException;

public class BugSearchDevSetup {

	public static final String DEFAULT_URL = "http:///bugsearch/";
	public static final String PLUGIN_ID_BUGSEARCH = "org.eclipse.cloudfree.examples.bugsearch";
	public static final String URL = "url";

	public static void enableServerRole() throws BackingStoreException {
		//"org.eclipse.cloudfree.examples.bugsearch"
		String roles = PlatformConfiguration.getConfigurationService().getString("org.eclipse.cloudfree.boot", "rolesToStart", null, null);
		if ((null == roles) || (roles.length() == 0)) {
			roles = PLUGIN_ID_BUGSEARCH;
		} else {
			roles += ",org.eclipse.cloudfree.examples.bugsearch";
		}
		PlatformConfiguration.getConfigurationService().putString("org.eclipse.cloudfree.boot", "rolesToStart", roles, null, false);
		new PlatformScope().getNode("org.eclipse.cloudfree.boot").flush();
	}

	public static void setFanShopUrl(final String url) throws BackingStoreException {
		final IConfigurationService configurationService = PlatformConfiguration.getConfigurationService();
		configurationService.putString(PLUGIN_ID_BUGSEARCH, URL, url, null, false);
		new PlatformScope().getNode(PLUGIN_ID_BUGSEARCH).flush();
	}

	private BugSearchDevSetup() {
	}

}
