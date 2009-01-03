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
package org.eclipse.cloudfree.examples.bugsearch.gwt.internal.client;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.cloudfree.examples.bugsearch.gwt.internal.client.service.Bug;
import org.eclipse.cloudfree.examples.bugsearch.gwt.internal.client.service.BugList;
import org.eclipse.cloudfree.examples.bugsearch.gwt.internal.client.service.BugListFilter;
import org.eclipse.cloudfree.examples.bugsearch.gwt.internal.client.service.BugSearchService;
import org.eclipse.cloudfree.examples.bugsearch.gwt.internal.client.service.BugSearchServiceAsync;
import org.eclipse.cloudfree.examples.bugsearch.gwt.internal.client.widgets.ActiveFilterWidget;
import org.eclipse.cloudfree.examples.bugsearch.gwt.internal.client.widgets.FilterCloud;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.HistoryListener;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FocusListener;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class BugSearch implements EntryPoint {

	class SearchProgress extends Timer {

		/** SEARCHING */
		private static final String SEARCHING = "Searching";
		String text = SEARCHING;

		@Override
		public void run() {
			if (text.length() > 100) {
				// reset
				text = SEARCHING;
			} else {
				text += ".";
			}

			searchResultMessage.setHTML("<em>" + text + "</em>");
		}

		public void start() {
			// cancel running instance
			cancel();

			// reset text
			text = SEARCHING;

			// set initial
			run();

			// schedule repeating
			scheduleRepeating(100);
		}

	}

	private final class SearchServiceCallback implements AsyncCallback<BugList> {

		private boolean canceled;

		public void cancel() {
			canceled = true;
		}

		public void onFailure(final Throwable caught) {
			if (!canceled) {
				getSearchProgress().cancel();
				error(caught);
			}
		}

		public void onSuccess(final BugList result) {
			if (!canceled) {
				getSearchProgress().cancel();
				populate(result);
			}
		}
	}

	private static final String SEARCH_UNRESOLVED = "Search Unresolved";
	private static final String SEARCH_RESOLVED = "Search Resolved";

	private static final String SEARCH = "Search";
	private static final int IDX_SUMMARY = 2;
	private static final int IDX_PROJECT = 1;
	private static final int IDX_BUG_NUM = 0;

	private static final int IDX_SCORE = 3;

	private static final String EMPTY_SEARCH_FIELD_TEXT = "Start typing your Bugzilla search query here...";
	private BugSearchServiceAsync bugSearchService;
	private Button searchUnresolvedButton;
	private FlexTable bugListTable;
	private HTML searchResultMessage;
	private final Element errorSanitizer = DOM.createSpan();
	private Button searchResolvedButton;

	private Button searchButton;
	private Panel filterCloudPanel;
	private NumberFormat queryTimeFormat;
	public static final BugSearchMessages MESSAGES = GWT.create(BugSearchMessages.class);
	public static final BugSearchImages IMAGES = GWT.create(BugSearchImages.class);
	private BugSearchHistoryManager historyManager;
	private TextBox searchField;
	private SearchServiceCallback currentSearchServiceCallback;
	private RootPanel mainPanel;
	private HorizontalPanel activeFiltersPanel;
	private SearchProgress searchProgress;

	private void clearAllResultsAndFilters() {
		activeFiltersPanel.clear();
		filterCloudPanel.clear();
		bugListTable.clear();
		historyManager.updateFromHistoryToken(null);
	}

	private void createActiveFiltersComposite(final Panel parent) {
		activeFiltersPanel = new HorizontalPanel();
		activeFiltersPanel.setSpacing(5);

		parent.add(new HTML("<h3>Active Filters</h3>"));
		parent.add(activeFiltersPanel);
	}

	private void createBugListComposite(final Panel parent) {
		bugListTable = new FlexTable();
		bugListTable.setCellSpacing(3);

		parent.add(new HTML("<h3>Bug List</h3>"));
		parent.add(bugListTable);
	}

	private void createSearchComposite(final Panel parent) {
		searchField = new TextBox();
		searchField.setText(EMPTY_SEARCH_FIELD_TEXT);
		searchField.setWidth("320px");
		searchField.addFocusListener(new FocusListener() {
			public void onFocus(final Widget sender) {
				if (EMPTY_SEARCH_FIELD_TEXT.equals(searchField.getText())) {
					searchField.setText("");
				}
			}

			public void onLostFocus(final Widget sender) {
				if (searchField.getText().length() == 0) {
					searchField.setText(EMPTY_SEARCH_FIELD_TEXT);
				}
			}
		});

		searchUnresolvedButton = new Button();
		searchUnresolvedButton.setText(SEARCH_UNRESOLVED);
		searchUnresolvedButton.addClickListener(new ClickListener() {
			public void onClick(final Widget sender) {
				String query = searchField.getText();
				if (EMPTY_SEARCH_FIELD_TEXT.equals(query)) {
					query = "";
				}
				getHistoryManager().setQuery(query);
				getHistoryManager().setActiveFilter("status", "UNCONFIRMED", "NEW", "ASSIGNED");
			}
		});

		searchResolvedButton = new Button();
		searchResolvedButton.setText(SEARCH_RESOLVED);
		searchResolvedButton.addClickListener(new ClickListener() {

			public void onClick(final Widget sender) {
				String query = searchField.getText();
				if (EMPTY_SEARCH_FIELD_TEXT.equals(query)) {
					query = "";
				}
				getHistoryManager().setQuery(query);
				getHistoryManager().setActiveFilter("status", "RESOLVED", "VERIFIED", "CLOSED");
			}
		});

		searchButton = new Button();
		searchButton.setText(SEARCH);
		searchButton.addClickListener(new ClickListener() {

			public void onClick(final Widget sender) {
				String query = searchField.getText();
				if (EMPTY_SEARCH_FIELD_TEXT.equals(query)) {
					query = "";
				}
				getHistoryManager().setQuery(query);
				//getHistoryManager().setActiveFilter("status", "RESOLVED", "VERIFIED", "CLOSED");
				search(query);
			}
		});

		final HorizontalPanel searchPanel = new HorizontalPanel();
		searchPanel.setSpacing(5);

		searchPanel.add(searchField);
		searchPanel.add(searchButton);
		searchPanel.add(searchUnresolvedButton);
		searchPanel.add(searchResolvedButton);

		searchResultMessage = new HTML();

		final VerticalPanel panel = new VerticalPanel();
		panel.add(searchPanel);
		panel.add(searchResultMessage);

		parent.add(panel);
	}

	void error(final Throwable caught) {
		resetSearchButtons();

		clearAllResultsAndFilters();
		setSearchError(caught);
	}

	private Widget getFilterWidget(final BugListFilter filter) {
		return new FilterCloud(filter, getHistoryManager());
	}

	/**
	 * Returns the historyManager.
	 * 
	 * @return the historyManager
	 */
	public BugSearchHistoryManager getHistoryManager() {
		if (null == historyManager) {
			historyManager = new BugSearchHistoryManager();
		}
		return historyManager;
	}

	/**
	 * Returns the searchProgress.
	 * 
	 * @return the searchProgress
	 */
	SearchProgress getSearchProgress() {
		if (null == searchProgress) {
			searchProgress = new SearchProgress();
		}
		return searchProgress;
	}

	private void hideLoadingMessage() {
		final RootPanel loadingMessage = RootPanel.get("initialLoading");
		if (null != loadingMessage) {
			loadingMessage.setVisible(false);
		}
	}

	/**
	 * This is the entry point method.
	 */
	public void onModuleLoad() {

		mainPanel = RootPanel.get("midcolumn");

		createSearchComposite(mainPanel);
		createActiveFiltersComposite(mainPanel);
		createBugListComposite(mainPanel);

		filterCloudPanel = RootPanel.get("rightcolumn");

		hideLoadingMessage();

		History.addHistoryListener(new HistoryListener() {

			public void onHistoryChanged(final String historyToken) {
				getHistoryManager().updateFromHistoryToken(historyToken);
				String query = getHistoryManager().getQuery();
				if (null == query) {
					query = "";
				}
				setSearchFieldText(query);
				search(query);
			}
		});

		// initial search
		History.fireCurrentHistoryState();
	}

	void populate(final BugList result) {
		resetSearchButtons();

		populateSearchResultMessage(result);
		populateActiveFilters(result);
		populateFilterClouds(result);
		populateBugList(result);
	}

	private void populateActiveFilters(final BugList result) {
		activeFiltersPanel.clear();

		final Map<String, List<String>> activeFilters = result.getActiveFilters();
		if (null != activeFilters) {
			for (final Entry<String, List<String>> filter : activeFilters.entrySet()) {
				final List<String> values = filter.getValue();
				for (final String value : values) {
					activeFiltersPanel.add(new ActiveFilterWidget(filter.getKey(), value, getHistoryManager()));
				}
			}
		}

	}

	private void populateBugList(final BugList result) {
		bugListTable.clear();

		bugListTable.setHTML(0, IDX_BUG_NUM, "<strong>Bug Number</strong>");
		bugListTable.setHTML(0, IDX_PROJECT, "<strong>Project</strong>");
		bugListTable.setHTML(0, IDX_SUMMARY, "<strong>Summary</strong>");
		bugListTable.setHTML(0, IDX_SCORE, "<strong>Relevancy</strong>");

		int i = 1;
		for (final Bug bug : result.getBugs()) {
			bugListTable.setWidget(i, IDX_BUG_NUM, new HTML("<a href=\"http://bugs.eclipse.org/" + bug.getId() + "\" target=\"_blank\">" + bug.getId() + "</a>"));
			bugListTable.setWidget(i, IDX_PROJECT, new Label(bug.getProduct()));
			bugListTable.setWidget(i, IDX_SUMMARY, new Label(bug.getSummary()));
			bugListTable.setWidget(i, IDX_SCORE, new Label(NumberFormat.getDecimalFormat().format(bug.getScore())));
			i++;
		}
	}

	private void populateFilterClouds(final BugList result) {
		filterCloudPanel.clear();

		for (final BugListFilter filter : result.getFilters()) {
			if (!filter.getValues().isEmpty()) {
				final Widget filterWidget = getFilterWidget(filter);
				filterCloudPanel.add(filterWidget);
			}
		}
	}

	private void populateSearchResultMessage(final BugList result) {
		if (null == queryTimeFormat) {
			//queryTimeFormat = NumberFormat.getFormat("#.#");
			queryTimeFormat = NumberFormat.getDecimalFormat();
		}
		final String queryTime = result.getQueryTime() > 1000 ? queryTimeFormat.format(result.getQueryTime() / 1000.0f) + " seconds" : "less than a second";
		searchResultMessage.setHTML("Found <strong>" + result.getNumFound() + " bugs</strong> in " + queryTime + ".");
	}

	private void resetSearchButtons() {
		searchField.setEnabled(true);

		searchUnresolvedButton.setText(SEARCH_UNRESOLVED);
		searchUnresolvedButton.setEnabled(true);

		searchResolvedButton.setText(SEARCH_RESOLVED);
		searchResolvedButton.setEnabled(true);

		searchButton.setText(SEARCH);
		searchButton.setEnabled(true);
	}

	void search(final String query) {
		getSearchProgress().start();
		searchField.setEnabled(false);
		searchUnresolvedButton.setEnabled(false);
		searchResolvedButton.setEnabled(false);
		searchButton.setEnabled(false);

		if (null == bugSearchService) {
			bugSearchService = (BugSearchServiceAsync) GWT.create(BugSearchService.class);
		}

		final SearchServiceCallback searchServiceCallback = new SearchServiceCallback();

		if (null != currentSearchServiceCallback) {
			currentSearchServiceCallback.cancel();
		}
		currentSearchServiceCallback = searchServiceCallback;

		bugSearchService.findBugs(query, getHistoryManager().getActiveFilters(), searchServiceCallback);
	}

	private void setSearchError(final Throwable caught) {
		errorSanitizer.setInnerText(caught.getMessage());
		searchResultMessage.setHTML("<em>" + errorSanitizer.getInnerText() + "</em>");
	}

	void setSearchFieldText(final String text) {
		searchField.setText(null != text ? text : "");
		// we set the focus to get the "type query text"
		searchField.setFocus(true);
	}
}
