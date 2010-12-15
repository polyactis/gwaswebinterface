package edu.nordborglab.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.http.client.URL;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.SplitLayoutPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiFactory;
import com.google.gwt.core.client.JavaScriptObject;

public class ViewCNV extends Composite  {
	
	interface Binder extends UiBinder<SplitLayoutPanel, ViewCNV> { }
	private static final Binder binder = GWT.create(Binder.class);
	
	@UiField ListBox overviewDataTypeListBox;
	@UiField ListBox cnvFrequencyCNVMethodListBox;
	@UiField ListBox cnvFrequencyChromosomeListBox;
	@UiField ListBox cnvFrequencySmoothTypeListBox;
	@UiField ListBox cnvFrequencyPlotTypeListBox;
	@UiField Button cnvFrequencySubmitButton;
	
	@UiField ListBox cnvCallHaplotypeCNVMethodListBox;
	@UiField Button cnvCallHaplotypeSubmitButton;
	
	@UiField FlowPanel protovisPanel;
	
	
	public String getChromsomeLsJsonFromTableCNVCallURL;
	public String getChromsomeLsJsonFromTableCNVURL;
	public String getFrequencyOverviewDataJsonFromTableCNVURL;
	
	public String getGeneModelDataJsonURL;
	
	public String getHaplotypeDataJsonFromTableCNVCallURL;
	
	private DisplayJSONObject jsonErrorDialog = new DisplayJSONObject("Error Dialog");
	private AccessionConstants constants = (AccessionConstants) GWT.create(AccessionConstants.class);
	
	private Integer number_of_divs = 0;
	private String chromosome = "";
	
	/** Used by MyUiBinder to instantiate CustomVerticalPanel */
	@UiFactory CustomVerticalPanel makeCustomVerticalPanel() { // method name is insignificant
		return new CustomVerticalPanel(constants, jsonErrorDialog, constants.ViewCNVHelpID());
	}
	
	ViewCNV() {
		super();
		initWidget(binder.createAndBindUi(this));
		
		this.getChromsomeLsJsonFromTableCNVCallURL = this.getChromsomeLsJsonFromTableCNVCallURLFromJS();
		this.getChromsomeLsJsonFromTableCNVURL = this.getChromsomeLsJsonFromTableCNVURLFromJS();
		this.getFrequencyOverviewDataJsonFromTableCNVURL = this.getFrequencyOverviewDataJsonFromTableCNVURLFromJS();
		this.getGeneModelDataJsonURL = this.getGeneModelDataJsonURLFromJS();
		this.getHaplotypeDataJsonFromTableCNVCallURL = this.getHaplotypeDataJsonFromTableCNVCallURLFromJS();
		
		fillSelectBoxWithJsArray(this.cnvFrequencyCNVMethodLsJsonFromJS(), cnvFrequencyCNVMethodListBox);
		fillSelectBoxWithJsArray(this.smoothTypeLsJsonFromJS(), cnvFrequencySmoothTypeListBox);
		fillSelectBoxWithJsArray(this.plotTypeLsJsonFromJS(), cnvFrequencyPlotTypeListBox);
		fillSelectBoxWithJsArray(this.cnvCallCNVMethodLsJsonFromJS(), cnvCallHaplotypeCNVMethodListBox);
		
		//VerticalPanel vPanel = new VerticalPanel();
		//vPanel.add(outer);
		//protovisDiv.setId(visDivID);
		//HTML protovisDiv = new HTML("<div id='"+visDivID+ "'></div>");
		//vPanel.add(protovisDiv);
		//runVis(visDivID);
	}
	
	public void fillSelectBoxWithJsArray(JsArray jsonArrayData, ListBox listBox)
	{
		JSONObject jsonObject = new JSONObject(jsonArrayData);
		JSONArray jsonArray =  jsonObject.get("options").isArray();
		Common.fillSelectBox(jsonArray, listBox);
	}
	
	public static native void addOverviewInJS(String divID, String title, String url, String fetchGeneModelURL)
	/*-{
		$wnd.initializeContextsByFetchingData(divID, title, url, fetchGeneModelURL, $wnd.width, $wnd.overviewHeight, $wnd.imageURL);
		}-*/;
	
	public native JsArray cnvCallCNVMethodLsJsonFromJS() /*-{ return $wnd.cnvCallCNVMethodLsJson; }-*/;
	public native JsArray cnvFrequencyCNVMethodLsJsonFromJS() /*-{ return $wnd.cnvFrequencyCNVMethodLsJson; }-*/;
	public native JsArray smoothTypeLsJsonFromJS() /*-{ return $wnd.smoothTypeLsJson; }-*/;
	public native JsArray plotTypeLsJsonFromJS() /*-{ return $wnd.plotTypeLsJson; }-*/;
	
	public native String getChromsomeLsJsonFromTableCNVCallURLFromJS()/*-{ return $wnd.getChromsomeLsJsonFromTableCNVCallURL; }-*/;
	public native String getChromsomeLsJsonFromTableCNVURLFromJS() /*-{ return $wnd.getChromsomeLsJsonFromTableCNVURL; }-*/;
	public native String getFrequencyOverviewDataJsonFromTableCNVURLFromJS() /*-{ return $wnd.getFrequencyOverviewDataJsonFromTableCNVURL; }-*/;
	public native String getGeneModelDataJsonURLFromJS() /*-{ return $wnd.getGeneModelDataJsonURL; }-*/;
	public native String getHaplotypeDataJsonFromTableCNVCallURLFromJS() /*-{ return $wnd.getHaplotypeDataJsonFromTableCNVCallURL; }-*/;
	
	
	@UiHandler(value={"cnvFrequencyCNVMethodListBox", "cnvFrequencyChromosomeListBox", "cnvCallHaplotypeCNVMethodListBox"})
	void onChangeListBox(ChangeEvent event)
	{
		ListBox source = (ListBox)event.getSource();
		
		if (source == cnvFrequencyCNVMethodListBox)
			loadCNVFrequencyChromosomeListBox();
		else if (source ==cnvFrequencyChromosomeListBox)
			setCNVFrequencySubmitButtonStatus();
		else if (source==cnvCallHaplotypeCNVMethodListBox)
			setCNVCallHaplotypeSubmitButtonStatus();
	}
	
	@UiHandler("cnvFrequencySubmitButton")
	void onClickCNVFrequencySubmitButton(ClickEvent event)
	{
		chromosome = cnvFrequencyChromosomeListBox.getValue(cnvFrequencyChromosomeListBox.getSelectedIndex());
		String cnv_method_id = cnvFrequencyCNVMethodListBox.getValue(cnvFrequencyCNVMethodListBox.getSelectedIndex());
		String smooth_type_id = cnvFrequencySmoothTypeListBox.getValue(cnvFrequencySmoothTypeListBox.getSelectedIndex());
		
		String url = URL.encode(getFrequencyOverviewDataJsonFromTableCNVURL + "?cnv_method_id="+ cnv_method_id + "&chromosome=" + chromosome);
		String fetchGeneModelURL = URL.encode(getGeneModelDataJsonURL + "?chromosome=" + chromosome);
		number_of_divs ++;
		
		String divID = "gwt_widget"+number_of_divs.toString();
		//DivElement div = new DivElement();
		//div.setId(divID);
		Document doc = Document.get();
		DivElement div = doc.createDivElement();
		div.setId(divID);
		//HTML div = new HTML("<div id='"+divID+ "'></div>");
		protovisPanel.getElement().appendChild(div);
		
		Integer plot_type_index = cnvFrequencyPlotTypeListBox.getSelectedIndex();
		String title = "Deletion Frequency of method "+cnv_method_id + " chromosome " + chromosome;
		if (plot_type_index==0){
			url = url + "&smooth_type_id="+smooth_type_id;	//to smooth the full data for the top global overview 
			addOverviewInJS(divID, title, url, fetchGeneModelURL);
		}
		else if (plot_type_index==1){
			addCNVFrequencyToLastOverview(divID, title, url);
		}
	}
	
	@UiHandler("cnvCallHaplotypeSubmitButton")
	void onClickCNVCallHaplotypeSubmitButton(ClickEvent event)
	{
		String cnv_method_id = cnvCallHaplotypeCNVMethodListBox.getValue(cnvCallHaplotypeCNVMethodListBox.getSelectedIndex());
		
		String fetchURL = URL.encode(getHaplotypeDataJsonFromTableCNVCallURL + "?cnv_method_id="+ cnv_method_id + "&chromosome=" + chromosome);
		number_of_divs ++;
		String divID = "gwt_widget"+number_of_divs.toString();
		HTML div = new HTML("<div id='"+divID+ "'></div>");
		protovisPanel.add(div);
		String title = "Deletion Haplotype of method " + cnv_method_id;
		addCNVCallHaplotypeToLastOverview(divID, title, fetchURL);
	}
	
	private void setCNVFrequencySubmitButtonStatus()
	{
		if (cnvFrequencyCNVMethodListBox.getSelectedIndex() > 0 && cnvFrequencyChromosomeListBox.getSelectedIndex() > 0)
		{
			cnvFrequencySubmitButton.setEnabled(true);
		}
		else
			cnvFrequencySubmitButton.setEnabled(false);
	}
	
	private void setCNVCallHaplotypeSubmitButtonStatus()
	{
		if (cnvCallHaplotypeCNVMethodListBox.getSelectedIndex() > 0 && chromosome.length()>0)
		{
			cnvCallHaplotypeSubmitButton.setEnabled(true);
		}
		else
			cnvCallHaplotypeSubmitButton.setEnabled(false);
	}
	
	private void fillListBox(String responseText, ListBox box)
	{
		box.clear();
		JSONValue jsonValue = JSONParser.parse(responseText);
		JSONObject jsonObject = jsonValue.isObject();
		JSONArray jsonArray = jsonObject.get("options").isArray();
		Common.fillSelectBox(jsonArray, box);
	}
	
	private void loadCNVFrequencyChromosomeListBox() 
	{
		String url = URL.encode(getChromsomeLsJsonFromTableCNVURL + "?cnv_method_id="+ 
				cnvFrequencyCNVMethodListBox.getValue(cnvFrequencyCNVMethodListBox.getSelectedIndex()));
		RequestBuilder requestBuilder = new RequestBuilder(RequestBuilder.GET, url);
		try {
			requestBuilder.sendRequest(null, new RequestCallback()
			{
				public void onResponseReceived(Request request, Response response) {
					if (200 == response.getStatusCode())
					{
						fillListBox(response.getText(), cnvFrequencyChromosomeListBox);
					}
				}
				@Override
				public void onError(Request request, Throwable exception) {
					
				}
			}
			
			);
		} catch (RequestException ex) {
			
		}
	}
	
	public static native void addCNVFrequencyToLastOverview(String divID, String title, String fetchURL)/*-{
		var data = [];
		var widget = new $wnd.contextPanel(divID, title, fetchURL, data, null, null, 0, 0, null, null, 
			$wnd.width, $wnd.overviewHeight*2, null, $wnd.imageURL);
		$wnd.addCrossHair(widget);
		widget.addPlot();
		$wnd.addWidgetAsFocusChildToLastContext(widget);
	}-*/;
	
	public static native void addCNVCallHaplotypeToLastOverview(String divID, String title, String fetchURL)/*-{
		var rowInfoData = null;
		var data = [];
		var widget = new $wnd.haplotypePanel(divID, title, fetchURL, rowInfoData, data, null, null, 0, 1,
			$wnd.width, $wnd.overviewHeight*10, $wnd.imageURL);
		$wnd.addWidgetAsFocusChildToLastContext(widget);
	}-*/;
	
	public static native JavaScriptObject parseJson(String jsonStr) /*-{
	  return eval(jsonStr);
	}-*/;
}
