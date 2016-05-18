package org.apache.usergrid.apm.service.crashlogparser;


/**
 * 
 * @author Paul Dardeau
 *
 */

public interface CrashLogParser
{
	public boolean parseCrashLog (String fileContents);
	/*public boolean parseCrashLog(String fileContents,HashMap<String,String> mapCrashAttributes);
	public Object toModel(HashMap<String,String> mapCrashAttributes);
	public Object toModel(String fileName);
	*/
	public String getCrashSummary();
}
