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

import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.gyrex.common.runtime.BaseBundleActivator;
import org.eclipse.gyrex.common.services.IServiceProxy;
import org.eclipse.gyrex.context.registry.IRuntimeContextRegistry;
import org.eclipse.gyrex.examples.bugsearch.internal.app.BugSearchApplicationProvider;
import org.eclipse.gyrex.http.application.provider.ApplicationProvider;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.util.tracker.ServiceTracker;

public class BugSearchActivator extends BaseBundleActivator {

	public static final String PLUGIN_ID = "org.eclipse.gyrex.examples.bugsearch";

	private static final AtomicReference<BugSearchActivator> instance = new AtomicReference<BugSearchActivator>();

	public static BugSearchActivator getInstance() {
		final BugSearchActivator fanShopActivator = instance.get();
		if (null == fanShopActivator) {
			throw new IllegalStateException("inactive");
		}
		return fanShopActivator;
	}

	private ServiceTracker bundleTracker;

	private IServiceProxy<IRuntimeContextRegistry> contextRegistry;

	/**
	 * Creates a new instance.
	 */
	public BugSearchActivator() {
		super(PLUGIN_ID);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.gyrex.common.runtime.BaseBundleActivator#doStart(org.osgi.framework.BundleContext)
	 */
	@Override
	protected void doStart(final BundleContext context) throws Exception {
		instance.set(this);

		// track the context registry
		contextRegistry = getServiceHelper().trackService(IRuntimeContextRegistry.class);

		// register application provider
		getServiceHelper().registerService(ApplicationProvider.class.getName(), new BugSearchApplicationProvider(), "Eclipse Gyrex", "Application provider for the extensible Fan Shop application.", null, null);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.gyrex.common.runtime.BaseBundleActivator#doStop(org.osgi.framework.BundleContext)
	 */
	@Override
	protected void doStop(final BundleContext context) throws Exception {
		BugzillaUpdateScheduler.cancelUpdateJob();
		instance.set(null);
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

	public IServiceProxy<IRuntimeContextRegistry> getContextRegistry() {
		final IServiceProxy<IRuntimeContextRegistry> proxy = contextRegistry;
		if (null == proxy) {
			throw createBundleInactiveException();
		}
		return proxy;
	}
}
