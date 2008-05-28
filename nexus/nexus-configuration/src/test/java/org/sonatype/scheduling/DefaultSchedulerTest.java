package org.sonatype.scheduling;

import java.util.Date;
import java.util.concurrent.Callable;

import org.codehaus.plexus.PlexusTestCase;
import org.sonatype.scheduling.iterators.ScheduleIterator;

public class DefaultSchedulerTest
    extends PlexusTestCase
{
    protected DefaultScheduler defaultScheduler;

    public void setUp()
        throws Exception
    {
        super.setUp();

        defaultScheduler = (DefaultScheduler) lookup( Scheduler.class.getName() );
    }

    public void testSimpleRunnable()
        throws Exception
    {
        TestRunnable tr = null;

        tr = new TestRunnable();

        SubmittedTask st = defaultScheduler.submit( tr );

        assertEquals( 1, defaultScheduler.getScheduledTasks().size() );

        while ( !st.isDone() )
        {
            Thread.sleep( 300 );
        }

        assertEquals( 1, tr.getRunCount() );

        assertEquals( TaskState.FINISHED, st.getTaskState() );

        assertEquals( 0, defaultScheduler.getScheduledTasks().size() );
    }

    public void testSimpleCallable()
        throws Exception
    {
        TestCallable tr = null;

        tr = new TestCallable();

        SubmittedCallableTask<Integer> st = defaultScheduler.submit( tr );

        assertEquals( 1, defaultScheduler.getScheduledTasks().size() );

        while ( !st.isDone() )
        {
            Thread.sleep( 300 );
        }

        assertEquals( 1, tr.getRunCount() );

        assertEquals( Integer.valueOf( 0 ), st.getIfDone() );

        assertEquals( TaskState.FINISHED, st.getTaskState() );

        assertEquals( 0, defaultScheduler.getScheduledTasks().size() );
    }

    public void testSecondsRunnable()
        throws Exception
    {
        TestRunnable tr = null;

        tr = new TestRunnable();

        long nearFuture = System.currentTimeMillis() + 500;

        ScheduleIterator iterator = new SecondScheduleIterator( new Date( nearFuture ), new Date( nearFuture + 4900 ) );

        IteratingTask st = defaultScheduler.iterate( tr, iterator );

        assertEquals( 1, defaultScheduler.getScheduledTasks().size() );

        while ( !st.isDone() )
        {
            Thread.sleep( 300 );
        }

        assertEquals( 5, tr.getRunCount() );

        assertEquals( TaskState.FINISHED, st.getTaskState() );

        assertEquals( 0, defaultScheduler.getScheduledTasks().size() );
    }

    public void testSecondsCallable()
        throws Exception
    {
        TestCallable tr = null;

        tr = new TestCallable();

        long nearFuture = System.currentTimeMillis() + 500;

        ScheduleIterator iterator = new SecondScheduleIterator( new Date( nearFuture ), new Date( nearFuture + 4900 ) );

        IteratingCallableTask<Integer> st = defaultScheduler.iterate( tr, iterator );

        assertEquals( 1, defaultScheduler.getScheduledTasks().size() );

        while ( !st.isDone() )
        {
            Thread.sleep( 300 );
        }

        assertEquals( 5, tr.getRunCount() );

        assertEquals( 5, st.getResultCount() );

        assertEquals( Integer.valueOf( 0 ), st.get( 0 ) );

        assertEquals( Integer.valueOf( 1 ), st.get( 1 ) );

        assertEquals( Integer.valueOf( 2 ), st.get( 2 ) );

        assertEquals( Integer.valueOf( 3 ), st.get( 3 ) );

        assertEquals( Integer.valueOf( 4 ), st.get( 4 ) );

        assertEquals( TaskState.FINISHED, st.getTaskState() );

        assertEquals( 0, defaultScheduler.getScheduledTasks().size() );
    }

    public void testCancelRunnable()
        throws Exception
    {
        TestRunnable tr = null;

        tr = new TestRunnable();

        long nearFuture = System.currentTimeMillis() + 500;

        ScheduleIterator iterator = new SecondScheduleIterator( new Date( nearFuture ), new Date( nearFuture + 4900 ) );

        IteratingTask st = defaultScheduler.iterate( tr, iterator );

        assertEquals( 1, defaultScheduler.getScheduledTasks().size() );

        st.cancel();

        assertEquals( 0, tr.getRunCount() );

        assertTrue( st.isDone() );

        assertEquals( TaskState.CANCELLED, st.getTaskState() );

        assertEquals( 0, defaultScheduler.getScheduledTasks().size() );
    }

    public void testCancelCallable()
        throws Exception
    {
        TestCallable tr = null;

        tr = new TestCallable();

        long nearFuture = System.currentTimeMillis() + 500;

        ScheduleIterator iterator = new SecondScheduleIterator( new Date( nearFuture ), new Date( nearFuture + 4900 ) );

        IteratingTask st = defaultScheduler.iterate( tr, iterator );

        assertEquals( 1, defaultScheduler.getScheduledTasks().size() );

        st.cancel();

        assertEquals( 0, tr.getRunCount() );

        assertTrue( st.isDone() );

        assertEquals( TaskState.CANCELLED, st.getTaskState() );

        assertEquals( 0, defaultScheduler.getScheduledTasks().size() );
    }

    // Helper classes

    public class TestRunnable
        implements Runnable
    {
        private int runCount = 0;

        public void run()
        {
            runCount++;
        }

        public int getRunCount()
        {
            return runCount;
        }
    }

    public class TestCallable
        implements Callable<Integer>
    {
        private int runCount = 0;

        public Integer call()
            throws Exception
        {
            return runCount++;
        }

        public int getRunCount()
        {
            return runCount;
        }
    }

}
