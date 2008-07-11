package org.sonatype.nexus.proxy.maven;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.TimeZone;

import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.Plugin;
import org.apache.maven.artifact.repository.metadata.Snapshot;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Writer;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.sonatype.nexus.artifact.Gav;
import org.sonatype.nexus.proxy.AccessDeniedException;
import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.NoSuchResourceStoreException;
import org.sonatype.nexus.proxy.RepositoryNotAvailableException;
import org.sonatype.nexus.proxy.StorageException;
import org.sonatype.nexus.proxy.item.DefaultStorageFileItem;
import org.sonatype.nexus.proxy.item.RepositoryItemUid;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.item.StringContentLocator;
import org.sonatype.nexus.proxy.storage.UnsupportedStorageOperationException;

/**
 * Component responsible for metadata maintenance.
 * 
 * @author cstamas
 * @plexus.component
 */
public class DefaultMetadataManager
    extends AbstractLogEnabled
    implements MetadataManager
{
    private DateFormat df = new SimpleDateFormat( "yyyyMMdd.HHmmss" );
    {
        df.setTimeZone( TimeZone.getTimeZone( "UTC" ) );
    }

    protected Metadata readOrCreateMetadata( RepositoryItemUid uid )
        throws RepositoryNotAvailableException,
            IOException,
            XmlPullParserException
    {
        Metadata result = null;

        try
        {
            StorageItem item = uid.getRepository().retrieveItem( true, uid );

            if ( StorageFileItem.class.isAssignableFrom( item.getClass() ) )
            {
                StorageFileItem fileItem = (StorageFileItem) item;

                MetadataXpp3Reader r = new MetadataXpp3Reader();

                InputStream is = null;

                try
                {
                    is = fileItem.getInputStream();

                    result = r.read( is );
                }
                finally
                {
                    IOUtil.close( is );
                }

                if ( getLogger().isDebugEnabled() )
                {
                    getLogger().debug( "Retrieved metadata from UID=" + uid.toString() );
                }
            }
            else
            {
                throw new IllegalArgumentException( "The UID " + uid.toString() + " is not a file!" );
            }
        }
        catch ( ItemNotFoundException e )
        {
            result = new Metadata();

            if ( getLogger().isDebugEnabled() )
            {
                getLogger().debug( "Created metadata for UID=" + uid.toString() );
            }
        }

        return result;
    }

    protected Metadata readOrCreateGAVMetadata( MavenRepository repository, Gav gav )
        throws RepositoryNotAvailableException,
            IOException,
            XmlPullParserException
    {
        String mdPath = repository.getGavCalculator().gavToPath( gav );

        // GAV
        mdPath = mdPath.substring( 0, mdPath.lastIndexOf( RepositoryItemUid.PATH_SEPARATOR ) ) + "/maven-metadata.xml";

        RepositoryItemUid uid = new RepositoryItemUid( repository, mdPath );

        Metadata result = readOrCreateMetadata( uid );

        result.setGroupId( gav.getGroupId() );

        result.setArtifactId( gav.getArtifactId() );

        result.setVersion( gav.getBaseVersion() );

        return result;
    }

    protected Metadata readOrCreateGAMetadata( MavenRepository repository, Gav gav )
        throws RepositoryNotAvailableException,
            IOException,
            XmlPullParserException
    {
        String mdPath = repository.getGavCalculator().gavToPath( gav );

        // GAV
        mdPath = mdPath.substring( 0, mdPath.lastIndexOf( RepositoryItemUid.PATH_SEPARATOR ) );

        // GA
        mdPath = mdPath.substring( 0, mdPath.lastIndexOf( RepositoryItemUid.PATH_SEPARATOR ) ) + "/maven-metadata.xml";

        RepositoryItemUid uid = new RepositoryItemUid( repository, mdPath );

        Metadata result = readOrCreateMetadata( uid );

        result.setGroupId( gav.getGroupId() );

        result.setArtifactId( gav.getArtifactId() );

        result.setVersion( null );

        return result;
    }

    protected Metadata readOrCreatePluginMetadata( MavenRepository repository, Gav gav )
        throws RepositoryNotAvailableException,
            IOException,
            XmlPullParserException
    {
        String mdPath = repository.getGavCalculator().gavToPath( gav );

        // GAV
        mdPath = mdPath.substring( 0, mdPath.lastIndexOf( RepositoryItemUid.PATH_SEPARATOR ) );

        // GA
        mdPath = mdPath.substring( 0, mdPath.lastIndexOf( RepositoryItemUid.PATH_SEPARATOR ) );

        // G
        mdPath = mdPath.substring( 0, mdPath.lastIndexOf( RepositoryItemUid.PATH_SEPARATOR ) ) + "/maven-metadata.xml";

        RepositoryItemUid uid = new RepositoryItemUid( repository, mdPath );

        Metadata result = readOrCreateMetadata( uid );

        result.setGroupId( null );

        result.setArtifactId( null );

        result.setVersion( null );

        return result;
    }

    protected void saveGAVMetadata( MavenRepository repository, Gav gav, Metadata md )
        throws UnsupportedStorageOperationException,
            RepositoryNotAvailableException,
            IOException
    {
        String mdPath = repository.getGavCalculator().gavToPath( gav );

        // GAV
        mdPath = mdPath.substring( 0, mdPath.lastIndexOf( RepositoryItemUid.PATH_SEPARATOR ) ) + "/maven-metadata.xml";

        MetadataXpp3Writer w = new MetadataXpp3Writer();

        StringWriter sw = new StringWriter();

        w.write( sw, md );

        StringContentLocator locator = new StringContentLocator( sw.toString() );

        repository.storeItemWithChecksums( new DefaultStorageFileItem( repository, mdPath, true, true, locator ) );
    }

    protected void saveGAMetadata( MavenRepository repository, Gav gav, Metadata md )
        throws UnsupportedStorageOperationException,
            RepositoryNotAvailableException,
            IOException
    {
        String mdPath = repository.getGavCalculator().gavToPath( gav );

        // GAV
        mdPath = mdPath.substring( 0, mdPath.lastIndexOf( RepositoryItemUid.PATH_SEPARATOR ) );

        // GA
        mdPath = mdPath.substring( 0, mdPath.lastIndexOf( RepositoryItemUid.PATH_SEPARATOR ) ) + "/maven-metadata.xml";

        MetadataXpp3Writer w = new MetadataXpp3Writer();

        StringWriter sw = new StringWriter();

        w.write( sw, md );

        StringContentLocator locator = new StringContentLocator( sw.toString() );

        repository.storeItemWithChecksums( new DefaultStorageFileItem( repository, mdPath, true, true, locator ) );
    }

    protected void savePluginMetadata( MavenRepository repository, Gav gav, Metadata md )
        throws UnsupportedStorageOperationException,
            RepositoryNotAvailableException,
            IOException
    {
        String mdPath = repository.getGavCalculator().gavToPath( gav );

        // GAV
        mdPath = mdPath.substring( 0, mdPath.lastIndexOf( RepositoryItemUid.PATH_SEPARATOR ) );

        // GA
        mdPath = mdPath.substring( 0, mdPath.lastIndexOf( RepositoryItemUid.PATH_SEPARATOR ) );

        // G
        mdPath = mdPath.substring( 0, mdPath.lastIndexOf( RepositoryItemUid.PATH_SEPARATOR ) ) + "/maven-metadata.xml";

        MetadataXpp3Writer w = new MetadataXpp3Writer();

        StringWriter sw = new StringWriter();

        w.write( sw, md );

        StringContentLocator locator = new StringContentLocator( sw.toString() );

        repository.storeItemWithChecksums( new DefaultStorageFileItem( repository, mdPath, true, true, locator ) );
    }

    public void deployArtifact( ArtifactStoreRequest req, MavenRepository repository )
        throws RepositoryNotAvailableException,
            IOException,
            UnsupportedStorageOperationException
    {
        if ( req.getClassifier() != null )
        {
            return;
        }

        if ( getLogger().isDebugEnabled() )
        {
            getLogger().debug( "Adding metadata for deployed artifact: " + req.getRequestPath() );
        }

        Gav gav = new Gav(
            req.getGroupId(),
            req.getArtifactId(),
            req.getVersion(),
            req.getClassifier(),
            repository.getArtifactPackagingMapper().getExtensionForPackaging( req.getPackaging() ),
            null,
            null,
            null,
            RepositoryPolicy.SNAPSHOT.equals( repository.getRepositoryPolicy() ),
            false,
            null,
            false,
            null );

        Metadata gavMd = null;

        Metadata gaMd = null;

        try
        {
            gavMd = readOrCreateGAVMetadata( repository, gav );

            if ( gavMd.getVersioning() == null )
            {
                gavMd.setVersioning( new Versioning() );
            }

            gaMd = readOrCreateGAMetadata( repository, gav );

            if ( gaMd.getVersioning() == null )
            {
                gaMd.setVersioning( new Versioning() );
            }
        }
        catch ( XmlPullParserException e )
        {
            throw new StorageException( "Could not read the metadatas!", e );
        }

        if ( RepositoryPolicy.SNAPSHOT.equals( repository.getRepositoryPolicy() ) )
        {
            // a snapshot GAV

            if ( gavMd.getVersioning().getLatest() != null )
            {
                DefaultArtifactVersion oldVersion = new DefaultArtifactVersion( gavMd.getVersioning().getLatest() );

                DefaultArtifactVersion newVersion = new DefaultArtifactVersion( gav.getVersion() );

                if ( newVersion.compareTo( oldVersion ) > -1 )
                {
                    gavMd.getVersioning().setLatest( gav.getVersion() );
                }
            }
            else
            {
                gavMd.getVersioning().setLatest( gav.getVersion() );
            }

            gavMd.getVersioning().setRelease( null );

            Snapshot snapshot = new Snapshot();

            // TODO: a potential NPE source: if gav has no snapshotTimestamp, then what?
            // Nexus will enforce using timestamped snaps in remote reposes anyway as "good practice"

            snapshot.setTimestamp( df.format( new Date( gav.getSnapshotTimeStamp().longValue() ) ) );

            snapshot.setBuildNumber( gav.getSnapshotBuildNumber().intValue() );

            gavMd.getVersioning().setSnapshot( snapshot );

            gavMd.getVersioning().updateTimestamp();

            // GA

            if ( gaMd.getVersioning().getLatest() != null )
            {
                DefaultArtifactVersion oldVersion = new DefaultArtifactVersion( gaMd.getVersioning().getLatest() );

                DefaultArtifactVersion newVersion = new DefaultArtifactVersion( gav.getBaseVersion() );

                if ( newVersion.compareTo( oldVersion ) > -1 )
                {
                    gaMd.getVersioning().setLatest( gav.getBaseVersion() );
                }
            }
            else
            {
                gaMd.getVersioning().setLatest( gav.getBaseVersion() );
            }

            gaMd.getVersioning().setRelease( null );
        }
        else
        {
            // GAV

            // nothing for release artifacts
            gavMd = null;

            // GA

            gaMd.getVersioning().setLatest( null );

            if ( gaMd.getVersioning().getRelease() != null )
            {
                DefaultArtifactVersion oldVersion = new DefaultArtifactVersion( gaMd.getVersioning().getRelease() );

                DefaultArtifactVersion newVersion = new DefaultArtifactVersion( gav.getBaseVersion() );

                if ( newVersion.compareTo( oldVersion ) > -1 )
                {
                    gaMd.getVersioning().setRelease( gav.getBaseVersion() );
                }
            }
            else
            {
                gaMd.getVersioning().setRelease( gav.getBaseVersion() );
            }

        }

        if ( !gaMd.getVersioning().getVersions().contains( gav.getBaseVersion() ) )
        {
            gaMd.getVersioning().addVersion( gav.getBaseVersion() );
        }

        gaMd.getVersioning().updateTimestamp();

        // save it
        if ( gavMd != null )
        {
            saveGAVMetadata( repository, gav, gavMd );
        }

        saveGAMetadata( repository, gav, gaMd );

        if ( "maven-plugin".equals( req.getPackaging() ) )
        {
            deployPlugin( req, repository );
        }
    }

    public void undeployArtifact( ArtifactStoreRequest req, MavenRepository repository )
        throws RepositoryNotAvailableException,
            IOException,
            UnsupportedStorageOperationException
    {
        if ( req.getClassifier() != null )
        {
            return;
        }

        if ( getLogger().isDebugEnabled() )
        {
            getLogger().debug( "Removing metadata for undeployed artifact: " + req.getRequestPath() );
        }

        Gav gav = new Gav(
            req.getGroupId(),
            req.getArtifactId(),
            req.getVersion(),
            req.getClassifier(),
            repository.getArtifactPackagingMapper().getExtensionForPackaging( req.getPackaging() ),
            null,
            null,
            null,
            RepositoryPolicy.SNAPSHOT.equals( repository.getRepositoryPolicy() ),
            false,
            null,
            false,
            null );

        Metadata gavMd = null;

        Metadata gaMd = null;

        try
        {
            gavMd = readOrCreateGAVMetadata( repository, gav );

            if ( gavMd.getVersioning() == null )
            {
                gavMd.setVersioning( new Versioning() );
            }

            gaMd = readOrCreateGAMetadata( repository, gav );

            if ( gaMd.getVersioning() == null )
            {
                gaMd.setVersioning( new Versioning() );
            }
        }
        catch ( XmlPullParserException e )
        {
            getLogger().warn( "Could not read the metadatas!", e );

            return;
        }

        if ( RepositoryPolicy.SNAPSHOT.equals( repository.getRepositoryPolicy() ) )
        {
            // a snapshot GAV

            // TODO: anything needed?

            // GA

            // TODO: anything needed?
        }
        else
        {
            // GA

            // TODO:
        }

        if ( !gaMd.getVersioning().getVersions().contains( gav.getBaseVersion() ) )
        {
            gaMd.getVersioning().removeVersion( gav.getBaseVersion() );
        }

        gaMd.getVersioning().updateTimestamp();

        // save it
        saveGAVMetadata( repository, gav, gavMd );
        saveGAMetadata( repository, gav, gaMd );

        if ( "maven-plugin".equals( req.getPackaging() ) )
        {
            undeployPlugin( req, repository );
        }
    }

    public void deployPlugin( ArtifactStoreRequest req, MavenRepository repository )
        throws IOException,
            RepositoryNotAvailableException,
            UnsupportedStorageOperationException
    {
        if ( req.getClassifier() != null )
        {
            return;
        }

        if ( !"maven-plugin".equals( req.getPackaging() ) )
        {
            return;
        }

        if ( getLogger().isDebugEnabled() )
        {
            getLogger().debug( "Adding metadata for deployed maven-plugin: " + req.getRequestPath() );
        }

        Gav gav = new Gav(
            req.getGroupId(),
            req.getArtifactId(),
            req.getVersion(),
            req.getClassifier(),
            repository.getArtifactPackagingMapper().getExtensionForPackaging( req.getPackaging() ),
            null,
            null,
            null,
            RepositoryPolicy.SNAPSHOT.equals( repository.getRepositoryPolicy() ),
            false,
            null,
            false,
            null );

        StorageFileItem pomFile = null;

        try
        {
            pomFile = repository.retrieveArtifactPom( req );
        }
        catch ( AccessDeniedException e )
        {
            getLogger().warn( "Could not maintain maven-plugin metadata!", e );

            return;
        }
        catch ( NoSuchResourceStoreException e )
        {
            getLogger().warn( "Could not maintain maven-plugin metadata!", e );

            return;
        }
        catch ( ItemNotFoundException e )
        {
            getLogger().warn( "Could not maintain maven-plugin metadata!", e );

            return;
        }

        Metadata md = null;

        Plugin plugin = new Plugin();

        try
        {
            PluginDescriptor pd = extractPluginDescriptor( pomFile.getInputStream() );

            plugin.setPrefix( pd.getGoalPrefix() );

            plugin.setName( pd.getName() );

            plugin.setArtifactId( pd.getArtifactId() );

            md = readOrCreatePluginMetadata( repository, gav );
        }
        catch ( XmlPullParserException e )
        {
            getLogger().warn( "Could not extract or read the metadatas!", e );

            return;
        }

        // remove if already exists
        for ( Iterator<Plugin> i = (Iterator<Plugin>) md.getPlugins().iterator(); i.hasNext(); )
        {
            Plugin p = i.next();

            if ( p.getArtifactId().equals( plugin.getArtifactId() ) )
            {
                i.remove();

                break;
            }
        }

        md.addPlugin( plugin );

        savePluginMetadata( repository, gav, md );
    }

    public void undeployPlugin( ArtifactStoreRequest req, MavenRepository repository )
        throws IOException,
            RepositoryNotAvailableException,
            UnsupportedStorageOperationException
    {
        if ( req.getClassifier() != null )
        {
            return;
        }

        if ( !"maven-plugin".equals( req.getPackaging() ) )
        {
            return;
        }

        if ( getLogger().isDebugEnabled() )
        {
            getLogger().debug( "Removing metadata for undeployed maven-plugin: " + req.getRequestPath() );
        }

        Gav gav = new Gav(
            req.getGroupId(),
            req.getArtifactId(),
            req.getVersion(),
            req.getClassifier(),
            repository.getArtifactPackagingMapper().getExtensionForPackaging( req.getPackaging() ),
            null,
            null,
            null,
            RepositoryPolicy.SNAPSHOT.equals( repository.getRepositoryPolicy() ),
            false,
            null,
            false,
            null );

        StorageFileItem pomFile = null;

        try
        {
            pomFile = repository.retrieveArtifactPom( req );
        }
        catch ( AccessDeniedException e )
        {
            getLogger().warn( "Could not maintain maven-plugin metadata!", e );

            return;
        }
        catch ( NoSuchResourceStoreException e )
        {
            getLogger().warn( "Could not maintain maven-plugin metadata!", e );

            return;
        }
        catch ( ItemNotFoundException e )
        {
            getLogger().warn( "Could not maintain maven-plugin metadata!", e );

            return;
        }

        PluginDescriptor pd = null;

        Metadata md = null;

        try
        {
            pd = extractPluginDescriptor( pomFile.getInputStream() );

            md = readOrCreatePluginMetadata( repository, gav );
        }
        catch ( XmlPullParserException e )
        {
            getLogger().warn( "Could not extract or read maven-plugin metadata!", e );

            return;
        }

        // remove if already exists
        for ( Iterator<Plugin> i = (Iterator<Plugin>) md.getPlugins().iterator(); i.hasNext(); )
        {
            Plugin p = i.next();

            if ( p.getArtifactId().equals( pd.getArtifactId() ) )
            {
                i.remove();

                break;
            }
        }

        savePluginMetadata( repository, gav, md );
    }

    protected PluginDescriptor extractPluginDescriptor( InputStream is )
        throws IOException,
            XmlPullParserException
    {
        Model model = null;

        try
        {
            MavenXpp3Reader rd = new MavenXpp3Reader();

            model = rd.read( is );
        }
        finally
        {
            IOUtil.close( is );
        }

        return extractPluginDescriptor( model );
    }

    protected PluginDescriptor extractPluginDescriptor( Model project )
    {
        PluginDescriptor result = new PluginDescriptor();

        result.setGroupId( project.getGroupId() != null ? project.getGroupId() : project.getParent().getGroupId() );

        result.setArtifactId( project.getArtifactId() );

        result.setVersion( project.getVersion() != null ? project.getVersion() : project.getParent().getVersion() );

        result.setName( project.getName() );

        result.setDescription( project.getDescription() );

        result.setGoalPrefix( PluginDescriptor.getGoalPrefixFromArtifactId( result.getArtifactId() ) );

        // TODO: look in the model for maven-plugin-plugin and read it;s configuration

        return result;
    }

}
