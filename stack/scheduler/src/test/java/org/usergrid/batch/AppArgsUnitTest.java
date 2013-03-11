package org.usergrid.batch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.google.common.base.CharMatcher;

/**
 * @author zznate
 */
public class AppArgsUnitTest {

  @Test
  public void verifyDefaults() {
    AppArgs aa = AppArgs.parseArgs(new String[]{""});
    assertEquals("127.0.0.1",aa.getHost());
    assertEquals(9160, aa.getPort());
  }

  @Test
  public void verifyArgs() {
    AppArgs aa = AppArgs.parseArgs(new String[]{"-host","127.0.0.2","-appContext","classpath:/appContext.xml"});
    assertEquals("127.0.0.2",aa.getHost());
    assertNotNull(aa.getAppContext());
  }

  @Test
  public void verifyContextSwitch() {
    AppArgs appArgs = AppArgs.parseArgs(new String[]{"-appContext","classpath:/appContext.xml"});
    assertEquals("/appContext.xml",getIndex(appArgs.getAppContext()));
    appArgs = AppArgs.parseArgs(new String[]{"-appContext","/appContext.xml"});
    assertEquals("/appContext.xml",getIndex(appArgs.getAppContext()));
  }


  private String getIndex(String path) {
    int index = CharMatcher.is(':').indexIn(path);
    if ( index > 0 ) {
      return path.substring(++index);
    } else {
      return path;
    }
  }
}
