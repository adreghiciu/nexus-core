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
package org.sonatype.nexus.integrationtests;

import java.io.File;
import java.io.IOException;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Response;
import org.restlet.resource.StringRepresentation;
import org.sonatype.jettytestsuite.ServletServer;
import org.sonatype.nexus.artifact.Gav;
import org.sonatype.nexus.test.utils.FileTestingUtils;
import org.sonatype.nexus.test.utils.TestProperties;

public abstract class AbstractNexusProxyIntegrationTest
    extends AbstractNexusIntegrationTest
{

    protected String baseProxyURL = null;

    protected String localStorageDir = null;

    protected Integer proxyPort;

    protected AbstractNexusProxyIntegrationTest()
    {
        this( "release-proxy-repo-1" );
    }

    protected AbstractNexusProxyIntegrationTest( String testRepositoryId )
    {
        super( testRepositoryId );

        this.baseProxyURL = TestProperties.getString( "proxy.repo.base.url" );
        this.localStorageDir = TestProperties.getString( "proxy.repo.base.dir" );
        this.proxyPort = TestProperties.getInteger( "proxy.server.port" );
    }

    @Before
    public void startProxy()
        throws Exception
    {
        ServletServer server = (ServletServer) this.lookup( ServletServer.ROLE );
        server.start();
    }

    @After
    public void stopProxy()
        throws Exception
    {
        ServletServer server = (ServletServer) this.lookup( ServletServer.ROLE );
        server.stop();
    }

    public File getLocalFile( String repositoryId, Gav gav )
    {
        return this.getLocalFile( repositoryId, gav.getGroupId(), gav.getArtifactId(), gav.getVersion(),
                                  gav.getExtension() );
    }

    public File getLocalFile( String repositoryId, String groupId, String artifact, String version, String type )
    {
        File result =
            new File( this.localStorageDir, repositoryId + "/" + groupId.replace( '.', '/' ) + "/" + artifact + "/"
                + version + "/" + artifact + "-" + version + "." + type );
        log.debug( "Returning file: " + result );
        return result;
    }

    // TODO: Refactor this into the AbstractNexusIntegrationTest or some util class, to make more generic

    public void setBlockProxy( String nexusBaseUrl, String repoId, boolean block ) throws IOException
    {

        String serviceURI = "service/local/repositories/" + repoId + "/status?undefined";

        // unblock string
        String blockOrNotCommand = "\"unavailable\",\"proxyMode\":\"allow\"";
        // change to block if true
        if ( block == true )
        {
            blockOrNotCommand = "\"available\",\"proxyMode\":\"blockedManual\"";
        }

        StringRepresentation representation = new StringRepresentation("{\"data\":{\"id\":\"" + repoId
                                 + "\",\"repoType\":\"proxy\",\"localStatus\":\"inService\",\"remoteStatus\":" + blockOrNotCommand + "}}",
                                 MediaType.APPLICATION_JSON);



        Response response = RequestFacade.sendMessage( serviceURI, Method.PUT, representation );

        if ( !response.getStatus().isSuccess() )
        {
            Assert.fail( "Could not unblock proxy: " + repoId + ", status: " + response.getStatus().getName() + " ("
                + response.getStatus().getCode() + ") - " + response.getStatus().getDescription() );
        }
    }

    public void setOutOfServiceProxy( String nexusBaseUrl, String repoId, boolean outOfService ) throws IOException
    {

        String serviceURI = "service/local/repositories/" + repoId + "/status?undefined";

        // unblock string
        String servicePart = outOfService ? "outOfService" : "inService";

        StringRepresentation representation = new StringRepresentation("{\"data\":{\"id\":\"" + repoId + "\",\"repoType\":\"proxy\",\"localStatus\":\""
            + servicePart + "\"}}", MediaType.APPLICATION_JSON );

        Response response = RequestFacade.sendMessage( serviceURI, Method.PUT, representation );

        if ( !response.getStatus().isSuccess() )
        {
            Assert.fail( "Could not set proxy out of service status (Status: "+response.getStatus()+ ": " + repoId + "\n" + response.getEntity().getText());
        }
    }

    @Override
    protected void copyTestResources()
        throws IOException
    {
        super.copyTestResources();

        File source = new File( TestProperties.getString( "test.resources.source.folder" ), "proxyRepo" );
        if ( !source.exists() )
        {
            return;
        }

        FileTestingUtils.interpolationDirectoryCopy( source, new File(localStorageDir), TestProperties.getAll() );

    }
}
