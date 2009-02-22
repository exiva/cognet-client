package org.twodot.cognet;

import danger.app.SettingsDB;
import danger.app.SettingsDBException;

/**
 * Created by IntelliJ IDEA.
 * User: jake
 * Date: Jan 19, 2004
 * Time: 5:03:03 PM
 * To change this template use Options | File Templates.
 */
public class CognetSettings {

    private static SettingsDB cogSettings;

    public static void init() {
        cogSettings = new SettingsDB("cognetsettings");
    }

    public static String getHost() {
        return cogSettings.getStringValue("host");
    }

    public static void setHost(String value) {
        cogSettings.setStringValue("host", value);
    }

    public static String getUser() {
        return cogSettings.getStringValue("user");
    }

    public static void setUser(String value) {
        cogSettings.setStringValue("user", value);
    }

    public static String getPassword() {
        return cogSettings.getStringValue("password");
    }

    public static void setPassword(String value) {
        cogSettings.setStringValue("password", value);
    }

    public static int getPort() {
        try {
            return cogSettings.getIntValue("port");
        } catch (SettingsDBException e) {
            return 7890;
        }
    }

    public static void setPort(int value) {
        cogSettings.setIntValue("port", value);
    }

    public static boolean getDoSmileys() {
        try {
            return 1 == cogSettings.getIntValue("doSmileys");
        } catch (SettingsDBException e) {
            return true;
        }
    }

    public static void setDoSmileys(boolean value) {
        cogSettings.setIntValue("doSmileys", value?1:0);
    }

    public static boolean getDoMarquee() {
        try {
            return 1 == cogSettings.getIntValue("doMarquee");
        } catch (SettingsDBException e) {
            return true;
        }
    }


    public static void setDoMarquee(boolean value) {
        cogSettings.setIntValue("doMarquee", value?1:0);
    }

    public static boolean getDoActivity() {
        try {
            return 1 == cogSettings.getIntValue("doActivity");
        } catch (SettingsDBException e) {
            return true;
        }
    }


    public static void setDoActivity(boolean value) {
        cogSettings.setIntValue("doActivity", value?1:0);
    }

    public static boolean getDoAutoConnect() {
        try {
            return 1 == cogSettings.getIntValue("doAutoConnect");
        } catch (SettingsDBException e) {
            return false;
        }
    }


    public static void setDoAutoConnect(boolean value) {
        cogSettings.setIntValue("doAutoConnect", value?1:0);
    }


    public static void setFont(String name) {
        cogSettings.setStringValue("font", name);
    }

    public static String getFont() {
        String fontName = cogSettings.getStringValue("font");
        if (fontName == null) fontName = "Bort9";
        return fontName;
    }

}
