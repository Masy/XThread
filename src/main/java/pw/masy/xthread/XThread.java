package pw.masy.xthread;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class representing the XThread.
 */
public abstract class XThread implements Runnable, Thread.UncaughtExceptionHandler {

	private static final List<XThread> THREADS = new ArrayList<>();
	private static final ReentrantReadWriteLock THREAD_LOCK = new ReentrantReadWriteLock();

	private Thread thread;
	/**
	 * The name of the thread.
	 */
	@Getter private String name;
	private final BlockingQueue<Runnable> taskExecutionQueue = new LinkedBlockingQueue<>();
	/**
	 * The warning threshold of the {@link #taskExecutionQueue}.<br>
	 * If the queue contains more tasks than the threshold a warning will be logged.
	 */
	@Getter @Setter private int taskThreshold;
	/**
	 * The list containing the {@link XThread}'s this thread is waiting for.
	 */
	@Getter private final CopyOnWriteArrayList<XThread> threadQueue = new CopyOnWriteArrayList<>();
	private final List<Runnable> setupCallbacks = new ArrayList<>();
	private final QueueOrder queueOrder;
	/**
	 * The {@link Logger} of the thread.
	 */
	@Getter protected final Logger logger;
	/**
	 * The ticks per second the thread tries to achieve.
	 */
	@Getter private int tps;
	private int[] sleepLookUpTable;
	/**
	 * Whether this thread is a no sleep thread or not.
	 */
	@Getter private boolean noSleepThread;
	/**
	 * Whether this thread is set up or not.
	 */
	@Getter private boolean setup;
	/**
	 * The number of ticks this thread has since it started.
	 */
	@Getter private long tickCount;
	/**
	 * The average time in ms a tick lasted in the last second.
	 */
	@Getter private float timings;

	/**
	 * Constructs a new XThread.
	 *
	 * @param name          the name of the thread and therefore the logger too
	 * @param priority      the priority of the thread
	 * @param isDaemon      whether this thread ios a daemon of the main thread or not
	 * @param tps           the ticks per second the thread tries to achieve
	 * @param queueOrder    the {@link QueueOrder} of the thread
	 * @param taskThreshold the task threshold from when to start to warn
	 * @param threadQueue   the {@link XThread}'s this thread is waiting for before it starts to setup
	 */
	public XThread(String name, int priority, boolean isDaemon, int tps, QueueOrder queueOrder, int taskThreshold, XThread... threadQueue) {
		this.thread = new Thread(this);
		this.name = name;
		this.logger = LoggerFactory.getLogger(name);
		this.queueOrder = queueOrder;
		this.taskThreshold = taskThreshold;
		this.setTps(tps);

		this.thread.setName(name);
		this.thread.setPriority(priority);
		this.thread.setDaemon(isDaemon);
		this.thread.setUncaughtExceptionHandler(this);

		for (XThread thread : threadQueue) {
			this.threadQueue.addIfAbsent(thread);
		}

		boolean locked = false;
		try {
			locked = THREAD_LOCK.writeLock().tryLock();
			THREADS.add(this);
		} finally {
			if (locked) THREAD_LOCK.writeLock().unlock();
		}
	}

	/**
	 * Thread-safe getter for the {@link #THREADS} list.
	 *
	 * @return the list of all {@link XThread}s
	 */
	public static List<XThread> getThreads() {
		boolean locked = false;
		try {
			locked = THREAD_LOCK.readLock().tryLock();
			return getThreads();
		} finally {
			if (locked) THREAD_LOCK.readLock().unlock();
		}
	}

	/**
	 * Sets the tps to the given value.
	 *
	 * <p>This will also recalculate the {@link #sleepLookUpTable}.</p>
	 *
	 * @param newTps the new tps of the thread.
	 * @see #calcSleepTable()
	 */
	public void setTps(int newTps) {
		this.tps = newTps;
		this.noSleepThread = this.tps < 1;
		if (!this.noSleepThread) this.calcSleepTable();
	}

	/**
	 * Calculates the values for the {@link #sleepLookUpTable}.
	 */
	private void calcSleepTable() {
		int[] table = new int[this.tps];

		int valueLeft = 1000 / this.tps;
		int valueRight = valueLeft + 1;
		int counterLeft = 0;
		int counterRight = 0;

		for (int n = 0, remainder = 1000; n < table.length; n++) {
			int maxSleepTime = remainder / (tps - n);
			remainder -= maxSleepTime;
			if (maxSleepTime == valueLeft) counterLeft++;
			else counterRight++;
		}

		boolean leftGreaterRight = counterLeft > counterRight;
		int filler = leftGreaterRight ? valueLeft : valueRight;
		int value = leftGreaterRight ? valueRight : valueLeft;
		int size = leftGreaterRight ? counterRight : counterLeft;
		float increment = leftGreaterRight ? ((float) counterLeft / counterRight) : ((float) counterRight / counterLeft);
		float counter = increment;
		for (int n = 0; n < this.tps; n++) {
			if (table[n] == 0) table[n] = filler;
			if (n < size) {
				table[Math.round(counter)] = value;
				counter += 1 + increment;
			}
		}

		this.sleepLookUpTable = table;
	}

	@Override
	public void run() {
		while (!this.threadQueue.isEmpty() && !Thread.interrupted()) {
			try {
				Thread.sleep(1L);
			} catch (InterruptedException ex) {
				this.logger.error("Error while waiting on other threads to start!", ex);
				return;
			}
		}

		if (!this.init()) {
			return;
		}

		long currentCycleTime;
		long lastCycleDuration = 0;
		int overhead = 0;
		int ticks;

		start: if (this.noSleepThread) {
			while (!Thread.interrupted()) {
				if (this.queueOrder.ordinal() == 0 || this.queueOrder.ordinal() == 1)
					this.processQueue();

				if (this.queueOrder.ordinal() != 0)
					this.tick(System.currentTimeMillis(), this.tickCount++);

				if (this.queueOrder.ordinal() == 2)
					this.processQueue();

				if (!this.noSleepThread) break start;
			}
		} else {
			while (!Thread.interrupted()) {
				long start = System.currentTimeMillis();
				for (ticks = 0; ticks < this.tps; ticks++) {
					currentCycleTime = System.currentTimeMillis();

					overhead += lastCycleDuration - this.sleepLookUpTable[ticks];
					if (overhead > 2000) overhead = 2000;
					else if (overhead < 0) overhead = 0;

					{
						if (this.queueOrder.ordinal() == 0 || this.queueOrder.ordinal() == 1)
							this.processQueue();

						if (this.queueOrder.ordinal() != 3)
							this.tick(currentCycleTime, this.tickCount++);

						if (this.queueOrder.ordinal() == 2)
							this.processQueue();
					}

					lastCycleDuration = System.currentTimeMillis() - currentCycleTime;

					try {
						Thread.sleep(Math.max(0L, this.sleepLookUpTable[ticks] - overhead - lastCycleDuration));
						if (this.noSleepThread) break start;
					} catch (InterruptedException ex) {
						return;
					}
				}
				long end = System.currentTimeMillis();
				this.timings = (float) (end - start) / this.tps;
			}
		}
	}

	/**
	 * Processes the {@link #taskExecutionQueue}.
	 *
	 * <p>If the {@link #taskExecutionQueue} contains more tasks than the the {@link #taskThreshold} allows a warning will be logged.</p>
	 */
	private void processQueue() {
		if (this.taskExecutionQueue.size() > this.taskThreshold) {
			this.logger.warn("{} tasks in my execution queue. Did i lag?", this.taskExecutionQueue.size());
		}

		try {
			while (!this.taskExecutionQueue.isEmpty()) {
				this.taskExecutionQueue.take().run();
			}
		} catch (InterruptedException ex) {
			this.logger.error("Error while processing task queue!", ex);
		}
	}

	/**
	 * Adds the given task to the {@link #taskExecutionQueue} of this thread if it isn't disabled.
	 *
	 * <p>Depending on the {@link #queueOrder} the task will be executed either before or after the next tick of this thread.</p>
	 *
	 * @param task the {@link Runnable} that will be added to the {@link #taskExecutionQueue}
	 */
	public void addTask(Runnable task) {
		if (this.queueOrder != QueueOrder.DISABLED) this.taskExecutionQueue.add(task);
	}

	/**
	 * Adds a callback to the {@link #setupCallbacks} list if the thread isn't already set up.
	 *
	 * @param callback the {@link Runnable} that will be added to the {@link #setupCallbacks} list
	 */
	public void addSetupCallback(Runnable callback) {
		if (this.setup) {
			this.logger.warn("Tried adding setup callback but thread is already setup.");
		} else {
			this.setupCallbacks.add(callback);
		}
	}

	/**
	 * Setup method that is called when the thread initializes.
	 *
	 * @throws Exception when an exception occurs. This is to prevent users from using a try/catch in the setup method.
	 */
	protected abstract void setup() throws Exception;

	/**
	 * Initializes the thread and then removed it from the thread queue of all known {@link XThread}'s.
	 *
	 * @return <i>true</i> if the setup method was called without an exception
	 * @see #setup
	 */
	private boolean init() {
		try {
			this.setup();
		} catch (Throwable t) {
			this.uncaughtException(this.thread, t);
			return false;
		}

		this.setup = true;

		for (Runnable callback : this.setupCallbacks) {
			callback.run();
		}

		this.setupCallbacks.clear();

		boolean locked = false;
		try {
			locked = THREAD_LOCK.readLock().tryLock();
			for (XThread thread : THREADS) {
				thread.getThreadQueue().remove(this);
			}
		} finally {
			if (locked) THREAD_LOCK.readLock().unlock();
		}

		return true;
	}

	/**
	 * Executes one tick of the thread.
	 *
	 * @param currentTime the current time in milliseconds
	 * @param tickCount   the current tick count
	 */
	protected abstract void tick(long currentTime, long tickCount);

	/**
	 * Method which gets called before the thread initializes.
	 *
	 * @throws Exception when an exception occurs. This is to prevent users from using a try/catch in this method.
	 */
	public abstract void onStart() throws Exception;

	/**
	 * Calls the {@link #onStart()} method and starts the thread.
	 *
	 * @see #onStart()
	 */
	public void start() {
		try {
			this.onStart();
		} catch (Throwable t) {
			this.uncaughtException(Thread.currentThread(), t);
		}
		this.thread.start();
	}

	/**
	 * Method which gets called before the thread stops.
	 *
	 * @throws Exception when an exception occurs. This is to prevent users from using a try/catch in the setup method.
	 */
	public abstract void onStop() throws Exception;

	/**
	 * Calls {@link #onStop()} and interrupts the thread.
	 *
	 * @see #onStop()
	 */
	public final void interrupt() {
		try {
			this.onStop();
		} catch (Throwable t) {
			this.uncaughtException(this.thread, t);
		}

		this.thread.interrupt();
	}

	@Override
	public void uncaughtException(Thread t, Throwable e) {
		this.logger.error("Caught uncaught exception in this thread!", t);
		// TODO: handle crash
	}
}
