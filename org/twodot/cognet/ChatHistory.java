/*
 * cognet chat app
 *
 * Copyright 2003, Brian Swetland <swetland@frotz.net>
 * See LICENSE for redistribution terms
 *
 * Derived from:
 *
 * @(#)ChatHistory.java	1.0 01/03/23
 *
 * Copyright 2000 by Danger Research, Inc.
 * 165 University Avenue, Palo Alto, CA  94301
 * All rights reserved.
 * See SAMPLE_CODE_LICENSE for redistribution terms.
 *
 */

package org.twodot.cognet;

import danger.ui.*;
import danger.ui.geometry.Rectangle;
import danger.util.LineBreaker;
import danger.util.DEBUG;
import java.util.LinkedList;
import java.util.ListIterator;
import danger.app.Application;

import java.util.Vector;

public class ChatHistory extends View implements Resources, Events
{
	public ChatHistory() {
        // default constructor for rsrc decoding
        init();

		setPlainFont(Font.findSystemFont());

		mLimit = 50;

        mRawLines = new LinkedList();
        mChatLines = new LinkedList();
    }

	public void onDecoded() {

	}


    private static void init() {
        if (isInited) return;
        Application app = ((Cognet)Application.getCurrentApp());
        tag_bitmap = app.getBitmap(kSpeechBubble); //replace with bubble
        tag_width = tag_bitmap.getWidth();
        tag_height = tag_bitmap.getHeight()+20;
        tag_font = Font.findFont("Bort7");

    }

	public void paint(Pen inPen) {

		Rect		r = mContentArea;
		int			i, x, y;
		int     	index = mChatLines.size() - mCurrentSelection - 1;

		//*	Clear the area
		if(mCurrentSelection != 0){
			inPen.setColor(Color.GRAY10);
		} else {
			inPen.setColor(Color.WHITE);
		}
		inPen.fillRect(r);

		inPen.setColor(Color.BLACK);

		x = r.left;
		y = mLineHeight * (mNumDisplayLines - 1);
		y += mAscent;
		y += r.top;

        smiley = new SmileyEngine(mPlainFont);
        inPen.setFont(mPlainFont);

        ListIterator iterator = mChatLines.listIterator(mCurrentSelection);

		//*	Draw the individual lines
		while ((y > r.top) && (index > -1)) {
//            if (engine.app.doSmileys)
              ChatLine line = (ChatLine)iterator.next();
              if (line == null) break;

              //DEBUG.p("Line: " + line.getText());
              inPen.setColor(line.mParent.mColor);
	          smiley.drawString(x,y,line.getText(), inPen);
//            else
//			    inPen.drawText(x, y, index.mText);

			y -= mLineHeight;
			index--;
		}

		y = 0;
		x = r.right - tag_width;


        //boolean alerts[] = app.engine.ALERTS;

        inPen.setColor(Color.BLACK);
        for(i = 0; i < 11; i++){
            if(mAlerts[i]){
                //inPen.setFont(tag_font);
                //inPen.drawBitmap(x, y, tag_bitmap);
                //inPen.drawText(x + 3, y + 9, alert_string[i]);
                drawSpeechBubble(inPen, alert_string[i].charAt(0), x, y);
                y += 17;
            }
        }

	}

	static final String alert_string[] = { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "?" };


    public void setSize(int inWidth, int inHeight) {

		super.setSize(inWidth, inHeight);

        //mPlainLineBreaker = new LineBreaker("", mPlainFont, inWidth - 8, 0);
		//mBoldLineBreaker = new LineBreaker("", mBoldFont, inWidth - 8, 0);

		mContentArea = getBounds();
		mContentArea.inset(kBorder);

		mNumDisplayLines = mContentArea.getHeight() / mLineHeight;
	}

    public int getContentWidth() {
        return mContentArea.getWidth();
    }

	public void AddChannel(String inText) {
		//DEBUG.p("addlines: "+inText);
		AddLines(inText, 0);

	}

	public void AddSent(String inText) {
		AddLines(inText, 3);
	}

	public void AddPrivate(String inText) {
		DEBUG.p("added private:"+inText);
		AddLines(inText, 1);
	}

	public void AddServer(String inText) {
		AddLines(inText, 2);
	}


    void AddLines(String inText, int glyph)  {


         RawChatLine rawLine = new RawChatLine(this, inText, mCurrentSelection != 0);
         rawLine.mColor = Color.BLACK;
         mRawLines.addLast(rawLine);
         rawLine.addChatLines(mChatLines);
	
         TrimForLength();
    }

    void AddTemporaryLines(String inText, int glyph)  {

         RawChatLine rawLine = new RawChatLine(this, inText, mCurrentSelection != 0);
         rawLine.mTemporary = true;
         rawLine.mColor = Color.GRAY7;
         mRawLines.addLast(rawLine);
         rawLine.addChatLines(mChatLines);

         TrimForLength();

    }

    void clearTemoraryLines() {
        ListIterator iterator = mRawLines.listIterator();
        RawChatLine line;

	try
	{
        while ((line = (RawChatLine)iterator.next()) != null) {
            if (line.mTemporary) {
                line.removeChatLines(mChatLines);
                mRawLines.remove(line);
            }
        }
	}
	catch (Exception e)
	{
	}
        this.invalidate();
    }


	void TrimForLength() {
/*		while (mTextSize > mLimit)
		{
			if (mNumDisplayLines >= (mNumLines - mCurrentSelection))
			{
				mCurrentSelection--;
				mBottomDisplayLine = mBottomDisplayLine.mNext;
			}

			mTextSize -= mHead.getText().length();

			mHead.mNext.mPrev = null;
			mHead = mHead.mNext;

			mNumLines--;
		}  */
        while (mRawLines.size() > mLimit) {
            RawChatLine first = (RawChatLine)mRawLines.getFirst();
            first.removeChatLines(mChatLines);
            mRawLines.removeFirst();
        }
	}

	void ScrollUp() {
		if ((mChatLines.size() - mCurrentSelection) <= mNumDisplayLines)
			return;

		mCurrentSelection++;
		//mBottomDisplayLine = mBottomDisplayLine.mPrev;

		mFirstMissed = true;
	}

	void ScrollDown() {
		if (mCurrentSelection == 0)
			return;

		mCurrentSelection--;
		//mBottomDisplayLine = mBottomDisplayLine.mNext;

		if (0 == mCurrentSelection)
			mFirstMissed = false;
	}

	public void Clear() {
		mTextSize = 0;
		mChatLines.clear();
        mRawLines.clear();
		mCurrentSelection = 0;
		mHead = null;
		mTail = null;
		//mBottomDisplayLine = null;
		mFirstMissed = false;
	}

    void setPlainFont(Font f) {
        //Clear();
        mPlainFont = f;
        mAscent = mPlainFont.getAscent() + mPlainFont.getDescent();
        mLineHeight = mAscent + kLeading - 2;
        mNumDisplayLines = getHeight() / mLineHeight;
    }

    Font  getPlainFont() {
        return mPlainFont;
    }

    void setEmoticons(boolean onOff) {
        mUseEmoticons = onOff;
    }

    boolean getEmoticons() {
        return mUseEmoticons;
    }

    void rebreak() {
        mChatLines.clear();
        ListIterator iterator = mRawLines.listIterator();
        RawChatLine line;

		try 
		{

        while ((line = (RawChatLine)iterator.next()) != null) {
            line.mFont = getPlainFont();
            line.mUseSmileys = getEmoticons();
            line.parse();
            line.addChatLines(mChatLines);
        }
		}
		catch(Exception ex)
		{

		}

    }

    private static void drawSpeechBubble(Pen p, char c, int x, int y) {
        p.drawBitmap(x,y, tag_bitmap);
        Font oldFont = p.getFont();
        // Font font = Font.findFont("Bort7");
        Font font = Font.findSystemFont();
        p.setFont(font);
        p.drawChar(x+7, y+font.getAscent(), c);
        p.setFont(oldFont);
    }

    void setBubbles(boolean[] flags) {
        mAlerts = flags;
    }

    private LinkedList      mRawLines;      //oldest = first, newest = last
    private LinkedList      mChatLines;     //newest = frirst, oldest = last
    private boolean[]       mAlerts;

	private int			mLimit;
	private int			mTextSize;
	private int			mNumDisplayLines;

    //private int			mNumLines;
	private int			mCurrentSelection;
    private int         mOffsetFromBottom;

	private int			mAscent;
	private int			mLineHeight;

	private ChatLine	mHead;
	//private ChatLine	mBottomDisplayLine;
	private ChatLine	mTail;

	private Font		mPlainFont;
	private Font		mItalicFont;
    private boolean     mUseEmoticons;
    private Rect        mContentArea;

	//private LineBreaker	mPlainLineBreaker;
	//private LineBreaker	mBoldLineBreaker;

	private boolean		mFirstMissed;

	static final int	kBorder		=	2;
	static final int	kLeading	=	2;

	//public static Cognet    app;
    public static Bitmap    tag_bitmap;
    public static int       tag_width, tag_height;
    public static Font      tag_font;
    public SmileyEngine smiley;
    public static boolean   isInited = false;


}



class ChatLine {

	public ChatLine(RawChatLine parent, int start, int end, boolean inFirstLine, boolean inFirstMissed) {
		//mText = inText;
		//mGlyph = glyph;
        mParent = parent;
		mFirstLine = inFirstLine;
		mFirstMissed = inFirstMissed;
        startIndex = start;
        endIndex = end;
	}

    public String getText() {
        if (mText != null) return mText;

        if (mParent.mUseSmileys) {
            mText = SmileyEngine.replaceWithUnicode(mParent.mText).substring(startIndex, endIndex);
        } else
            mText = mParent.mText.substring(startIndex, endIndex);

        return mText;
    }

	//String		mText;
	//int		    mGlyph;

    int         startIndex;
    int         endIndex;

	boolean		mFirstLine;
	boolean		mFirstMissed;
    //Font        mFont;
    String      mText;
    RawChatLine    mParent;

	//ChatLine	mPrev;
	//ChatLine	mNext;
}

class RawChatLine {

    String   mText;
    LinkedList   mChatLines;
    ChatHistory mParent;
    boolean  mFirstMissed;

    // current parsed settings
    boolean  mUseSmileys;
    Font     mFont;
    int      mColor;
    int      mWidth;
    boolean  mTemporary;

    RawChatLine(ChatHistory parent, String text, boolean firstMissed) {

        mText = text;
        mChatLines = new LinkedList();
        mParent = parent;

        mUseSmileys = parent.getEmoticons();
        mFont = parent.getPlainFont();
        mWidth = parent.getContentWidth();

        parse();
    }

//    public void addLine(ChatLine addme) {
//        mChatLines.addElement(addme);
//    }

    public int getLineCount() {
        return mChatLines.size();
    }

    public void removeChatLines(LinkedList removeFromMe) {
        //todo: a more optimized rotuine that takes advantage of the ordering.
        ListIterator iterator = mChatLines.listIterator();
        ChatLine line;
	try
	{
        while ((line = (ChatLine)iterator.next()) != null)
            removeFromMe.remove(line);
	}
	catch (Exception e)
	{
	}


    }

    public void addChatLines(LinkedList addToMe) {
        //for (int x=mChatLines.size()-1; x>=0; x--) {
        ListIterator iterator = mChatLines.listIterator();

        ChatLine line;
	try
	{
        while ((line = (ChatLine)iterator.next()) != null)
            addToMe.addFirst(line);
	}
	catch (Exception e)
	{
	}

    }

    public void parse() {
        mChatLines.clear();
        if (mParent.getEmoticons())
            parseSmileys();
        else
            parseNoSmileys();
    }

    private void parseNoSmileys() {
        int					firstChar = 0, lastChar = 0;
        boolean				firstLine = true;
        LineBreaker			lineBreaker;

        //*	Pick the right line breaker depending on the font
        LineBreaker mPlainLineBreaker = new LineBreaker("", mFont, mWidth - 8, 0);

        lineBreaker = mPlainLineBreaker;
        lineBreaker.setText(mText);

        while (lastChar < mText.length())
        {
            String		s;
            ChatLine	line;

            lastChar = lineBreaker.nextLineBreak(firstChar);

            line = new ChatLine(this, firstChar, lastChar, firstLine, mFirstMissed);
            mChatLines.addLast(line);

            firstLine = false;
            mFirstMissed = false;

            firstChar = lastChar;
        }

    }

    private void parseSmileys() {
        int					firstChar = 0, lastChar = 0;
        boolean				firstLine = true;
        //LineBreaker			lineBreaker;
        SmileyEngine        smileyEngine;
        String              inText = (mText);

        smileyEngine = new SmileyEngine(mFont, inText, mWidth-8);
        int start = 0, width;

        while (lastChar < smileyEngine.getUnicodeText().length())
             {
                 String		s;
                 ChatLine	line;

                 lastChar = smileyEngine.nextLineBreak(firstChar);

                 line = new ChatLine(this, firstChar, lastChar, firstLine, mFirstMissed);
                 mChatLines.addLast(line);

                 firstLine = false;
                 mFirstMissed = false;

                 firstChar = lastChar+1;
             }

/*        while ((width = smileyEngine.getNextLength(mWidth - 8)) != -1)
        {
            width--;
            ChatLine line = new ChatLine(this, start, start + width, firstLine, mFirstMissed);
            mChatLines.addElement(line);
            start += width;

            firstLine = false;
            mFirstMissed = false;

        }
  */
    }

}
