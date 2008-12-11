/**
 * Sonatype Nexus™ [Open Source Version].
 * Copyright © 2008 Sonatype, Inc. All rights reserved.
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
package org.sonatype.nexus.rest.artifact;

import org.codehaus.plexus.component.annotations.Component;
import org.sonatype.plexus.rest.resource.PathProtectionDescriptor;
import org.sonatype.plexus.rest.resource.PlexusResource;

@Component( role = PlexusResource.class, hint = "ArtifactContentPlexusResource" )
public class ArtifactContentPlexusResource
    extends AbstractArtifactPlexusResource
{
    public ArtifactContentPlexusResource()
    {
        this.setReadable( false );
        this.setModifiable( true );
    }

    @Override
    public Object getPayloadInstance()
    {
        return null;
    }

    @Override
    public String getResourceUri()
    {
        return "/artifact/maven/content";
    }

    @Override
    public PathProtectionDescriptor getResourceProtection()
    {
        return new PathProtectionDescriptor( getResourceUri(), "authcBasic,perms[nexus:artifact]" );
    }

    @Override
    public boolean acceptsUpload()
    {
        return true;
    }

}
