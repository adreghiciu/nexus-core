package org.sonatype.nexus.scheduling;

import java.util.List;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;

import org.codehaus.plexus.logging.LoggerManager;
import org.sonatype.scheduling.IteratingTask;
import org.sonatype.scheduling.NoSuchTaskException;
import org.sonatype.scheduling.ScheduledTask;
import org.sonatype.scheduling.Scheduler;
import org.sonatype.scheduling.SubmittedTask;
import org.sonatype.scheduling.iterators.SchedulerIterator;
import org.sonatype.scheduling.schedules.Schedule;

/**
 * The Nexus scheduler.
 * 
 * @author cstamas
 * @plexus.component
 */
public class DefaultNexusScheduler
    implements NexusScheduler
{
    /**
     * The scheduler.
     * 
     * @plexus.requirement
     */
    private Scheduler scheduler;

    /**
     * The lm
     * 
     * @plexus.requirement
     */
    private LoggerManager loggerManager;

    public <T> SubmittedTask<T> submit( NexusTask<T> nexusTask )
        throws RejectedExecutionException,
            NullPointerException
    {
        nexusTask.setLogger( loggerManager.getLoggerForComponent( nexusTask.getClass().getName() ) );

        Class<?> cls = nexusTask.getClass();

        List<SubmittedTask<?>> existingTasks = scheduler.getActiveTasks().get( cls );

        if ( existingTasks == null || nexusTask.allowConcurrentExecution( existingTasks ) )
        {
            return scheduler.submit( nexusTask );
        }
        else
        {
            throw new RejectedExecutionException( "Task of this type is already submitted!" );
        }
    }

    public <T> IteratingTask<T> iterate( NexusTask<T> nexusTask, SchedulerIterator iterator )
        throws RejectedExecutionException,
            NullPointerException
    {
        nexusTask.setLogger( loggerManager.getLoggerForComponent( nexusTask.getClass().getName() ) );

        Class<?> cls = nexusTask.getClass();

        List<SubmittedTask<?>> existingTasks = scheduler.getActiveTasks().get( cls );

        if ( existingTasks == null || nexusTask.allowConcurrentExecution( existingTasks ) )
        {
            return scheduler.iterate( nexusTask, iterator );
        }
        else
        {
            throw new RejectedExecutionException( "Task of this type is already submitted!" );
        }
    }

    public <T> ScheduledTask<T> schedule( NexusTask<T> nexusTask, Schedule schedule )
        throws RejectedExecutionException,
            NullPointerException
    {
        nexusTask.setLogger( loggerManager.getLoggerForComponent( nexusTask.getClass().getName() ) );

        Class<?> cls = nexusTask.getClass();

        List<SubmittedTask<?>> existingTasks = scheduler.getActiveTasks().get( cls );

        if ( existingTasks == null || nexusTask.allowConcurrentExecution( existingTasks ) )
        {
            return scheduler.schedule( nexusTask, schedule );
        }
        else
        {
            throw new RejectedExecutionException( "Task of this type is already scheduled!" );
        }
    }

    public Map<String, List<SubmittedTask<?>>> getActiveTasks()
    {
        return scheduler.getActiveTasks();
    }

    public SubmittedTask<?> getTaskById( String id )
        throws NoSuchTaskException
    {
        return scheduler.getTaskById( id );
    }

}
