package pw.masy.xthread.crash;

import java.text.SimpleDateFormat;
import pw.masy.xthread.IXThread;

/**
 * Base interface for all crash managers.
 */
public interface ICrashManager {

	SimpleDateFormat DATE = new SimpleDateFormat("YYYY-MM-dd-HH-mm-ss");

	/**
	 * Handles a crash report.
	 *
	 * @param thread    the {@link IXThread} the crash happened in
	 * @param throwable the {@link Throwable} that caused the crash
	 */
	void handleCrash(IXThread thread, Throwable throwable);

}
