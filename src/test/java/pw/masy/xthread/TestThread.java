package pw.masy.xthread;

import java.util.concurrent.locks.LockSupport;

public class TestThread extends XThread {

	/**
	 * Constructs a new XThread.
	 *
	 * @param tps           the ticks per second the thread tries to achieve
	 */
	public TestThread(int tps) {
		super("Test", 5, true, tps, QueueOrder.QUEUE_BEFORE_TICK, 250);
	}

	@Override
	public void onStart() throws Exception {

	}

	@Override
	protected void setup() throws Exception {

	}

	@Override
	protected void tick(long currentTime, long tickCount) {
		int sleep = 1000 / this.getTps() - 2;
		LockSupport.parkUntil(currentTime + sleep);
	}

	@Override
	public void onStop() throws Exception {

	}

}
