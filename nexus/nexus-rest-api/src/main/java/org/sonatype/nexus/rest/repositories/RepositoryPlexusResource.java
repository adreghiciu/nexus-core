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
package org.sonatype.nexus.rest.repositories;

import java.io.IOException;

import org.codehaus.plexus.component.annotations.Component;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;
import org.sonatype.nexus.configuration.ConfigurationException;
import org.sonatype.nexus.proxy.NoSuchRepositoryException;
import org.sonatype.nexus.proxy.StorageException;
import org.sonatype.nexus.proxy.maven.ChecksumPolicy;
import org.sonatype.nexus.proxy.maven.MavenProxyRepository;
import org.sonatype.nexus.proxy.maven.MavenRepository;
import org.sonatype.nexus.proxy.maven.RepositoryPolicy;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.repository.ShadowRepository;
import org.sonatype.nexus.rest.model.RepositoryBaseResource;
import org.sonatype.nexus.rest.model.RepositoryProxyResource;
import org.sonatype.nexus.rest.model.RepositoryResource;
import org.sonatype.nexus.rest.model.RepositoryResourceResponse;
import org.sonatype.nexus.rest.model.RepositoryShadowResource;
import org.sonatype.nexus.rest.util.EnumUtil;
import org.sonatype.plexus.rest.resource.PathProtectionDescriptor;
import org.sonatype.plexus.rest.resource.PlexusResource;
import org.sonatype.plexus.rest.resource.PlexusResourceException;
import org.sonatype.plexus.rest.resource.error.ErrorResponse;

/**
 * Resource handler for Repository resource.
 *
 * @author cstamas
 */
@Component( role = PlexusResource.class, hint = "RepositoryPlexusResource" )
public class RepositoryPlexusResource
    extends AbstractRepositoryPlexusResource
{

    public RepositoryPlexusResource()
    {
        this.setModifiable( true );
    }

    @Override
    public Object getPayloadInstance()
    {
        return new RepositoryResourceResponse();
    }

    @Override
    public String getResourceUri()
    {
        return "/repositories/{" + REPOSITORY_ID_KEY + "}";
    }

    @Override
    public PathProtectionDescriptor getResourceProtection()
    {
        return new PathProtectionDescriptor( "/repositories/*", "authcBasic,perms[nexus:repositories]" );
    }

    @Override
    public Object get( Context context, Request request, Response response, Variant variant )
        throws ResourceException
    {
        return this.getRepositoryResourceResponse( getRepositoryId( request ) );
    }

    @Override
    public Object put( Context context, Request request, Response response, Object payload )
        throws ResourceException
    {
        RepositoryResourceResponse repoRequest = (RepositoryResourceResponse) payload;

        String repoId = this.getRepositoryId( request );

        if ( repoRequest != null )
        {
            try
            {
                RepositoryBaseResource resource = repoRequest.getData();

                if ( REPO_TYPE_VIRTUAL.equals( resource.getRepoType() ) )
                {
                    RepositoryShadowResource model = (RepositoryShadowResource) resource;

                    try
                    {
                        ShadowRepository shadow =
                            getRepositoryRegistry().getRepositoryWithFacet( repoId, ShadowRepository.class );

                        shadow.setName( model.getName() );

                        shadow.setMasterRepositoryId( model.getShadowOf() );

                        shadow.setSynchronizeAtStartup( model.isSyncAtStartup() );

                        getNexusConfiguration().saveConfiguration();
                    }
                    catch ( NoSuchRepositoryException e )
                    {
                        getLogger().warn( "Virtual repository not found, id=" + repoId );

                        throw new ResourceException( Status.CLIENT_ERROR_NOT_FOUND, "Virtual repository Not Found" );
                    }
                }
                else
                {
                    RepositoryResource model = (RepositoryResource) resource;

                    try
                    {
                        Repository repository = getRepositoryRegistry().getRepository( repoId );

                        repository.setName( model.getName() );

                        repository.setAllowWrite( model.isAllowWrite() );

                        repository.setBrowseable( model.isBrowseable() );

                        repository.setIndexable( model.isIndexable() );

                        repository.setNotFoundCacheTimeToLive( model.getNotFoundCacheTTL() );

                        if ( repository.getRepositoryKind().isFacetAvailable( MavenRepository.class ) )
                        {
                            RepositoryPolicy repoPolicy =
                                EnumUtil.valueOf( model.getRepoPolicy(), RepositoryPolicy.class );
                            repository.adaptToFacet( MavenRepository.class ).setRepositoryPolicy( repoPolicy );

                            if ( repository.getRepositoryKind().isFacetAvailable( MavenProxyRepository.class ) )
                            {
                                ChecksumPolicy checksum =
                                    EnumUtil.valueOf( model.getChecksumPolicy(), ChecksumPolicy.class );
                                repository.adaptToFacet( MavenProxyRepository.class ).setChecksumPolicy( checksum );

                                repository.adaptToFacet( MavenProxyRepository.class ).setDownloadRemoteIndexes(
                                                                                                                model.isDownloadRemoteIndexes() );
                            }
                        }

                        if ( model.getOverrideLocalStorageUrl() != null )
                        {
                            repository.setLocalUrl( model.getOverrideLocalStorageUrl() );
                        }

                        if ( RepositoryProxyResource.class.isAssignableFrom( model.getClass() ) )
                        {
                            // XXX cstamas!
                            // appModel = getRepositoryProxyAppModel( (RepositoryProxyResource) model, appModel );
                        }

                        getNexusConfiguration().saveConfiguration();
                    }
                    catch ( NoSuchRepositoryException e )
                    {
                        getLogger().warn( "Repository not found, id=" + repoId );

                        throw new ResourceException( Status.CLIENT_ERROR_NOT_FOUND, "Repository Not Found" );
                    }
                }
            }
            catch ( ConfigurationException e )
            {
                handleConfigurationException( e );
            }
            catch ( StorageException e )
            {
                ErrorResponse nexusErrorResponse = getNexusErrorResponse( "*", e.getMessage() );
                throw new PlexusResourceException( Status.CLIENT_ERROR_BAD_REQUEST, "Configuration error.",
                                                   nexusErrorResponse );
            }
            catch ( IOException e )
            {
                getLogger().warn( "Got IO Exception!", e );

                throw new ResourceException( Status.SERVER_ERROR_INTERNAL );
            }
        }

        // return current repo
        return this.getRepositoryResourceResponse( getRepositoryId( request ) );
    }

    @Override
    public void delete( Context context, Request request, Response response )
        throws ResourceException
    {
        String repoId = this.getRepositoryId( request );
        try
        {
            getNexus().deleteRepository( repoId );

            response.setStatus( Status.SUCCESS_NO_CONTENT );
        }
        catch ( ConfigurationException e )
        {
            getLogger().warn( "Repository not deletable, it has dependants, id=" + repoId );

            throw new ResourceException( Status.CLIENT_ERROR_BAD_REQUEST,
                                         "Repository is not deletable, it has dependants." );
        }
        catch ( NoSuchRepositoryException e )
        {
            getLogger().warn( "Repository not found, id=" + repoId );

            throw new ResourceException( Status.CLIENT_ERROR_NOT_FOUND, "Repository Not Found" );
        }
        catch ( IOException e )
        {
            getLogger().warn( "Got IO Exception!", e );

            throw new ResourceException( Status.SERVER_ERROR_INTERNAL );
        }
    }

}
