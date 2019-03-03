package pw.masy.xthread;

/**
 * Enum defining the type of the task queue of a thread.
 */
public enum QueueOrder {

	/**
	 * The XThread won't call the tick method and only handle the task execution queue.
	 */
	QUEUE_ONLY,
	/**
	 * The XThread will handle the task execution queue before the tick method is called.
	 */
	QUEUE_BEFORE_TICK,
	/**
	 * The XThread will handle the task execution queue after the tick method is called.
	 */
	QUEUE_AFTER_TICK,
	/**
	 * The XThread won't handle the task execution queue at all.
	 */
	DISABLED

}
