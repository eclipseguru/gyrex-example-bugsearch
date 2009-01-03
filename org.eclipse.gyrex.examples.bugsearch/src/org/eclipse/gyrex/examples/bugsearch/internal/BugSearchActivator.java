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

import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.cloudfree.common.runtime.BaseBundleActivator;
import org.eclipse.cloudfree.common.services.IServiceProxy;
import org.eclipse.cloudfree.configuration.PlatformConfiguration;
import org.eclipse.cloudfree.examples.bugsearch.internal.app.BugSearchApplicationProvider;
import org.eclipse.cloudfree.http.application.provider.ApplicationProvider;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.osgi.service.datalocation.Location;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.util.tracker.ServiceTracker;

public class BugSearchActivator extends BaseBundleActivator {

	public static final String PLUGIN_ID = "org.eclipse.cloudfree.examples.bugsearch";

	private static final AtomicReference<BugSearchActivator> instance = new AtomicReference<BugSearchActivator>();

	public static BugSearchActivator getInstance() {
		final BugSearchActivator fanShopActivator = instance.get();
		if (null == fanShopActivator) {
			throw new IllegalStateException("inactive");
		}
		return fanShopActivator;
	}

	private volatile BugSearchRTSetup fanShopRTSetup;

	private ServiceTracker bundleTracker;

	private final AtomicReference<IServiceProxy<Location>> instanceLocationRef = new AtomicReference<IServiceProxy<Location>>();

	/**
	 * Creates a new instance.
	 */
	public BugSearchActivator() {
		super(PLUGIN_ID);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cloudfree.common.runtime.BaseBundleActivator#doStart(org.osgi.framework.BundleContext)
	 */
	@Override
	protected void doStart(final BundleContext context) throws Exception {
		instance.set(this);

		// get instance location
		instanceLocationRef.set(getServiceHelper().trackService(Location.class, context.createFilter(Location.INSTANCE_FILTER)));

		// register application provider
		getServiceHelper().registerService(ApplicationProvider.class.getName(), new BugSearchApplicationProvider(), "CloudFree.net", "Application provider for the extensible Fan Shop application.", null, null);

		// create default environment in dev mode
		if (PlatformConfiguration.isOperatingInDevelopmentMode()) {
			new Job("Initializing Fan Shop") {

				@Override
				protected IStatus run(final IProgressMonitor monitor) {
					try {
						fanShopRTSetup = new BugSearchRTSetup(BugSearchActivator.getInstance().getBundle().getBundleContext());
						fanShopRTSetup.runtimeSetup();
					} catch (final IllegalStateException e) {
						// already shutdown
						return Status.CANCEL_STATUS;
					} catch (final Exception e) {
						// TODO consider logging this
						System.err.println("Error during fan shop initialization!");
						e.printStackTrace();
						return Status.CANCEL_STATUS;
					}
					return Status.OK_STATUS;
				}

			}.schedule();
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cloudfree.common.runtime.BaseBundleActivator#doStop(org.osgi.framework.BundleContext)
	 */
	@Override
	protected void doStop(final BundleContext context) throws Exception {
		BugzillaUpdateScheduler.cancelUpdateJob();
		instance.set(null);
		fanShopRTSetup.close();
	}

	public Bundle getBundle(final String symbolicName) {
		final PackageAdmin packageAdmin = getBundleAdmin();
		if (packageAdmin == null) {
			return null;
		}
		final Bundle[] bundles = packageAdmin.getBundles(symbolicName, null);
		if (bundles == null) {
			return null;
		}
		// return the first bundle that is not installed or uninstalled
		for (int i = 0; i < bundles.length; i++) {
			if ((bundles[i].getState() & (Bundle.INSTALLED | Bundle.UNINSTALLED)) == 0) {
				return bundles[i];
			}
		}
		return null;
	}

	private PackageAdmin getBundleAdmin() {
		if (bundleTracker == null) {
			final Bundle bundle = getBundle();
			if (bundle == null) {
				return null;
			}
			bundleTracker = new ServiceTracker(bundle.getBundleContext(), PackageAdmin.class.getName(), null);
			bundleTracker.open();
		}
		return (PackageAdmin) bundleTracker.getService();
	}

	public Location getInstanceLocation() {
		final IServiceProxy<Location> serviceProxy = instanceLocationRef.get();
		if (null == serviceProxy) {
			throw createBundleInactiveException();
		}

		return serviceProxy.getService();
	}
}
