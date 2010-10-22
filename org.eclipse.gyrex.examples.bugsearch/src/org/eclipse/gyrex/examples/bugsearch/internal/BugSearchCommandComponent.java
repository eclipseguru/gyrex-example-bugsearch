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

import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.concurrent.TimeUnit;

import org.eclipse.gyrex.cds.documents.IDocumentManager;
import org.eclipse.gyrex.context.IRuntimeContext;
import org.eclipse.gyrex.context.preferences.IRuntimeContextPreferences;
import org.eclipse.gyrex.context.preferences.PreferencesUtil;
import org.eclipse.gyrex.context.registry.IRuntimeContextRegistry;
import org.eclipse.gyrex.examples.bugsearch.internal.indexing.BugSearchDataImport;
import org.eclipse.gyrex.examples.bugsearch.internal.indexing.BugSearchIndexJob;
import org.eclipse.gyrex.examples.bugsearch.internal.indexing.DocumentsPublisher;
import org.eclipse.gyrex.examples.bugsearch.internal.indexing.OptimizeIndexJob;
import org.eclipse.gyrex.model.common.ModelUtil;
import org.eclipse.gyrex.monitoring.metrics.ErrorMetric.ErrorStats;
import org.eclipse.gyrex.persistence.solr.SolrServerRepository;
import org.eclipse.gyrex.persistence.solr.internal.SolrRepository;
import org.eclipse.gyrex.persistence.solr.internal.SolrRepositoryMetrics;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.mylyn.internal.bugzilla.core.BugzillaRepositoryConnector;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.eclipse.osgi.framework.console.CommandProvider;

import org.osgi.service.component.ComponentContext;

import org.apache.commons.lang.math.NumberUtils;

/**
 * Console commands
 */
@SuppressWarnings("restriction")
public class BugSearchCommandComponent implements CommandProvider {

	static final DateFormat ISO_8601_UTC = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");

	private IRuntimeContextRegistry contextRegistry;
	private IJobManager jobManager;
	private final NumberFormat NUMBER_FORMAT = NumberFormat.getNumberInstance();
	private final DateFormat DATE_FORMAT = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG);

	private IRuntimeContext eclipseBugSearchContext;

	public void _bsCancel(final CommandInterpreter ci) {
		jobManager.cancel(BugSearchIndexJob.FAMILY);
		ci.println("Canceld BugSearch jobs.");
	}

	public void _bsConcurrency(final CommandInterpreter ci) {
		// get (optional) fetch length
		final int concurrencyLevel = NumberUtils.toInt(ci.nextArgument(), 8);
		BugSearchIndexJob.PARALLEL_THREADS = concurrencyLevel;
		ci.println("Concurrency set to " + concurrencyLevel + " threads for next scheduled indexing job.");

	}

	public void _bsIndex(final CommandInterpreter ci) {
		if (null == eclipseBugSearchContext) {
			ci.println("Eclipse bug search context not found!");
			return;
		}

		// get bug number
		final int startId = NumberUtils.toInt(ci.nextArgument());
		if (startId <= 0) {
			ci.println("Bug number must be greater 0!");
			return;
		}

		// get (optional) fetch length
		final int length = NumberUtils.toInt(ci.nextArgument(), 1);
		if (length <= 0) {
			ci.println("Length must be greater 0!");
			return;
		}
		// schedule indexing
		new BugSearchIndexJob("indexing bugs " + startId + " to " + (startId + length - 1), eclipseBugSearchContext) {
			@Override
			protected void doIndex(final IProgressMonitor monitor, final TaskRepository repository, final BugzillaRepositoryConnector connector, final DocumentsPublisher publisher) {
				queryForBugsRange(monitor, repository, connector, publisher, startId, length);
			}
		}.schedule(5000);
		ci.println("Scheduled indexing bug " + startId + ".");
	}

	public void _bsIndexChanges(final CommandInterpreter ci) {
		final IRuntimeContext eclipseBugSearchContext = contextRegistry.get(IEclipseBugSearchConstants.CONTEXT_PATH);
		if (null == eclipseBugSearchContext) {
			ci.println("Eclipse bug search context not found!");
			return;
		}

		// get bug number
		final int hours = NumberUtils.toInt(ci.nextArgument());
		if (hours <= 0) {
			ci.println("Hours number must be greater 0!");
			return;
		}

		// schedule indexing
		new BugSearchIndexJob("indexing changes during last " + hours + " hours", eclipseBugSearchContext) {
			@Override
			protected void doIndex(final IProgressMonitor monitor, final TaskRepository repository, final BugzillaRepositoryConnector connector, final DocumentsPublisher publisher) {
				queryForChanges(monitor, repository, connector, publisher, hours + "h", BugSearchDataImport.NOW);
			}
		}.schedule(5000);
		ci.println("Scheduled indexing changes during last " + hours + " hours.");
	}

	public void _bsMetrics(final CommandInterpreter ci) {
		final IRuntimeContext eclipseBugSearchContext = contextRegistry.get(IEclipseBugSearchConstants.CONTEXT_PATH);
		if (null == eclipseBugSearchContext) {
			ci.println("Eclipse bug search context not found!");
			return;
		}

		final IDocumentManager documentManager = ModelUtil.getManager(IDocumentManager.class, eclipseBugSearchContext);
		final SolrServerRepository solrRepository = (SolrServerRepository) documentManager.getAdapter(SolrServerRepository.class);
		if (null == solrRepository) {
			ci.println("Bug search Solr repository not found!");
			return;
		}

		final SolrRepositoryMetrics metrics = ((SolrRepository) solrRepository).getSolrRepositoryMetrics();

		final String nextArgument = ci.nextArgument();
		if ("reset".equals(nextArgument)) {

			metrics.resetStats();

			ci.println("Bug Search Solr repository metrics have been resetted.");
			return;
		}

		String statsSince;
		try {
			statsSince = DATE_FORMAT.format(ISO_8601_UTC.parse(metrics.getStatsSince()));
		} catch (final ParseException e) {
			statsSince = metrics.getStatsSince();
		}

		ci.println();
		ci.println();
		ci.println("Bug Search Solr Repository Metrics");
		ci.println("----------------------------------");
		ci.println();
		ci.println(" Stats since: " + statsSince);
		ci.println("      Status: " + metrics.getStatusMetric().getStatus() + " (" + metrics.getStatusMetric().getStatusChangeReason() + ")");
		ci.println();
		ci.println(" There were " + metrics.getErrorMetric().getTotalNumberOfErrors() + " errors.");
		if (metrics.getErrorMetric().getTotalNumberOfErrors() > 0) {
			ci.println("  Last error: " + metrics.getErrorMetric().getLastError() + " at " + metrics.getErrorMetric().getLastErrorChangeTime() + " (" + metrics.getErrorMetric().getLastErrorDetails() + ")");
			ci.println();
			for (final ErrorStats errorStats : metrics.getErrorMetric().getErrorStats()) {
				ci.println(errorStats);
			}
		}
		ci.println();
		ci.println(" Processed " + metrics.getQueryThroughputMetric().getRequestsStatsProcessed() + " queries with an average of " + metrics.getQueryThroughputMetric().getRequestsStatsProcessingTimeAverage() + "ms per query.");
		ci.println(" Rate of failed queries is at " + NUMBER_FORMAT.format(metrics.getQueryThroughputMetric().getRequestsStatsFailureRate()) + "%.");
		ci.println(" There were at most " + metrics.getQueryThroughputMetric().getRequestsStatsHigh() + " parallel queries running.");
		ci.println(" We had an average of " + metrics.getQueryThroughputMetric().getRequestsStatsHitRatePerHour() + " queries per hour.");
		ci.println();
		ci.println();
	}

	public void _bsOptimize(final CommandInterpreter ci) {
		final IRuntimeContext eclipseBugSearchContext = contextRegistry.get(IEclipseBugSearchConstants.CONTEXT_PATH);
		if (null == eclipseBugSearchContext) {
			ci.println("Eclipse bug search context not found!");
			return;
		}

		// schedule optimization
		new OptimizeIndexJob(eclipseBugSearchContext).schedule(5000);
		ci.println("Scheduled index optimization.");
	}

	public void _bsReindex(final CommandInterpreter ci) {
		final IRuntimeContext eclipseBugSearchContext = contextRegistry.get(IEclipseBugSearchConstants.CONTEXT_PATH);
		if (null == eclipseBugSearchContext) {
			ci.println("Eclipse bug search context not found!");
			return;
		}

		// reset the index counter if we created a new index
		final IRuntimeContextPreferences preferences = PreferencesUtil.getPreferences(eclipseBugSearchContext);
		preferences.remove(BugSearchActivator.PLUGIN_ID, "import.start");
		try {
			preferences.flush(BugSearchActivator.PLUGIN_ID);
		} catch (final Exception e) {
			ci.println("Error while flushing preferences after resetting the index counter: " + e);
			// but continue
		}

		// re-schedule initial indexing
		BugzillaUpdateScheduler.rescheduleInitialImportFollowedByUpdate();
		ci.println("Rescheduled indexing.");
	}

	public void _bsScheduleAutoUpdate(final CommandInterpreter ci) {
		BugzillaUpdateScheduler.scheduleUpdateJob(eclipseBugSearchContext, 20, TimeUnit.MINUTES);
		ci.println("Schedule auto-update to check for update every 20 minutes.");
	}

	public void _bsStatus(final CommandInterpreter ci) {
		final Job[] jobs = jobManager.find(BugSearchIndexJob.FAMILY);
		if ((null == jobs) || (jobs.length == 0)) {
			ci.println("No jobs scheduled.");
			return;
		}

		for (final Job job : jobs) {
			ci.println(job);
		}
	}

	protected void activate(final ComponentContext context) {
		contextRegistry = (IRuntimeContextRegistry) context.locateService("IRuntimeContextRegistry");
		jobManager = (IJobManager) context.locateService("IJobManager");
		eclipseBugSearchContext = contextRegistry.get(IEclipseBugSearchConstants.CONTEXT_PATH);
	}

	protected void deactivate(final ComponentContext context) {
		// release references
		contextRegistry = null;
		jobManager = null;
	}

	@Override
	public String getHelp() {
		final StringBuilder buffer = new StringBuilder();
		buffer.append("---BugSearch---\n");
		buffer.append("\tbsIndex <bug id> - indexes a specific bug\n");
		buffer.append("\tbsReindex  - kicks off re-indexing of the whole bugs index\n");
		buffer.append("\tbsScheduleAutoUpdate - reschedules auto-indexing\n");
		buffer.append("\tbsOptimize - kicks off indexing optimization\n");
		buffer.append("\tbsCommit - commit Solr index\n");
		buffer.append("\tbsCancel - cancel running jobst\n");
		buffer.append("\tbsConcurrency - set indexing concurrency level\n");
		buffer.append("\tbsStatus - show job status\n");
		buffer.append("\tbsIndexChanges <hours> - indexes changes from the last X hours\n");
		buffer.append("\tbsMetrics - print some metrics\n");
		return buffer.toString();
	}

}
