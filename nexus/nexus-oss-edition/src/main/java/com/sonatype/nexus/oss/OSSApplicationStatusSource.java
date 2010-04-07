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
package com.sonatype.nexus.oss;

import org.codehaus.plexus.component.annotations.Component;
import org.sonatype.nexus.AbstractApplicationStatusSource;
import org.sonatype.nexus.ApplicationStatusSource;
import org.sonatype.nexus.SystemStatus;

@Component( role = ApplicationStatusSource.class )
public class OSSApplicationStatusSource
    extends AbstractApplicationStatusSource
    implements ApplicationStatusSource
{
    private static final String FORMATTED_APP_NAME_BASE = "Sonatype Nexus&trade;";

    public OSSApplicationStatusSource()
    {
        super();

        getSystemStatusInternal().setVersion( discoverApplicationVersion() );

        getSystemStatusInternal().setApiVersion( getSystemStatusInternal().getVersion() );

        getSystemStatusInternal().setFormattedAppName(
            FORMATTED_APP_NAME_BASE + " " + getSystemStatusInternal().getEditionLong() + " Edition, Version: "
                + getSystemStatusInternal().getVersion() );
    }

    @Override
    protected void renewSystemStatus( SystemStatus systemStatus )
    {
        // nothing changes in OSS yet
    }

    @Override
    protected String discoverApplicationVersion()
    {
        return readVersion( "/META-INF/maven/org.sonatype.nexus/nexus-oss-edition/pom.properties" );
    }
}
