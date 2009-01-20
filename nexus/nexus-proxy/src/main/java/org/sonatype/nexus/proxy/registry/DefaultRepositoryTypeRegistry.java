/**
 * Sonatype Nexus (TM) Open Source Version.
 * Copyright (c) 2008 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://nexus.sonatype.org/dev/attributions.html
 * This program is licensed to you under Version 3 only of the GNU General Public License as published by the Free Software Foundation.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License Version 3 for more details.
 * You should have received a copy of the GNU General Public License Version 3 along with this program.
 * If not, see http://www.gnu.org/licenses/.
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc.
 * "Sonatype" and "Sonatype Nexus" are trademarks of Sonatype, Inc.
 */
package org.sonatype.nexus.proxy.registry;

import java.util.Map;
import java.util.Set;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.repository.RepositoryKind;

@Component( role = RepositoryTypeRegistry.class )
public class DefaultRepositoryTypeRegistry
    extends AbstractLogEnabled
    implements RepositoryTypeRegistry
{
    @Requirement( role = Repository.class )
    private Map<String, Repository> existingRepositoryTypes;

    public Set<String> getExistingRepositoryTypes()
    {
        return existingRepositoryTypes.keySet();
    }

    public ContentClass getTypeContentClass( String repositoryType )
    {
        if ( existingRepositoryTypes.containsKey( repositoryType ) )
        {
            return existingRepositoryTypes.get( repositoryType ).getRepositoryContentClass();
        }
        else
        {
            return null;
        }
    }

    public RepositoryKind getTypeRepositoryKind( String repositoryType )
    {
        if ( existingRepositoryTypes.containsKey( repositoryType ) )
        {
            return existingRepositoryTypes.get( repositoryType ).getRepositoryKind();
        }
        else
        {
            return null;
        }
    }

}
