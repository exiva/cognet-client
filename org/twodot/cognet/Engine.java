/*
 * cognet chat app
 *
 * Copyright 2003, Brian Swetland <swetland@frotz.net>
 * See LICENSE for redistribution terms
 *
 */
package org.twodot.cognet;

import danger.app.Application;
import danger.app.AppResources;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Hashtable;

import danger.ui.*;
import danger.app.Listener;
import danger.app.Event;
import danger.app.Application;
import danger.util.DEBUG;
import java.util.LinkedList;
import java.util.ListIterator;

public class Engine extends Listener implements Resources, Runnable
{
	public Engine() {
		DEBUG.p("ENGINE INIT-------------");
		init();

        initSettings();

		out_lock = new Object();
		in_buffer = new byte[512];

        outputBuffer = new LinkedList();
		DEBUG.p("Created output buffer");
        //state = DISCONNECT;
		login = LoginWindow.newLoginWindow(this);
		DEBUG.p("Created login window");

		first = ChatWindow.newChatWindow("Cognet Server","%server",
                Font.findSystemFont(),CognetSettings.getDoSmileys());
             // Font.findFont(CognetSettings.getFont()),CognetSettings.getDoSmileys());
        //first.sendEvent(ChatWindow.kChatWindowSetReadOnly,1,0,null);
        first.setIcon(app.getBitmap(kSpeechBubble));
		
        changeState(DISCONNECT);
		DEBUG.p("Created chat window");

		first.next = first;
		first.prev = first;
		windows = new Hashtable();
		windows.put("%server",first);

        //Select("%server", false);
        //MakeActive(first);

		login.show();
		DEBUG.p("ENGINE INIT COMPLETED---");
	}

    private void changeState(int state) {
        DEBUG.p("State was " + this.state + " and is becomming " + state);
        this.state = state;
        switch (state) {

            case ONLINE:
				app.setSplashState(ONLINE);
                // setAllWindowsIcons(app.getBitmap(kIconOnline));
                // setAllWindowsIcons(kOnlineIcon);
                this.sendEventAllWindows(new Event(ChatWindow.kChatWindowSetReadOnly,0,0,null));

                break;

            case OFFLINE:
				app.setSplashState(OFFLINE);
                // setAllWindowsIcons(kOfflineIcon);
                // setAllWindowsIcons(app.getBitmap(kIconOffline));
                this.sendEventAllWindows(new Event(ChatWindow.kChatWindowSetReadOnly,1,0,null));

                break;

            case DISCONNECT:
				app.setSplashState(DISCONNECT);
                // setAllWindowsIcons(app.getBitmap(kIconDisconnected));
                // setAllWindowsIcons(kDisconnectedIcon);
                this.sendEventAllWindows(new Event(ChatWindow.kChatWindowSetReadOnly,1,0,null));

                break;

            case CONNECTING:
                break;

            default:
                DEBUG.p("Unknown engine state: " + state);
        }

        login.sendEvent(LoginWindow.kLoginWindowStateCheck, state,0,null);
    }

    void initSettings() {
        String settingsHost, settingsUser, settingsPasswd;
        int settingsPort;

        //get Host
        settingsHost = CognetSettings.getHost();
        if (settingsHost != null) HOST = settingsHost;

        //try {
        //get port
            	PORT = CognetSettings.getPort();

        //get user
        	settingsUser = CognetSettings.getUser();
        	if (settingsUser != null) USER = settingsUser;

        //get pass
        	settingsPasswd = CognetSettings.getPassword();
        	if (settingsPasswd != null) PASSWD = settingsPasswd;

		//boolean settingsSmiley, settingsMarquee;
		//boolean settingsActivity, settingsAutoConnect;

	//get prefs
		//settingsSmiley = 1 == app.cogSettings.getIntValue("doSmileys");
		//settingsMarquee = 1 == app.cogSettings.getIntValue("doMarquee");
		//settingsActivity = 1 == app.cogSettings.getIntValue("doActivity");
		//settingsAutoConnect = 1 == app.cogSettings.getIntValue("doAutoConnect");

		//app.doSmileys = settingsSmiley;
		//app.doMarquee = settingsMarquee;
		//app.doActivity = settingsActivity;
		//app.doAutoConnect = settingsAutoConnect;
        //} catch (Throwable e) {}
    }

	void AddWindows(Menu a) {
		synchronized(lock) {
			ChatWindow w = first;
			do {
				if(!w.special){
					Character c = (Character) QuickMap.get(w.key);
					MenuItem mi = a.addItem(w.context, Cognet.kGotoWindow, 0, w.key, app);
					mi.setIcon(w.mailbox ? kMailIcon : kBlankIcon);
					if(c != null) mi.setShortcut(c.charValue());
					mi.setChecked(w == active);
				}
				w = w.next;
			} while(w != first);
			w = first;
			do {
				if(w.special){
					Character c = (Character) QuickMap.get(w.key);
					MenuItem mi = a.addItem(w.context, Cognet.kGotoWindow, 0, w.key, app);
					mi.setIcon(w.mailbox ? kMailIcon : kBlankIcon);
					if(c != null) mi.setShortcut(c.charValue());
					mi.setChecked(w == active);
				}
				w = w.next;
			} while(w != first);
		}
	}

	ChatWindow NextMailbox(ChatWindow start) {
		synchronized(lock) {
			ChatWindow w = start.next;
			while(w != start){
				if(w.mailbox) return w;
				w = w.next;
			}
		}
		return null;
	}
	
	
	void nuke() {
		ChatWindow w,n;
		
		synchronized(lock){
			w = first;
			w.CLR();
			w = w.next;
			while(w != first){
				w.hide();
				w = w.next;
			}

			first.next = first;
			first.prev = first;
			windows = new Hashtable();
			windows.put("%server",first);
		}
		RX_SERIAL="0";
	}
	
	public void run() {
		System.err.println("cognet: starting...");
		//login.SendEvent(5039);

        // if we don't have a last window selected
        // or the select fails to find a window, then show
        // the server window.
		if (lastWindowSelected == null || !Select(lastWindowSelected,false))
            Select("%server",true);

		try {
			synchronized(out_lock) {
				s = new Socket(HOST, PORT);
				System.err.println("cognet: Connected to "+HOST+":"+PORT);
				in = s.getInputStream();
				out = s.getOutputStream();
			}
			System.err.println("cognet: Signing On");

            changeState(ONLINE);

			SignOn();
			System.err.println("cognet: IO Loop Starting");
            login.hide();
            this.MakeActive(first);

			HandleIO();
		} catch (Throwable e){
			// System.err.println("cognet: whoops: " + e);
			// e.printStackTrace();
			try {
				s.close();
			} catch (Exception x){
			}
		}

        // If we've exited the thread because of DISCONNECT, then
        // don't set state to offline.  Otherwise, we're here because
        // we've dropped connection.  Set state = OFFLINE.
        if (state != DISCONNECT) changeState(OFFLINE);
		//login.SendEvent(5039);
		//login.show();
		
		System.err.println("cognet: Network Thread Exiting");
	}

    public void relogin() {
            System.err.println("Engine: relogin (requested)");
            if (PASSWD != null && PASSWD.length()>0)
                login();
    }

    public void autorelogin() {
        if (CognetSettings.getDoAutoConnect() && state == OFFLINE) {
            System.err.println("Engine: relogin (autologin)");
            if (PASSWD != null && PASSWD.length()>0)
                login();
        }
    }

	void login() {
		synchronized(lock){
			if(state == OFFLINE || state == DISCONNECT) {
				changeState(CONNECTING);
				try {
					(new Thread(this,"cognet: engine")).start();
				} catch (Throwable t){
					changeState(OFFLINE);

					System.err.println("cognet: can't start engine thread");
				}
			}
		}
	}
	
	
	void SignOn() throws IOException {
		Write("serial",RX_SERIAL,TOKEN);
        System.out.println("Serial sent:" + RX_SERIAL);
		Write("test",USER,PASSWD);
	}

	void logout() {
		System.err.println("cognet: logout");
		changeState(DISCONNECT);
		PASSWD="";
   		try {
			s.close();
		} catch (Throwable t){
		}
		try {
			in.close();
		} catch (Throwable t){
		}
		try {
			out.close();
		} catch (Throwable t){
		}
		System.err.println("cognet: logged out");
        login.show();
	}
	
	void Close(String context) {
		String key = context.toLowerCase();
		if(key.equals("%server")) return;

		ChatWindow w = null;
		ChatWindow nextup = null;

		synchronized(lock){
			w = (ChatWindow) windows.remove(key);
			if( w == null )
				return;
			nextup = w.next;
			if(w != null){
				w.Remove();
			}
		}

		if(w != null) {
			nextup.show();
			try {
				Write("clear",context,"");
			} catch (IOException iox) {
			}
		}
	}

	void Command(String context, String str) {
		int l = str.length();
		if((l > 0) && (str.charAt(l - 1) < ' ')){
			/* Why textfield likes to tack a return on the end,
			   I'll never know... */
			str = str.substring(0, l - 1);
		}

		try {
            String serial = String.valueOf(TX_SERIAL++);

			if(str.charAt(0) == '/'){

				if(str.startsWith("/clear")){
					Write(serial, "clear",context,"");
                    return;
				}
				if(str.startsWith("/bind ")){
					char c = str.charAt(6);
					System.err.println("cognet: bind '"+c+"' to '"+context+"'");

					if((c >= ' ') && (c <= '9')){
						SetQuick(context, c);
						Write(serial, "save","quick/"+c,context);
					}
					return;
				}
				if(str.startsWith("/win ")){
					Select(str.substring(5), false);
					return;
				}
				if(str.startsWith("/zap")) {
					logout();
					return;
				}
                if(str.startsWith("/simulatedrop"))  {
                    DEBUG.p("simulating dropped connection");
                    try { s.close(); }
                    catch (Exception e) {

                    }
                    return;
                }

                if (str.startsWith("/simulatereconnect")) {
                    DEBUG.p("simulating reconnect...");
                    this.relogin();
                    return;
                }

//				if(str.startsWith("/query ")) {
//					Select(str.substring(7),true);
//					return;
//				}
			}

	/*		if (str.startsWith("/font ")) {
				String fontName = str.substring(6);
				ChatWindow w = (ChatWindow)windows.get(context.toLowerCase());
                    	if (w != null) {
                        Font font = Font.findFont(fontName);
                        w.setFont(font);
                        MSG(context, "<*> Font set to " + fontName);
                    } else {
                        MSG(context, "<*> Unknown font.");
                    }
		   	}
    */
		/* all other commands are handled on the proxy */
			if( str.startsWith("/") ){
				int n = str.indexOf(' ');
				if(n != -1){
					Write(serial, str.substring(1,n), context, str.substring(n+1));
				} else {
					Write(serial, str.substring(1), context, "");
				}
			} else {
				Write(serial, "send",context,str);
			}

			return;
		} catch (Exception e) {
		}
	}
	
	
	void HandleIO() throws IOException {
		byte[] b = in_buffer;
		int i, j, n, p, x;
		byte mark = (byte) ':';
		
		for(;;) {
				/* read one line from the server */
			if(state != ONLINE){
				throw new IOException("not online");
			}

			p = 0;
			for(;;) {
				x = in.read();
				if(x < 0) throw new IOException("EOF");
				if((x == 10) || (x == 13)){
					if(p == 0) {
						continue;
					} else {
						break;
					}
				}
				
					/* truncate lines that are too long */
				if(p == 510) {
					continue;
				} else {
					b[p++] = (byte) x;
				}
			}
			b[p] = 0;

			// System.err.println("LINE: " + new String(b,0,p));
			
			String sn, tag, src, dst, txt;
			
			sn = null;
			tag = null;
			src = null;
			
			i = 0;
			while(i < p){
				if(b[i] == mark) {
					sn = new String(b,0,i);
					break;
				}
				i++;
			}

			j = i = i + 1;
			while(i < p){
				if(b[i] == mark) {
					tag = new String(b,j,i-j);
					break;
				}
				i++;
			}

			j = i = i + 1;
			while(i < p){
				if(b[i] == mark) {
					src = new String(b,j,i-j);
					break;
				}
				i++;
			}

			j = i = i + 1;
			while(i < p){
				if(b[i] == mark) {
					dst = new String(b,j,i-j);
					i++;
					txt = new String(b,i,p-i);

					if(sn.length() > 0) RX_SERIAL = sn;
					
						// System.err.println("["+sn+":"+tag+":"+src+":"+dst+":"+txt+"]");
                    //todo: figure out why txt has a trailing space.
					Process(tag,src,dst,txt.trim());
					break;
				}
				i++;
			}

		}
	}
	

	void Process(String tag, String src, String dst, String txt) {
		//System.err.println(tag+":"+src+":"+dst+":"+txt);
		if(tag.equals("fmsg")){
			MSG(dst, txt);
		} else if(tag.equals("dmsg")){
			MSG(dst,"<"+src+"> "+txt);
		} else if(tag.equals("dmsg/act")){
			MSG(dst, "* "+src+" "+txt);
		} else if(tag.equals("smsg")){
			MSG(src,"<"+src+"> "+txt);
		} else if(tag.equals("smsg/act")){
			MSG(src, "* "+src+" "+txt);
		} else if(tag.equals("show")){
			Select(dst, true);
		} else if(tag.equals("info")){
			MSG(dst, "<-> " + txt);
		} else if(tag.equals("repl")){
			MSG(dst, "<::> " + txt);
		} else if(tag.equals("fail")){
			MSG(dst, "<::> " + txt);
		} else if(tag.equals("clear")){
			CLR(dst);
		} else if(tag.equals("kill")){
			Close(dst);
		} else if(tag.equals("url")){
			URL(dst, src, txt);
        } else if(tag.equals("serial")) {
            //DEBUG.p("Got a serial request");
            int serial = Integer.parseInt(txt.substring(0, txt.indexOf(':')));
            String inToken = txt.substring(txt.indexOf(':')+1);
            if (!inToken.equals(TOKEN)) {
                DEBUG.p("Token mismatch: clearing outgoing buffer.");
                outputBuffer.clear();
                TX_SERIAL=0;
            } else {
                DEBUG.p("Sending lines after: " + serial);
                boolean startPlayback = false;
                ListIterator iterator = outputBuffer.listIterator();
                CognetFrame line;
                synchronized (out_lock) {
		   try
   		   {
                    while ((line = (CognetFrame)iterator.next()) != null) {
                        DEBUG.p(startPlayback + " >" + line.message);
                        if (!startPlayback && line.serial == serial) {
                            startPlayback = true;
                            DEBUG.p("Sending lines after serial# " + serial);
                        } else if (startPlayback) {
                            DEBUG.p("buffered> " + line.message);
                            try {
                                out.write(line.message.getBytes());
                            } catch (IOException e) {
                                return;
                            }
                        }
			
                    }


		   }
		   catch (Exception e)
		   {
		   }
                    this.sendEventAllWindows(new Event(ChatWindow.kChatWindowClearTemporaryMessages));
                }
            }
		} else if(tag.equals("set")){
			if(dst.equals("quick/0")) SetQuick(txt,'0');
			if(dst.equals("quick/1")) SetQuick(txt,'1');
			if(dst.equals("quick/2")) SetQuick(txt,'2');
			if(dst.equals("quick/3")) SetQuick(txt,'3');
			if(dst.equals("quick/4")) SetQuick(txt,'4');
			if(dst.equals("quick/5")) SetQuick(txt,'5');
			if(dst.equals("quick/6")) SetQuick(txt,'6');
			if(dst.equals("quick/7")) SetQuick(txt,'7');
			if(dst.equals("quick/8")) SetQuick(txt,'8');
			if(dst.equals("quick/9")) SetQuick(txt,'9');
		} else if(tag.equals("sync")){
			System.err.println("Got sync token '"+dst+"'");
			System.err.println("Have token '"+TOKEN+"'");
			if(!TOKEN.equals(dst)){
				System.err.println("Token mismatch. Clearing");
				nuke();
			}
			TOKEN = dst;
		}
	}

    public boolean receiveEvent(Event event) {
        return false;
    }

	ChatWindow Create(String context, String key) {
		ChatWindow w = ChatWindow.newChatWindow(context, key,
                Font.findSystemFont(), CognetSettings.getDoSmileys());

        // if (state == ONLINE) {
        //     w.setIcon(kOnlineIcon);
        // } else if (state == OFFLINE) {
        //     w.setIcon(kOfflineIcon);
        // } else {
        //     w.setIcon(kDisconnectedIcon);
        // }

		windows.put(key, w);

		Character qk = (Character) QuickMap.get(key);
		if(qk != null) w.SetQuickKey(qk.charValue());

		w.next = first;
		w.prev = first.prev;

		first.prev.next = w;
		first.prev = w;

		return w;
	}
	
	boolean Select(String context, boolean create) {
		ChatWindow w;
		String key = context.toLowerCase();
		
		synchronized (lock) {
			w = (ChatWindow) windows.get(key);
			if(w != null){
				w.show();
                lastWindowSelected = context;
                return true;
			} else {
				if(create){
					w = Create(context, key);
					w.show();
                    lastWindowSelected = context;
                    return true;
				}
			}
		}
        return false;
	}

    void MSG(String context, String text) {
        MSG(context, text, false);
    }

	void MSG(String context, String text, boolean temporary) {
		ChatWindow w;
		String key = context.toLowerCase();
		//DEBUG.p("key was: '"+key+"'");
		if((key.indexOf("#") < 0) && (key.indexOf(" ") < (key.length()-1)) && (key.indexOf("%") < 0))
		{
			pendingkey = key;
			//DEBUG.p("STORE THIS KEY");
			if (!app.mIsAppForeground) {
				NotificationManager.setPendingIconVisible("cognet", true);				
				// app.setChooserFolderLabel("PM on "+pendingkey);
				// this.addQuickAccessItem(app.getBitmap(kSpeechBubble),text, 1);
				// DEBUG.p("*********:Text: "+text);
				app.addQuickJump(text);
				// app.updatePreviewScreen();
			}
			String from = pendingkey.substring(pendingkey.indexOf(" ")+1);
			app.setMessagePending("Private message from "+from);
		}
		synchronized (lock) {
			w = (ChatWindow) windows.get(key);
			if(w == null){
				w = Create(context, key);
                	if (!app.fgapp && CognetSettings.getDoMarquee()) {
                    		String msg = text;
                    		NotificationManager.marqueeAlertNotify(new MarqueeAlert(msg,app.getBitmap(kSpeechBubble),1));
                	}
			app.Notify();

			}
			if(text != null) w.MSG(text, temporary);
		}
	}
	
	void CLR(String context) {
		ChatWindow w;
		String key = context.toLowerCase();
		
		synchronized (lock) {
			w = (ChatWindow) windows.get(key);
			if(w != null){
				w.CLR();
			}
		}
	}

	void URL(String context, String src, String text)
	{
		ChatWindow w;
		String key = context.toLowerCase();

		synchronized (lock) {
			w = (ChatWindow) windows.get(key);
			if( w == null ){
				w = Create(context, key);
			}
			if( text != null )
			{
				String URL, Name, Category;
				int splitLoc;

				splitLoc = text.indexOf(' ');

				if( -1 == splitLoc )
				{
					URL = text;
					Name = text;
					Category = src;
				}
				else
				{
					URL = text.substring(0,splitLoc);
					Name = text.substring(splitLoc+1);
					Category = src;
				}
				w.URL(URL,Category,Name);
			}
		}
	}


	void Write(String tag, String dst, String txt) throws IOException {
        Write(null, tag, dst, txt);
    }

    void Write(String serial, String tag, String dst, String txt) throws IOException {
        String mark = ":";

        String line = mark + tag + mark + dst + mark + txt + "\n";
        if (serial != null) {
            line = serial + line;

            //only queue messages with a serial number
            CognetFrame frame = new CognetFrame(Integer.parseInt(serial), line);

            outputBuffer.addLast(frame);
            if (outputBuffer.size() > MAX_OUTPUT_BUFFER_SIZE)
                outputBuffer.removeFirst();

            if (state != ONLINE) {
                // show temporary
                MSG(dst, "<O> {" + tag + "} " + txt, true);
            }
        }

		if(state == ONLINE){
			byte[] bytes = line.getBytes();

			synchronized (out_lock) {
				try {
					out.write(bytes, 0, bytes.length);
				} catch (IOException io) {
					s.close();
				}
			}
		}
	}
	
	void SetQuick(String context, char c) {
		String key = context.toLowerCase();
		int n = ((int) c) - '0';
		
		synchronized(lock){
			if(QuickList[n] != null){
				QuickMap.remove(QuickList[n]);
				ChatWindow cw = (ChatWindow) windows.get(QuickList[n]);
				if(cw != null) cw.SetQuickKey('\0');
			}
			QuickList[n] = key;
			QuickMap.put(key,new Character(c));
			ChatWindow cw = (ChatWindow) windows.get(key);
			if(cw != null) cw.SetQuickKey(c);
		}
	}
	
		/* always called from the ui thread */
	void MakeActive(ChatWindow cw) {
		if(active != null) {
			active.active = false;
		}	
		active = cw;
		if(cw != null){
			try
			{
			if(cw.key.equals(pendingkey))
			{
				//DEBUG.p("key matched unpending");
				pendingkey = "";
				// app.setChooserFolderLabel("Cognet++");
				// app.updatePreviewScreen();
				
				app.setMessagePending("");
				NotificationManager.setPendingIconVisible("cognet", false);
				//DEBUG.p("Splash painted");
			}
			}
			catch (Exception e)
			{
			}
			cw.active = true;
			cw.mailbox = false;
		}
		UpdateAlerts();
	}

	void UpdateAlerts() {
		boolean any = false;
		StringBuffer sb = new StringBuffer(64);
			
		for(int i = 0; i < 11; i++) ALERTS[i] = false;
		
		synchronized(lock){
			ChatWindow w = first;
			do {
				if(w.mailbox) {
					any = true;
					if(w.quickkey != '\0'){
						ALERTS[w.quickkey - '0'] = true;
					} else {
						ALERTS[10] = true;
					}
				}
				w = w.next;
			} while(w != first);
		}
        sendEventAllWindows(new Event(ChatWindow.kChatWindowSetBubbles, 0,0, ALERTS));

		if(active != null) active.invalidate();
		if(any) app.Notify();
	}

    void sendEventAllWindows(Event event) {
        ChatWindow w = first;
        first.sendLowPriorityEvent(new Event(event));
        w = w.next;
        while (w != null && w != first) {
            w.sendLowPriorityEvent(new Event(event));
            w = w.next;
        }

    }

    private void setAllWindowsIcons(Bitmap icon) {
        login.setIcon(icon);
        ChatWindow w = first;
        first.setIcon(icon);
        w = w.next;
        while (w != null && w != first) {
            w.setIcon(icon);
            w = w.next;
        }
    }

	boolean ALERTS[] = new boolean[11];
	
	String HOST = "";
	int PORT = 3000;
	String USER = "";
	String PASSWD = "";

	String TOKEN = "";
	String RX_SERIAL = "0";
    long TX_SERIAL = 0;

	Socket s;
	InputStream in;
	OutputStream out;
	byte in_buffer[];
	Object out_lock;
    LinkedList outputBuffer;
    public static final int MAX_OUTPUT_BUFFER_SIZE = 50;

	Hashtable windows;
	LoginWindow login;

	static final int OFFLINE = 1;
	static final int ONLINE = 2;
	static final int CONNECTING = 3;
	static final int DISCONNECT = 4;

	int state;
	
	String QuickList[] = new String[10];
	Hashtable QuickMap = new Hashtable();
	ChatWindow first;
	ChatWindow active;

    String lastWindowSelected;

	Object lock = new Object();

    public static final int kEngineUpdateAlerts = 400;
    public static final int kEngineSaveQuick = 401;
    public static final int kEngineShowLogin = 402;

//hacking shit here.


    public static boolean isInited;
    public static Cognet app;
    public static Bitmap kBlankIcon;
    public static Bitmap kMailIcon;
    public static Bitmap kNotificationIcon;
    public static Bitmap kOnlineIcon;
    public static Bitmap kDisconnectedIcon;
    public static Bitmap kOfflineIcon;
	public String pendingkey = new String(" ");
    private static void init() {
        if (isInited) return;
        app = ((Cognet)Application.getCurrentApp());
    }
}

class CognetFrame {
    int serial;
    String message;

    public CognetFrame(int s, String m) {
        serial = s;
        message = m;
    }
}