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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.gyrex.cds.model.IListingManager;
import org.eclipse.gyrex.cds.model.documents.Document;
import org.eclipse.gyrex.persistence.solr.internal.SolrRepository;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.mylyn.internal.bugzilla.core.BugzillaAttribute;
import org.eclipse.mylyn.internal.bugzilla.core.BugzillaRepositoryConnector;
import org.eclipse.mylyn.tasks.core.ITaskMapping;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.core.data.TaskAttribute;
import org.eclipse.mylyn.tasks.core.data.TaskCommentMapper;
import org.eclipse.mylyn.tasks.core.data.TaskData;
import org.eclipse.mylyn.tasks.core.data.TaskDataCollector;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("restriction")
public final class DocumentsPublisher extends TaskDataCollector {

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

	/** PROPERTY */
	private static final String LINE_SEPARATOR = System.getProperty("line.separator");

	private static final Logger LOG = LoggerFactory.getLogger(DocumentsPublisher.class);

	/** connector */
	private final BugzillaRepositoryConnector connector;
	private final SolrRepository repository;

	/** bugsCount */
	private final AtomicInteger bugsCount;
	private final AtomicInteger openTasks;
	private final TaskRepository taskRepository;
	private final ThreadPoolExecutor executorService;
	private final AtomicBoolean isCanceled;
	private final ConcurrentMap<String, String> activeTasks;

	/**
	 * Creates a new instance.
	 * 
	 * @param connector
	 * @param documents
	 * @param bugsCount
	 * @param batchSize
	 * @param listingManager
	 */
	DocumentsPublisher(final TaskRepository taskRepository, final BugzillaRepositoryConnector connector, final IListingManager listingManager, final SolrRepository repository) {
		this.taskRepository = taskRepository;
		this.connector = connector;
		this.repository = repository;
		bugsCount = new AtomicInteger();
		executorService = (ThreadPoolExecutor) Executors.newFixedThreadPool(BugSearchIndexJob.PARALLEL_THREADS);
		activeTasks = new ConcurrentHashMap<String, String>(BugSearchIndexJob.PARALLEL_THREADS);
		openTasks = new AtomicInteger();
		isCanceled = new AtomicBoolean();
	}

	@Override
	public void accept(final TaskData partialTaskData) {
		if (isCanceled()) {
			return;
		}
		final String taskId = partialTaskData.getTaskId();
		openTasks.incrementAndGet();
		executorService.execute(new PublishTaskRunnable(taskId));
		bugsCount.incrementAndGet();
	}

	public void cancel() {
		final boolean wasCanceled = isCanceled.getAndSet(true);
		if (!wasCanceled) {
			executorService.shutdownNow();
			LOG.info("Cancelledindexing at " + toString());
		}
	}

	public void commit() {
		if (isCanceled()) {
			return;
		}
		repository.commit(false, false);
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

	public boolean isCanceled() {
		return isCanceled.get();
	}

	void publishTask(final String taskId) {
		try {
			if (isCanceled()) {
				return;
			}

			LOG.debug("Processing bug {}", taskId);

			activeTasks.put(taskId, "Fetching task data...");
			final TaskData taskData = connector.getTaskData(taskRepository, taskId, new NullProgressMonitor());

			// access task information
			activeTasks.put(taskId, "Access task information...");
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

			if (isCanceled()) {
				return;
			}
			activeTasks.put(taskId, "Adding to index...");
			repository.add(document);

		} catch (final Exception e) {
			LOG.warn("error while fetching bug data: " + e.getMessage());

			// reschedule
			if (!isCanceled()) {
				openTasks.incrementAndGet();
				executorService.execute(new PublishTaskRunnable(taskId));
				LOG.info("rescheduled bug " + taskId + " for indexing because of previous error");
			}
		} finally {
			openTasks.decrementAndGet();
			activeTasks.remove(taskId);
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

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("[bugsCount=").append(bugsCount).append(", openTasks=").append(openTasks).append(", completedTasks=").append(executorService.getCompletedTaskCount()).append(", activeThreads=").append(executorService.getActiveCount()).append("]");
		final Object[] entries = activeTasks.entrySet().toArray();
		if (entries.length > 0) {
			builder.append(LINE_SEPARATOR);
			for (final Object entryObject : entries) {
				final Entry entry = (Entry) entryObject;
				builder.append("\t").append(entry.getKey()).append(" - ").append(entry.getValue()).append(LINE_SEPARATOR);
			}
		}
		return builder.toString();
	}

	public void waitForOpenTasks(final IProgressMonitor monitor) {
		// ordered shutdown
		executorService.shutdown();
		// wait for finish
		while ((openTasks.get() > 0) && !monitor.isCanceled() && !isCanceled()) {
			try {
				Thread.sleep(1000);
			} catch (final InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
		// done
		final int remaining = openTasks.get();
		if (remaining > 0) {
			LOG.info("Aborted waiting, " + remaining + " open task still remaining");
		}
	}
}