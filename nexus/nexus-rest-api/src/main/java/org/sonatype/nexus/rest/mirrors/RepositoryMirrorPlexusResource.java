package org.sonatype.nexus.rest.mirrors;

import org.codehaus.plexus.component.annotations.Component;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;
import org.sonatype.nexus.configuration.model.CMirrors;
import org.sonatype.nexus.configuration.model.CRepository;
import org.sonatype.nexus.proxy.NoSuchRepositoryException;
import org.sonatype.plexus.rest.resource.PathProtectionDescriptor;
import org.sonatype.plexus.rest.resource.PlexusResource;

@Component( role = PlexusResource.class, hint = "RepositoryMirrorPlexusResource" )
public class RepositoryMirrorPlexusResource
    extends AbstractRepositoryMirrorPlexusResource
{
    public RepositoryMirrorPlexusResource()
    {
        setModifiable( true );
    }
    
    @Override
    //TODO: define payload object
    public Object getPayloadInstance()
    {
        return null;
    }

    @Override
    public PathProtectionDescriptor getResourceProtection()
    {
        return new PathProtectionDescriptor( "/repository_mirrors/*/*", "authcBasic,perms[nexus:repositorymirrors]" );
    }

    @Override
    public String getResourceUri()
    {
        return "/repository_mirrors/{" + REPOSITORY_ID_KEY + "}/{" + MIRROR_ID_KEY + "}";
    }

    @Override
    //TODO: return mirror in a rest object
    public Object get( Context context, Request request, Response response, Variant variant )
        throws ResourceException
    {
        try
        {
            CMirrors mirrors = getNexus().readRepository( getRepositoryId( request ) ).getRemoteStorage().getMirrors();
        }
        catch ( NoSuchRepositoryException e )
        {
            throw new ResourceException( Status.CLIENT_ERROR_BAD_REQUEST, "Invalid repository id " + getRepositoryId( request ), e);
        }
        
        return null;
    }
    
    @Override
    //TODO: remove mirror and update repository
    public void delete( Context context, Request request, Response response )
        throws ResourceException
    {
        try
        {
            CRepository repository = getNexus().readRepository( getRepositoryId( request ) );
            
            repository.getRemoteStorage().getMirrors();
        }
        catch ( NoSuchRepositoryException e )
        {
            throw new ResourceException( Status.CLIENT_ERROR_BAD_REQUEST, "Invalid repository id " + getRepositoryId( request ), e);
        }
    }
}
