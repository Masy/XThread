package pw.masy.xthread;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.slf4j.Logger;

/**
 * Base Interface for the {@link XThread}.
 */
public interface IXThread extends Runnable, Thread.UncaughtExceptionHandler {

	List<IXThread> THREADS = new ArrayList<>();
	ReadWriteLock THREAD_LOCK = new ReentrantReadWriteLock();

	/**
	 * Gets the name of the thread.
	 *
	 * @return the name of the thread
	 */
	String getName();

	/**
	 * Gets the task threshold of the thread.
	 *
	 * @return the task threshold of the thread
	 */
	int getTaskThreshold();

	/**
	 * Sets the task threshold.
	 *
	 * @param newThreshold the new task threshold
	 */
	void setTaskThreshold(int newThreshold);

	/**
	 * Gets the thread queue of the thread.
	 *
	 * @return the thread queue of the thread as {@link CopyOnWriteArrayList}
	 */
	CopyOnWriteArrayList<IXThread> getThreadQueue();

	/**
	 * Gets the {@link Logger} of the thread.
	 *
	 * @return the {@link Logger} of the thread
	 */
	Logger getLogger();

	/**
	 * Gets the tps the thread currently tries to achieve.
	 *
	 * @return the tps the thread currently tries to achieve
	 */
	int getTps();

	/**
	 * Sets the tps to the given value.
	 *
	 * <p>This will also recalculate the sleep lookup table.</p>
	 *
	 * @param newTps the new tps of the thread.
	 */
	void setTps(int newTps);

	/**
	 * Checks if the thread is a no sleep thread.
	 *
	 * @return <i>true</i> if the thread is a no sleep thread
	 */
	boolean isNoSleepThread();

	/**
	 * Checks if the thread has been started.
	 *
	 * @return <i>true</i> if the thread already started
	 */
	boolean isStarted();

	/**
	 * Checks if the thread is set up.
	 *
	 * @return <i>true</i> if the thread is setup
	 */
	boolean isSetup();

	/**
	 * Checks if the thread is currently stopping.
	 *
	 * @return <i>true</i> if the thread is stopping
	 */
	boolean isStopping();

	/**
	 * Get the current state of the thread.
	 *
	 * @return the current state of the thread as {@link State}
	 */
	State getThreadState();

	/**
	 * Gets the number of ticks the thread has ticked.
	 *
	 * @return the number of ticks the thread has ticked
	 */
	long getTickCount();

	/**
	 * Gets the timings of the last second.
	 *
	 * @return the timings of the last second
	 */
	float getTimings();

	/**
	 * Thread-safe getter for the {@link #THREADS} list.
	 *
	 * @return the list of all {@link XThread}s
	 */
	static List<IXThread> getThreads() {
		try {
			THREAD_LOCK.readLock().lock();
			return THREADS;
		} finally {
			THREAD_LOCK.readLock().unlock();
		}
	}

	/**
	 * Adds the given task to the task execution queue of this thread if it isn't disabled.
	 *
	 * <p>Depending on the queue order the task will be executed either before or after the next tick of this thread.</p>
	 *
	 * @param task the {@link Runnable} that will be added to the task execution queue
	 */
	void addTask(Runnable task);

	/**
	 * Adds a callback to the list of setup callbacks if the thread isn't already set up.
	 *
	 * @param callback the {@link Runnable} that will be added to the list of setup callbacks
	 */
	void addSetupCallback(Runnable callback);

	/**
	 * Starts the thread.
	 */
	void start();

	/**
	 * Interrupts the thread.
	 */
	void interrupt();

	enum State {
		STOPPED,
		STARTING,
		INITIALIZING,
		RUNNING,
		STOPPING,
	}

}
