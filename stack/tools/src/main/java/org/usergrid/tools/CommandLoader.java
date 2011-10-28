package org.usergrid.tools;

import com.jdotsoft.jarloader.JarClassLoader;

public class CommandLoader {

	public static void main(String[] args) {
		JarClassLoader jcl = new JarClassLoader();
		try {
			jcl.invokeMain("org.usergrid.tools.Command", args);
		} catch (Throwable e) {
			e.printStackTrace();
		}
	} // main()

}
