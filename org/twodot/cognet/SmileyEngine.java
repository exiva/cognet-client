package org.twodot.cognet;

import danger.ui.Pen;
import danger.ui.Font;
import danger.ui.Bitmap;
import danger.app.Application;
import danger.app.ResourceDatabase;
import danger.util.StringUtils;
import org.twodot.cognet.Resources;

public class SmileyEngine implements Resources {

    private String text;
    private int    pixels;

    private static int FIRST_ICON = 0xE000;
    private int      iconWidth;
    private int      iconCount;
    private Bitmap[] iconSet;
    Font             font;

    public static final char ICON_INNOCENT = 0xE000;
    public static final char ICON_SMILING  = 0xE001;
    public static final char ICON_FROWNING = 0xE002;
    public static final char ICON_WINKING  = 0xE003;
    public static final char ICON_STICKING_OUT_TONGUE = 0xE004;
    public static final char ICON_SURPRISED = 0xE005;
    public static final char ICON_KISSING   = 0xE006;
    public static final char ICON_YELLING   = 0xE007;
    public static final char ICON_COOL      = 0xE008;
    public static final char ICON_MONEY_MOUTH = 0xE009;
    public static final char ICON_FOOT_IN_MOUTH = 0xE00A;
    public static final char ICON_EMBARASSED = 0xE00B;
    public static final char ICON_INDIFFERENT = 0xE00C;
    public static final char ICON_CRYING      = 0xE00D;
    public static final char ICON_LIPS_SEALED = 0xE00E;
    public static final char ICON_LAUGHING    = 0xE00F;
    public static final char ICON_COG         = 0xE010;
    public static final char ICON_IRC         = 0xE011;
    public static final char ICON_FLAG     = 0xE012;
    public static final char ICON_INFO     = 0xE013;
    public static final char ICON_BULLET  = 0xE014;
    public static final char ICON_CLOCK   = 0xE015;

    public static final int  LARGE_ICON_LIMIT = 11;

    public SmileyEngine(Font f, String _text, int pixels) {
        this(f);
        this.text = replaceWithUnicode(_text);
        this.pixels = pixels;
    }

    public String getUnicodeText() {
        return this.text;
    }

    public SmileyEngine(Font f) {
        font = f;
        ResourceDatabase rdb = Application.getCurrentApp().getResources();
        iconSet=null;
        iconWidth = 0;
    }

    public String getNext(int pixels) {
        if (text.length() <= 0) return null;

        int indexOfLastSpace = 0;
        int totalWidth = 0;
        String returnVal;

        int x=0;
        for (x=0; x<text.length(); x++) {
            char c = text.charAt(x);
            if (c == ' ') indexOfLastSpace = x;
            totalWidth += charWidth(c);
            if (totalWidth >= pixels) break;
        }

        // if indexOfLastSpace == 0, then we've got
        // one big 'ol word that needs to be broken up
        if (totalWidth < pixels && x == text.length()) {
            returnVal = text;
            text = "";
        } else if (indexOfLastSpace == 0) {
            returnVal = text.substring(0,x);
            //if (x+1 < text.length())
            text = text.substring(x);
        } else {
            // otherwise, break the line at indexOfLastSpace
            returnVal = text.substring(0,indexOfLastSpace);
            //if (indexOfLastSpace+1 < text.length())
                text = text.substring(indexOfLastSpace+1);
        }
        return returnVal;
    }

    public int getNextLength(int pixels) {
        if (text.length() <= 0) return -1;

        int indexOfLastSpace = 0;
        int totalWidth = 0;
        int returnVal;

        int x=0;
        for (x=0; x<text.length(); x++) {
            char c = text.charAt(x);
            if (c == ' ') indexOfLastSpace = x;
            totalWidth += charWidth(c);
            if (totalWidth >= pixels) break;
        }

        // if indexOfLastSpace == 0, then we've got
        // one big 'ol word that needs to be broken up
        if (totalWidth < pixels && x == text.length()) {
            returnVal = text.length();
            text = "";
        } else if (indexOfLastSpace == 0) {
            returnVal = x;
            //if (x+1 < text.length())
            text = text.substring(x);
        } else {
            // otherwise, break the line at indexOfLastSpace
            returnVal = indexOfLastSpace;
            //if (indexOfLastSpace+1 < text.length())
            text = text.substring(indexOfLastSpace+1);
        }
        return returnVal;
    }

    public int nextLineBreak(int start) {
        int pixelWidth=0;
        int lastSpace = -1;
        boolean hitMargin = false;

        int ptr;
        for (ptr=start; ptr < text.length(); ptr++) {
            char c = text.charAt(ptr);
            if (c == ' ') lastSpace = ptr;
            pixelWidth += charWidth(c);
            if (pixelWidth >= pixels) {
                hitMargin = true;
                break;
            }
        }

        if (!hitMargin) return text.length();

        if (lastSpace == -1) {
            //we have one really long word
            return ptr;
        } else {
            return lastSpace;
        }

    }

    public static String replaceWithUnicode(String inText) {

        //replace all emoticons with corresponding character
        return inText;
    }

    public static String replaceAll(String in, String what, String with) {
        int pos = 0;
        int whatLen = what.length();
        int withLen = with.length();
        while ((pos = in.indexOf(what, pos)) >= 0) {
            in = in.substring(0,pos) + with + in.substring(pos+whatLen);
            pos = pos + withLen;
        }
        return in;
    }

    public void drawString(int x, int y, String s, Pen p) {
        int xpos = x;
        p.setFont(font);
        for (int c=0; c<s.length(); c++) {
            xpos += drawChar(p, xpos, y, s.charAt(c));
        }
    }

    public int charWidth(char c) {
	 {
            return font.charWidth(c);
        }
    }

    public int drawChar(Pen p, int xpos, int ypos, char chr) {
        if (isIcon(chr)) {
            p.drawBitmap(xpos, ypos-font.getAscent(), getIconBitmap(chr));
            return charWidth(chr) + font.getGap();
        } else {
            p.drawChar(xpos, ypos, chr);
            return font.charWidth(chr);
        }
    }

    public boolean isIcon(char chr) {
         return (chr >= FIRST_ICON && chr < FIRST_ICON + iconWidth);
    }

    public Bitmap getIconBitmap(char c) {
        int ndx = c - 0xE000;
        return iconSet[ndx];
    }
}
