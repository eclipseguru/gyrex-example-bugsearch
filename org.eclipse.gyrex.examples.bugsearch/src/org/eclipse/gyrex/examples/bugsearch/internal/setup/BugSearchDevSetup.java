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

import org.apache.commons.lang.StringUtils;
import org.eclipse.cloudfree.common.debug.BundleDebug;
import org.eclipse.cloudfree.configuration.PlatformConfiguration;
import org.eclipse.cloudfree.configuration.preferences.PlatformScope;
import org.eclipse.cloudfree.configuration.service.IConfigurationService;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.osgi.service.prefs.BackingStoreException;

public class BugSearchDevSetup {

	public static final String DEFAULT_URL = "http:///bugsearch/";
	public static final String PLUGIN_ID_BUGSEARCH = "org.eclipse.cloudfree.examples.bugsearch";
	public static final String URL = "url";

	public static void enableServerRole() throws BackingStoreException {
		// enable required server roles
		//  - org.eclipse.cloudfree.boot.role.admin.gwt
		//  - org.eclipse.cloudfree.boot.role.http.registry
		//  - org.eclipse.cloudfree.boot.role.http.jetty
		//  - org.eclipse.cloudfree.examples.bugsearch
		try {
			final IEclipsePreferences preferences = new PlatformScope().getNode("org.eclipse.cloudfree.boot");
			String roles = preferences.get("rolesToStart", "");
			if (StringUtils.isNotBlank(roles)) {
				roles += ",";
			}
			roles += "org.eclipse.cloudfree.boot.role.admin.gwt,org.eclipse.cloudfree.boot.role.http.registry,org.eclipse.cloudfree.boot.role.http.jetty,org.eclipse.cloudfree.examples.bugsearch";
			preferences.put("rolesToStart", roles);
			preferences.flush();
		} catch (final Exception e) {
			BundleDebug.debug("Error while activating required server roles. " + e.getMessage(), e);
		}
	}

	public static void setFanShopUrl(final String url) throws BackingStoreException {
		final IConfigurationService configurationService = PlatformConfiguration.getConfigurationService();
		configurationService.putString(PLUGIN_ID_BUGSEARCH, URL, url, null, false);
		new PlatformScope().getNode(PLUGIN_ID_BUGSEARCH).flush();
	}

	private BugSearchDevSetup() {
	}

}
