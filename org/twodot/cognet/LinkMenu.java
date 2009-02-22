/*
 * cognet chat app
 *
 * Copyright 2003, Daniel Grobe Scahs <sachs@uiuc.edu>
 * See LICENSE for redistribution terms
 *
 */
package org.twodot.cognet;

import danger.ui.Menu;
import danger.ui.MenuItem;

class LinkQueue 
{
	public static final int maxQueueLen = 9;
	
	private String LinkURLs[] = new String[maxQueueLen];
	private String LinkNames[] = new String[maxQueueLen]; 
	private int queueLen = 0;

	public void addLink(String URL, String Name)
	{
		System.err.println("Adding "+URL);
		if( queueLen < maxQueueLen )
			queueLen++;
		else
		{
			int i;

			for( i = 0; i < queueLen-1; i++ )
			{
				LinkURLs[i] = LinkURLs[i+1];
				LinkNames[i] = LinkNames[i+1];
			}
		}

		if( Name.length() == 0 )
			Name = URL;

		LinkURLs[queueLen-1] = URL;
		LinkNames[queueLen-1] = Name;
	}

	public String[] getNames()
	{
		return LinkNames;
	}

	public String[] getURLs()
	{
		return LinkURLs;
	}

	public int getQueueLen()
	{
		return queueLen;
	}
}


public class LinkMenu
{
	public static final int maxLinkMenus = 9;
	public static final int maxLinkTargets = maxLinkMenus * LinkQueue.maxQueueLen;

	int numMenus;

	public String Categories[] = new String[maxLinkMenus];
	public LinkQueue Queue[] = new LinkQueue[maxLinkMenus];

	public String LinkTarget[] = new String[maxLinkTargets];

	public void AddLink(String URL, String Category, String Name)
	{
		synchronized(this)
		{
			int catIndex = findCategory(Category);
			Queue[catIndex].addLink(URL, Name);
		}
	}

	public Menu makeLinkMenu(int base)
	{
		Menu Linkmenu;
		int i,j;
		int index = 0; 

                Menu links = new Menu("Links");
		MenuItem mi;

		synchronized(this)
		{
			if( numMenus == 0 )
				return null;

			for( i = 0; i < numMenus; i++ )
			{
				int size;
				String Names[], URLs[];

				Menu submenu = new Menu(Categories[i]);

	                	size = Queue[i].getQueueLen();
				Names = Queue[i].getNames();
                		URLs = Queue[i].getURLs();

                		for( j = (size-1); j > -1; j-- )
				{
                			submenu.addItem(Names[j],index+base);
					LinkTarget[index++] = URLs[j];
				}

				mi = links.addItem(Categories[i]);
				mi.addSubMenu(submenu);
			}
		}

		return links;
	}

	public void dispatchLink(int target, int base)
	{
		try 
		{
			danger.net.URL.gotoURL(LinkTarget[target-base]);
		}
		catch( Exception e )
		{
		}
	}

	private int findCategory(String Category)
	{
		int i;

		for( i=0; i < numMenus; i++ )
		{
			if( 0 == Category.compareTo(Categories[i]) )
				return i;
		}

		if( numMenus == maxLinkMenus )
		{
			for( i=0; i < numMenus-1; i++ )
			{
				Categories[i] = Categories[i+1];
				Queue[i] = Queue[i+1];
			}
		}
		else
			numMenus ++;

		Categories[numMenus - 1] = Category;
		Queue[numMenus - 1] = new LinkQueue();

		return numMenus - 1;
	}
}
