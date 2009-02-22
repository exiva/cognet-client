/*
 * cognet chat app
 *
 * Copyright 2003, Brian Swetland <swetland@frotz.net>
 * See LICENSE for redistribution terms
 *
 */
package org.twodot.cognet;

import danger.app.AppResources;
import danger.app.Application;
import danger.app.Event;
import danger.app.SettingsDB;
import danger.app.Resource;
import danger.ui.*;
import danger.util.DEBUG;

public class Cognet extends Application implements Resources {
	public Cognet() {
		//bm_plain = getBitmap(kBlank);
		//bm_mail = getBitmap(kMarker);
		//bm_tag = getBitmap(kTag);
		bm_notify = getBitmap(kPending);

		try {
			NotificationManager.registerPendingIcon("cognet", getBitmap(kPending), getBitmap(kPending));
		} catch (Throwable t){
			System.out.println(t.getMessage());
		}

		mSplashScreen = Application.getCurrentApp().getResources().getSplashScreen(AppResources.ID_SPLASH_SCREEN_RESOURCE);
		sState = (StaticTextBox) mSplashScreen.getDescendantWithID(ID_STATE);
		sMsg = (StaticTextBox) mSplashScreen.getDescendantWithID(ID_MSG);		
		//cogSettings = new SettingsDB("prefs");
		CognetSettings.init();

		DEBUG.p("cognet: setReceiveConnectivityEvents(true");
		try {
			setReceiveConnectivityEvents(true);
			DEBUG.p("Set elements worked");
		}
		catch (Exception e) {
			DEBUG.p("Caught exception setting receiveconnex");
		}
	}

	void Notify() {
		// synchronized (this) {
		// 	//System.err.println("cognet: notify");
		// 	if(!fgapp&&doActivity) 
		// 		NotificationManager.setPendingIconVisible("cognet", true);
		// }
	}

	public void resume() {
		DEBUG.p("Resume attempt");
		if(engine == null) {
			DEBUG.p("Null engine, creating");
			engine = new Engine();
			engine.MSG("%server", "Welcome to Cognet++");
//			engine.MSG("%server", "Please set your login settings under 'Settings/Login Settings...'");
//			engine.MSG("%server", "To connect, select 'Connect' from the menu.");
		}
		DEBUG.p("Created engine");
		synchronized (this) {
			fgapp = true;
			System.err.println("cognet: no notify");
//			NotificationManager.setPendingIconVisible("cognet", false);
		}
		mIsAppForeground=true;
	}

	public void quit() {
		try {
			synchronized (this) {
				fgapp = true;
				System.err.println("cognet: no notify");
//				NotificationManager.setPendingIconVisible("cognet", false);
			}
			engine.logout();
		} catch (Throwable t){}
		unload();
	}

	public void suspend() {
		synchronized (this) {
			fgapp = false;
		}
		mIsAppForeground=false;
	}
	
	public void renderSplashScreen(View inView, Pen inPen) {
		mSplashScreen.paint(inView, inPen);
		DEBUG.p("Rendering Splash Screen.");
	}

	public void setMessagePending(String message) {
		sMsg.setText(message);
		updatePreviewScreen();
	}
	
	public void setSplashState(int type) {
		switch (type) {
			case ONLINE:
			sState.setText("Online");
			break;

			case OFFLINE:
			sState.setText("Offline");
			break;

			case DISCONNECT:
			sState.setText("Disconnected");
			break;
		}
	updatePreviewScreen();
	}

	public void addQuickJump(String text) {
		if (!this.mIsAppForeground) {
			addQuickAccessItem(getBundle().getMiniIcon(),text, 1);
		}
	}
	
	public void MakeMenu(Menu a, ChatWindow w) {
		MenuItem mi;

		a.removeAllItems();
		engine.AddWindows(a);

		a.addDivider();

		if( w != null ) {
			Menu links;
			links = w.links.makeLinkMenu(kURL);
			
			if( links != null ) {
				mi = a.addItem("Links");
				mi.addSubMenu(links);
			}

			menuWindow = w;
		}

		Menu options = new Menu("Options");

//		mi = options.addItem("Login Settings...",kLoginSettings);

		// mi = options.addItem("Icons and Smileys",kSmileys);
		// mi.setChecked(CognetSettings.getDoSmileys());

		mi = options.addItem("Activity Indicator",kActivity);
		mi.setChecked(CognetSettings.getDoActivity());

		mi = options.addItem("Marquee", kMarquee);
		mi.setChecked(CognetSettings.getDoMarquee());

		mi = options.addItem("Auto-Reconnect",kAutoConnect);
		mi.setChecked(CognetSettings.getDoAutoConnect());

		// Menu fonts = new Menu("Font");
		// mi = fonts.addItem("Bort 7pt", kSetFont, 0,"Bort7", this);
		// mi = fonts.addItem("Bort 9pt", kSetFont, 0, "Bort9", this);
		// mi = fonts.addItem("Bort 12pt", kSetFont, 0, "Bort12", this);
		// fonts.addDivider();
		// 
		// mi = fonts.addItem("Fixed 4x6", kSetFont, 0, "Fixed4x6", this);
		// mi = fonts.addItem("Fixed 4x8", kSetFont, 0, "Fixed4x8", this);
		// mi = fonts.addItem("Fixed 5x7", kSetFont, 0, "Fixed5x7", this);
		// mi = fonts.addItem("Fixed 6x10", kSetFont, 0, "Fixed6x10", this);
		// fonts.addDivider();
		// 
		// mi = a.addItem("Font");
		// mi.addSubMenu(fonts);

		mi = a.addItem("Options");
		mi.addSubMenu(options);

		if(w != null){
			mi = a.addItem("Close Tab",kCloseWindow,0,w.key,this);
			mi.setShortcut('.');
		}

		a.addDivider();

		if (engine.state == Engine.ONLINE){
			a.addItem("Disconnect",kDisconnect);
		} else if (engine.state == Engine.OFFLINE) {
			a.addItem("Disconnect",kDisconnect);
			a.addItem("Reconnect",kReconnect);
		} else if (engine.state == Engine.DISCONNECT) {
			a.addItem("Connect", kConnect);
		} else {
			a.addItem("Cancel Connect", kDisconnect);
		}

		if (w== null)  {
			a.addDivider();
			a.addItem("About...",kAbout);
		}
	}

	public boolean receiveEvent(Event e) {
		if( (e.type >= kURL) && (e.type <= kURLend) ) {
			menuWindow.links.dispatchLink(e.type,kURL);
			return true;
		}

		switch(e.type){
		//	case kGotoLogin:
			//engine.login.show();
		//return true;
		case kGotoWindow:
			engine.Select((String)e.argument,false);
		return true;
		case kCloseWindow:
			engine.Close((String)e.argument);
		return true;
		case kReconnect:
			engine.relogin();
		return true;
		case Event.EVENT_CONNECTION_UP:
			engine.autorelogin();
		return true;
		case kConnect:
			//load settings into Engine.
			//engine.USER = CognetSettings.getUser();
			//engine.PASSWD = CognetSettings.getPassword();
			//engine.HOST = CognetSettings.getHost();
			//engine.PORT = CognetSettings.getPort();
			//engine.login();
			engine.login.show();
		return true;
		case kDisconnect:
			engine.logout();
		return true;
		case kSmileys:
			CognetSettings.setDoSmileys(!CognetSettings.getDoSmileys());
			engine.sendEventAllWindows(new Event(ChatWindow.kChatWindowSetSmileys,
				CognetSettings.getDoSmileys()?1:0, 0, null));
		return true;
		case kActivity:
			CognetSettings.setDoActivity(!CognetSettings.getDoActivity());
		return true;
		case kMarquee:
			CognetSettings.setDoMarquee(!CognetSettings.getDoMarquee());
		return true;
		case kAutoConnect:
			CognetSettings.setDoAutoConnect(!CognetSettings.getDoAutoConnect());
		return true;
		case kSetFont:
			String fontName = (String)e.argument;
			CognetSettings.setFont(fontName);
			engine.sendEventAllWindows(new Event(ChatWindow.kChatWindowSetFont,
				0, 0, Font.findFont(fontName)));
		return true;
		case kAbout:
			this.showAbout();
		return true;
		default:
		return super.receiveEvent(e);
		}
	}

	void showAbout() {
		DialogWindow w = getDialog(kAboutDialog, this);
		StaticTextBox s = (StaticTextBox)w.getDescendantWithID(kID_AboutText);
		Resource r = getResources().getResource(257, kLicense);
		byte b[] = new byte[r.getSize()];
		r.getBytes(b,0,r.getSize());
		s.setText(new String(b));
		w.show();
	}

	//static final int kGotoLogin = 1;
	static final int kGotoWindow = 2;
	static final int kCloseWindow = 3;
	static final int kDisconnect = 4;
	static final int kConnect = 5;
	static final int kPrefs = 6;
	static final int kLinks = 7;
	static final int kSmileys = 8;
	static final int kActivity = 9;
	static final int kMarquee = 10;
	static final int kAutoConnect = 11;
    static final int kAbout = 12;
    static final int kReconnect = 13;


	static final int OFFLINE = 1;
	static final int ONLINE = 2;
	static final int DISCONNECT = 4;
	
	static final int kURL = 100;
	static final int kURLend = kURL + LinkMenu.maxLinkTargets;

	ChatWindow menuWindow;

	Engine engine;

	//Bitmap bm_plain;
	//Bitmap bm_mail;
	//Bitmap bm_tag;
    Bitmap bm_notify;

    SettingsDB cogSettings;

	boolean fgapp;
	
	//boolean doActivity;
	//boolean doMarquee;
	//boolean doSmileys;
	//boolean doAutoConnect;
	public SplashScreen mSplashScreen;
	private static StaticTextBox sState, sMsg;
	public static boolean mIsAppForeground;
	
    public static final int kSetFont = 600;
}
