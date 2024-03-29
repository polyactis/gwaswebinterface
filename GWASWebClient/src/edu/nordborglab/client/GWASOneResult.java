package edu.nordborglab.client;

import java.util.Set;

import org.danvk.dygraphs.client.events.DataPoint;
import org.danvk.dygraphs.client.events.SelectHandler;
import org.danvk.dygraphs.client.events.SelectHandler.SelectEvent;

import at.gmi.nordborglab.widgets.geneviewer.client.datasource.impl.GeneSuggestion;
import at.gmi.nordborglab.widgets.geneviewer.client.datasource.impl.JBrowseDataSourceImpl;
import at.gmi.nordborglab.widgets.geneviewer.client.datasource.impl.ServerSuggestOracle;
import at.gmi.nordborglab.widgets.geneviewer.client.event.ClickGeneEvent;
import at.gmi.nordborglab.widgets.geneviewer.client.event.ClickGeneHandler;
import at.gmi.nordborglab.widgets.gwasgeneviewer.client.GWASGeneViewer;

import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.http.client.URL;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONException;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.DecoratedPopupPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.SuggestBox;
import com.google.gwt.user.client.ui.SuggestOracle;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.SuggestBox.DefaultSuggestionDisplay;
import com.google.gwt.user.client.ui.SuggestOracle.Suggestion;
import com.google.gwt.visualization.client.DataTable;



public class GWASOneResult extends CustomVerticalPanel{
	private AccessionConstants constants;
	private DisplayJSONObject jsonErrorDialog;
	
	private int analysisMethodID;
	private String analysisMethodDescription;
	private String GWABaseURL;
	private String SNPBaseURL;
	private String getOneResultRawURL;
	private Double pseudoHeritability;
	private String geneInfoUrl;
	private HTML statusReport;
	private String[] colors = {"blue", "green", "red", "cyan", "purple"};
	private String[] gene_mark_colors = {"red", "red", "blue", "red", "green"};
	
	private DataTable dataTable;
	private DecoratedPopupPanel popupLink;
	private HorizontalPanel searchGenePanel = new HorizontalPanel();
	private SuggestBox searchGeneBox;
	private JBrowseDataSourceImpl datasource = new JBrowseDataSourceImpl("/Genes/");
	
	public GWASOneResult(AccessionConstants constants, DisplayJSONObject jsonErrorDialog, DecoratedPopupPanel popupLink, 
			int analysisMethodID, String analysisMethodDescription, String GWABaseURL, String SNPBaseURL, 
			String getOneResultRawURL,Double pseudoHeritability,String geneInfoUrl)
	{
		super(constants, jsonErrorDialog, constants.GWASOneResultHelpID());
		this.constants = constants;
		this.jsonErrorDialog = jsonErrorDialog;
		this.analysisMethodID = analysisMethodID;
		this.analysisMethodDescription = analysisMethodDescription;
		this.GWABaseURL = GWABaseURL;
		this.SNPBaseURL = SNPBaseURL;
		this.getOneResultRawURL = getOneResultRawURL;
		this.pseudoHeritability = pseudoHeritability;
		this.geneInfoUrl = geneInfoUrl;
		this.popupLink = popupLink;
		
		statusReport = new HTML(this.constants.LoadingText());
		this.add(statusReport);
		
		HTML getOneResultRawLink = new HTML("<a href=" + this.getOneResultRawURL + 
					" target='_blank'>Click here to download the association file</a>");
		this.add(getOneResultRawLink);
		
		this.add(new HTML(this.analysisMethodDescription));
		if (pseudoHeritability != null)
			this.add(new HTML("psuedo-heritability: " + pseudoHeritability.toString()));
		
		add(searchGenePanel);
		searchGeneBox = new SuggestBox(new ServerSuggestOracle(datasource,5));
		((DefaultSuggestionDisplay)searchGeneBox.getSuggestionDisplay()).setAnimationEnabled(true);
		searchGeneBox.addSelectionHandler(new SelectionHandler<SuggestOracle.Suggestion>() {
			
			@Override
			public void onSelection(SelectionEvent<Suggestion> event) {
				GeneSuggestion suggestion =  (GeneSuggestion)event.getSelectedItem();
				GWASGeneViewer viewer = getGWASGeneViewer(suggestion.getGene().getChromosome());
				if (viewer != null)
				{
					viewer.clearDisplayGenes();
					viewer.addDisplayGene(suggestion.getGene());
					viewer.refresh();
				}
			}
		});
		searchGenePanel.add(new HTML("Search genes:"));
		searchGenePanel.add(searchGeneBox);
		loadGWA();
	}
	
	private class LoadGWAResponseHandler implements RequestCallback {
		public void onError(Request request, Throwable exception) {
			jsonErrorDialog.displayRequestError(exception.toString());
			resetTitle();
		}

		public void onResponseReceived(Request request, Response response) {
			String responseText = response.getText();
			try {
				JSONValue jsonValue = JSONParser.parse(response.getText());
				JSONObject serverData = jsonValue.isObject();	//jsonValue.isArray();
				JSONObject chr2data = serverData.get("chr2data").isObject();
				JSONObject chr2length = serverData.get("chr2length").isObject();
				double max_value = serverData.get("max_value").isNumber().doubleValue();
				int max_length = (int) serverData.get("max_length").isNumber().doubleValue();
				double bonferroniThreshold = serverData.get("bonferroniThreshold").isNumber().doubleValue();
				Set<String> keys = chr2data.keySet();
				int i =0;
				
				
				
				for (String chromosome : keys) 
				//for (i=0; i<keys.size(); i++)
				{
					//String chromosome = String.valueOf(i);
					//JSONString data = chr2data.get(chromosome).isString();
					String data = chr2data.get(chromosome).toString();
					
					//jsonErrorDialog.displayJSONObject(chr2data.get(chromosome));
					int chrLength = (int) chr2length.get(chromosome).isNumber().doubleValue();
					//jsonErrorDialog.displayRequestError("chromosome "+ chromosome + "length: " + chrLength);
					String color = colors[i%colors.length];
					String gene_mark_color = gene_mark_colors[i%gene_mark_colors.length];
					GWASGeneViewer associationChart = new GWASGeneViewer("Chr"+chromosome, color,gene_mark_color, 1000,datasource);
					associationChart.setGeneInfoUrl(geneInfoUrl);
					/*AssociationScatterChart associationChart = new AssociationScatterChart(chromosome,
							color, chrLength, max_length, max_value, SNPBaseURL);*/
					add(associationChart);
					dataTable = Common.asDataTable(data.substring(1, data.length()-1));	//2009-4-25 data has extra " on both ends
					dataTable.insertRows(0,1);
					dataTable.setValue(0, 0, 0);
					int index = dataTable.addRow();
					dataTable.setValue(index, 0, chrLength);
					associationChart.draw(dataTable,max_value,0,chrLength,bonferroniThreshold);
					final String SNPUrl = SNPBaseURL+"&chromosome="+chromosome; 
					associationChart.addSelectionHandler(new SelectHandler() {
						
						@Override
						public void onSelect(SelectEvent event) {
							DataPoint point = event.point;
							int position = (int)point.getXVal();
							double score = point.getYVal();
							String _SNPURL = URL.encode(SNPUrl + "&position=" + position +"&score="+score);
							Window.open(_SNPURL, "", "");
						}
					});
					//associationChart.initHandler();
					i += 1;
				}
			} catch (JSONException e) {
				jsonErrorDialog.displayParseError(responseText);
			}
			resetTitle();
		}
	}
	
	private void loadGWA()
	{
		setIntoWaitState();
		String url = URL.encode(this.GWABaseURL + "&analysis_method_id="+this.analysisMethodID);
		RequestBuilder requestBuilder = new RequestBuilder(RequestBuilder.GET, url);
		try {
			requestBuilder.sendRequest(null, new LoadGWAResponseHandler());
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
	
	public GWASGeneViewer getGWASGeneViewer(String chromosome) 
	{
		for (int i = 0;i < getWidgetCount();i++) {
			Widget widget = getWidget(i);
			if ((widget instanceof GWASGeneViewer) && ((GWASGeneViewer)widget).getChromosome().equals(chromosome)) 
				return (GWASGeneViewer)widget;
		}
		return null;
	}
}
