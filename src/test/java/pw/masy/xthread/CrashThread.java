package pw.masy.xthread;

public class CrashThread extends XThread {

	public CrashThread() {
		super("CrashThread", 5, true, 20);
	}

	@Override
	protected void onStart() throws Exception {

	}

	@Override
	protected void setup() throws Exception {
		throw new IllegalArgumentException("This is a test");
	}

	@Override
	protected void tick(long currentTime, long tickCount) {

	}

	@Override
	public void onStop() throws Exception {

	}

}
