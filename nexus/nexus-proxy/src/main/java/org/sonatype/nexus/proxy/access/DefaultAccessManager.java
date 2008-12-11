/**
 * ﻿Sonatype Nexus (TM) [Open Source Version].
 * Copyright (c) 2008 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at ${thirdpartyurl}.
 *
 * This program is licensed to you under Version 3 only of the GNU General
 * Public License as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * Version 3 for more details.
 *
 * You should have received a copy of the GNU General Public License
 * Version 3 along with this program. If not, see http://www.gnu.org/licenses/.
 */
package org.sonatype.nexus.proxy.access;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.sonatype.nexus.proxy.AccessDeniedException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.item.RepositoryItemUid;
import org.sonatype.nexus.proxy.repository.Repository;

/**
 * A default access manager relying onto default NexusAuthorizer.
 * 
 * @author cstamas
 */
@Component( role = AccessManager.class )
public class DefaultAccessManager
    implements AccessManager
{
    @Requirement
    private NexusItemAuthorizer nexusItemAuthorizer;

    public void decide( ResourceStoreRequest request, Repository repository, Action action )
        throws AccessDeniedException
    {
        RepositoryItemUid uid = repository.createUid( request.getRequestPath() );

        if ( !nexusItemAuthorizer.authorizePath( uid, request.getRequestContext(), action ) )
        {
            // deny the access
            throw new AccessDeniedException( request, "Access denied on repository ID='" + repository.getId()
                + "', path='" + request.getRequestPath() + "', action='" + action + "'!" );
        }
    }
}
