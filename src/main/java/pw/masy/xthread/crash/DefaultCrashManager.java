package pw.masy.xthread.crash;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pw.masy.xthread.IXThread;

/**
 * The default crash manager of every {@link IXThread}.
 */
public class DefaultCrashManager implements ICrashManager {

	private static final Logger LOGGER = LoggerFactory.getLogger("XThread");

	@Override
	public void handleCrash(IXThread thread, Throwable throwable) {
		List<String> crashReportLines = new ArrayList<>();

		// Thread info
		crashReportLines.add("-----<[ XThread Crash Report ]>-----");
		crashReportLines.add("Thread: " + thread.getName());
		crashReportLines.add("State: " + thread.getThreadState().name());

		this.addSpacer(crashReportLines, 2);

		// Throwable
		crashReportLines.add("-------------[ Cause ]-------------");
		crashReportLines.add("Type: " + throwable.getClass().getName());
		crashReportLines.add("Description: " + throwable.getMessage());
		this.addThrowable(crashReportLines, throwable);

		Throwable cause = throwable.getCause();
		if (cause != null) {
			crashReportLines.add("Caused by: " + cause.getMessage());
			this.addThrowable(crashReportLines, cause);
		}

		File crashReportDir = new File("crash_reports");

		if (!crashReportDir.exists()) crashReportDir.mkdirs();

		File crashReportFile = new File(crashReportDir, "crash_" + DATE.format(new Date(System.currentTimeMillis())) + ".txt");

		try {
			Files.write(crashReportFile.toPath(), crashReportLines, StandardCharsets.UTF_8);
		} catch (IOException ex) {
			LOGGER.error("Could not write crash report to file.", ex);
		}

		crashReportLines.forEach(LOGGER::error);
		System.exit(-1);
	}

	/**
	 * Adds the given {@link Throwable} to the crash report list.
	 *
	 * @param crashReportLines the list containing the lines of the crash report
	 * @param throwable        the {@link Throwable} to add
	 */
	private void addThrowable(List<String> crashReportLines, Throwable throwable) {
		StackTraceElement[] elements = throwable.getStackTrace();
		for (StackTraceElement element : elements) {
			crashReportLines.add("\tat " + element.toString());
		}
		if (throwable.getSuppressed().length > 0) {
			crashReportLines.add("\t... and " + throwable.getSuppressed().length + " more.");
		}
	}

	/**
	 * Adds the given number of empty lines to the crash report list.
	 *
	 * @param crashReportLines the list containing the lines of the crash report
	 * @param spaceNum         the number of empty lines to add
	 */
	private void addSpacer(List<String> crashReportLines, int spaceNum) {
		for (int n = 0; n < spaceNum; n++) {
			crashReportLines.add("");
		}
	}


}
