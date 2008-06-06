/*
 * Nexus: Maven Repository Manager
 * Copyright (C) 2008 Sonatype Inc.                                                                                                                          
 * 
 * This file is part of Nexus.                                                                                                                                  
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 */
package org.sonatype.nexus.maven.tasks;

import org.sonatype.nexus.Nexus;
import org.sonatype.nexus.feeds.FeedRecorder;
import org.sonatype.nexus.proxy.maven.MavenRepository;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.scheduling.AbstractNexusRepositoriesTask;

public class SnapshotRemoverTask
    extends AbstractNexusRepositoriesTask<SnapshotRemovalResult>
{
    private final int minSnapshotsToKeep;

    private final int removeOlderThanDays;

    private final boolean removeIfReleaseExists;

    public SnapshotRemoverTask( Nexus nexus, String repositoryId, String repositoryGroupId, int minSnapshotsToKeep,
        int removeOlderThanDays, boolean removeIfReleaseExists )
    {
        super( nexus, repositoryId, repositoryGroupId );

        this.minSnapshotsToKeep = minSnapshotsToKeep;

        this.removeOlderThanDays = removeOlderThanDays;

        this.removeIfReleaseExists = removeIfReleaseExists;
    }

    public SnapshotRemovalResult doRun()
        throws Exception
    {
        SnapshotRemovalRequest req = new SnapshotRemovalRequest(
            minSnapshotsToKeep,
            removeOlderThanDays,
            removeIfReleaseExists );

        if ( getRepositoryId() != null )
        {
            getLogger().info( "Removing old SNAPSHOT deployments from " + getRepositoryId() + " repository." );

            Repository repository = getNexus().getRepository( getRepositoryId() );

            if ( MavenRepository.class.isAssignableFrom( repository.getClass() ) )
            {
                req.getRepositories().add( (MavenRepository) repository );
            }
            else
            {
                throw new IllegalArgumentException( "The repository with ID=" + repository.getId()
                    + " is not MavenRepository!" );
            }
        }
        else if ( getRepositoryGroupId() != null )
        {
            getLogger()
                .info( "Removing old SNAPSHOT deployments from " + getRepositoryGroupId() + " repository group." );

            for ( Repository repository : getNexus().getRepositoryGroup( getRepositoryGroupId() ) )
            {
                // only from maven repositories, stay silent for others and simply skip
                if ( MavenRepository.class.isAssignableFrom( repository.getClass() ) )
                {
                    req.getRepositories().add( (MavenRepository) repository );
                }
            }
        }
        else
        {
            getLogger().info( "Removing old SNAPSHOT deployments from all repositories." );

            for ( Repository repository : getNexus().getRepositories() )
            {
                // only from maven repositories, stay silent for others and simply skip
                if ( MavenRepository.class.isAssignableFrom( repository.getClass() ) )
                {
                    req.getRepositories().add( (MavenRepository) repository );
                }
            }
        }

        DefaultSnapshotRemover sr = new DefaultSnapshotRemover();

        sr.enableLogging( getLogger() );

        return sr.removeSnapshots( req );
    }

    protected String getAction()
    {
        return FeedRecorder.SYSTEM_REMOVE_SNAPSHOTS_ACTION;
    }

    protected String getMessage()
    {
        if ( getRepositoryGroupId() != null )
        {
            return "Removing snapshots from repository group with ID=" + getRepositoryGroupId();
        }
        else if ( getRepositoryId() != null )
        {
            return "Removing snapshots from repository with ID=" + getRepositoryId();
        }
        else
        {
            return "Removing snapshots from all registered repositories";
        }
    }

}
