package pw.masy.xthread;

import java.util.concurrent.locks.LockSupport;
import org.junit.Ignore;
import org.junit.Test;

public class TPSTest {

	@Ignore
	@Test
	public void testTPS() {
		int seconds = 5;
		System.out.println("Testing ticks over a period of " + seconds + " seconds...");
		for (int tps = 10; tps <= 250; tps += 10) {
			TestThread thread = new TestThread(tps);
			thread.start();
			long startTime = System.currentTimeMillis();

			LockSupport.parkUntil(startTime + (seconds * 1000));

			double endTicks = thread.getTickCount() / (float) seconds;
			System.out.println("Fluctuation: " + endTicks + "/" + tps + " TPS");
			System.out.println("Average time in last second: " + thread.getTimings() + "/" + (1000.0f / tps) + "ms");
			thread.interrupt();
		}
	}

}
