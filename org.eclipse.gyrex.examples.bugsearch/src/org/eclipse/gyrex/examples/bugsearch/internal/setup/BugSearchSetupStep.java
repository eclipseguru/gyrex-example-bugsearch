/*******************************************************************************
 * Copyright (c) 2009, 2010 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.examples.bugsearch.internal.setup;

import org.eclipse.gyrex.admin.configuration.wizard.ConfigurationWizardStep;
import org.eclipse.gyrex.context.IRuntimeContext;
import org.eclipse.gyrex.context.preferences.IRuntimeContextPreferences;
import org.eclipse.gyrex.context.preferences.PreferencesUtil;
import org.eclipse.gyrex.examples.bugsearch.internal.BugSearchActivator;
import org.eclipse.gyrex.examples.bugsearch.internal.IEclipseBugSearchConstants;
import org.eclipse.gyrex.toolkit.Toolkit;
import org.eclipse.gyrex.toolkit.content.BooleanContent;
import org.eclipse.gyrex.toolkit.content.TextContent;
import org.eclipse.gyrex.toolkit.runtime.commands.CommandExecutionEvent;
import org.eclipse.gyrex.toolkit.widgets.Checkbox;
import org.eclipse.gyrex.toolkit.widgets.DialogFieldGroup;
import org.eclipse.gyrex.toolkit.widgets.DialogFieldRules;
import org.eclipse.gyrex.toolkit.widgets.TextInput;
import org.eclipse.gyrex.toolkit.wizard.WizardContainer;
import org.eclipse.gyrex.toolkit.wizard.WizardPage;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BugSearchSetupStep extends ConfigurationWizardStep {

	private static final Logger LOG = LoggerFactory.getLogger(BugSearchSetupStep.class);

	public BugSearchSetupStep() {
		super("org.eclipse.gyrex.examples.bugsearch.setup");
	}

	@Override
	public void createPages(final WizardContainer wizard) {
		final WizardPage wizardPage = new WizardPage("bugsearch-setup", wizard);
		wizardPage.setLabel("The Eclipse Bug Search");
		wizardPage.setDescription("Deploy the Eclipse Bug Search example application.");

		final DialogFieldGroup enableFanShopGroup = new DialogFieldGroup("bugsearch-deployment", wizardPage, Toolkit.NONE);
		enableFanShopGroup.setLabel("Deployment");
		enableFanShopGroup.setDescription("Select whether the Bug Search application should be deployed.");

		final Checkbox deploy = new Checkbox("bugsearch-deploy", enableFanShopGroup, Toolkit.NONE);
		deploy.setLabel("Deploy the Eclipse Bug Search demo application.");

		final DialogFieldGroup container = new DialogFieldGroup("bugsearch-urls", wizardPage, Toolkit.NONE);
		container.setLabel("Configuration");
		container.setDescription("Configure the KEY_URL the Bug Search application should be deployed to. Please make sure the domain name actually resolves to the machine Gyrex is running on.");
		//container.setVisibilityRule(DialogFieldRules.field(deploy).isSet());

		final TextInput urlInput = new TextInput("bugsearch-url", container, Toolkit.NONE);
		urlInput.setLabel("KEY_URL:");
		urlInput.setDescription("Enter the KEY_URL the Bug Search application should be mounted on (eg. http://localhost/). ");
		urlInput.setEnablementRule(DialogFieldRules.field(deploy).isSet());

		wizardPage.setContinueRule(DialogFieldRules.allFields().areValid());

	}

	@Override
	public IStatus wizardFinished(final CommandExecutionEvent finishEvent, final IProgressMonitor monitor) {
		final BooleanContent deploy = (BooleanContent) finishEvent.getContentSet().getEntry("bugsearch-deploy");
		if ((null != deploy) && deploy.getValue()) {
			try {

				// get the context
				final IRuntimeContext eclipseBugSearchContext = BugSearchActivator.getInstance().getContextRegistry().getService().get(IEclipseBugSearchConstants.CONTEXT_PATH);
				final IRuntimeContextPreferences preferences = PreferencesUtil.getPreferences(eclipseBugSearchContext);

				// enable the server roles
				BugSearchDevSetup.enableServerRole();

				// set the url
				final TextContent url = (TextContent) finishEvent.getContentSet().getEntry("bugsearch-url");
				if (null != url) {
					final String text = url.getText();
					if ((null != text) && (text.trim().length() > 0)) {
						preferences.put(BugSearchActivator.PLUGIN_ID, IEclipseBugSearchConstants.KEY_URL, text, false);
						preferences.flush(BugSearchActivator.PLUGIN_ID);
					}
				}

			} catch (final Exception e) {
				LOG.error("Error while setting up Eclipse BugSearch: " + e, e);
				return new Status(IStatus.ERROR, BugSearchActivator.PLUGIN_ID, e.getMessage());
			}
		}

		return Status.OK_STATUS;
	}

}
