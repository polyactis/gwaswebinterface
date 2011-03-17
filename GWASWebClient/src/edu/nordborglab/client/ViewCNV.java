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
	
	// cnv frequency or fractionDeletedInLyrata
	@UiField ListBox cnvDataTypeListBox;
	@UiField ListBox cnvFrequencyCNVMethodListBox;
	@UiField ListBox cnvFrequencyChromosomeListBox;
	@UiField ListBox cnvFrequencySmoothTypeListBox;
	@UiField ListBox cnvFrequencyPlotTypeListBox;
	@UiField Button cnvFrequencySubmitButton;
	
	// haplotype
	@UiField ListBox haplotypeDataTypeListBox;
	@UiField ListBox haplotypeCNVMethodListBox;
	@UiField ListBox haplotypeCNVTypeListBox;
	@UiField ListBox haplotypeSortByListBox;
	@UiField Button haplotypeSubmitButton;
	
	@UiField FlowPanel protovisPanel;
	
	// probe position
	@UiField ListBox probeTypeListBox;
	@UiField ListBox tilingProbePlotTypeListBox;
	@UiField ListBox tilingProbeChromosomeListBox;
	@UiField ListBox tilingProbeSmoothTypeListBox;
	@UiField Button tilingProbeSubmitButton;
	
	//Genome-Wide-result
	@UiField ListBox GWRMethodListBox;
	@UiField ListBox GWRPlotTypeListBox;
	@UiField ListBox GWRChromosomeListBox;
	@UiField ListBox GWRSmoothTypeListBox;
	@UiField Button GWRSubmitButton;
	
	// structural change (SC) for single accession
	@UiField ListBox SCDataTypeListBox;
	@UiField ListBox SCCNVMethodListBox;
	@UiField ListBox SCAccessionListBox;
	@UiField ListBox SCYValueListBox;
	@UiField ListBox SCPlotTypeListBox;
	@UiField ListBox SCChromosomeListBox;
	@UiField ListBox SCSmoothTypeListBox;
	@UiField Button SCSubmitButton;
	
	// GWAS
	@UiField ListBox GWASDataTypeListBox;
	@UiField ListBox GWASCallMethodListBox;
	@UiField ListBox GWASPhenotypeListBox;
	@UiField ListBox GWASAnalysisMethodListBox;
	@UiField ListBox GWASPlotTypeListBox;
	@UiField ListBox GWASChromosomeListBox;
	@UiField ListBox GWASSmoothTypeListBox;
	@UiField Button GWASSubmitButton;
	
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
		
		// cnv frequency or fractionDeletedInLyrata
		fillSelectBoxWithJsArray(this.dataTypeOptionLsJson(), cnvDataTypeListBox);
		fillSelectBoxWithJsArray(this.cnvFrequencyCNVMethodLsJsonFromJS(), cnvFrequencyCNVMethodListBox);
		fillSelectBoxWithJsArray(this.smoothTypeLsJsonFromJS(), cnvFrequencySmoothTypeListBox);
		fillSelectBoxWithJsArray(this.plotTypeLsJsonFromJS(), cnvFrequencyPlotTypeListBox);
		
		//fillSelectBoxWithJsArray(this.cnvCallCNVMethodLsJsonFromJS(), haplotypeCNVMethodListBox);
		
		// haplotype
		fillSelectBoxWithJsArray(this.haplotypeDataTypeListJson(), haplotypeDataTypeListBox);
		
		// probe position
		fillSelectBoxWithJsArray(this.probeTypeListJson(), probeTypeListBox);
		fillSelectBoxWithJsArray(this.plotTypeLsJsonFromJS(), tilingProbePlotTypeListBox);
		fillSelectBoxWithJsArray(this.chromosomeOptionLsJson(), tilingProbeChromosomeListBox);
		fillSelectBoxWithJsArray(this.smoothTypeLsJsonFromJS(), tilingProbeSmoothTypeListBox);
		
		// GWAS
		fillSelectBoxWithJsArray(this.callMethodLsJson(), GWASCallMethodListBox);
		fillSelectBoxWithJsArray(this.plotTypeLsJsonFromJS(), GWASPlotTypeListBox);
		fillSelectBoxWithJsArray(this.chromosomeOptionLsJson(), GWASChromosomeListBox);
		fillSelectBoxWithJsArray(this.smoothTypeLsJsonFromJS(), GWASSmoothTypeListBox);
		
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
	//cnv frequency or fractionDeletedInLyrata
	public native JsArray dataTypeOptionLsJson() /*-{ return $wnd.dataTypeOptionLsJson; }-*/;
	public native JsArray cnvFrequencyCNVMethodLsJsonFromJS() /*-{ return $wnd.cnvFrequencyCNVMethodLsJson; }-*/;
	public native String getChromsomeLsJsonFromTableCNVURL() /*-{ return $wnd.getChromsomeLsJsonFromTableCNVURL; }-*/;
	public native String getFrequencyOverviewDataJsonFromTableCNVURL() /*-{ return $wnd.getFrequencyOverviewDataJsonFromTableCNVURL; }-*/;

	public native String getGeneModelDataJsonURL() /*-{ return $wnd.getGeneModelDataJsonURL; }-*/;

	public native JsArray smoothTypeLsJsonFromJS() /*-{ return $wnd.smoothTypeLsJson; }-*/;
	public native JsArray plotTypeLsJsonFromJS() /*-{ return $wnd.plotTypeLsJson; }-*/;
	public native JsArray chromosomeOptionLsJson() /*-{ return $wnd.chromosomeOptionLsJson; }-*/;
	//haplotype
	public native JsArray cnvCallCNVMethodLsJsonFromJS() /*-{ return $wnd.cnvCallCNVMethodLsJson; }-*/;
	public native JsArray haplotypeDataTypeListJson() /*-{ return $wnd.haplotypeDataTypeListJson; }-*/;
	public native String getCNVMethodLsJsonURL() /*-{ return $wnd.getCNVMethodLsJsonURL; }-*/;
	public native String getCNVTypeLsJsonURL() /*-{ return $wnd.getCNVTypeLsJsonURL; }-*/;
	public native String getHaplotypeDataJsonURL() /*-{ return $wnd.getHaplotypeDataJsonURL; }-*/;
	
	// probe position
	public native JsArray probeTypeListJson() /*-{ return $wnd.probeTypeListJson; }-*/;
	public native String getTilingProbeDataJsonURL() /*-{ return $wnd.getTilingProbeDataJsonURL; }-*/;
	
	// gwas
	public native JsArray callMethodLsJson() /*-{ return $wnd.callMethodLsJson; }-*/;
	public native String getPhenotypeMethodLsJsonURL() /*-{ return $wnd.getPhenotypeMethodLsJsonURL; }-*/;
	public native String getAnalysisMethodLsJsonURL() /*-{ return $wnd.getAnalysisMethodLsJsonURL; }-*/;
	public native String getGWASDataJsonURL() /*-{ return $wnd.getGWASDataJsonURL; }-*/;
	
	@UiHandler(value={"cnvFrequencyCNVMethodListBox", "cnvFrequencyChromosomeListBox", "haplotypeCNVMethodListBox",
			"tilingProbePlotTypeListBox", "cnvFrequencyPlotTypeListBox", "haplotypeDataTypeListBox", "haplotypeCNVTypeListBox",
			"GWASCallMethodListBox", "GWASPhenotypeListBox", "GWASAnalysisMethodListBox"})
	void onChangeListBox(ChangeEvent event)
	{
		ListBox source = (ListBox)event.getSource();
		
		if (source == cnvFrequencyCNVMethodListBox)
			loadCNVFrequencyChromosomeListBox();
		else if (source ==cnvFrequencyChromosomeListBox)
			setCNVFrequencySubmitButtonStatus();
		else if (source==haplotypeDataTypeListBox)
			loadHaplotypeCNVMethodListBox();
		else if (source==haplotypeCNVMethodListBox)
			loadHaplotypeCNVTypeListBox();
		else if (source==haplotypeCNVTypeListBox)
			setHaplotypeSubmitButtonStatus();
		else if (source ==cnvFrequencyPlotTypeListBox)
		{
			Integer plot_type_id = cnvFrequencyPlotTypeListBox.getSelectedIndex()+1;
			if (plot_type_id==1)
			{
				cnvFrequencySmoothTypeListBox.setEnabled(true);
				cnvFrequencyChromosomeListBox.setEnabled(true);
			}
			else if (plot_type_id==3)
			{
				cnvFrequencySmoothTypeListBox.setEnabled(true);
				cnvFrequencyChromosomeListBox.setEnabled(false);
			}
			else
			{
				cnvFrequencySmoothTypeListBox.setEnabled(false);
				cnvFrequencyChromosomeListBox.setEnabled(false);
			}
		}
		else if (source==tilingProbePlotTypeListBox)
		{
			Integer plot_type_id = tilingProbePlotTypeListBox.getSelectedIndex()+1;
			if (plot_type_id==1)
			{
				tilingProbeSmoothTypeListBox.setEnabled(true);
				tilingProbeChromosomeListBox.setEnabled(true);
			}
			else if (plot_type_id==3)
			{
				tilingProbeSmoothTypeListBox.setEnabled(true);
				tilingProbeChromosomeListBox.setEnabled(false);
			}
			else
			{
				tilingProbeSmoothTypeListBox.setEnabled(false);
				tilingProbeChromosomeListBox.setEnabled(false);
			}
		}
		else if (source==GWASCallMethodListBox)
		{
			loadGWASPhenotypeListBox();
		}
		else if (source==GWASPhenotypeListBox)
		{
			loadGWASAnalysisMethodListBox();
		}
		else if (source==GWASAnalysisMethodListBox)
		{
			setGWASSubmitButtonStatus();
		}
	}
	
	@UiHandler("cnvFrequencySubmitButton")
	void onClickCNVFrequencySubmitButton(ClickEvent event)
	{
		chromosome = cnvFrequencyChromosomeListBox.getValue(cnvFrequencyChromosomeListBox.getSelectedIndex());
		String cnv_method_id = cnvFrequencyCNVMethodListBox.getValue(cnvFrequencyCNVMethodListBox.getSelectedIndex());
		String smooth_type_id = cnvFrequencySmoothTypeListBox.getValue(cnvFrequencySmoothTypeListBox.getSelectedIndex());
		String data_type_id = cnvDataTypeListBox.getValue(cnvDataTypeListBox.getSelectedIndex());
		String url = URL.encode(this.getFrequencyOverviewDataJsonFromTableCNVURL() + "?cnv_method_id="+ cnv_method_id + "&chromosome=" + chromosome
				+"&data_type_id="+data_type_id);
		String fetchGeneModelURL = URL.encode(this.getGeneModelDataJsonURL() + "?chromosome=" + chromosome);
		
		Integer plot_type_id = cnvFrequencyPlotTypeListBox.getSelectedIndex() +1;
		String title = "Deletion Frequency of method "+cnv_method_id + " chromosome " + chromosome;
		
		String divID = addNewDivToProtovisPanel();
		if (plot_type_id==1){
			url = url + "&smooth_type_id="+smooth_type_id;	//to smooth the full data for the top global overview 
			addOverviewInJS(divID, title, url, fetchGeneModelURL);
		}
		else if (plot_type_id==2){
			addCNVFrequencyToLastOverview(divID, title, url);
		}
	}
	
	@UiHandler("haplotypeSubmitButton")
	void onClickHaplotypeSubmitButton(ClickEvent event)
	{
		String cnv_method_id = Common.getSelectedValueInListBox(haplotypeCNVMethodListBox);
		String cnv_type_id = Common.getSelectedValueInListBox(haplotypeCNVTypeListBox);
		String table_name = Common.getSelectedItemTextInListBox(haplotypeDataTypeListBox);
		String fetchURL = URL.encode(this.getHaplotypeDataJsonURL() + "?table_name="+ table_name + "&cnv_method_id="+ 
				cnv_method_id + "&cnv_type_id=" + cnv_type_id + "&chromosome=" + chromosome);
		String divID = addNewDivToProtovisPanel();
		String title = "Haplotype of method " + cnv_method_id + " type " + cnv_type_id;
		addCNVCallHaplotypeToLastOverview(divID, title, fetchURL);
	}
	
	
	@UiHandler("tilingProbeSubmitButton")
	void onClickTilingProbeSubmitButton(ClickEvent event)
	{
		String data_type = Common.getSelectedValueInListBox(probeTypeListBox);
		String data_type_name = Common.getSelectedItemTextInListBox(probeTypeListBox);
		String smooth_type_id = Common.getSelectedValueInListBox(tilingProbeSmoothTypeListBox);
		Integer plot_type_id = tilingProbePlotTypeListBox.getSelectedIndex()+1;
		String fetchURL = this.getTilingProbeDataJsonURL() + "?data_type="+ data_type;
		String divID = addNewDivToProtovisPanel();
		String title = data_type_name + " position";
		if (plot_type_id==1){
			chromosome = Common.getSelectedValueInListBox(tilingProbeChromosomeListBox);
			//to smooth the full data for the top global overview 
			fetchURL = URL.encode(fetchURL + "&chromosome="+ chromosome + "&smooth_type_id="+smooth_type_id);
			String fetchGeneModelURL = URL.encode(this.getGeneModelDataJsonURL() + "?chromosome=" + chromosome);
			addOverviewInJS(divID, title, fetchURL, fetchGeneModelURL);
		}
		else if (plot_type_id==2){
			if (chromosome=="")
			{
				jsonErrorDialog.displayParseError("Chromosome not chosen by previous overview. Probably no overview has been generated yet.");
			}
			else
			{
				fetchURL = URL.encode(fetchURL + "&chromosome=" + chromosome);	// chromosome is global value
				addTilingProbePositonToLastOverview(divID, title, fetchURL);
			}
		}
		
	}
	
	@UiHandler("GWASSubmitButton")
	void onClickGWASSubmitButton(ClickEvent event)
	{
		String call_method_id = Common.getSelectedValueInListBox(GWASCallMethodListBox);
		String phenotype_method_id = Common.getSelectedItemTextInListBox(GWASPhenotypeListBox);
		String analysis_method_id = Common.getSelectedItemTextInListBox(GWASAnalysisMethodListBox);
		String smooth_type_id = Common.getSelectedValueInListBox(tilingProbeSmoothTypeListBox);
		Integer plot_type_id = GWASPlotTypeListBox.getSelectedIndex()+1;
		String fetchURL = this.getGWASDataJsonURL() + "?call_method_id="+ call_method_id + "&phenotype_method_id" + phenotype_method_id
			+"&analysis_method_id="+analysis_method_id;
		String divID = addNewDivToProtovisPanel();
		String title = "GWAS" + " position";
		if (plot_type_id==1){
			chromosome = Common.getSelectedValueInListBox(GWASChromosomeListBox);
			//to smooth the full data for the top global overview 
			fetchURL = URL.encode(fetchURL + "&chromosome="+ chromosome + "&smooth_type_id="+smooth_type_id);
			String fetchGeneModelURL = URL.encode(this.getGeneModelDataJsonURL() + "?chromosome=" + chromosome);
			addOverviewInJS(divID, title, fetchURL, fetchGeneModelURL);
		}
		else if (plot_type_id==2){
			if (chromosome=="")
			{
				jsonErrorDialog.displayParseError("Chromosome not chosen by previous overview. Probably no overview has been generated yet.");
			}
			else
			{
				fetchURL = URL.encode(fetchURL + "&chromosome=" + chromosome);	// chromosome is global value
				addTilingProbePositonToLastOverview(divID, title, fetchURL);
			}
		}
		
	}
	
	private String addNewDivToProtovisPanel(){
		/*
		 * 2010-10-18 create a new div element based on number_of_divs and return its id
		 */
		number_of_divs ++;
		//DivElement div = new DivElement();	//2010-10-19 doesn't work.
		String divID = "gwt_widget"+number_of_divs.toString();
		HTML div = new HTML("<div id='"+divID+ "'></div>");
		protovisPanel.add(div);
		/*
		 * a different way of creating a div element
		Document doc = Document.get();
		DivElement div = doc.createDivElement();
		div.setId(divID);
		protovisPanel.getElement().appendChild(div);
		 */
		return divID;
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
	
	private void setHaplotypeSubmitButtonStatus()
	{
		if (haplotypeDataTypeListBox.getSelectedIndex()>0 && haplotypeCNVMethodListBox.getSelectedIndex() > 0 && 
				haplotypeCNVTypeListBox.getSelectedIndex() > 0 && chromosome.length()>0)
		{
			haplotypeSubmitButton.setEnabled(true);
		}
		else
			haplotypeSubmitButton.setEnabled(false);
	}
	
	private void setGWASSubmitButtonStatus()
	{
		if (GWASCallMethodListBox.getSelectedIndex()>0 && GWASPhenotypeListBox.getSelectedIndex() > 0 && 
				GWASAnalysisMethodListBox.getSelectedIndex() > 0 && chromosome.length()>0)
		{
			GWASSubmitButton.setEnabled(true);
		}
		else
			GWASSubmitButton.setEnabled(false);
	}
	
	private void loadCNVFrequencyChromosomeListBox()
	{
		String cnv_method_id = Common.getSelectedValueInListBox(cnvFrequencyCNVMethodListBox);
		String url = this.getChromsomeLsJsonFromTableCNVURL() + "?cnv_method_id="+ cnv_method_id;
		Common.fillSelectBox(url, cnvFrequencyChromosomeListBox, jsonErrorDialog);
	}
	private void loadHaplotypeCNVMethodListBox() 
	{
		String table_name = Common.getSelectedItemTextInListBox(haplotypeDataTypeListBox);
		String url = this.getCNVMethodLsJsonURL() + "?table_name="+ table_name;
		Common.fillSelectBox(url, haplotypeCNVMethodListBox, jsonErrorDialog);
		
	}
	
	private void loadHaplotypeCNVTypeListBox()
	{
		String cnv_method_id = Common.getSelectedValueInListBox(haplotypeCNVMethodListBox);
		String table_name = Common.getSelectedItemTextInListBox(haplotypeDataTypeListBox);
		String url = this.getCNVTypeLsJsonURL() + "?table_name="+ table_name + "&cnv_method_id="+ cnv_method_id;
		Common.fillSelectBox(url, haplotypeCNVTypeListBox, jsonErrorDialog);
	}
	private void loadGWASPhenotypeListBox()
	{
		String call_method_id = Common.getSelectedValueInListBox(GWASCallMethodListBox);
		String url = this.getPhenotypeMethodLsJsonURL() + "?call_method_id="+ call_method_id;
		Common.fillSelectBox(url, GWASPhenotypeListBox, jsonErrorDialog);
	}
	private void loadGWASAnalysisMethodListBox()
	{
		String call_method_id = Common.getSelectedValueInListBox(GWASCallMethodListBox);
		String phenotype_method_id = Common.getSelectedValueInListBox(GWASPhenotypeListBox);
		String url = this.getPhenotypeMethodLsJsonURL() + "?call_method_id="+ call_method_id +
			"&phenotype_method_id="+phenotype_method_id;
		Common.fillSelectBox(url, GWASAnalysisMethodListBox, jsonErrorDialog);
	}
	
	public static native void addOverviewInJS(String divID, String title, String url, String fetchGeneModelURL)
	/*-{
		$wnd.initializeContextsByFetchingData(divID, title, url, fetchGeneModelURL, $wnd.width, $wnd.overviewHeight, $wnd.imageURL);
		}-*/;
	
	
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
	
	public static native void addTilingProbePositonToLastOverview(String divID, String title, String fetchURL)/*-{
	var data = [];
	var originalDataStart = null;
	var originalDataStop = null;
	var widget = new $wnd.tickPanel(divID, title, fetchURL, data, originalDataStart, originalDataStop,
		$wnd.width, $wnd.overviewHeight/2, $wnd.imageURL);
	$wnd.addWidgetAsFocusChildToLastContext(widget);
	}-*/;
	
	public static native JavaScriptObject parseJson(String jsonStr) /*-{
	  return eval(jsonStr);
	}-*/;
}
