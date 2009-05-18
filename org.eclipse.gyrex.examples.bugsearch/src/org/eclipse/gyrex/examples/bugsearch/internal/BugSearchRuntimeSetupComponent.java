/**
 * Copyright (c) 2009 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 */
package org.eclipse.gyrex.examples.bugsearch.internal;

import java.io.File;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrCore;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.gyrex.context.IRuntimeContext;
import org.eclipse.gyrex.context.preferences.IRuntimeContextPreferences;
import org.eclipse.gyrex.context.preferences.PreferencesUtil;
import org.eclipse.gyrex.context.registry.IRuntimeContextRegistry;
import org.eclipse.gyrex.examples.bugsearch.internal.app.BugSearchApplicationProvider;
import org.eclipse.gyrex.http.application.manager.IApplicationManager;
import org.eclipse.gyrex.persistence.solr.internal.SolrActivator;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class performs the runtime setup of the BugSearch application.
 * Currently, this happens dynamically at runtime because Gyrex does not provide
 * a persistent system yet. However, in the future this becomes obsolete because
 * the idea is that the setup happens only once (either during the config wizard
 * or through some other sort of admin interface) and Gyrex remembers the setup.
 */
public class BugSearchRuntimeSetupComponent {

	private static final Logger LOG = LoggerFactory.getLogger(BugSearchRuntimeSetupComponent.class);
	private IApplicationManager applicationManager;
	private IRuntimeContextRegistry contextRegistry;
	private String url;
	private IRuntimeContext eclipseBugSearchContext;

	protected void activate(final ComponentContext context) {
		LOG.trace("BugSearchRuntimeSetupComponent activation triggered");

		contextRegistry = (IRuntimeContextRegistry) context.locateService("IRuntimeContextRegistry");
		applicationManager = (IApplicationManager) context.locateService("IApplicationManager");

		eclipseBugSearchContext = contextRegistry.get(IEclipseBugSearchConstants.CONTEXT_PATH);
		if (null == eclipseBugSearchContext) {
			LOG.error("Eclipse bug search context not found!");
			return;
		}

		// lookup the configured URL
		final IRuntimeContextPreferences preferences = PreferencesUtil.getPreferences(eclipseBugSearchContext);
		url = preferences.get(BugSearchActivator.PLUGIN_ID, IEclipseBugSearchConstants.KEY_URL, null);
		if (null == url) {
			LOG.debug("No URL configured for BugSearch, not registering the application");
			return;
		}

		// create the Solr index if necessary & initialize it
		boolean created;
		try {
			created = initializeSolrCore();
		} catch (final Exception e) {
			LOG.error("Error while creating the Solr index: " + e.getMessage(), e);
			return;
		}

		// configure the repository which the application should use
		preferences.put("org.eclipse.gyrex.persistence", "repositories//application/x-cf-listings-solr", IEclipseBugSearchConstants.REPOSITORY_ID, false);
		preferences.put("org.eclipse.gyrex.persistence", "repositories/" + IEclipseBugSearchConstants.REPOSITORY_ID + "//type", "org.eclipse.gyrex.persistence.solr.embedded", false);
		try {
			preferences.flush("org.eclipse.gyrex.persistence");
		} catch (final Exception e) {
			LOG.error("Error while flushing preferences after setting repositories: " + e, e);
			return;
		}

		// configure the facets
		preferences.put("org.eclipse.gyrex.cds.service.solr", "facets/classification", "Classification,field,classification_facet", false);
		preferences.put("org.eclipse.gyrex.cds.service.solr", "facets/product", "Product,field,product_facet", false);
		preferences.put("org.eclipse.gyrex.cds.service.solr", "facets/component", "Component,field,component_facet", false);
		preferences.put("org.eclipse.gyrex.cds.service.solr", "facets/keywords", "Keywords,field,keywords_facet", false);
		preferences.put("org.eclipse.gyrex.cds.service.solr", "facets/tags", "Tags,field,tags_facet", false);
		preferences.put("org.eclipse.gyrex.cds.service.solr", "facets/status", "Status,field,status_facet", false);
		preferences.put("org.eclipse.gyrex.cds.service.solr", "facets/resolution", "Resolution,field,resolution_facet", false);
		preferences.put("org.eclipse.gyrex.cds.service.solr", "facets/targetMilestone", "Milestone,field,targetMilestone_facet", false);
		preferences.put("org.eclipse.gyrex.cds.service.solr", "facets/version", "Version,field,version_facet", false);
		preferences.put("org.eclipse.gyrex.cds.service.solr", "facets/statusWhiteboard", "Whiteboard,field,statusWhiteboard_facet", false);
		preferences.put("org.eclipse.gyrex.cds.service.solr", "facets/priority", "Priority,field,priority_facet", false);
		preferences.put("org.eclipse.gyrex.cds.service.solr", "facets/severity", "Severity,field,severity_facet", false);
		preferences.put("org.eclipse.gyrex.cds.service.solr", "facets/hardware", "Hardware,field,hardware_facet", false);
		preferences.put("org.eclipse.gyrex.cds.service.solr", "facets/os", "OS,field,os_facet", false);
		preferences.put("org.eclipse.gyrex.cds.service.solr", "facets/assignee", "Assignee,field,assignee_facet", false);
		preferences.put("org.eclipse.gyrex.cds.service.solr", "facets/reporter", "Reporter,field,reporter_facet", false);
		preferences.put("org.eclipse.gyrex.cds.service.solr", "facets/cc", "CC,field,cc_facet", false);
		preferences.put("org.eclipse.gyrex.cds.service.solr", "facets/commenter", "Commenter,field,commenter_facet", false);
		//		preferences.put("org.eclipse.gyrex.cds.service.solr", "facets/", ",field,_facet", false);
		//		preferences.put("org.eclipse.gyrex.cds.service.solr", "facets/", ",field,_facet", false);
		try {
			preferences.flush("org.eclipse.gyrex.cds.service.solr");
		} catch (final Exception e) {
			LOG.error("Error while flushing preferences after configuring the facets: " + e, e);
			return;
		}

		// define  & mount the Eclipse BugSearch application
		try {
			applicationManager.register(IEclipseBugSearchConstants.APPLICATION_ID, BugSearchApplicationProvider.ID, eclipseBugSearchContext, null);
			applicationManager.mount(url, IEclipseBugSearchConstants.APPLICATION_ID);
		} catch (final Exception e) {
			LOG.error("Error while registering the bugsearch application", e);
			return;
		}

		// reset the index counter if we created a new index
		if (created) {
			preferences.remove(BugSearchActivator.PLUGIN_ID, "import.start");
			try {
				preferences.flush(BugSearchActivator.PLUGIN_ID);
			} catch (final Exception e) {
				LOG.error("Error while flushing preferences after resetting the index counter: " + e, e);
				// but continue
			}
		}

		// schedule initial indexing followed by updates every 20 minutes
		BugzillaUpdateScheduler.scheduleInitialImportFollowedByUpdate(eclipseBugSearchContext, 20, TimeUnit.MINUTES);
	}

	protected void deactivate(final ComponentContext context) {
		LOG.trace("BugSearchRuntimeSetupComponent de-activation triggered");

		// cancel updates
		BugzillaUpdateScheduler.cancelUpdateJob();

		// unmount the application
		try {
			if (null != url) {
				applicationManager.unmount(url);
			}
			applicationManager.unregister(IEclipseBugSearchConstants.APPLICATION_ID);
		} catch (final Exception e) {
			LOG.error("Error while un-registering the bugsearch application", e);
		}

		// release references
		url = null;
		applicationManager = null;
		contextRegistry = null;
	}

	@SuppressWarnings("restriction")
	private boolean initializeSolrCore() throws Exception {
		boolean created = false;
		// copy config and schema
		// the configuration template
		final File configTemplate = new File(FileLocator.toFileURL(BugSearchActivator.getInstance().getBundle().getEntry("conf-solr")).getFile());

		// create Solr instance directory
		final File solrBase = SolrActivator.getInstance().getEmbeddedSolrBase();
		if (null == solrBase) {
			throw new IllegalStateException("no Solr base directory");
		}
		final File instanceDir = new File(solrBase, IEclipseBugSearchConstants.REPOSITORY_ID);
		if (!instanceDir.isDirectory()) {
			// initialize dir
			instanceDir.mkdirs();
			created = true;
		}

		// publish configuration
		FileUtils.copyDirectory(configTemplate, instanceDir);

		// create core or reload core
		final CoreContainer coreContainer = SolrActivator.getInstance().getEmbeddedCoreContainer();
		if (null == coreContainer) {
			throw new IllegalStateException("no coreContainer");
		}
		final SolrCore core = coreContainer.getCore(IEclipseBugSearchConstants.REPOSITORY_ID);
		try {
			if (null == core) {
				final SolrCore adminCore = coreContainer.getAdminCore();
				try {
					final EmbeddedSolrServer adminServer = new EmbeddedSolrServer(coreContainer, adminCore.getName());
					CoreAdminRequest.createCore(IEclipseBugSearchConstants.REPOSITORY_ID, IEclipseBugSearchConstants.REPOSITORY_ID, adminServer);
					created = true;
				} finally {
					adminCore.close();
				}
			} else {
				coreContainer.reload(IEclipseBugSearchConstants.REPOSITORY_ID);
			}
		} finally {
			if (null != core) {
				core.close();
			}
		}

		return created;
	}

}
