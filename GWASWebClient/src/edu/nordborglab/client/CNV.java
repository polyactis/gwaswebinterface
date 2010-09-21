package edu.nordborglab.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.user.client.ui.HTML;

import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;

public class CNV implements EntryPoint {
	
	//interface Binder extends UiBinder<ViewCNV, CNV> { }
	//private static final Binder binder = GWT.create(Binder.class);
	
	
	
	public void onModuleLoad() {
		//ViewCNV outer = binder.createAndBindUi(this);
		RootLayoutPanel root = RootLayoutPanel.get();
		
		ViewCNV  outer = new ViewCNV(); 
		//VerticalPanel vPanel = new VerticalPanel();
		//vPanel.add(outer);

		//RootPanel.get("gwt").add(outer);
		
		//RootLayoutPanel root = RootLayoutPanel.get();
		//root.add(vPanel);
		root.add(outer);
	}
	
}
