/**
 * Copyright (c) 2008-2011 Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://www.sonatype.com/products/nexus/attributions.
 *
 * This program is free software: you can redistribute it and/or modify it only under the terms of the GNU Affero General
 * Public License Version 3 as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Affero General Public License Version 3
 * for more details.
 *
 * You should have received a copy of the GNU Affero General Public License Version 3 along with this program.  If not, see
 * http://www.gnu.org/licenses.
 *
 * Sonatype Nexus (TM) Open Source Version is available from Sonatype, Inc. Sonatype and Sonatype Nexus are trademarks of
 * Sonatype, Inc. Apache Maven is a trademark of the Apache Foundation. M2Eclipse is a trademark of the Eclipse Foundation.
 * All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.integrationtests.webproxy.nexus1101;

import static org.sonatype.nexus.integrationtests.ITGroups.PROXY;

import java.io.File;

import org.sonatype.nexus.integrationtests.webproxy.AbstractNexusWebProxyIntegrationTest;
import org.sonatype.nexus.test.utils.FileTestingUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

public class Nexus1101NexusOverWebproxyIT
    extends AbstractNexusWebProxyIntegrationTest
{

    @Test(groups = PROXY)
    public void downloadArtifactOverWebProxy()
        throws Exception
    {
        File pomFile = this.getLocalFile( "release-proxy-repo-1", "nexus1101", "artifact", "1.0", "pom" );
        File pomArtifact = this.downloadArtifact( "nexus1101", "artifact", "1.0", "pom", null, "target/downloads" );
        Assert.assertTrue( FileTestingUtils.compareFileSHA1s( pomArtifact, pomFile ) );

        File jarFile = this.getLocalFile( "release-proxy-repo-1", "nexus1101", "artifact", "1.0", "jar" );
        File jarArtifact = this.downloadArtifact( "nexus1101", "artifact", "1.0", "jar", null, "target/downloads" );
        Assert.assertTrue( FileTestingUtils.compareFileSHA1s( jarArtifact, jarFile ) );

        String artifactUrl = baseProxyURL + "release-proxy-repo-1/nexus1101/artifact/1.0/artifact-1.0.jar";
        Assert.assertTrue( server.getAccessedUris().contains( artifactUrl ), "Proxy was not accessed" );
    }

}
