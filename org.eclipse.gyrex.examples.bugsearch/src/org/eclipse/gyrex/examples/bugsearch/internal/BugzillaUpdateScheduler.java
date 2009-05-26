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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.gyrex.context.IRuntimeContext;
import org.eclipse.gyrex.examples.bugsearch.internal.BugSearchDataImport.Mode;

/**
 *
 */
public class BugzillaUpdateScheduler {

	private static BugSearchDataImport initialImport;
	private static BugSearchDataImport update;

	public static synchronized void cancelUpdateJob() {
		if (null == update) {
			return;
		}

		update.cancel();
		update = null;
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
			/* (non-Javadoc)
			 * @see org.eclipse.gyrex.examples.bugsearch.internal.BugSearchDataImport#run(org.eclipse.core.runtime.IProgressMonitor)
			 */
			@Override
			protected IStatus run(final IProgressMonitor monitor) {
				final IStatus status = super.run(monitor);
				// re-schedule
				schedule(timeUnit.toMillis(interval));
				return status;
			}
		};
		update.schedule();
	}
}
