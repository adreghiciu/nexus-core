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
package org.sonatype.nexus.jsecurity.realms;

import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.sonatype.jsecurity.realms.XmlMethodAuthorizingRealm;
import org.sonatype.nexus.configuration.ConfigurationChangeEvent;
import org.sonatype.nexus.jsecurity.NexusSecurity;
import org.sonatype.nexus.proxy.events.AbstractEvent;

public abstract class AbstractNexusAuthorizingRealm
    extends XmlMethodAuthorizingRealm
    implements NexusAuthorizingRealm, Initializable
{
    @Requirement
    private NexusSecurity security;

    public void initialize()
        throws InitializationException
    {
        security.addProximityEventListener( this );
    }

    public void onProximityEvent( AbstractEvent evt )
    {
        if ( ConfigurationChangeEvent.class.isAssignableFrom( evt.getClass() ) )
        {
            if ( getAuthorizationCache() != null )
            {
                getAuthorizationCache().clear();
            }
            if ( getConfigurationManager() != null )
            {
                getConfigurationManager().clearCache();
            }
        }
    }
}
