package pw.masy.xthread.crash;

import java.text.SimpleDateFormat;
import pw.masy.xthread.IXThread;

public interface ICrashManager {

	SimpleDateFormat DATE = new SimpleDateFormat("YYYY-MM-dd-HH-mm-ss");

	void handleCrash(IXThread thread, Throwable throwable);

}
