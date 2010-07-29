package edu.nordborglab.client;

import com.google.gwt.ajaxloader.client.Properties.TypeException;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.AnchorElement;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
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
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Hyperlink;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.visualization.client.DataTable;
import com.google.gwt.visualization.client.Selection;
import com.google.gwt.visualization.client.VisualizationUtils;
import com.google.gwt.visualization.client.AbstractDataTable.ColumnType;
import com.google.gwt.visualization.client.events.OnMouseOverHandler;
import com.google.gwt.visualization.client.events.SelectHandler;
import com.google.gwt.visualization.client.events.OnMouseOverHandler.OnMouseOverEvent;
import com.google.gwt.visualization.client.events.SelectHandler.SelectEvent;
import com.google.gwt.visualization.client.visualizations.ColumnChart;
import com.google.gwt.visualization.client.visualizations.Table;

import edu.nordborglab.client.visualizations.BioHeatMap;
import edu.nordborglab.client.visualizations.OnMouseMoveHandler;
import edu.nordborglab.client.visualizations.BioHeatMap.Options;
import edu.nordborglab.client.visualizations.OnMouseMoveHandler.OnMouseMoveEvent;

public class ComparisonHeatMap extends Composite {

	private static ComparisonHeatMapUiBinder uiBinder = GWT
			.create(ComparisonHeatMapUiBinder.class);

	interface ComparisonHeatMapUiBinder extends
			UiBinder<Widget, ComparisonHeatMap> {
	}

	private DataTable dataTable = null;
	
	@UiField BioHeatMap bioHeatMap;
	@UiField ListBox heatmap_file;
	@UiField Button submitButton;
	@UiField ListBox call_method_listbox;
	@UiField ListBox phenotype_listbox;
	
	@UiField AnchorElement snp1;
	@UiField AnchorElement snp2;
	@UiField SpanElement pvalue;
	

	public ComparisonHeatMap() {
		initWidget(uiBinder.createAndBindUi(this));
		
		filterHeatMapListbox();
		
		JSONObject callMethodList = new JSONObject(this.getCallMethodList());
		JSONArray callMethodOptions = callMethodList.get("options").isArray();
		for (int i = 0;i< callMethodOptions.size();i++)
		{
			JSONObject idValueDict= callMethodOptions.get(i).isObject();
			String item = idValueDict.get("id").isString().stringValue();
			Double value = idValueDict.get("value").isNumber().doubleValue();
			Integer valueInt = value.intValue();
		    this.call_method_listbox.addItem(item,valueInt.toString());
		}
		
		
		bioHeatMap.addSelectHandler(new SelectHandler(){

			@Override
			public void onSelect(SelectEvent event) {
				JsArray<Selection> selectionLs = bioHeatMap.getSelections();
				for (int i=0; i< selectionLs.length(); i++ )
				{
					Selection s = selectionLs.get(i);
					int col = s.getColumn();
					int row = s.getRow();
					String snp1_text = dataTable.getColumnLabel(col+1);
					String snp2_text = dataTable.getValueString(row, 0);
					Double value = dataTable.getValueDouble(row, col+1);
					String url = "/SNP/?score=1";
					url = url +  getDataSetQueryString(heatmap_file.getItemText(heatmap_file.getSelectedIndex()));
					snp1.setInnerText(snp1_text);
					snp1.setHref(url+getSNPQueryString(snp1_text));
					snp2.setInnerText(snp2_text);
					snp2.setHref(url+getSNPQueryString(snp2_text));
					pvalue.setInnerText(value.toString());
				}
			}
			private String getDataSetQueryString(String filename)
			{
				String[] parts = filename.split("[_]");
				String call_method_id = parts[0];
				String phenotype_method_id = parts[1];
				String analysis_method_id = parts[2];
				return "&call_method_id="+call_method_id+"&phenotype_method_id="+phenotype_method_id+"&analysis_method_id="+analysis_method_id;
			}
			
			private String getSNPQueryString(String snp)
			{
				String[] parts = snp.split("[\\.\\.]");
				String chr = parts[0].substring(1);
				String pos = parts[2];
				return "&pos="+pos+"&chr="+chr;
			}
		}
		);
	}
	
	@UiHandler("submitButton")
	void onClickSubmit(ClickEvent event)
	{
		loadCharts();
	}
	
	@UiHandler(value={"call_method_listbox","phenotype_listbox"})
	void onChangeListBox(ChangeEvent event)
	{
		ListBox source = (ListBox)event.getSource();
		if (source == call_method_listbox)
			loadPhenotypeListbox();
		
		filterHeatMapListbox();
	}
	
	private void loadPhenotypeListbox()
	{
		String url = URL.encode("/DisplayResults/getPhenotypeMethodLsJson?call_method_id="+call_method_listbox.getValue(call_method_listbox.getSelectedIndex()));
		RequestBuilder requestBuilder = new RequestBuilder(RequestBuilder.GET, url);
		phenotype_listbox.setSelectedIndex(0);
		try {
			requestBuilder.sendRequest(null, new RequestCallback()
			{
				public void onResponseReceived(Request request, Response response) {
					if (200 == response.getStatusCode())
					{
						fillListBox(response.getText(),phenotype_listbox);
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
	
	private void filterHeatMapListbox()
	{
		String call_method_filter = "";
		String phenotype_filter = "";
		if (call_method_listbox.getSelectedIndex() > 0)
			call_method_filter = call_method_listbox.getValue(call_method_listbox.getSelectedIndex());

		if (phenotype_listbox.getSelectedIndex() > 0)
			phenotype_filter =  phenotype_listbox.getValue(phenotype_listbox.getSelectedIndex());
			
		JSONObject heatmap_info = new JSONObject(this.getHeatMapFiles());
		JSONArray heatmap_files = heatmap_info.get("files").isArray();
		heatmap_file.clear();
		String regex = "^";
		if (call_method_filter != "")
			regex+=call_method_filter;
		else
			regex+="\\d+";
		regex+="_";
		if (phenotype_filter!="")
			regex+=phenotype_filter;
		else
			regex+="\\d+";
		regex+="_\\d+_.+";
		for (int i = 0;i< heatmap_files.size();i++)
		{
			String file= heatmap_files.get(i).isString().stringValue();
			if (file.matches(regex) || i==0)
				this.heatmap_file.addItem(file,file);
		}
	}
	
	private void fillListBox(String responseText,ListBox box)
	{
		box.clear();
		JSONValue jsonValue = JSONParser.parse(responseText);
		JSONObject jsonObject = jsonValue.isObject();
		JSONArray phenotypeMethodLs = jsonObject.get("options").isArray();
		Common.fillSelectBox(phenotypeMethodLs,box);
	}
	
	private void loadCharts() {
		
		if (heatmap_file.getSelectedIndex() >0 )
		{
			String file = heatmap_file.getValue(heatmap_file.getSelectedIndex());
			String url = URL.encode("/Comparison/getBioHeatMapData?heatmap_file="+file);
			
			//submitButton.setEnabled(false);
			submitButton.setText("LOADING");
			RequestBuilder requestBuilder = new RequestBuilder(RequestBuilder.GET, url);
			
			try {
				requestBuilder.sendRequest(null, new RequestCallback()
				{
					public void onResponseReceived(Request request, Response response) {
						if (200 == response.getStatusCode())
						{
							submitButton.setEnabled(true);
							submitButton.setText("Submit");
							loadHeatMap(response.getText());
						}
					}
					@Override
					public void onError(Request request, Throwable exception) {
						submitButton.setEnabled(true);
						submitButton.setText("Submit");
					}
				}
				
				);
			} catch (RequestException ex) {
			}
		}
	}
	
	private void loadHeatMap(String data) {
		dataTable = Common.doubleJsonify2DataTable(data);
		bioHeatMap.draw(dataTable,createTableOptions());
	}
	
		
	
	private BioHeatMap.Options createTableOptions() {
		BioHeatMap.Options options = BioHeatMap.Options.create();
		options.setNumberOfColors(256);
		Options.Colors endColor = Options.Colors.create();
		Options.Colors startColor = Options.Colors.create();
		
		endColor.setColor(255,255, 0, 1);
		startColor.setColor(255,0,0,1);
		options.setEndColor(endColor);
		options.setStartColor(startColor);
		options.setFontSize(20);
		options.setPassThroughBlack(false);
		options.setMapHeight(2024);
		options.setMapWidth(2024);
		options.setCellBorder(true);
		return options;
	 }
	
	public native JsArray<JsArray> getHeatMapFiles()/*-{
		return $wnd.heatmap_files;
	}-*/;
	
	public native JsArray getCallMethodList()/*-{
		return $wnd.call_method_ls_json;
	}-*/;

}
