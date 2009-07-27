package org.sonatype.nexus.events;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.sonatype.nexus.proxy.events.AbstractEventInspector;
import org.sonatype.nexus.proxy.events.EventInspector;
import org.sonatype.nexus.proxy.events.RepositoryEventLocalUrlChanged;
import org.sonatype.nexus.scheduling.NexusScheduler;
import org.sonatype.nexus.tasks.ExpireCacheTask;
import org.sonatype.plexus.appevents.Event;

@Component( role = EventInspector.class, hint = "RepositoryLocalUrlChanged" )
public class RepositoryLocalUrlChangedInspector
    extends AbstractEventInspector
{
    @Requirement
    private NexusScheduler nexusScheduler;

    public boolean accepts( Event<?> evt )
    {
        return ( evt instanceof RepositoryEventLocalUrlChanged );
    }

    public void inspect( Event<?> evt )
    {
        RepositoryEventLocalUrlChanged event = (RepositoryEventLocalUrlChanged) evt;

        if ( event.getOldLocalUrl().equals( event.getNewLocalUrl() ) )
        {
            return;
        }

        getLogger().info(
            "The local url of repository '" + event.getRepository().getId()
                + "' has been changed, now expire its caches." );

        ExpireCacheTask task = nexusScheduler.createTaskInstance( ExpireCacheTask.class );

        task.setRepositoryId( event.getRepository().getId() );

        nexusScheduler.submit( "Expire caches for repository '" + event.getRepository().getId() + "'.", task );
    }
}
