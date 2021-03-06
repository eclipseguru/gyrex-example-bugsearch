/**
 * Copyright (c) 2010 Gunnar Wagenknecht and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 */
package org.eclipse.gyrex.examples.bugsearch.internal.indexing;

import org.eclipse.gyrex.cds.documents.IDocumentManager;
import org.eclipse.gyrex.context.IRuntimeContext;
import org.eclipse.gyrex.context.preferences.IRuntimeContextPreferences;
import org.eclipse.gyrex.context.preferences.PreferencesUtil;
import org.eclipse.gyrex.examples.bugsearch.internal.BugSearchActivator;
import org.eclipse.gyrex.model.common.ModelUtil;
import org.eclipse.gyrex.persistence.solr.SolrServerRepository;
import org.eclipse.gyrex.server.Platform;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.mylyn.internal.bugzilla.core.BugzillaCorePlugin;
import org.eclipse.mylyn.internal.bugzilla.core.BugzillaRepositoryConnector;
import org.eclipse.mylyn.internal.tasks.core.RepositoryQuery;
import org.eclipse.mylyn.tasks.core.IRepositoryQuery;
import org.eclipse.mylyn.tasks.core.TaskRepository;

import org.osgi.framework.Bundle;
import org.osgi.service.prefs.BackingStoreException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for indexing jobs.
 */
@SuppressWarnings("restriction")
public abstract class BugSearchIndexJob extends Job {

	private static final Logger LOG = LoggerFactory.getLogger(BugSearchIndexJob.class);

	protected static String getJobStateAsString(final int state) {
		switch (state) {
			case Job.RUNNING:
				return "RUNNING";
			case Job.WAITING:
				return "WAITING";
			case Job.SLEEPING:
				return "SLEEPING";
			case Job.NONE:
				return "NONE";
			default:
				return "(unknown)";
		}
	}

	private final IRuntimeContext context;
	public static final Object FAMILY = new Object();
	public static String URL = "https://bugs.eclipse.org/bugs/";
	public static int PARALLEL_THREADS = 8;

	private volatile DocumentsPublisher publisher;

	/**
	 * Creates a new instance.
	 * 
	 * @param name
	 */
	public BugSearchIndexJob(final String name, final IRuntimeContext context) {
		super(name);
		this.context = context;
		setPriority(LONG);
		setRule(new MutexRule(context.getContextPath().toString().intern()));
	}

	@Override
	public boolean belongsTo(final Object family) {
		return FAMILY == family;
	}

	@Override
	protected void canceling() {
		final DocumentsPublisher documentsPublisher = publisher;
		if (null != documentsPublisher) {
			documentsPublisher.cancel();
		}
	}

	protected abstract void doIndex(final IProgressMonitor monitor, final TaskRepository repository, final BugzillaRepositoryConnector connector, final DocumentsPublisher publisher);

	/**
	 * Returns the context.
	 * 
	 * @return the context
	 */
	protected IRuntimeContext getContext() {
		return context;
	}

	protected void queryByUrl(final IProgressMonitor monitor, final TaskRepository repository, final BugzillaRepositoryConnector connector, final DocumentsPublisher publisher, final String url) {
		if (monitor.isCanceled()) {
			throw new OperationCanceledException();
		}

		LOG.debug("Query bugs by url: {}", url);

		final IRepositoryQuery query = new RepositoryQuery(repository.getConnectorKind(), "");
		query.setSummary("Query for changed tasks");
		query.setUrl(url);
		connector.performQuery(repository, query, publisher, null, monitor);
	}

	protected void queryForAllBugs(final IProgressMonitor monitor, final TaskRepository repository, final BugzillaRepositoryConnector connector, final DocumentsPublisher publisher) {
		// for demo purposes we'll fetch only a few bugs in dev mode
		if (Platform.inDevelopmentMode()) {
			LOG.debug("Operating in development mode, only fetching a small number of bugs");
			final String url = URL + "buglist.cgi?field0-0-0=bug_id&type0-0-0=lessthan&value0-0-0=100&field0-1-0=bug_id&type0-1-0=greaterthan&value0-1-0=0&order=Bug+Number";
			queryByUrl(monitor, repository, connector, publisher, url);
			return;
		}

		final IRuntimeContextPreferences preferences = PreferencesUtil.getPreferences(getContext());
		int start = preferences.getInt(BugSearchActivator.PLUGIN_ID, "import.start", 0);
		int oldBugsCount = 0;
		int processed = 0;
		final int fetchSize = 10000;
		do {
			queryForBugsRange(monitor, repository, connector, publisher, start, fetchSize);
			processed = publisher.getBugsCount() - oldBugsCount;
			start += processed;
			oldBugsCount = publisher.getBugsCount();
			try {
				publisher.commit();
			} catch (final Exception e) {
				LOG.error("Error while committing Solr index for context " + getContext(), e);
			}
		} while ((processed > 0));//&& (oldBugsCount < 100000));

		preferences.putInt(BugSearchActivator.PLUGIN_ID, "import.start", start - 1, false);
		try {
			preferences.flush(BugSearchActivator.PLUGIN_ID);
		} catch (final BackingStoreException e) {
			LOG.error("Error while flushing bug search preferences for context " + getContext(), e);
		}
	}

	protected void queryForBugsRange(final IProgressMonitor monitor, final TaskRepository repository, final BugzillaRepositoryConnector connector, final DocumentsPublisher publisher, final int start, final int numberOfBugs) {
		final String url = URL + "buglist.cgi?field0-0-0=bug_id&type0-0-0=lessthan&value0-0-0=" + (start + numberOfBugs + 1) + "&field0-1-0=bug_id&type0-1-0=greaterthan&value0-1-0=" + start + "&order=Bug+Number";
		queryByUrl(monitor, repository, connector, publisher, url);
	}

	protected void queryForChanges(final IProgressMonitor monitor, final TaskRepository repository, final BugzillaRepositoryConnector connector, final DocumentsPublisher publisher, final String start, final String end) {
		final String url = URL + "buglist.cgi?query_format=advanced&chfieldfrom=" + start + "&chfieldto=" + end + "&order=Last+Changed";
		queryByUrl(monitor, repository, connector, publisher, url);
	}

	@Override
	protected IStatus run(final IProgressMonitor progressMonitor) {
		final SubMonitor monitor = SubMonitor.convert(progressMonitor, "Indexing Bugzilla...", 100);
		try {
			LOG.info("Started Indexing Bugzilla: " + toString());

			final Bundle bundle = BugSearchActivator.getInstance().getBundle();
			if (null == bundle) {
				// abort, bundle is inactive
				return Status.CANCEL_STATUS;
			}

			final IDocumentManager documentManager = ModelUtil.getManager(IDocumentManager.class, getContext());
			final SolrServerRepository solrRepository = (SolrServerRepository) documentManager.getAdapter(SolrServerRepository.class);
			if (null == solrRepository) {
				return Status.CANCEL_STATUS;
			}

			// create task repository
			final TaskRepository repository = new TaskRepository(BugzillaCorePlugin.CONNECTOR_KIND, URL);

			// create bugzilla connector
			final BugzillaRepositoryConnector connector = new BugzillaRepositoryConnector();

			try {

				publisher = new DocumentsPublisher(repository, connector, documentManager, solrRepository);

				// fetch bugs and index
				doIndex(monitor.newChild(50), repository, connector, publisher);

				// finish publishing
				publisher.waitForOpenTasks(monitor.newChild(10));

				LOG.debug("Published " + publisher.getBugsCount() + " bugs.");

				// commit
				solrRepository.getSolrServer().commit(true, false);
				monitor.worked(10);
			} finally {
				//CommonsNetPlugin.getExecutorService().shutdown();
				publisher = null;
			}
			// publish the docs
			//listingManager.publish(docs.values());
		} catch (final IllegalStateException e) {
			// abort, bundle is inactive
			LOG.warn("Something is missing, cancelling job.", e);
			return Status.CANCEL_STATUS;
		} catch (final Exception e) {
			LOG.warn("Error during indexing. " + e.getMessage(), e);
			return Status.CANCEL_STATUS;
		} finally {
			monitor.done();
			LOG.info("Finished Indexing Bugzilla: " + toString());
		}

		return Status.OK_STATUS;
	}

	@Override
	public String toString() {
		return super.toString() + " " + getJobStateAsString(getState()) + " " + toStringDetail();
	}

	protected String toStringDetail() {
		return String.valueOf(publisher);
	}
}