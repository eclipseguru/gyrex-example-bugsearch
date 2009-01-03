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
package org.eclipse.cloudfree.examples.bugsearch.internal;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FileUtils;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrCore;
import org.eclipse.cloudfree.configuration.PlatformConfiguration;
import org.eclipse.cloudfree.configuration.preferences.PlatformScope;
import org.eclipse.cloudfree.configuration.service.IConfigurationService;
import org.eclipse.cloudfree.examples.bugsearch.internal.app.BugSearchApplicationProvider;
import org.eclipse.cloudfree.examples.bugsearch.internal.setup.BugSearchDevSetup;
import org.eclipse.cloudfree.http.application.manager.ApplicationRegistrationException;
import org.eclipse.cloudfree.http.application.manager.IApplicationManager;
import org.eclipse.cloudfree.http.application.manager.MountConflictException;
import org.eclipse.cloudfree.http.internal.apps.dummy.RootContext;
import org.eclipse.cloudfree.persistence.solr.internal.SolrActivator;
import org.eclipse.core.runtime.FileLocator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.util.tracker.ServiceTracker;
import org.xml.sax.SAXException;

@SuppressWarnings("restriction")
public class BugSearchRTSetup {

	public static final String APPLICATION_ID = "bugsearch";

	public static final String REPOSITORY_ID = "bugsearch.listings";

	private final BundleContext context;

	private ServiceTracker appManagerTracker;

	public BugSearchRTSetup(final BundleContext context) {
		this.context = context;
	}

	public void close() {
		appManagerTracker.close();
	}

	private void configureRepositories() throws BackingStoreException {
		final IConfigurationService configurationService = PlatformConfiguration.getConfigurationService();
		configurationService.putString("org.eclipse.cloudfree.persistence", "repositories//application/x-cf-bugs-solr", REPOSITORY_ID, null, false);
		configurationService.putString("org.eclipse.cloudfree.persistence", "repositories/" + REPOSITORY_ID + "//type", "org.eclipse.cloudfree.persistence.solr.embedded", null, false);
		new PlatformScope().getNode("org.eclipse.cloudfree.persistence").flush();

		configurationService.putString("org.eclipse.cloudfree.listings.service.solr", "facets/classification", "Classification,field,classification_facet", null, false);
		configurationService.putString("org.eclipse.cloudfree.listings.service.solr", "facets/product", "Product,field,product_facet", null, false);
		configurationService.putString("org.eclipse.cloudfree.listings.service.solr", "facets/component", "Component,field,component_facet", null, false);
		configurationService.putString("org.eclipse.cloudfree.listings.service.solr", "facets/keywords", "Keywords,field,keywords_facet", null, false);
		configurationService.putString("org.eclipse.cloudfree.listings.service.solr", "facets/tags", "Tags,field,tags_facet", null, false);
		configurationService.putString("org.eclipse.cloudfree.listings.service.solr", "facets/status", "Status,field,status_facet", null, false);
		configurationService.putString("org.eclipse.cloudfree.listings.service.solr", "facets/resolution", "Resolution,field,resolution_facet", null, false);
		configurationService.putString("org.eclipse.cloudfree.listings.service.solr", "facets/targetMilestone", "Milestone,field,targetMilestone_facet", null, false);
		configurationService.putString("org.eclipse.cloudfree.listings.service.solr", "facets/version", "Version,field,version_facet", null, false);
		configurationService.putString("org.eclipse.cloudfree.listings.service.solr", "facets/statusWhiteboard", "Whiteboard,field,statusWhiteboard_facet", null, false);
		configurationService.putString("org.eclipse.cloudfree.listings.service.solr", "facets/priority", "Priority,field,priority_facet", null, false);
		configurationService.putString("org.eclipse.cloudfree.listings.service.solr", "facets/severity", "Severity,field,severity_facet", null, false);
		configurationService.putString("org.eclipse.cloudfree.listings.service.solr", "facets/hardware", "Hardware,field,hardware_facet", null, false);
		configurationService.putString("org.eclipse.cloudfree.listings.service.solr", "facets/os", "OS,field,os_facet", null, false);
		configurationService.putString("org.eclipse.cloudfree.listings.service.solr", "facets/assignee", "Assignee,field,assignee_facet", null, false);
		configurationService.putString("org.eclipse.cloudfree.listings.service.solr", "facets/reporter", "Reporter,field,reporter_facet", null, false);
		configurationService.putString("org.eclipse.cloudfree.listings.service.solr", "facets/cc", "CC,field,cc_facet", null, false);
		configurationService.putString("org.eclipse.cloudfree.listings.service.solr", "facets/commenter", "Commenter,field,commenter_facet", null, false);
		//		configurationService.putString("org.eclipse.cloudfree.listings.service.solr", "facets/", ",field,_facet", null, false);
		//		configurationService.putString("org.eclipse.cloudfree.listings.service.solr", "facets/", ",field,_facet", null, false);

		new PlatformScope().getNode("org.eclipse.cloudfree.listings.service.solr").flush();
	}

	private boolean initializeSolrCore() throws URISyntaxException, IOException, SolrServerException, ParserConfigurationException, SAXException {
		boolean created = false;
		// copy config and schema
		// the configuration template
		final File configTemplate = new File(FileLocator.toFileURL(BugSearchActivator.getInstance().getBundle().getEntry("conf-solr")).getFile());

		// create Solr instance directory
		final File solrBase = SolrActivator.getInstance().getEmbeddedSolrBase();
		if (null == solrBase) {
			throw new IllegalStateException("no Solr base directory");
		}
		final File instanceDir = new File(solrBase, REPOSITORY_ID);
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
		final SolrCore core = coreContainer.getCore(REPOSITORY_ID);
		try {
			if (null == core) {
				final SolrCore adminCore = coreContainer.getAdminCore();
				try {
					final EmbeddedSolrServer adminServer = new EmbeddedSolrServer(coreContainer, adminCore.getName());
					CoreAdminRequest.createCore(REPOSITORY_ID, REPOSITORY_ID, adminServer);
					created = true;
				} finally {
					adminCore.close();
				}
			} else {
				coreContainer.reload(REPOSITORY_ID);
			}
		} finally {
			if (null != core) {
				core.close();
			}
		}

		return created;
	}

	private void registerApplication() {
		// register the application
		appManagerTracker = new ServiceTracker(context, IApplicationManager.class.getName(), null) {
			/* (non-Javadoc)
			 * @see org.osgi.util.tracker.ServiceTracker#addingService(org.osgi.framework.ServiceReference)
			 */
			@Override
			public Object addingService(final ServiceReference reference) {
				final IApplicationManager applicationManager = (IApplicationManager) super.addingService(reference);
				if (null != applicationManager) {
					try {
						applicationManager.register(APPLICATION_ID, BugSearchApplicationProvider.ID, new RootContext(), null);
					} catch (final ApplicationRegistrationException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

					try {
						final String url = PlatformConfiguration.getConfigurationService().getString(BugSearchDevSetup.PLUGIN_ID_BUGSEARCH, BugSearchDevSetup.URL, BugSearchDevSetup.DEFAULT_URL, null);
						applicationManager.mount(url, APPLICATION_ID);
					} catch (final MountConflictException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (final MalformedURLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				return applicationManager;
			}

			/* (non-Javadoc)
			 * @see org.osgi.util.tracker.ServiceTracker#removedService(org.osgi.framework.ServiceReference, java.lang.Object)
			 */
			@Override
			public void removedService(final ServiceReference reference, final Object service) {
				final IApplicationManager applicationManager = (IApplicationManager) service;
				if (null != applicationManager) {
					final String url = PlatformConfiguration.getConfigurationService().getString(BugSearchDevSetup.PLUGIN_ID_BUGSEARCH, BugSearchDevSetup.URL, BugSearchDevSetup.DEFAULT_URL, null);
					try {
						applicationManager.unmount(url);
					} catch (final IllegalArgumentException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (final MalformedURLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					applicationManager.unregister(APPLICATION_ID);
				}
				// unget
				super.removedService(reference, service);
			}
		};
		appManagerTracker.open();
	}

	public void runtimeSetup() throws Exception {
		final boolean created = initializeSolrCore();
		configureRepositories();
		registerApplication();

		final RootContext rootContext = new RootContext();

		// reset import counter
		if (created) {
			PlatformConfiguration.getConfigurationService().remove(BugSearchActivator.PLUGIN_ID, "import.start", rootContext);
			try {
				new PlatformScope().getNode(BugSearchActivator.PLUGIN_ID).flush();
			} catch (final BackingStoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		BugzillaUpdateScheduler.scheduleInitialImportFollowedByUpdate(rootContext, 1, TimeUnit.HOURS);

		//		if (created) {
		//			BugzillaUpdateScheduler.scheduleInitialImportFollowedByUpdate(new RootContext(), 1, TimeUnit.HOURS);
		//		} else {
		//			BugzillaUpdateScheduler.scheduleUpdateJob(new RootContext(), 1, TimeUnit.HOURS);
		//		}
	}

}
