package org.sonatype.nexus.mock;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.codehaus.plexus.ContainerConfiguration;
import org.codehaus.plexus.DefaultContainerConfiguration;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.PlexusContainerException;
import org.sonatype.nexus.test.utils.TestProperties;

public class TestContext
{

    protected static Logger log = Logger.getLogger( TestContext.class );

    private static final PlexusContainer container = setupContainer();

    public static final File RESOURCES_DIR = new File( "target/resources" );

    public static final File RESOURCES_SOURCE_DIR = new File( "resources" );

    private static final ThreadLocal<String> testId = new ThreadLocal<String>();

    public static String getTestId()
    {
        if ( testId.get() == null )
        {
            throw new NullPointerException( "TestId undefined!" );
        }
        return testId.get();
    }

    public static void setTestId( String testId )
    {
        TestContext.testId.set( testId );
    }

    public static File getTestResourceAsFile( String relativePath )
    {
        String resource = getTestId() + "/" + relativePath;
        return getResource( resource );
    }

    public static File getResource( String resource )
    {
        log.debug( "Looking for resource: " + resource );

        File file = new File( RESOURCES_DIR, resource );

        if ( !file.exists() )
        {
            return null;
        }

        log.debug( "found: " + file );

        return file.getAbsoluteFile();
    }

    public static PlexusContainer getContainer()
    {
        return container;
    }

    private static synchronized PlexusContainer setupContainer()
    {
        File f = new File( "target/plexus-home" );

        if ( !f.isDirectory() )
        {
            f.mkdirs();
        }

        Map<Object, Object> context = new HashMap<Object, Object>();
        context.put( "plexus.home", f.getAbsolutePath() );
        context.putAll( TestProperties.getAll() );

        // ----------------------------------------------------------------------------
        // Configuration
        // ----------------------------------------------------------------------------

        ContainerConfiguration containerConfiguration =
            new DefaultContainerConfiguration().setName( "test" ).setContext( context );

        containerConfiguration.setContainerConfigurationURL( TestContext.class.getClassLoader().getResource(
                                                                                                             "PlexusTestContainerConfig.xml" ) );

        try
        {
            return new DefaultPlexusContainer( containerConfiguration );
        }
        catch ( PlexusContainerException e )
        {
            throw new RuntimeException( "Failed to create plexus container: " + e.getMessage(), e );
        }
    }

}
