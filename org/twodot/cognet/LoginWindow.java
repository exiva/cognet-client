/*
 * cognet chat app
 *
 * Copyright 2003, Brian Swetland <swetland@frotz.net>
 * See LICENSE for redistribution terms
 *
 */
package org.twodot.cognet;

import danger.ui.*;
import danger.app.Resource;
import danger.app.Event;
import danger.app.Application;
import danger.util.DEBUG;

public class LoginWindow extends ScreenWindow implements Resources, Events
{
	public LoginWindow() {
		app = (Cognet)getApplication();
	}

	public static LoginWindow newLoginWindow(Engine e) {
		LoginWindow w = (LoginWindow)Application.getCurrentApp().getResources().getScreen(kLoginWindow);
		w.engine = e;
		return w;
	}

	public void onDecoded() {
		this.setTitle("Connect to Server");
		box = (RoundRectContainer)this.getDescendantWithID(kLoginWindowRoundRect);

		host = (TextField)this.getDescendantWithID(kTxtHost);
		port = (TextField)this.getDescendantWithID(kTxtPort);
		user = (TextField)this.getDescendantWithID(kTxtUser);
		passwd = (TextField)this.getDescendantWithID(kTxtPassword);
		button = (Button)this.getDescendantWithID(kBtnLogin);
		button.setTitle("Connect");

		host.setText(CognetSettings.getHost());
		port.setText(String.valueOf(CognetSettings.getPort()));
		user.setText(CognetSettings.getUser());
		passwd.setText(CognetSettings.getPassword());
	}

	// public void adjustActionMenuState() {
	// 	app.MakeMenu(getActionMenu(),null);
	// }

	
	public final void adjustActionMenuState(Menu menu) {
		app.MakeMenu(getActionMenu(),null);
	    }

	public boolean eventWidgetUp(int widget, Event event) {
		switch(widget) {
			case Event.DEVICE_BUTTON_BACK: 
			getApplication().returnToLauncher();
			return true;
			default:
			return super.eventWidgetUp(widget,event);
		}
	}

	public boolean receiveEvent(Event e) {
		switch (e.type) {
			case kEvtLoginButton:
			switch(engine.state){
				case Engine.DISCONNECT:
					engine.HOST = new String(host.getChars());
					engine.PORT = Integer.parseInt(new String(port.getChars()));
					engine.USER = new String(user.getChars());
					engine.PASSWD = new String(passwd.getChars());
					CognetSettings.setHost(app.engine.HOST);
					CognetSettings.setPort(app.engine.PORT);
					CognetSettings.setUser(app.engine.USER);
					CognetSettings.setPassword(app.engine.PASSWD);
					DEBUG.p("DEBUG INFO: "+app.engine.HOST+" "+app.engine.PORT+" "+app.engine.USER+" "+app.engine.PASSWD);
					engine.login();
					return true;

				case Engine.OFFLINE:
					engine.relogin();
					return true;

				case Engine.CONNECTING:
				case Engine.ONLINE:
					app.engine.logout();
					return true;

				default:
					return true;
				}

			case kLoginWindowStateCheck:
				System.err.println("statecheck");
				int state = e.what;
				switch(state) {
					case Engine.DISCONNECT:
						button.setTitle("Connect");
						box.enable();
					return true;

					case Engine.OFFLINE:
						button.setTitle("Re-connect");
					return true;

					case Engine.CONNECTING:
						button.setTitle("Cancel");
						box.disable();
					return true;

					case Engine.ONLINE:
						// save settings
						DEBUG.p("cognet: commit settings");
						button.setTitle("Disconnect");
						box.disable();
						return true;

					default:
						return true;
				}
			default:
				return false;
        }
    }
	TextField host, port, user, passwd;
	Button button;
	RoundRectContainer box;
	//Engine engine;
	Cognet app;
	Engine engine;
	public static final int kLoginWindowStateCheck = 700;
}