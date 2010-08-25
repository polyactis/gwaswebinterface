package edu.nordborglab.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.user.client.ui.TabLayoutPanel;

import com.google.gwt.uibinder.client.UiBinder;

public class Comparison implements EntryPoint {
 
	interface Binder extends UiBinder<TabLayoutPanel, Comparison> { }
	private static final Binder binder = GWT.create(Binder.class);



	@Override
	public void onModuleLoad() {
		TabLayoutPanel outer = binder.createAndBindUi(this);
		RootLayoutPanel root = RootLayoutPanel.get();
	    root.add(outer);
	}

}
