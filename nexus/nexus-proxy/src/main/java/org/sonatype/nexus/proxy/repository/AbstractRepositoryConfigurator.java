package org.sonatype.nexus.proxy.repository;

import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.sonatype.nexus.configuration.ConfigurationException;
import org.sonatype.nexus.configuration.application.ApplicationConfiguration;
import org.sonatype.nexus.configuration.application.ExternalConfiguration;
import org.sonatype.nexus.configuration.model.CMirror;
import org.sonatype.nexus.configuration.model.CRepository;
import org.sonatype.nexus.configuration.validator.ApplicationValidationResponse;
import org.sonatype.nexus.configuration.validator.InvalidConfigurationException;
import org.sonatype.nexus.configuration.validator.ValidationMessage;
import org.sonatype.nexus.configuration.validator.ValidationResponse;
import org.sonatype.nexus.plugins.PluginRepositoryConfigurator;
import org.sonatype.nexus.proxy.StorageException;
import org.sonatype.nexus.proxy.registry.RepositoryRegistry;
import org.sonatype.nexus.proxy.registry.RepositoryTypeRegistry;
import org.sonatype.nexus.proxy.storage.local.LocalRepositoryStorage;
import org.sonatype.nexus.proxy.storage.remote.RemoteRepositoryStorage;

public abstract class AbstractRepositoryConfigurator
    implements RepositoryConfigurator
{
    @Requirement
    private PlexusContainer plexusContainer;

    @Requirement
    private RepositoryRegistry repositoryRegistry;

    @Requirement
    private RepositoryTypeRegistry repositoryTypeRegistry;

    @Requirement( role = PluginRepositoryConfigurator.class )
    private Map<String, PluginRepositoryConfigurator> pluginRepositoryConfigurators;

    public final void validate( ApplicationConfiguration configuration, CRepository repoConfig )
        throws ConfigurationException
    {
        prepareExternalConfiguration( repoConfig );

        doValidate( configuration, repoConfig, repoConfig.externalConfigurationImple );
    }

    public final void applyConfiguration( Repository repository, ApplicationConfiguration configuration,
        CRepository repoConfig )
        throws ConfigurationException
    {
        prepareExternalConfiguration( repoConfig );

        doConfigure( repository, configuration, repoConfig, repoConfig.externalConfigurationImple );
    }

    public final void prepareForSave( Repository repository, ApplicationConfiguration configuration,
        CRepository repoConfig )
    {
        prepareExternalConfiguration( repoConfig );

        // in 1st round, i intentionally choosed to make our lives bitter, and handle plexus config manually
        // later we will see about it
        doPrepareForSave( repository, configuration, repoConfig, repoConfig.externalConfigurationImple );
    }

    protected void prepareExternalConfiguration( CRepository repoConfig )
    {
        if ( repoConfig.getExternalConfiguration() == null )
        {
            // just put an elephant in South Africa to find it for sure ;)
            repoConfig.setExternalConfiguration( new Xpp3Dom( "externalConfiguration" ) );
        }

        if ( repoConfig.externalConfigurationImple == null )
        {
            // in 1st round, i intentionally choosed to make our lives bitter, and handle plexus config manually
            // later we will see about it
            repoConfig.externalConfigurationImple = createExternalConfiguration( (Xpp3Dom) repoConfig
                .getExternalConfiguration() );
        }
    }

    protected abstract ExternalConfiguration createExternalConfiguration( Xpp3Dom dom );

    public ExternalConfiguration getExternalConfiguration( Repository repository )
    {
        return ( (AbstractRepository) repository ).getCurrentConfiguration().externalConfigurationImple;
    }

    protected void doValidate( ApplicationConfiguration configuration, CRepository repo,
        ExternalConfiguration externalConfiguration )
        throws ConfigurationException
    {
        // TODO:

    }

    @SuppressWarnings( "unchecked" )
    protected void doConfigure( Repository repository, ApplicationConfiguration configuration, CRepository repo,
        ExternalConfiguration externalConfiguration )
        throws ConfigurationException
    {
        List<CMirror> mirrors = (List<CMirror>) repo.getMirrors();

        if ( mirrors != null && mirrors.size() > 0 )
        {
            List<Mirror> runtimeMirrors = new ArrayList<Mirror>();

            for ( CMirror mirror : mirrors )
            {
                runtimeMirrors.add( new Mirror( mirror.getId(), mirror.getUrl() ) );
            }

            repository.getPublishedMirrors().setMirrors( runtimeMirrors );
        }
        else
        {
            repository.getPublishedMirrors().setMirrors( null );
        }

        // Setting common things on a repository

        // NX-198: filling up the default variable to store the "default" local URL
        File defaultStorageFile = new File( new File( configuration.getWorkingDirectory(), "storage" ), repository
            .getId() );

        try
        {
            repo.defaultLocalStorageUrl = defaultStorageFile.toURL().toString();
        }
        catch ( MalformedURLException e )
        {
            // will not happen, not user settable
            throw new InvalidConfigurationException( "Malformed URL for LocalRepositoryStorage!", e );
        }

        String localUrl = null;

        if ( repo.getLocalStorage() != null && !StringUtils.isEmpty( repo.getLocalStorage().getUrl() ) )
        {
            localUrl = repo.getLocalStorage().getUrl();
        }
        else
        {
            localUrl = repo.defaultLocalStorageUrl;

            // Default dir is going to be valid
            defaultStorageFile.mkdirs();
        }

        LocalRepositoryStorage ls = getLocalRepositoryStorage( repo.getId(), repo.getLocalStorage().getProvider() );

        try
        {
            ls.validateStorageUrl( localUrl );

            repository.setLocalUrl( localUrl );
            repository.setLocalStorage( ls );
        }
        catch ( StorageException e )
        {
            ValidationResponse response = new ApplicationValidationResponse();

            ValidationMessage error = new ValidationMessage(
                "overrideLocalStorageUrl",
                "Repository has an invalid local storage URL '" + localUrl,
                "Invalid file location" );

            response.addValidationError( error );

            throw new InvalidConfigurationException( response );
        }

        for ( PluginRepositoryConfigurator configurator : pluginRepositoryConfigurators.values() )
        {
            if ( configurator.isHandledRepository( repository ) )
            {
                configurator.configureRepository( repository );
            }
        }

        // clear the NotFoundCache
        if ( repository.getNotFoundCache() != null )
        {
            repository.getNotFoundCache().purge();
        }
    }

    protected void doPrepareForSave( Repository repository, ApplicationConfiguration configuration,
        CRepository repoConfig, ExternalConfiguration externalConfiguration )
    {
        List<Mirror> mirrors = (List<Mirror>) repository.getPublishedMirrors().getMirrors();

        if ( mirrors != null && mirrors.size() > 0 )
        {
            List<CMirror> runtimeMirrors = new ArrayList<CMirror>();

            for ( Mirror mirror : mirrors )
            {
                CMirror cmirror = new CMirror();

                cmirror.setId( mirror.getId() );
                cmirror.setUrl( mirror.getUrl() );
                runtimeMirrors.add( cmirror );
            }

            repoConfig.setMirrors( runtimeMirrors );
        }
        else
        {
            repoConfig.getMirrors().clear();
        }

        // Setting common things on a repository

        repoConfig.getLocalStorage().setUrl( repository.getLocalUrl() );
    }

    // ==

    protected PlexusContainer getPlexusContainer()
    {
        return plexusContainer;
    }

    protected RepositoryRegistry getRepositoryRegistry()
    {
        return repositoryRegistry;
    }

    protected RepositoryTypeRegistry getRepositoryTypeRegistry()
    {
        return repositoryTypeRegistry;
    }

    protected boolean existsRepositoryType( Class<?> role, String hint )
        throws InvalidConfigurationException
    {
        return componentExists( role, hint );
    }

    protected boolean existsLocalRepositoryStorage( String repoId, String provider )
        throws InvalidConfigurationException
    {
        return componentExists( LocalRepositoryStorage.class, provider );
    }

    protected boolean existsRemoteRepositoryStorage( String repoId, String provider )
        throws InvalidConfigurationException
    {
        return componentExists( RemoteRepositoryStorage.class, provider );
    }

    protected boolean componentExists( Class<?> role, String hint )
    {
        return getPlexusContainer().hasComponent( role, hint );
    }

    protected LocalRepositoryStorage getLocalRepositoryStorage( String repoId, String provider )
        throws InvalidConfigurationException
    {
        try
        {
            return getPlexusContainer().lookup( LocalRepositoryStorage.class, provider );
        }
        catch ( ComponentLookupException e )
        {
            throw new InvalidConfigurationException( "Repository " + repoId
                + " have local storage with unsupported provider: " + provider, e );
        }
    }

}
