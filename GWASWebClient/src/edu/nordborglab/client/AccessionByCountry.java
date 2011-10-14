package edu.nordborglab.client;

import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.Tree;
import com.google.gwt.user.client.ui.TreeItem;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HTML;

import com.google.gwt.user.client.ui.SuggestBox;

import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.http.client.URL;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONException;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.json.client.JSONValue;

import com.google.gwt.user.client.ui.SuggestBox;
//SuggestionHandler;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;

import com.google.gwt.user.client.ui.SuggestionEvent;

import com.google.gwt.visualization.client.DataTable;


public class AccessionByCountry extends Sink implements ClickHandler{
	private CustomSuggestOracle oracle;
	//String[] words = constants.cwSuggestBoxWords();
	//for (int i = 0; i < words.length; ++i) {
	//	oracle.add(words[i]);
	//}

	// Create the suggest box
	private SuggestBox suggestBox;
	private Button suggestButton;
	//suggestBox.ensureDebugId("cwSuggestBox");
	private HorizontalPanel suggestPanel;
	private VerticalPanel panel;
	
	//private String dataUrl = "http://spreadsheets.google.com/tq?key=prll1aQH05yQqp_DKPP9TNg&pub=1";
	//private Query query = Query.create(dataUrl);
	private DisplayJSONObject jsonErrorDialog;
	private MapTableTree.PassingData passingData;
	private MapTableTree contentTree;
	

	public String findAccessionsByCountryURL;
	/**
	 * An instance of the constants.
	 */
	private AccessionConstants constants;
	private static final String SUGGEST_BUTTON_DEFAULT_TEXT = "Search";
	private static final String SUGGEST_BUTTON_WAITING_TEXT = "Waiting...";

	/*
	private class SuggestBoxChangeListener implements ChangeListener {
		public void onChange(Widget sender) {
			oracle.requestSuggestions(SuggestOracle.Request request,
					new AccessionSuggestOracleCallback());
		}
	}

	private class OracleRequestSuggestionsCallBack implements SuggestOracle.Callback {
		public void onSuggestionsReady(SuggestOracle.Request request, SuggestOracle.Response response) {
			String responseText = response.toString();	//getText();
			try {
				JSONValue jsonValue = JSONParser.parse(responseText);
				displayJSONObject(jsonValue);
			} catch (JSONException e) {
				displayParseError(responseText);
			}
		}
	}
	*/
	
	
	/**
	 * Constructor.
	 * 
	 * @param constants the constants
	 */
	public AccessionByCountry(AccessionConstants constants, DisplayJSONObject jsonErrorDialog, MapTableTree.PassingData passingData) {
		//super(constants);
		this.constants = constants;
		findAccessionsByCountryURL = get_findAccessionsByCountryURL();
		oracle = new CustomSuggestOracle(get_AccessionCountrySuggestOracleURL() + "&country=");
		
		this.jsonErrorDialog = jsonErrorDialog;
		
		panel = new CustomVerticalPanel(constants, jsonErrorDialog, constants.AccessionByCountryHelpID());
		
		suggestBox = new SuggestBox(oracle);
		//Label lbl = new Label(constants.cwAccessionByNameLabel());
		//suggestBox.addChangeListener(new SuggestBoxChangeListener());
		suggestBox.addSelectionHandler(new SuggestBoxSuggestionHandler());
		suggestBox.setTitle(constants.cwAccessionByNameDescription());
		
		suggestButton = new Button();
		suggestButton.setText(SUGGEST_BUTTON_DEFAULT_TEXT);
		suggestButton.addClickHandler(this);
		
		suggestPanel = new HorizontalPanel();
		suggestPanel.add(new HTML("<b>Enter a country acronym (i.e. USA): </b>"));
		suggestPanel.add(suggestBox);
		suggestPanel.add(suggestButton);
		suggestPanel.setSpacing(5);
		
		panel.add(suggestPanel);
		//panel.add(textBox);
		
		contentTree = new MapTableTree(constants, jsonErrorDialog, passingData);
		panel.add(contentTree);
		
		// All composites must call initWidget() in their constructors.
		initWidget(panel);

		// Give the overall composite a style name.
		setStyleName("AccessionByCountry");
	}
	public final native String get_findAccessionsByCountryURL() /*-{ return $wnd.findAccessionsByCountryURL;}-*/;
	public final native String get_AccessionCountrySuggestOracleURL() /*-{ return $wnd.AccessionCountrySuggestOracleURL;}-*/;
	
	public void onClick(ClickEvent event) {
		doFetchURL();
	}
	
	private class SuggestBoxSuggestionHandler implements SelectionHandler {
		public void onSelection(SelectionEvent event){
			doFetchURL();
		}
	}
	
	//@Override
	public String getDescription() {
		return "Support wild character like .* or ? etc. 'V.*r'";
	}
	
	@Override
	public HTML getDescriptionHTML() {
		return new HTML("<p>" + getDescription() + "</p>");
	}
	
	@Override
	public String getName() {
		return "By Country";
	}
	
	/**
	 * Class for handling the response text associated with a request for a JSON
	 * object.
	 * 
	 */
	private class JSONResponseTextHandler implements RequestCallback {
		public void onError(Request request, Throwable exception) {
			jsonErrorDialog.displayRequestError(exception.toString());
			resetSearchButtonCaption();
		}

		public void onResponseReceived(Request request, Response response) {
			String responseText = response.getText();
			try {
				//JSONValue jsonValue = JSONParser.parse(responseText);
				//displayJSONObject(jsonValue);
				DataTable data = Common.asDataTable(responseText);	//DataTable.create();//new google.visualization.DataTable(eval("("+response+")"), 0.5)
				contentTree.populateData(data);
				
			} catch (JSONException e) {
				jsonErrorDialog.displayParseError(responseText);
			}
			resetSearchButtonCaption();
		}
	}

	private void doFetchURL() {
		suggestButton.setText(SUGGEST_BUTTON_WAITING_TEXT);
		String url = URL.encode(findAccessionsByCountryURL + "&country=" + suggestBox.getText());
		RequestBuilder requestBuilder = new RequestBuilder(RequestBuilder.GET, url);
		try {
			requestBuilder.sendRequest(null, new JSONResponseTextHandler());
		} catch (RequestException ex) {
			jsonErrorDialog.displaySendError(ex.toString());
			resetSearchButtonCaption();
		}
		
	}

	private void resetSearchButtonCaption() {
		suggestButton.setText(SUGGEST_BUTTON_DEFAULT_TEXT);
	}
	public void resetSize()
	{
		contentTree.resetSize();
	}
}
