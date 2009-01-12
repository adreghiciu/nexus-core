/**
 * Sonatype Nexus (TM) [Open Source Version].
 * Copyright (c) 2008 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at ${thirdPartyUrl}.
 *
 * This program is licensed to you under Version 3 only of the GNU
 * General Public License as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License Version 3 for more details.
 *
 * You should have received a copy of the GNU General Public License
 * Version 3 along with this program. If not, see http://www.gnu.org/licenses/.
 */
package org.sonatype.nexus.rest.metadata;

import org.codehaus.plexus.component.annotations.Component;
import org.sonatype.nexus.rest.restore.AbstractRestorePlexusResource;
import org.sonatype.plexus.rest.resource.PathProtectionDescriptor;
import org.sonatype.plexus.rest.resource.PlexusResource;

/**
 * @author Juven Xu
 */
@Component( role = PlexusResource.class, hint = "RepositoryOrGroupMetadataPlexusResource" )
public class RepositoryOrGroupMetadataPlexusResource
    extends AbstractMetadataPlexusResource
{

    @Override
    public PathProtectionDescriptor getResourceProtection()
    {
        return new PathProtectionDescriptor( "/metadata/*/**", "authcBasic,perms[nexus:cache]" );
    }

    @Override
    public String getResourceUri()
    {
        return "/metadata/{" + AbstractRestorePlexusResource.DOMAIN + "}/{" + AbstractRestorePlexusResource.TARGET_ID
            + "}/content";
    }

}
