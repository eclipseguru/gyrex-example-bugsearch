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
package org.eclipse.cloudfree.examples.bugsearch.gwt.internal.client.widgets;

import org.eclipse.cloudfree.examples.bugsearch.gwt.internal.client.BugSearch;
import org.eclipse.cloudfree.examples.bugsearch.gwt.internal.client.BugSearchHistoryManager;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.AbstractImagePrototype.ImagePrototypeElement;

/**
 * 
 */
public class ActiveFilterWidget extends Widget {

	private final ImagePrototypeElement removeElement;
	private final BugSearchHistoryManager historyManager;
	private final String id;
	private final String value;

	public ActiveFilterWidget(final String id, final String value, final BugSearchHistoryManager historyManager) {
		this.id = id;
		this.value = value;
		this.historyManager = historyManager;
		setElement(DOM.createDiv());
		setStyleName("active-filter");
		addStyleDependentName(id);

		final Element idElement = DOM.createSpan();
		idElement.setInnerText(id);
		setStyleName(idElement, "id");

		final Element separatorElement = DOM.createSpan();
		separatorElement.setInnerText(": ");
		setStyleName(separatorElement, "separator");

		final Element valueElement = DOM.createSpan();
		valueElement.setInnerText(value);
		setStyleName(valueElement, "value");

		removeElement = BugSearch.IMAGES.clear().createElement().<ImagePrototypeElement> cast();
		setStyleName(removeElement, "clear-icon");
		DOM.sinkEvents(removeElement.<Element> cast(), Event.ONCLICK);

		getElement().appendChild(idElement);
		getElement().appendChild(separatorElement);
		getElement().appendChild(valueElement);
		getElement().appendChild(removeElement);
	}

	/* (non-Javadoc)
	 * @see com.google.gwt.user.client.ui.Widget#onBrowserEvent(com.google.gwt.user.client.Event)
	 */
	@Override
	public void onBrowserEvent(final Event event) {
		if (event.getTypeInt() == Event.ONCLICK) {
			event.preventDefault();
			if ((event.getTarget() == removeElement) || (event.getCurrentTarget() == removeElement)) {
				historyManager.toggleFilter(id, value);
				setVisible(false);
			}
		}
	}

}
