package pw.masy.xthread;

import org.junit.Ignore;
import org.junit.Test;

public class CrashReportTest {

	@Ignore
	@Test
	public void testCrashReport() throws InterruptedException {
		CrashThread thread = new CrashThread();
		thread.start();

		Thread.sleep(1000);
	}

}
