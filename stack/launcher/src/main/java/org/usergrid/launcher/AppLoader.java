package org.usergrid.launcher;

import com.jdotsoft.jarloader.JarClassLoader;

public class AppLoader {

	public static void main(String[] args) {
		JarClassLoader jcl = new JarClassLoader();
		try {
			jcl.invokeMain("org.usergrid.launcher.App", args);
		} catch (Throwable e) {
			e.printStackTrace();
		}
	} // main()

}
