/*
 * 2010-3-11
 * 	a popup window when one row in PhenotypeTable (which is embedded in GWASPhenotypes) is clicked.
 * 	to display a tree of links to various association results
 */
package edu.nordborglab.client;


import java.util.Set;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
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
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Tree;
import com.google.gwt.user.client.ui.TreeItem;
import com.google.gwt.user.client.ui.VerticalPanel;



public class PhenotypeTablePopup extends DialogBox implements ClickHandler{
	private AccessionConstants constants;
	private DisplayJSONObject jsonErrorDialog;
	
	public String requestCallAndAnalysisMethodURL;
	public String fetchGWAURL;
	public String displayResultsGeneURL;
	public String fetchPhenotypeURL;
	public String getOneResultRawURL;
	
	public HTML statusReport;
	public Tree jsonTree = new Tree();
	private VerticalPanel dialogVPanel = new VerticalPanel();
	
	
	
	public PhenotypeTablePopup(AccessionConstants constants, DisplayJSONObject jsonErrorDialog, 
			String requestCallAndAnalysisMethodURL, String fetchGWAURL, 
			String displayResultsGeneURL, String fetchPhenotypeURL, String getOneResultRawURL)
	{
		this.constants = constants;
		this.jsonErrorDialog = jsonErrorDialog;
		this.requestCallAndAnalysisMethodURL = requestCallAndAnalysisMethodURL;
		this.fetchGWAURL = fetchGWAURL;
		this.displayResultsGeneURL = displayResultsGeneURL;
		this.fetchPhenotypeURL = fetchPhenotypeURL;
		this.getOneResultRawURL = getOneResultRawURL;
		
		statusReport = new HTML(this.constants.LoadingText());
		dialogVPanel.add(statusReport);
		
		HTML report = new HTML("Click below to open in new window.");
		//vpanel.add(report);
		dialogVPanel.add(report);
		
		dialogVPanel.add(jsonTree);
		//Avoids showing an "empty" cell
		jsonTree.setVisible(true);
		
		Button closeButton = new Button("Close");
		closeButton.addClickHandler(this);
		dialogVPanel.add(closeButton);
		setWidget(dialogVPanel);
		
		this.setModal(false);	// this dialog won't block keyboard/mouse inputs to other widgets.
	}
	
	private class JSONResponseTextHandler implements RequestCallback {
		private String phenotypeMethodID;
		private String phenotypeMethodName;
		
		public JSONResponseTextHandler(String phenotypeMethodID, String phenotypeMethodName)
		{
			this.phenotypeMethodID = phenotypeMethodID;
			this.phenotypeMethodName = phenotypeMethodName;
		}
		
		public void onError(Request request, Throwable exception) {
			jsonErrorDialog.displayRequestError(exception.toString());
			resetTitle();
		}

		public void onResponseReceived(Request request, Response response) {
			String responseText = response.getText();
			try {
				
				final String _fetchPhenotypeURL = fetchPhenotypeURL + "?phenotype_method_id=" + this.phenotypeMethodID;
				HTML phenotypeLink = new HTML("<a href=" + _fetchPhenotypeURL + " target='_blank'>Information for phenotype: " + 
							this.phenotypeMethodID + " " + this.phenotypeMethodName + "</a>");
				jsonTree.addItem(phenotypeLink);
				
				JSONValue returnJSONValue = JSONParser.parse(responseText);	
				
				JSONObject jsonObject;
				JSONValue jsonValue;
				
				jsonObject = returnJSONValue.isObject();
				JSONObject call_method_id2analysis_method_id_ls = jsonObject.get("call_method_id2analysis_method_id_ls").isObject();
				JSONObject call_method_id2label = jsonObject.get("call_method_id2label").isObject();
				JSONObject analysis_method_id2label = jsonObject.get("analysis_method_id2label").isObject();
				
				Set<String> keys = call_method_id2analysis_method_id_ls.keySet();
				for (String callMethodID : keys) {
					String callMethodLabel = call_method_id2label.get(callMethodID).isString().stringValue();
					TreeItem callMethodTreeItem = jsonTree.addItem(callMethodLabel);
					
					final String _fetchGWAURL = fetchGWAURL + "?call_method_id="+callMethodID+"&phenotype_method_id=" + phenotypeMethodID;
					//HTML gwaLink = new HTML("<a href=" + _fetchGWAURL + " target='_blank'>GWAS plots (all methods)" + "</a>");
					HTML gwaLink = new HTML("GWAS plots (all methods)");
					//HTML gwaLink = new HTML("GWAS plots (all methods)");
					TreeItem gwasPlotTreeItem = callMethodTreeItem.addItem(gwaLink);
					final String _displayResultsGeneURL = displayResultsGeneURL + "?call_method_id="+callMethodID+"&phenotype_method_id=" + phenotypeMethodID;
					HTML resultsGeneLink = new HTML("<a href=" + _displayResultsGeneURL + " target='_blank'>Genes within 20kb of Top SNPs (all methods)" + "</a>");
					TreeItem gwasGenesTreeItem = callMethodTreeItem.addItem(resultsGeneLink);
										
					JSONArray analysisMethodJsonArray = call_method_id2analysis_method_id_ls.get(callMethodID).isArray();
					for (int i = 0; i < analysisMethodJsonArray.size(); ++i) {
						jsonValue = analysisMethodJsonArray.get(i);
						String analysisMethodID = jsonValue.isString().stringValue();
						String analysisMethodLabel = analysis_method_id2label.get(analysisMethodID).isString().stringValue();
						final String _fetchOneGWAURL = fetchGWAURL + "?call_method_id="+callMethodID+"&phenotype_method_id=" + phenotypeMethodID
								+"&analysis_method_id=" + analysisMethodID;
						final String _getOneResultRawURL = getOneResultRawURL + "?call_method_id="+callMethodID+"&phenotype_method_id=" + phenotypeMethodID
								+"&analysis_method_id=" + analysisMethodID;
						HTML oneGWALink = new HTML("<a href=" + _fetchOneGWAURL + " target='_blank'>" +analysisMethodLabel + "</a>, " +
								"<a href=" + _getOneResultRawURL + " target='_blank'>download</a>");						
						gwasPlotTreeItem.addItem(oneGWALink);
					
					}
				}
				
			}
			catch (JSONException e) {
				jsonErrorDialog.displayParseError(responseText);
			}
			resetTitle();
		}
	}
	
	public void fillIn(String phenotypeMethodID, String phenotypeMethodName)
	{
		/*
		 * each category is gonna be a tab.
		 * fetch all categories from server (which further fetches from db)
		 * 		add a tab for each category
		 */
		jsonTree.clear();
		setIntoWaitState();
		//DOM.getElementById("title").setInnerText(TITLE_WAITING_TEXT);
		String url = URL.encode(this.requestCallAndAnalysisMethodURL + "&phenotype_method_id="+phenotypeMethodID);
		RequestBuilder requestBuilder = new RequestBuilder(RequestBuilder.GET, url);
		try {
			requestBuilder.sendRequest(null, new JSONResponseTextHandler(phenotypeMethodID, phenotypeMethodName));
		} catch (RequestException ex) {
			jsonErrorDialog.displaySendError(ex.toString());
			resetTitle();
		}
	}
	
	public void setIntoWaitState()
	{
		statusReport.setVisible(true);
	}
	public void resetTitle()
	{
		statusReport.setVisible(false);
	}
	
	
	public void displayJSONObject(JSONValue jsonValue) {
		this.show();
		jsonTree.removeItems();
		jsonTree.setVisible(true);
		TreeItem treeItem = jsonTree.addItem("JSON Response");
		//addChildren(treeItem, jsonValue);
		treeItem.setStyleName("JSON-JSONResponseObject");
		treeItem.setState(true);
	}
	
	public void onClick(ClickEvent cEvent)
	{
		/*
		 * close button is hit. hide the window.
		 */
		this.hide();
	}
}
