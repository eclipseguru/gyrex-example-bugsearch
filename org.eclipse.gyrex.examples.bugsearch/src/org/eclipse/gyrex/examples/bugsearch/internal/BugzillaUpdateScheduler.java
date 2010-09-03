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

import java.util.concurrent.TimeUnit;

import org.eclipse.gyrex.context.IRuntimeContext;
import org.eclipse.gyrex.examples.bugsearch.internal.indexing.BugSearchDataImport;
import org.eclipse.gyrex.examples.bugsearch.internal.indexing.BugSearchDataImport.Mode;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class BugzillaUpdateScheduler {

	private static final Logger LOG = LoggerFactory.getLogger(BugzillaUpdateScheduler.class);

	private static BugSearchDataImport initialImport;
	private static BugSearchDataImport update;

	public static synchronized void cancelImportJob() {
		if (null == initialImport) {
			return;
		}

		initialImport.cancel();
	}

	public static synchronized void cancelUpdateJob() {
		if (null == update) {
			return;
		}

		update.cancel();
		update = null;
	}

	public static synchronized void rescheduleInitialImportFollowedByUpdate() {
		if (null == initialImport) {
			return;
		}

		// cancel
		cancelUpdateJob();
		initialImport.cancel();

		// re-schedule
		// wait a few seconds before starting
		initialImport.schedule(5000);
	}

	public static synchronized void scheduleInitialImportFollowedByUpdate(final IRuntimeContext context, final long interval, final TimeUnit timeUnit) {
		if (null != initialImport) {
			return;
		}

		initialImport = new BugSearchDataImport(context, Mode.INITIAL, interval, timeUnit) {
			/* (non-Javadoc)
			 * @see org.eclipse.gyrex.examples.bugsearch.internal.BugSearchDataImport#run(org.eclipse.core.runtime.IProgressMonitor)
			 */
			@Override
			protected IStatus run(final IProgressMonitor monitor) {
				final IStatus status = super.run(monitor);
				scheduleUpdateJob(getContext(), interval, timeUnit);
				return status;
			}
		};

		// wait a few seconds before starting
		initialImport.schedule(5000);
	}

	public static synchronized void scheduleUpdateJob(final IRuntimeContext context, final long interval, final TimeUnit timeUnit) {
		if (null != update) {
			return;
		}
		update = new BugSearchDataImport(context, Mode.UPDATE, interval, timeUnit) {
			@Override
			protected IStatus run(final IProgressMonitor monitor) {
				final IStatus status = super.run(monitor);
				// re-schedule
				schedule(timeUnit.toMillis(interval));
				return status;
			}
		};
		update.schedule(10000);
		LOG.info("Scheduled auto-update every " + interval + " " + timeUnit.toString());
	}
}
