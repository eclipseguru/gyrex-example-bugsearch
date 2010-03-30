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

import org.eclipse.gyrex.context.IRuntimeContext;
import org.eclipse.gyrex.context.preferences.IRuntimeContextPreferences;
import org.eclipse.gyrex.context.preferences.PreferencesUtil;
import org.eclipse.gyrex.context.registry.IRuntimeContextRegistry;
import org.eclipse.gyrex.examples.bugsearch.internal.indexing.BugSearchIndexJob;
import org.eclipse.gyrex.examples.bugsearch.internal.indexing.DocumentsPublisher;
import org.eclipse.gyrex.examples.bugsearch.internal.indexing.OptimizeIndexJob;

import org.eclipse.core.runtime.IProgressMonitor;
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

	private IRuntimeContextRegistry contextRegistry;

	public void _bsindex(final CommandInterpreter ci) {
		final IRuntimeContext eclipseBugSearchContext = contextRegistry.get(IEclipseBugSearchConstants.CONTEXT_PATH);
		if (null == eclipseBugSearchContext) {
			ci.println("Eclipse bug search context not found!");
			return;
		}

		// get bug numbers
		final int startId = NumberUtils.toInt(ci.nextArgument());
		if (startId == 0) {
			ci.println("Bug number must be greater 0!");
			return;
		}

		// schedule indexing
		new BugSearchIndexJob("bug indexing", eclipseBugSearchContext) {
			@Override
			protected void doIndex(final IProgressMonitor monitor, final TaskRepository repository, final BugzillaRepositoryConnector connector, final DocumentsPublisher publisher) {
				queryForBugsRange(monitor, repository, connector, publisher, startId, 1);
			}
		}.schedule(5000);
		ci.println("Scheduled indexing bug " + startId + ".");
	}

	public void _bsoptimize(final CommandInterpreter ci) {
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

	public void _bsreindex(final CommandInterpreter ci) {
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

		// schedule optimization
		new OptimizeIndexJob(eclipseBugSearchContext).schedule(5000);
		ci.println("Scheduled index optimization.");
	}

	protected void activate(final ComponentContext context) {
		contextRegistry = (IRuntimeContextRegistry) context.locateService("IRuntimeContextRegistry");

	}

	protected void deactivate(final ComponentContext context) {
		// release references
		contextRegistry = null;

	}

	@Override
	public String getHelp() {
		final StringBuilder buffer = new StringBuilder();
		buffer.append("---BugSearch---\n");
		buffer.append("\tbsindex <bug id> - indexes a specific bug\n");
		buffer.append("\tbsoptimize - kicks off indexing optimization\n");
		buffer.append("\tbsreindex  - kicks off re-indexing of the whole bugs index\n");
		return buffer.toString();
	}

}
