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
package org.eclipse.gyrex.examples.bugsearch.internal;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.gyrex.cds.model.IListingManager;
import org.eclipse.gyrex.cds.model.documents.Document;
import org.eclipse.gyrex.cds.model.solr.internal.SolrListingsManager;
import org.eclipse.gyrex.configuration.PlatformConfiguration;
import org.eclipse.gyrex.context.IRuntimeContext;
import org.eclipse.gyrex.context.preferences.IRuntimeContextPreferences;
import org.eclipse.gyrex.context.preferences.PreferencesUtil;
import org.eclipse.gyrex.model.common.ModelUtil;
import org.eclipse.gyrex.persistence.solr.internal.SolrRepository;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.mylyn.internal.bugzilla.core.BugzillaAttribute;
import org.eclipse.mylyn.internal.bugzilla.core.BugzillaCorePlugin;
import org.eclipse.mylyn.internal.bugzilla.core.BugzillaRepositoryConnector;
import org.eclipse.mylyn.internal.tasks.core.RepositoryQuery;
import org.eclipse.mylyn.tasks.core.IRepositoryQuery;
import org.eclipse.mylyn.tasks.core.ITaskMapping;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.core.data.TaskAttribute;
import org.eclipse.mylyn.tasks.core.data.TaskCommentMapper;
import org.eclipse.mylyn.tasks.core.data.TaskData;
import org.eclipse.mylyn.tasks.core.data.TaskDataCollector;

import org.osgi.framework.Bundle;
import org.osgi.service.prefs.BackingStoreException;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("restriction")
public class BugSearchDataImport extends Job {

	private static final class DocumentsPublisher extends TaskDataCollector {

		/**
		 *
		 */
		private final class PublishTaskRunnable implements Runnable {
			/** taskId */
			private final String taskId;

			/**
			 * Creates a new instance.
			 * 
			 * @param taskId
			 */
			private PublishTaskRunnable(final String taskId) {
				this.taskId = taskId;
			}

			@Override
			public void run() {
				publishTask(taskId);
			}
		}

		private static final Logger LOG = LoggerFactory.getLogger(BugSearchDataImport.DocumentsPublisher.class);

		/** connector */
		private final BugzillaRepositoryConnector connector;
		private final SolrRepository repository;
		/** bugsCount */
		private final AtomicInteger bugsCount;
		private final TaskRepository taskRepository;
		private final ExecutorService executorService;
		private final IProgressMonitor cancelMonitor;
		private final AtomicInteger openTasks;

		/**
		 * Creates a new instance.
		 * 
		 * @param connector
		 * @param documents
		 * @param bugsCount
		 * @param batchSize
		 * @param listingManager
		 */
		private DocumentsPublisher(final TaskRepository taskRepository, final BugzillaRepositoryConnector connector, final IListingManager listingManager, final SolrRepository repository, final IProgressMonitor cancelMonitor) {
			this.taskRepository = taskRepository;
			this.connector = connector;
			this.repository = repository;
			this.cancelMonitor = cancelMonitor;
			bugsCount = new AtomicInteger();
			executorService = Executors.newFixedThreadPool(5);
			openTasks = new AtomicInteger();
		}

		@Override
		public void accept(final TaskData partialTaskData) {
			if (cancelMonitor.isCanceled()) {
				throw new OperationCanceledException();
			}
			final String taskId = partialTaskData.getTaskId();
			openTasks.incrementAndGet();
			executorService.execute(new PublishTaskRunnable(taskId));
			bugsCount.incrementAndGet();
		}

		private Collection<String> extractKeywords(final ITaskMapping taskMapping) {
			final Set<String> keywords = new LinkedHashSet<String>();
			for (final String keyword : taskMapping.getKeywords()) {
				final String[] splittedKeywords = StringUtils.split(keyword, ", ");
				if (null != splittedKeywords) {
					for (final String singleKeyword : splittedKeywords) {
						keywords.add(singleKeyword);
					}
				}
			}
			return keywords;
		}

		private Collection<String> extractSummaryTags(final String summary) {
			final Set<String> tags = new LinkedHashSet<String>();
			final String[] strings = StringUtils.substringsBetween(summary, "[", "]");
			if (null != strings) {
				for (final String tag : strings) {
					tags.add(StringUtils.trim(tag));
				}
			}
			return tags;
		}

		/**
		 * Returns the bugsCount.
		 * 
		 * @return the bugsCount
		 */
		public int getBugsCount() {
			return bugsCount.get();
		}

		void publishTask(final String taskId) {
			try {
				if (cancelMonitor.isCanceled()) {
					throw new OperationCanceledException();
				}

				LOG.debug("Processing bug {}", taskId);

				final TaskData taskData = connector.getTaskData(taskRepository, taskId, new NullProgressMonitor());

				// access task information
				final ITaskMapping taskMapping = connector.getTaskMapping(taskData);

				final SolrInputDocument document = new SolrInputDocument();

				setField(document, Document.ID, taskData.getTaskId());
				setField(document, Document.NAME, taskData.getTaskId());
				setField(document, Document.URI_PATH, taskData.getTaskId());
				setField(document, Document.TITLE, taskMapping.getSummary());
				setField(document, Document.DESCRIPTION, taskMapping.getDescription());

				// comments
				final List<TaskAttribute> taskComments = taskData.getAttributeMapper().getAttributesByType(taskData, TaskAttribute.TYPE_COMMENT);
				if (taskComments != null) {
					final List<String> commenters = new ArrayList<String>(taskComments.size());
					final List<String> comments = new ArrayList<String>(taskComments.size());
					for (final TaskAttribute commentAttribute : taskComments) {
						final TaskCommentMapper taskComment = TaskCommentMapper.createFrom(commentAttribute);
						commenters.add(taskComment.getAuthor().getName());
						comments.add(taskComment.getText());
					}
					setField(document, "commenter", commenters);
					setField(document, "comment", comments);
					setField(document, "commentsCount", taskComments.size());
				}

				setField(document, "created", taskMapping.getCreationDate());
				setField(document, "product", taskMapping.getProduct());
				setField(document, "component", taskMapping.getComponent());
				setField(document, "priority", taskMapping.getPriority());
				setField(document, "severity", taskData, BugzillaAttribute.BUG_SEVERITY);
				setField(document, "status", taskMapping.getStatus());
				setField(document, "resolution", taskMapping.getResolution());
				setField(document, "reporter", taskData, BugzillaAttribute.REPORTER_NAME);
				setField(document, "assignee", taskData, BugzillaAttribute.ASSIGNED_TO_NAME);
				setField(document, "classification", taskData, BugzillaAttribute.CLASSIFICATION);
				setField(document, "keywords", extractKeywords(taskMapping));

				setField(document, "hardware", taskData, BugzillaAttribute.REP_PLATFORM);
				setField(document, "os", taskData, BugzillaAttribute.OP_SYS);

				setField(document, "targetMilestone", taskData, BugzillaAttribute.TARGET_MILESTONE);
				setField(document, "version", taskData, BugzillaAttribute.VERSION);
				setField(document, "statusWhiteboard", taskData, BugzillaAttribute.STATUS_WHITEBOARD);

				final List<String> cc = taskMapping.getCc();
				setField(document, "ccCount", null != cc ? cc.size() : 0);
				setField(document, "cc", cc);

				setField(document, "votes", taskData, BugzillaAttribute.VOTES);

				// collect tags
				final Set<String> tags = new HashSet<String>();
				tags.add(taskMapping.getProduct());
				tags.addAll(extractKeywords(taskMapping));
				tags.addAll(extractSummaryTags(taskMapping.getSummary()));
				setField(document, "tags", tags);

				repository.add(document);

			} catch (final CoreException e) {
				LOG.error("error while fetching bug data", e);
			} finally {
				openTasks.decrementAndGet();
			}
		}

		private void setField(final SolrInputDocument document, final String name, final Object value) {
			if (value instanceof Collection) {
				document.setField(name, ((Collection) value).toArray());
			} else {
				document.setField(name, value);
			}
		}

		private void setField(final SolrInputDocument document, final String name, final TaskData taskData, final BugzillaAttribute bugzillaAttribute) {
			final TaskAttribute attribute = taskData.getRoot().getAttribute(bugzillaAttribute.getKey());
			if (null != attribute) {
				final List<String> values = attribute.getValues();
				if (null != values) {
					document.addField(name, values);
				} else {
					setField(document, name, attribute.getValue());
				}
			}
		}

		public void shutdown() {
			executorService.shutdown();
			while (openTasks.get() > 0) {
				try {
					Thread.sleep(1000);
				} catch (final InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
		}
	}

	static enum Mode {
		INITIAL, UPDATE
	}

	private static final Logger LOG = LoggerFactory.getLogger(BugSearchDataImport.class);

	public static final String NOW = "Now";

	private static final String URL = "https://bugs.eclipse.org/bugs/";

	private final IRuntimeContext context;

	private final Mode mode;

	private final long interval;

	private final TimeUnit unit;

	/**
	 * Creates a new instance.
	 * 
	 * @param context
	 * @param name
	 */
	BugSearchDataImport(final IRuntimeContext context, final Mode mode, final long interval, final TimeUnit unit) {
		super("fan shop data import");
		this.context = context;
		this.mode = mode;
		this.interval = interval;
		this.unit = unit;
		setPriority(LONG);
		setRule(new MutexRule(context));
	}

	/**
	 * Returns the context.
	 * 
	 * @return the context
	 */
	public IRuntimeContext getContext() {
		return context;
	}

	private void queryByUrl(final IProgressMonitor monitor, final TaskRepository repository, final BugzillaRepositoryConnector connector, final DocumentsPublisher publisher, final String url) {
		if (monitor.isCanceled()) {
			throw new OperationCanceledException();
		}

		LOG.debug("Query bugs by url: {}", url);

		final IRepositoryQuery query = new RepositoryQuery(repository.getConnectorKind(), "");
		query.setSummary("Query for changed tasks");
		query.setUrl(url);
		connector.performQuery(repository, query, publisher, null, monitor);
	}

	private void queryForAllBugs(final IProgressMonitor monitor, final TaskRepository repository, final BugzillaRepositoryConnector connector, final DocumentsPublisher publisher) {
		// for demo purposes we'll fetch only a few bugs in dev mode
		if (PlatformConfiguration.isOperatingInDevelopmentMode()) {
			LOG.debug("Operating in development mode, only fetching a small number of bugs");
			final String url = "https://bugs.eclipse.org/bugs/buglist.cgi?field0-0-0=bug_id&type0-0-0=lessthan&value0-0-0=100&field0-1-0=bug_id&type0-1-0=greaterthan&value0-1-0=0&order=Bug+Number";
			queryByUrl(monitor, repository, connector, publisher, url);
			return;
		}

		final IRuntimeContextPreferences preferences = PreferencesUtil.getPreferences(getContext());
		int start = preferences.getInt(BugSearchActivator.PLUGIN_ID, "import.start", 0);
		int oldBugsCount = 0;
		int processed = 0;
		final int fetchSize = 10000;
		do {
			final String url = "https://bugs.eclipse.org/bugs/buglist.cgi?field0-0-0=bug_id&type0-0-0=lessthan&value0-0-0=" + (start + fetchSize + 1) + "&field0-1-0=bug_id&type0-1-0=greaterthan&value0-1-0=" + start + "&order=Bug+Number";
			queryByUrl(monitor, repository, connector, publisher, url);
			processed = publisher.getBugsCount() - oldBugsCount;
			start += processed;
			oldBugsCount = publisher.getBugsCount();
		} while ((processed > 0));//&& (oldBugsCount < 100000));

		preferences.putInt(BugSearchActivator.PLUGIN_ID, "import.start", start - 1, false);
		try {
			preferences.flush(BugSearchActivator.PLUGIN_ID);
		} catch (final BackingStoreException e) {
			LOG.error("Error while flushing bug search preferences for context " + getContext(), e);
		}
	}

	private void queryForChanges(final IProgressMonitor monitor, final TaskRepository repository, final BugzillaRepositoryConnector connector, final DocumentsPublisher publisher, final String start, final String end) {
		final String url = "https://bugs.eclipse.org/bugs/buglist.cgi?query_format=advanced&chfieldfrom=" + start + "&chfieldto=" + end + "&order=Last+Changed";
		queryByUrl(monitor, repository, connector, publisher, url);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.jobs.Job#run(org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	protected IStatus run(final IProgressMonitor monitor) {
		try {

			final Bundle bundle = BugSearchActivator.getInstance().getBundle();
			if (null == bundle) {
				// abort, bundle is inactive
				return Status.CANCEL_STATUS;
			}

			final IListingManager listingManager = ModelUtil.getManager(IListingManager.class, getContext());
			final SolrRepository solrRepository = (SolrRepository) ((SolrListingsManager) listingManager).getAdapter(SolrRepository.class);
			if (null == solrRepository) {
				return Status.CANCEL_STATUS;
			}

			// create task repository
			final TaskRepository repository = new TaskRepository(BugzillaCorePlugin.CONNECTOR_KIND, URL);

			// create bugzilla connector
			final BugzillaRepositoryConnector connector = new BugzillaRepositoryConnector();

			try {

				// process docs in batches
				final DocumentsPublisher publisher = new DocumentsPublisher(repository, connector, listingManager, solrRepository, monitor);

				// get a report from repository
				switch (mode) {
					case INITIAL:
						LOG.debug("initial indexing");
						queryForAllBugs(monitor, repository, connector, publisher);
						break;

					case UPDATE:
						LOG.debug("updating index");
						queryForChanges(monitor, repository, connector, publisher, (1 + unit.toHours(interval)) + "h", NOW);
						break;
				}

				// finish publishing
				publisher.shutdown();

				LOG.debug("Published " + publisher.getBugsCount() + " bugs.");

				// commit
				solrRepository.commit(true, false);

				// optimize after initial import or at night
				if ((mode == Mode.INITIAL) || (Calendar.getInstance().get(Calendar.HOUR_OF_DAY) == 1)) {
					solrRepository.optimize(false, false);
				}
			} finally {
				//CommonsNetPlugin.getExecutorService().shutdown();
			}
			// publish the docs
			//listingManager.publish(docs.values());
		} catch (final IllegalStateException e) {
			// abort, bundle is inactive
			LOG.warn("Something is missing, cancelling job.", e);
			return Status.CANCEL_STATUS;
		} catch (final Exception e) {
			e.printStackTrace();
			return BugSearchActivator.getInstance().getStatusUtil().createError(0, e.getMessage(), e);
		}

		return Status.OK_STATUS;
	}
}
