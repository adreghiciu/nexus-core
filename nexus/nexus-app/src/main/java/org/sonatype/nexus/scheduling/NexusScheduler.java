package org.sonatype.nexus.scheduling;

import java.util.List;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;

import org.sonatype.scheduling.IteratingTask;
import org.sonatype.scheduling.NoSuchTaskException;
import org.sonatype.scheduling.ScheduledTask;
import org.sonatype.scheduling.SubmittedTask;
import org.sonatype.scheduling.iterators.SchedulerIterator;
import org.sonatype.scheduling.schedules.Schedule;

public interface NexusScheduler
{
    String ROLE = NexusScheduler.class.getName();

    /**
     * Issue a NexusTask for immediate execution, but have a control over it.
     * 
     * @param runnable
     * @return
     */
    <T> SubmittedTask<T> submit( NexusTask<T> nexusTask )
        throws RejectedExecutionException,
            NullPointerException;

    /**
     * Issue a NexusTask for iterating execution.
     * 
     * @param runnable
     * @param iterator
     * @return
     */
    <T> IteratingTask<T> iterate( NexusTask<T> nexusTask, SchedulerIterator iterator )
        throws RejectedExecutionException,
            NullPointerException;

    /**
     * Issue a NexusTask for scheduled execution.
     * 
     * @param runnable
     * @param iterator
     * @return
     */
    <T> ScheduledTask<T> schedule( NexusTask<T> nexusTask, Schedule schedule )
        throws RejectedExecutionException,
            NullPointerException;

    /**
     * Returns the map of currently active tasks. The resturned collection is an unmodifiable snapshot. It may differ
     * from current one (if some thread finishes for example during processing of the returned list).
     * 
     * @return
     */
    Map<String, List<SubmittedTask<?>>> getActiveTasks();

    /**
     * Returns an active task by it's ID.
     * 
     * @param id
     * @return
     */
    SubmittedTask<?> getTaskById( String id )
        throws NoSuchTaskException;
}
