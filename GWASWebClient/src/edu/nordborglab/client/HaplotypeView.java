package edu.nordborglab.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;

import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class HaplotypeView implements EntryPoint, ClickHandler, ValueChangeHandler {

	/**
	 * This is the entry point method.
	 */
	private SinkList sinkList;
	public DisplayJSONObject jsonErrorDialog;
	public AccessionConstants constants;
	private RootPanel rootPanel;
	private int curSinkIndex=-1;
	
	private String callMethodLsURL;
	private String geneListLsURL;
	private String callMethodOnChangeURL;
	private String haplotypeImgURL;
	
	public void onModuleLoad() {
		jsonErrorDialog = new DisplayJSONObject("Error Dialog");
		constants = (AccessionConstants) GWT.create(AccessionConstants.class);

		sinkList = new SinkList(jsonErrorDialog);
		rootPanel = RootPanel.get("gwt");
		
		String initToken = History.getToken();
		if (initToken.length() > 0) {
			History.newItem("baz");
			//onHistoryChanged(initToken);
		}
		callMethodLsURL = getCallMethodLsURL();
		geneListLsURL = getGeneListLsURL();
		callMethodOnChangeURL = getCallMethodOnChangeURL();
		haplotypeImgURL = getHaplotypeImgURL();
		
		HaplotypeSingleView haplotypeSingleView = new HaplotypeSingleView(constants, jsonErrorDialog, callMethodLsURL,
				geneListLsURL, callMethodOnChangeURL, haplotypeImgURL);
		haplotypeSingleView.submitButton.addClickHandler(this);
		rootPanel.add(haplotypeSingleView);
		
		// Add history listener
		History.addValueChangeHandler(this);
		
		// Now that we've setup our listener, fire the initial history state.
		History.fireCurrentHistoryState();

	}
	public native String getCallMethodLsURL()/*-{ return $wnd.callMethodLsURL; }-*/;
	public native String getGeneListLsURL()/*-{ return $wnd.geneListLsURL; }-*/;
	public native String getCallMethodOnChangeURL()/*-{ return $wnd.callMethodOnChangeURL; }-*/;
	public native String getHaplotypeImgURL()/*-{ return $wnd.haplotypeImgURL; }-*/;
	
	
	public void onClick(ClickEvent event) {
		Sink oldSink = (Sink)rootPanel.getWidget(0);
		HaplotypeSingleView oldHaplotypeSingleView = (HaplotypeSingleView) oldSink;
		
		Button submitButton = (Button) event.getSource();	// 2010-9-19 use getSource() to get sender.
		if (submitButton.getText()== oldHaplotypeSingleView.SUBMIT_BUTTON_DEFAULT_TEXT)
		{
			String urlArguments = oldHaplotypeSingleView.constructFetchImageArguments();
			if (!urlArguments.isEmpty())	// remove the current and replace it with new one upon good urlArguments.
			{
				/*
				//oldHaplotypeSingleView.submitButton.removeClickListener(this);	//remove the click listener to avoid double clicking
				sinkList.addSink(oldSink);
				rootPanel.remove(0);
				// create a new one
				HaplotypeSingleView haplotypeSingleView = new HaplotypeSingleView(constants, jsonErrorDialog, callMethodLsURL,
						geneListLsURL, callMethodOnChangeURL, haplotypeImgURL);
				// pass old parameters to old one ??
				haplotypeSingleView.copySetting((HaplotypeSingleView)oldSink);
				haplotypeSingleView.submitButton.addClickHandler(this);
				//haplotypeSingleView.submitButton.setEnabled(false);
				curSinkIndex = sinkList.addSink(haplotypeSingleView);
				HaplotypeSingleView curSink = (HaplotypeSingleView) sinkList.getSelectedSink(curSinkIndex);
				*/
				HaplotypeSingleView curSink = oldHaplotypeSingleView;
				curSink.setSubmitButtonInProgress();
		
				String url = haplotypeImgURL + "?" + urlArguments;
				curSink.image.setUrl(url);
				curSink.image.setVisible(true);
				
				//rootPanel.add(curSink);
				//History.newItem(sinkList.getSinkName(curSinkIndex));
				//curSink.resetSubmitButtonCaption();
				//img.setVisibleRect(70, 0, 47, 110);
			}
		}
		else{
			if (submitButton.getText()== oldHaplotypeSingleView.SUBMIT_BUTTON_WAITING_TEXT)
			{
				oldHaplotypeSingleView.image.setUrl("");
				oldHaplotypeSingleView.image.setVisible(true);
				oldHaplotypeSingleView.resetSubmitButtonCaption();
			}
		}
	}

	public void onValueChange(ValueChangeEvent event) {
		// This method is called whenever the application's history changes. Set
		// the label to reflect the current history token.
		// lbl.setText("The current history token is: " + event.getValue());
		
		int i = sinkList.find(event.getValue().toString());
		if (i == -1) {
			//showInfo();
			//Window.alert("Couldn't find " + token);
			return;
		}
		//show(i, false);	#2010-9-20 disable history
	}

	public void onHistoryChanged(String token) {
		// This method is called whenever the application's history changes. Set
		// the label to reflect the current history token.
		//lbl.setText("The current history token is: " + historyToken);
		//	 Find the MapsDemoInfo associated with the history context. If one is
		// found, show it (It may not be found, for example, when the user mis-
		// types a URL, or on startup, when the first context will be "").

		//jsonErrorDialog.displayRequestError("The current history token is: " + token);

		int i = sinkList.find(token);
		if (i == -1) {
			//showInfo();
			//Window.alert("Couldn't find " + token);
			return;
		}
		show(i, false);
	}

	public void show(int selectedSink, boolean affectHistory) {
		// Don't bother re-displaying the existing MapsDemo. This can be an issue
		// in practice, because when the history context is set, our
		// onHistoryChanged() handler will attempt to show the currently-visible
		// MapsDemo.
		if (selectedSink == curSinkIndex) {
			return;
		}
		curSinkIndex = selectedSink;
		HaplotypeSingleView curSink = (HaplotypeSingleView) sinkList.getSelectedSink(curSinkIndex);
		curSink.submitButton.addClickHandler(this);
		rootPanel.add((Sink) curSink);

		// If affectHistory is set, create a new item on the history stack. This
		// will ultimately result in onHistoryChanged() being called. It will call
		// show() again, but nothing will happen because it will request the exact
		// same MapsDemo we're already showing.
		if (affectHistory) {
			History.newItem(sinkList.getSinkName(curSinkIndex));
		}
		//curSink.onShow();
	}
	
	/*
	private void showInfo() {
		if (sinkList.getNumberOfSinks()>0)
		{
			rootPanel.add(sinkList.getSelectedSink(0));
		}
		else
		{
			HaplotypeSingleView haplotypeSingleView = new HaplotypeSingleView(constants, jsonErrorDialog);
			haplotypeSingleView.submitButton.addClickListener(this);
			sinkList.addSink(haplotypeSingleView);
			curSinkIndex = 0;
			show(0, false);
		}
	}
	*/
}
