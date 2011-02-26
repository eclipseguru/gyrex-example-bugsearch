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

import org.eclipse.gyrex.cds.facets.IFacet;
import org.eclipse.gyrex.cds.facets.IFacetManager;
import org.eclipse.gyrex.cds.query.FacetSelectionStrategy;
import org.eclipse.gyrex.cds.solr.ISolrCdsConstants;
import org.eclipse.gyrex.context.IRuntimeContext;
import org.eclipse.gyrex.context.preferences.IRuntimeContextPreferences;
import org.eclipse.gyrex.context.registry.IRuntimeContextRegistry;
import org.eclipse.gyrex.examples.bugsearch.internal.app.BugSearchApplicationProvider;
import org.eclipse.gyrex.http.application.manager.IApplicationManager;
import org.eclipse.gyrex.model.common.ModelException;
import org.eclipse.gyrex.model.common.ModelUtil;
import org.eclipse.gyrex.persistence.context.preferences.IContextPreferencesRepositoryConstants;
import org.eclipse.gyrex.persistence.internal.storage.DefaultRepositoryLookupStrategy;
import org.eclipse.gyrex.persistence.solr.internal.SolrActivator;
import org.eclipse.gyrex.persistence.storage.registry.IRepositoryRegistry;
import org.eclipse.gyrex.persistence.storage.settings.IRepositoryPreferences;
import org.eclipse.gyrex.preferences.CloudScope;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;

import org.osgi.service.component.ComponentContext;

import org.apache.commons.io.FileUtils;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrCore;
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
	private IRepositoryRegistry repositoryRegistry;

	@SuppressWarnings("restriction")
	protected void activate(final ComponentContext context) {
		LOG.trace("BugSearchRuntimeSetupComponent activation triggered");

		contextRegistry = (IRuntimeContextRegistry) context.locateService("IRuntimeContextRegistry");
		applicationManager = (IApplicationManager) context.locateService("IApplicationManager");
		repositoryRegistry = (IRepositoryRegistry) context.locateService("IRepositoryRegistry");

		eclipseBugSearchContext = contextRegistry.get(IEclipseBugSearchConstants.CONTEXT_PATH);
		if (null == eclipseBugSearchContext) {
			LOG.error("Eclipse bug search context not found!");
			return;
		}

		// FIXME: use a different technique, ZooKeeper might not be available at this point
		try {
			Thread.sleep(5000);
		} catch (final InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		// lookup the configured URL
		final IRuntimeContextPreferences preferences = eclipseBugSearchContext.getPreferences();
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

		// define the repository
		final IEclipsePreferences repositoryStore = new CloudScope().getNode("org.eclipse.gyrex.persistence");
		repositoryStore.node("repositories/" + IEclipseBugSearchConstants.REPOSITORY_ID).put("type", "org.eclipse.gyrex.persistence.solr.embedded");
		try {
			repositoryStore.flush();
		} catch (final Exception e) {
			LOG.error("Error while flushing preferences after defining repository: " + e, e);
			return;
		}

		// configure the repository which the application should use
		preferences.put("org.eclipse.gyrex.persistence", "repositories//application/x-cf-listings-solr", IEclipseBugSearchConstants.REPOSITORY_ID, false);
		try {
			preferences.flush("org.eclipse.gyrex.persistence");
		} catch (final Exception e) {
			LOG.error("Error while flushing preferences after setting repositories: " + e, e);
			return;
		}

		// configure the facets
		try {
			// create facets repo
			final String repositoryId = "eclipsebugsearch.facets";
			if (null == repositoryRegistry.getRepositoryDefinition(repositoryId)) {
				final IRepositoryPreferences repository = repositoryRegistry.createRepository(repositoryId, IContextPreferencesRepositoryConstants.PROVIDER_ID);
				final IEclipsePreferences prefs = repository.getPreferences();
				prefs.put(IContextPreferencesRepositoryConstants.PREF_KEY_CONTEXT_PATH, eclipseBugSearchContext.getContextPath().toString());
				prefs.flush();
				DefaultRepositoryLookupStrategy.setRepository(eclipseBugSearchContext, ISolrCdsConstants.FACET_CONTENT_TYPE, repositoryId);
			}

			final IFacetManager facetManager = ModelUtil.getManager(IFacetManager.class, eclipseBugSearchContext);
			createFacet(facetManager, "tags", "Tags", true);
			createFacet(facetManager, "keywords", "Keywords", true);
			createFacet(facetManager, "classification", "Classification", true);
			createFacet(facetManager, "product", "Product", true);
			createFacet(facetManager, "component", "Component", true);
			createFacet(facetManager, "status", "Status", true);
			createFacet(facetManager, "resolution", "Resolution", true);
			createFacet(facetManager, "targetMilestone", "Milestone", true);
			createFacet(facetManager, "version", "Version", true);
			createFacet(facetManager, "statusWhiteboard", "Whiteboard", true);
			createFacet(facetManager, "priority", "Priority", true);
			createFacet(facetManager, "severity", "Severity", true);
			createFacet(facetManager, "hardware", "Hardware", true);
			createFacet(facetManager, "os", "OS", true);
			createFacet(facetManager, "assignee", "Assignee", true);
			createFacet(facetManager, "reporter", "Reporter", true);
			createFacet(facetManager, "cc", "CC", true);
			createFacet(facetManager, "commenter", "Commenter", true);
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

	private void createFacet(final IFacetManager facetManager, final String attributeId, final String name, final boolean enabled) throws ModelException {
		final IFacet facet = facetManager.create(attributeId);
		facet.setName(name);
		facet.setSelectionStrategy(FacetSelectionStrategy.MULTI);
		facet.setEnabled(enabled);
		facetManager.save(facet);
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
