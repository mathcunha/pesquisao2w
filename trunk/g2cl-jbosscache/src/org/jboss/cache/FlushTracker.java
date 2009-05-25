package org.jboss.cache;

public interface FlushTracker {

	public void block();

	public void unblock();

	public int getFlushCompletionCount();

	public void waitForFlushCompletion(long timeout);

	public void waitForFlushStart(long timeout);

}
