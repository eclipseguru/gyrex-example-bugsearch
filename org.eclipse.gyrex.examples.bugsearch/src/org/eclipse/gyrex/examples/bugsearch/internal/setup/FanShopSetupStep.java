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
package org.eclipse.cloudfree.examples.bugsearch.internal.setup;

import org.eclipse.cloudfree.admin.configuration.wizard.ConfigurationWizardStep;
import org.eclipse.cloudfree.toolkit.CWT;
import org.eclipse.cloudfree.toolkit.content.BooleanContent;
import org.eclipse.cloudfree.toolkit.content.TextContent;
import org.eclipse.cloudfree.toolkit.runtime.commands.CommandExecutionEvent;
import org.eclipse.cloudfree.toolkit.widgets.Checkbox;
import org.eclipse.cloudfree.toolkit.widgets.DialogFieldGroup;
import org.eclipse.cloudfree.toolkit.widgets.DialogFieldRules;
import org.eclipse.cloudfree.toolkit.widgets.TextInput;
import org.eclipse.cloudfree.toolkit.wizard.WizardContainer;
import org.eclipse.cloudfree.toolkit.wizard.WizardPage;
import org.osgi.service.prefs.BackingStoreException;

public class FanShopSetupStep extends ConfigurationWizardStep {

	public FanShopSetupStep() {
		super("org.eclipse.cloudfree.examples.bugsearch.setup");
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cloudfree.admin.configuration.wizard.ConfigurationWizardStep#createPages(org.eclipse.cloudfree.admin.configuration.wizard.WizardContainer)
	 */
	@Override
	public void createPages(final WizardContainer wizard) {
		final WizardPage wizardPage = new WizardPage("bugsearch-setup", wizard);
		wizardPage.setTitle("The Eclipse Bug Search");
		wizardPage.setDescription("Deploy the Eclipse Bug Search example application.");

		final DialogFieldGroup enableFanShopGroup = new DialogFieldGroup("bugsearch-deployment", wizardPage, CWT.NONE);
		enableFanShopGroup.setTitle("Deployment");
		enableFanShopGroup.setDescription("Select whether the Bug Search application should be deployed.");

		final Checkbox deploy = new Checkbox("bugsearch-deploy", enableFanShopGroup, CWT.NONE);
		deploy.setLabel("Deploy the Eclipse FanShop demo application.");

		final DialogFieldGroup container = new DialogFieldGroup("bugsearch-urls", wizardPage, CWT.NONE);
		container.setTitle("Configuration");
		container.setDescription("Configure the URL the Bug Search application should be deployed to. Please make sure the domain name actually resolves to the machine CloudFree is running on.");
		//container.setVisibilityRule(DialogFieldRules.field(deploy).isSet());

		final TextInput urlInput = new TextInput("bugsearch-url", container, CWT.NONE);
		urlInput.setLabel("URL:");
		urlInput.setDescription("Enter the URL the Bug Search application should be mounted on (eg. http://localhost/). ");
		urlInput.setEnablementRule(DialogFieldRules.field(deploy).isSet());

		wizardPage.setContinueRule(DialogFieldRules.allFields().areValid());

	}

	/* (non-Javadoc)
	 * @see org.eclipse.cloudfree.admin.configuration.wizard.ConfigurationWizardStep#wizardFinished(org.eclipse.cloudfree.admin.configuration.wizard.CommandExecutionEvent)
	 */
	@Override
	public boolean wizardFinished(final CommandExecutionEvent finishEvent) {
		final BooleanContent deploy = (BooleanContent) finishEvent.getContentSet().getEntry("bugsearch-deploy");
		if ((null != deploy) && deploy.getValue()) {
			try {
				BugSearchDevSetup.enableServerRole();
				final TextContent url = (TextContent) finishEvent.getContentSet().getEntry("bugsearch-url");
				if (null != url) {
					final String text = url.getText();
					if ((null != text) && (text.trim().length() > 0)) {
						BugSearchDevSetup.setFanShopUrl(text.trim());
					}
				}
			} catch (final BackingStoreException e) {
				e.printStackTrace();
				return false;
			}
		}

		return true;
	}

}
