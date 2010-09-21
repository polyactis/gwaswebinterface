package edu.nordborglab.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.event.dom.client.ChangeEvent;
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
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.uibinder.client.UiHandler;

import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiFactory;
import com.google.gwt.core.client.JavaScriptObject;

public class ViewCNV extends Composite  {
	
	interface Binder extends UiBinder<CustomVerticalPanel, ViewCNV> { }
	private static final Binder binder = GWT.create(Binder.class);
	
	@UiField ListBox cnvMethodListBox;
	@UiField ListBox chromosomeListBox;
	@UiField DivElement protovisDiv;
	@UiField HTMLPanel protovisPanel;
	
	private String ChromsomeLsJsonURL;
	private String FullDataJsonURL;
	public String visDivID = "protovisDiv";
	
	private DisplayJSONObject jsonErrorDialog = new DisplayJSONObject("Error Dialog");
	private AccessionConstants constants = (AccessionConstants) GWT.create(AccessionConstants.class);
	
	/** Used by MyUiBinder to instantiate CricketScores */
	@UiFactory CustomVerticalPanel makeCustomVerticalPanel() { // method name is insignificant
		return new CustomVerticalPanel(constants, jsonErrorDialog, constants.HaplotypeSingleViewHelpID());
	}
	
	ViewCNV() {
		super();
		initWidget(binder.createAndBindUi(this));
		
		
		JSONObject cnvMethodList = new JSONObject(this.getCNVMethodList());
		JSONArray cnvMethodOptions = cnvMethodList.get("options").isArray();
		for (int i = 0;i< cnvMethodOptions.size();i++)
		{
			JSONObject idValueDict= cnvMethodOptions.get(i).isObject();
			String item = idValueDict.get("id").isString().stringValue();
			Double value = idValueDict.get("value").isNumber().doubleValue();
			Integer valueInt = value.intValue();
			this.cnvMethodListBox.addItem(item, valueInt.toString());
		}
		
		ChromsomeLsJsonURL = this.getChromsomeLsJsonURL();
		FullDataJsonURL = this.getFullDataJsonURL();
		//VerticalPanel vPanel = new VerticalPanel();
		//vPanel.add(outer);
		protovisDiv.setId(visDivID);
		//HTML protovisDiv = new HTML("<div id='"+visDivID+ "'></div>");
		//vPanel.add(protovisDiv);
		//runVis(visDivID);
	}
	
	public static native void runVis(String divID, String fullData) /*-{ var fullData = eval("("+fullData+")");
	$wnd.zoomTest(divID, fullData); }-*/;
	
	public native JsArray getCNVMethodList()/*-{ return $wnd.cnv_method_ls_json; }-*/;
	public native String getChromsomeLsJsonURL()/*-{ return $wnd.getChromsomeLsJsonURL; }-*/;
	public native String getFullDataJsonURL()/*-{ return $wnd.getFullDataJsonURL; }-*/;
	
	@UiHandler(value={"cnvMethodListBox", "chromosomeListBox"})
	void onChangeListBox(ChangeEvent event)
	{
		ListBox source = (ListBox)event.getSource();
		
		if (source == cnvMethodListBox)
			loadChromosomeListBox();
		else if (source==chromosomeListBox)
			redrawProtovis();
	}
	
	private void fillListBox(String responseText,ListBox box)
	{
		box.clear();
		JSONValue jsonValue = JSONParser.parse(responseText);
		JSONObject jsonObject = jsonValue.isObject();
		JSONArray jsonArray = jsonObject.get("options").isArray();
		Common.fillSelectBox(jsonArray, box);
	}
	
	private void loadChromosomeListBox() 
	{
		String url = URL.encode(ChromsomeLsJsonURL + "?cnv_method_id="+cnvMethodListBox.getValue(cnvMethodListBox.getSelectedIndex()));
		RequestBuilder requestBuilder = new RequestBuilder(RequestBuilder.GET, url);
		try {
			requestBuilder.sendRequest(null, new RequestCallback()
			{
				public void onResponseReceived(Request request, Response response) {
					if (200 == response.getStatusCode())
					{
						fillListBox(response.getText(), chromosomeListBox);
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
	
	private void redrawProtovis()
	{
		String chromosome = chromosomeListBox.getValue(chromosomeListBox.getSelectedIndex());
		String url = URL.encode(FullDataJsonURL + "?cnv_method_id="+cnvMethodListBox.getValue(cnvMethodListBox.getSelectedIndex())
					+ "&chromosome=" + chromosome );
		RequestBuilder requestBuilder = new RequestBuilder(RequestBuilder.GET, url);
		try {
			requestBuilder.sendRequest(null, new RequestCallback()
			{
				public void onResponseReceived(Request request, Response response) {
					if (200 == response.getStatusCode())
					{
						//JSONValue jsonValue = JSONParser.parse(response.getText());
						//JSONObject jsonObject = jsonValue.isObject();
						//JSONArray deletionData = jsonObject.get("deletionData").isArray();
						//JavaScriptObject geneModelData = parseJson(jsonObject.get("geneModelData").isString().toString());
						runVis(visDivID, response.getText());
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
	
	public static native JavaScriptObject parseJson(String jsonStr) /*-{
	  return eval(jsonStr);
	}-*/;
}
