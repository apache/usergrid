/*
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements.  See the NOTICE file distributed with
* this work for additional information regarding copyright ownership.
* The ASF licenses this file to You under the Apache License, Version 2.0
* (the "License"); you may not use this file except in compliance with
* the License.  You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.apache.usergrid.apm.service.crashlogparser;

import java.io.IOException;
import java.io.FileReader;
import java.io.BufferedReader;
import java.util.HashMap;

/**
 * 
 * @author Paul Dardeau
 *
 */


public abstract class AbstractCrashLogParser implements CrashLogParser
{
	
	protected HashMap<String,String> mapCrashAttributes = null;
	
	public abstract boolean parseCrashLog(String fileContents);
	
	public abstract Object toModel(HashMap<String,String> mapCrashAttributes);
	
	public AbstractCrashLogParser () {
		mapCrashAttributes = new HashMap<String, String> ();
	}
	
		
	public String readFile(String fileName) throws IOException
	{
		BufferedReader br = new BufferedReader(new FileReader(fileName));
		try {
			StringBuilder sb = new StringBuilder();
		    String line = br.readLine();

		    while (line != null) {
		    	sb.append(line);
		        sb.append("\n");
		        line = br.readLine();
		    }
		    return sb.toString();
		} finally {
			br.close();
		}
	}
	
	public String getValueForKey(String key,String fileContents)
	{
		String value = null;
		int posKey = fileContents.indexOf(key);
		
		if( posKey > -1 ) {
			final int keyLength = key.length();
			final int posEndOfKey = posKey + keyLength;
			final int posCR = fileContents.indexOf('\r', posEndOfKey);
			final int posLF = fileContents.indexOf('\n', posEndOfKey);
			
			int posEOL = -1;
			
			if( (posCR > -1) && (posLF > -1) )
			{
				// have both
				if( posCR < posLF )
				{
					posEOL = posCR;
				}
				else
				{
					posEOL = posLF;
				}
			}
			else if( posCR > -1 )
			{
				posEOL = posCR;
			}
			else if( posLF > -1 )
			{
				posEOL = posLF;
			}
			
			if( posEOL > -1 )
			{
				value = fileContents.substring(posEndOfKey,posEOL);
			}
			else
			{
				value = fileContents.substring(posEndOfKey);
			}
			
			value = value.trim();
		}
		
		return value;
	}
	
	public String getLineAfterString(String stringToLocate,String fileContents)
	{
		String followingLine = null;
		
		final int posStringToLocate = fileContents.indexOf(stringToLocate);
		
		if( posStringToLocate > -1 )
		{
			int posCurrentEOL = fileContents.indexOf('\n', posStringToLocate + stringToLocate.length());
			
			if( posCurrentEOL > posStringToLocate )
			{
				int posNextEOL = fileContents.indexOf('\n', posCurrentEOL + 1);
				
				if( posNextEOL > posCurrentEOL )
				{
					followingLine = fileContents.substring(posCurrentEOL+1,posNextEOL);
					if( followingLine.length() > 0 && followingLine.endsWith("\r") )
					{
						followingLine = followingLine.substring(0,followingLine.length()-1);
					}
				}
			}
		}
		
		return followingLine;
	}
	
	public String getNonBlankLinesAfterString(String stringToLocate,String fileContents)
	{
		String followingLines = null;
		
		final int posStringToLocate = fileContents.indexOf(stringToLocate);
		
		if( posStringToLocate > -1 )
		{
			int posSearchEOL = fileContents.indexOf('\n', posStringToLocate + stringToLocate.length());
			
			if( posSearchEOL > posStringToLocate )
			{
				final int posStart = posSearchEOL + 1;
				int posEnd = -1;
				final int posEndOfString = fileContents.length() - 1;
				
				int posCurrentEOL = posSearchEOL;
				
				boolean retrievingLines = true;
				
				while(retrievingLines) {
					// keep pulling subsequent lines until we find a blank line or reach the end
					int posNextEOL = fileContents.indexOf('\n', posCurrentEOL + 1);
				
					if( posNextEOL > posCurrentEOL )
					{
						String followingLine = fileContents.substring(posCurrentEOL+1,posNextEOL).trim();
						if( followingLine.length() == 0 )
						{
							retrievingLines = false;
							posEnd = posCurrentEOL;
						}
						else
						{
							posCurrentEOL = posNextEOL;
						}
					}
					else
					{
						// reached end of string
						posEnd = posEndOfString;
					}
				}
				
				if( posEnd > -1 && posEnd > posStart)
				{
					followingLines = fileContents.substring(posStart,posEnd);
				}
				else
				{
					followingLines = fileContents.substring(posStart);
				}
			}
		}
		
		return followingLines;
	}

}
