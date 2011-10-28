package org.usergrid.standalone;

import com.jdotsoft.jarloader.JarClassLoader;

public class ServerLoader {

	public static void main(String[] args) {
		JarClassLoader jcl = new JarClassLoader();
		try {
			jcl.invokeMain("org.usergrid.standalone.Server", args);
		} catch (Throwable e) {
			e.printStackTrace();
		}
	} // main()

}