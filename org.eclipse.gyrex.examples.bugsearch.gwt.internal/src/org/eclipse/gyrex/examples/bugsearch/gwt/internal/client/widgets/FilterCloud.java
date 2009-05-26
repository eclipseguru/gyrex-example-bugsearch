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
package org.eclipse.gyrex.examples.bugsearch.gwt.internal.client.widgets;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.gyrex.examples.bugsearch.gwt.internal.client.BugSearch;
import org.eclipse.gyrex.examples.bugsearch.gwt.internal.client.BugSearchHistoryManager;
import org.eclipse.gyrex.examples.bugsearch.gwt.internal.client.service.BugListFilter;
import org.eclipse.gyrex.examples.bugsearch.gwt.internal.client.service.BugListFilterValue;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.AbstractImagePrototype.ImagePrototypeElement;

/**
 *
 */
public class FilterCloud extends Widget {

	/** FILTER_VALUE_COUNT */
	private static final String ATTR_COUNT = "filterValueCount";
	/** FILTER_VALUE */
	private static final String ATTR_VALUE = "filterValue";
	/** FILTER_ID */
	private static final String ATTR_FILTER_ID = "filterId";

	private static final Set<String> defaultExpandFilters;
	static {
		defaultExpandFilters = new HashSet<String>();
		defaultExpandFilters.add("tags");
		defaultExpandFilters.add("keywords");
	}

	private final Element labelElement;
	private List<Element> valueElements;
	private final BugListFilter filter;
	private final Element contentElement;

	private boolean collapsed = true;

	private final ImagePrototypeElement labelImage;

	private final Element labelText;
	private final BugSearchHistoryManager filterManager;

	/**
	 * Creates a new instance.
	 */
	public FilterCloud(final BugListFilter filter, final BugSearchHistoryManager filterManager) {
		this.filter = filter;
		this.filterManager = filterManager;

		labelImage = BugSearch.IMAGES.arrowRight().createElement();
		setStyleName(labelImage, "image");
		//		DOM.sinkEvents(labelImage.<Element> cast(), Event.ONCLICK);

		labelText = DOM.createSpan();
		labelText.setInnerText(filter.getLabel());
		setStyleName(labelText, "text");
		//		DOM.sinkEvents(labelText, Event.ONCLICK);

		labelElement = DOM.createElement("h6");
		labelElement.appendChild(labelImage);
		labelElement.appendChild(labelText);
		setStyleName(labelElement, "filter-label");
		DOM.sinkEvents(labelElement, Event.ONCLICK);

		contentElement = DOM.createDiv();
		contentElement.setClassName("filter-content");

		setElement(DOM.createDiv());
		setStyleName("sideitem");

		getElement().appendChild(labelElement);
		getElement().appendChild(contentElement);

		// expand some filters by default
		if (defaultExpandFilters.contains(filter.getId())) {
			collapsed = false;
		}

		setCollapsed(collapsed);

		// tag size calculation is based on value count distribution
		// 1 = 0-10%
		// 2 = 10-40%
		// 3 = 40-60%  --> mean
		// 4 = 60-90%
		// 5 = 90-100%

		final List<BugListFilterValue> values = filter.getValues();
		if (!values.isEmpty()) {
			final long totalHits = countTotalHits(values);
			final int valueMean = Math.round((float) totalHits / values.size());
			for (final BugListFilterValue value : values) {
				final int ratio = Math.round(((float) value.getCount() / valueMean) * 100.0f);
				final int size = getSize(ratio);
				addValue(value, size);
			}
		}
	}

	private void addValue(final BugListFilterValue value, final int size) {
		if (null == valueElements) {
			valueElements = new ArrayList<Element>();
		}
		if (!valueElements.isEmpty()) {
			final Element separatorElem = DOM.createSpan();
			separatorElem.setInnerText(", ");
			setStyleName(separatorElem, "separator");
			contentElement.appendChild(separatorElem);
		}
		final Element valueElement = DOM.createAnchor();
		valueElement.setInnerText(value.getValue());
		valueElement.setAttribute("href", "#" + valueElement.getInnerText());
		valueElement.setAttribute(ATTR_FILTER_ID, filter.getId());
		valueElement.setAttribute(ATTR_VALUE, value.getValue());
		valueElement.setAttribute(ATTR_COUNT, String.valueOf(value.getCount()));
		valueElement.setAttribute("title", "(" + BugSearch.MESSAGES.bugs(value.getCount() > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) value.getCount()) + ")");
		DOM.sinkEvents(valueElement, Event.ONCLICK | Event.ONMOUSEOVER | Event.ONMOUSEOUT);
		setStyleName(valueElement, "size" + size);

		contentElement.appendChild(valueElement);
		valueElements.add(valueElement);
	}

	private long countTotalHits(final List<BugListFilterValue> values) {
		long total = 0;
		for (final BugListFilterValue value : values) {
			total += value.getCount();
		}
		return total;
	}

	private int getSize(final int ratio) {
		if (ratio < 10) {
			return 0;
		} else if (ratio < 30) {
			return 1;
		} else if (ratio < 60) {
			return 2;
		} else if (ratio < 120) {
			return 3;
		} else if (ratio < 180) {
			return 4;
		} else if (ratio < 220) {
			return 5;
		} else {
			return 6;
		}
	}

	/* (non-Javadoc)
	 * @see com.google.gwt.user.client.ui.Widget#onBrowserEvent(com.google.gwt.user.client.Event)
	 */
	@Override
	public void onBrowserEvent(final Event event) {
		if (event.getTypeInt() == Event.ONCLICK) {
			event.preventDefault();
			final com.google.gwt.dom.client.Element target = event.getTarget();

			// collapse/expand if label was clicked
			if ((target == labelElement) || (target == labelText) || (target == labelImage)) {
				collapsed = !collapsed;
				setCollapsed(collapsed);
				return;
			}

			// check if a filter was clicked
			final String filterId = target.getAttribute(ATTR_FILTER_ID);
			if (null != filterId) {
				final String value = target.getAttribute(ATTR_VALUE);
				final boolean active = filterManager.toggleFilter(filterId, value);
				setValueActiveStyle(target.<Element> cast(), active);
			}
		}
	}

	private void setCollapsed(final boolean collapse) {
		if (collapse) {
			setVisible(contentElement, false);
			BugSearch.IMAGES.arrowRight().applyTo(labelImage);
		} else {
			BugSearch.IMAGES.arrowDown().applyTo(labelImage);
			setVisible(contentElement, true);
		}
	}

	private void setValueActiveStyle(final Element valueElement, final boolean active) {
		// TODO Auto-generated method stub

	}

}
