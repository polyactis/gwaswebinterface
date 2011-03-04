package edu.nordborglab.client;


import java.util.Set;

import org.danvk.dygraphs.client.events.DataPoint;
import org.danvk.dygraphs.client.events.SelectHandler;
import org.danvk.dygraphs.client.events.SelectHandler.SelectEvent;


import at.gmi.nordborglab.widgets.geneviewer.client.datasource.impl.JBrowseDataSourceImpl;
import at.gmi.nordborglab.widgets.gwasgeneviewer.client.GWASGeneViewer;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;
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

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.Frame;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.TabPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.visualization.client.DataTable;



/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class SNP implements EntryPoint {
	public DisplayJSONObject jsonErrorDialog;
	public AccessionConstants constants;
	
	private TabPanel tPanel;
	
	private int callMethodID;
	private int phenotypeMethodID;
	private int analysisMethodID;
	private int panelWidth = 0;
	private String pageTitle;
	private JsArrayString  snpSpace;
	
	private String snpSummaryQueryURL;
	private String snpSignificantHitsQueryURL;
	private String ecotypeAllelePhenotypeURL;
	private String GBrowseURLJS;
	private String GBrowseURL;
	private String[] colors = {"blue", "green", "red", "cyan", "purple"};
	private String[] gene_mark_colors = {"red", "red", "blue", "red", "green"};
	
	private CustomVerticalPanel vPanel;
	private JBrowseDataSourceImpl datasource = new JBrowseDataSourceImpl("/Genes/");
	private GWASGeneViewer gwasgeneviewer = null;
	private Frame GBrowseFrame;
	
	private String TITLE_DEFAULT_TEXT = "SNP";
	private String TITLE_WAITING_TEXT = "Waiting...";
	
	
	private class LoadGWAResponseHandler implements RequestCallback {
		public void onError(Request request, Throwable exception) {
			jsonErrorDialog.displayRequestError(exception.toString());
			resetTitle();
		}

		public void onResponseReceived(Request request, Response response) {
			String responseText = response.getText();
			try {
				final int chromosome = getChromosome();
				int startPos = getStartPos();
				int endPos = getEndPos();
				JSONObject jsonValue = JSONParser.parse(response.getText()).isObject();
				String chr2data = jsonValue.get("chr2data").isString().stringValue();
				//String data = chr2data.toString();
				double chrLength = jsonValue.get("chr2length").isNumber().doubleValue();
				double max_value = jsonValue.get("max_value").isNumber().doubleValue();
				DataTable dataTable = Common.asDataTable(chr2data);	
				dataTable.insertRows(0,1);
				dataTable.setValue(0, 0, 0);
				
				int index = dataTable.addRow();
				dataTable.setValue(index, 0, chrLength);
				String color = colors[chromosome%colors.length];
				String gene_mark_color = gene_mark_colors[chromosome%gene_mark_colors.length];
				gwasgeneviewer = new GWASGeneViewer("Chr"+chromosome, color,gene_mark_color, panelWidth,datasource);
				gwasgeneviewer.setGeneInfoUrl(getGeneInfoUrl());
				gwasgeneviewer.setGeneViewerHeight(400);
				gwasgeneviewer.setSnpPosX(getSnpPosition());
				vPanel.add(gwasgeneviewer);
				gwasgeneviewer.draw(dataTable,max_value,startPos,endPos);
				gwasgeneviewer.addSelectionHandler(new SelectHandler() {
					
					@Override
					public void onSelect(SelectEvent event) {
						DataPoint point = event.point;
						int position = (int)point.getXVal();
						if (getSnpPosition() != position) {
							double score = point.getYVal();
							String _SNPURL = URL.encode(getSNPUrl() + "&chromosome="+chromosome+"&position=" + position +"&score="+score);
							Window.open(_SNPURL, "", "");
						}
					}
				});
				tPanel.selectTab(0);				
			}
			catch (Exception e) 
			{
				jsonErrorDialog.displayParseError(responseText);
			}
		}
	}
	
	/**
	 * This is the entry point method.
	 */
	public void onModuleLoad() {
		jsonErrorDialog = new DisplayJSONObject("Error Dialog");
		constants = (AccessionConstants) GWT.create(AccessionConstants.class);
		
		tPanel = new TabPanel();
		
		//tPanel.selectTab(1);
		//tPanel.setWidth("1000px");
		tPanel.setAnimationEnabled(true);
		
		
		// Add it to the root panel.
		RootPanel.get("snp").add(tPanel);
		
		snpSpace = getSNPSpace();
		snpSummaryQueryURL = snpSpace.get(0);
		snpSignificantHitsQueryURL =  snpSpace.get(1);
		ecotypeAllelePhenotypeURL = snpSpace.get(2);
		GBrowseURL = snpSpace.get(3);
		
		//callMethodID = getCallMethodID();
		//phenotypeMethodID = getPhenotypeMethodID();
		//analysisMethodID = getAnalysisMethodID();
		pageTitle = getPageTitle();
		TITLE_DEFAULT_TEXT = pageTitle;
		TITLE_WAITING_TEXT = TITLE_WAITING_TEXT + pageTitle;
		
		//tPanel.setSize("100%", "100%");
		tPanel.setWidth("100%");
		panelWidth = tPanel.getOffsetWidth();
		vPanel = new CustomVerticalPanel(constants, jsonErrorDialog, constants.GBrowseHelpID());
		vPanel.setSize("100%", "100%");
		tPanel.add(vPanel, "GWASGeneViewer");
		loadGWASGeneViewer();

		//GBrowseFrame = new Frame(GBrowseURL);
		
		//GBrowseFrame.setSize("100%", "800px");
		//GBrowseHTML.getElement().getId();
		//vPanel.add(GBrowseFrame);
		
		
		
		RootPanel SNPSummaryDiv = RootPanel.get("SNPSummary");
		RootPanel.detachNow(SNPSummaryDiv);
		CustomVerticalPanel SNPSummaryPanel = new CustomVerticalPanel(constants, jsonErrorDialog, constants.SNPSummaryPanelHelpID());
		SNPSummaryPanel.add(SNPSummaryDiv);
		//SNPSummaryDiv.setVisible(true);
		tPanel.add(SNPSummaryPanel, "SNP Summary Info");
		
		RootPanel SignificantHitsInAllPhenotypeDiv = RootPanel.get("SignificantHitsInAllPhenotype");
		RootPanel.detachNow(SignificantHitsInAllPhenotypeDiv);
		CustomVerticalPanel SignificantHitsInAllPhenotypePanel = new CustomVerticalPanel(constants, jsonErrorDialog, constants.SignificantHitsInAllPhenotypeHelpID());
		SignificantHitsInAllPhenotypePanel.add(SignificantHitsInAllPhenotypeDiv);
		tPanel.add(SignificantHitsInAllPhenotypePanel, "All Phenotypes in which SNP is significant");
		
		RootPanel EcotypeAlleleMotionChartDiv = RootPanel.get("EcotypeAlleleMotionChart");
		RootPanel.detachNow(EcotypeAlleleMotionChartDiv);
		CustomVerticalPanel EcotypeAlleleMotionChartPanel = new CustomVerticalPanel(constants, jsonErrorDialog, constants.EcotypeAlleleMotionChartHelpID());
		EcotypeAlleleMotionChartPanel.add(EcotypeAlleleMotionChartDiv);
		tPanel.add(EcotypeAlleleMotionChartPanel, "Ecotype Allele Phenotype BarChart");
		
		RootPanel EcotypeAlleleTableDiv = RootPanel.get("EcotypeAlleleTable");
		RootPanel.detachNow(EcotypeAlleleTableDiv);
		CustomVerticalPanel EcotypeAlleleTablePanel = new CustomVerticalPanel(constants, jsonErrorDialog, constants.EcotypeAlleleTableHelpID());
		EcotypeAlleleTablePanel.add(EcotypeAlleleTableDiv);
		tPanel.add(EcotypeAlleleTablePanel, "Ecotype Allele Phenotype Table");
		
		resetTitle();
		//fetchGBrowseHTML();
	}
	
	private void loadGWASGeneViewer()
	{
		//http://arabidopsis.gmi.oeaw.ac.at:5000=32&amp;phenotype_method_id=1&analysis_method_id=1
		String url = URL.encode(getGWASGeneViewerURL());
		RequestBuilder requestBuilder = new RequestBuilder(RequestBuilder.GET, url);
		try {
			requestBuilder.sendRequest(null, new LoadGWAResponseHandler());
		} catch (RequestException ex) {
			jsonErrorDialog.displaySendError(ex.toString());
			resetTitle();
		}
	}
	
	public native int getCallMethodID()/*-{	return $wnd.call_method_id; }-*/;
	public native String getGWASGeneViewerURL() /*-{ return $wnd.gwasgeneViewerQueryURL; }-*/;
		
	public native int getChromosome()/*-{	return $wnd.chromosome; }-*/;
	public native int getSnpPosition()/*-{	return $wnd.snpPosition; }-*/;
	
	public native int getStartPos()/*-{	return $wnd.start_pos; }-*/;
	public native int getEndPos()/*-{	return $wnd.stop_pos; }-*/;

	public native int getPhenotypeMethodID()/*-{ return $wnd.phenotype_method_id; }-*/;
	public native int getAnalysisMethodID()/*-{ return $wnd.analysis_method_id; }-*/;
	public native String getPageTitle()/*-{ return $wnd.pageTitle; }-*/;
	public native JsArrayString getSNPSpace()/*-{
	return [$wnd.snpSummaryQueryURL, $wnd.snpSignificantHitsQueryURL, $wnd.ecotypeAllelePhenotypeURL, $wnd.GBrowseURL];
	}-*/;
	
	public native String getSNPUrl() /*-{ return $wnd.SNPUrl;}-*/;
	public native String getGeneInfoUrl() /*-{ return $wnd.GeneInfoUrl;}-*/;
	
	private class JSONResponseTextHandler implements RequestCallback {
		public void onError(Request request, Throwable exception) {
			jsonErrorDialog.displayRequestError(exception.toString());
			resetTitle();
		}
		public void onResponseReceived(Request request, Response response) {
			String responseText = response.getText();
			try {
				//GBrowseHTML.setHTML(responseText);
				tPanel.selectTab(0);
			}
			catch (JSONException e) {
				jsonErrorDialog.displayParseError(responseText);
			}
			resetTitle();
		}
	}
	
	public void fetchGBrowseHTML()
	{
		/*
		 * each category is gonna be a tab.
		 * fetch all categories from server (which further fetches from db)
		 * 		add a tab for each category
		 */
		setIntoWaitState();
		GBrowseFrame.setUrl(GBrowseURL);
		/*
		String url = URL.encode(GBrowseURL);
		RequestBuilder requestBuilder = new RequestBuilder(RequestBuilder.GET, url);
		try {
			requestBuilder.sendRequest(null, new JSONResponseTextHandler());
		} catch (RequestException ex) {
			jsonErrorDialog.displaySendError(ex.toString());
			resetTitle();
		}
		*/
		resetTitle();
	}
	
	public void setIntoWaitState()
	{
		Window.setTitle(TITLE_WAITING_TEXT);
		//editSaveButton.setEnabled(false);
	}
	
	public void resetTitle()
	{
		Window.setTitle(TITLE_DEFAULT_TEXT);
		//DOM.getElementById("title").setInnerText(TITLE_DEFAULT_TEXT);
	}
	
}
