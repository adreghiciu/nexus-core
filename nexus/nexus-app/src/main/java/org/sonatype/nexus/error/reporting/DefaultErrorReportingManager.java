package org.sonatype.nexus.error.reporting;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.codehaus.plexus.swizzle.IssueSubmissionException;
import org.codehaus.plexus.swizzle.IssueSubmissionRequest;
import org.codehaus.plexus.swizzle.IssueSubmissionResult;
import org.codehaus.plexus.swizzle.IssueSubmitter;
import org.codehaus.plexus.swizzle.JiraIssueSubmitter;
import org.codehaus.plexus.swizzle.jira.authentication.DefaultAuthenticationSource;
import org.codehaus.plexus.util.ExceptionUtils;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.swizzle.jira.Issue;
import org.codehaus.swizzle.jira.Jira;
import org.sonatype.nexus.ApplicationStatusSource;
import org.sonatype.nexus.configuration.AbstractConfigurable;
import org.sonatype.nexus.configuration.ConfigurationException;
import org.sonatype.nexus.configuration.Configurator;
import org.sonatype.nexus.configuration.CoreConfiguration;
import org.sonatype.nexus.configuration.application.ApplicationConfiguration;
import org.sonatype.nexus.configuration.application.NexusConfiguration;
import org.sonatype.nexus.configuration.model.CErrorReporting;
import org.sonatype.nexus.configuration.model.CErrorReportingCoreConfiguration;
import org.sonatype.nexus.configuration.model.ConfigurationHelper;
import org.sonatype.nexus.util.StringDigester;
import org.sonatype.plexus.encryptor.PlexusEncryptor;
import org.sonatype.security.configuration.source.SecurityConfigurationSource;
import org.sonatype.security.model.source.SecurityModelConfigurationSource;

@Component( role = ErrorReportingManager.class )
public class DefaultErrorReportingManager
    extends AbstractConfigurable
    implements ErrorReportingManager
{
    @Requirement
    private Logger logger;

    @Requirement
    private NexusConfiguration nexusConfig;

    @Requirement( role = SecurityModelConfigurationSource.class, hint = "file" )
    private SecurityModelConfigurationSource securityXmlSource;

    @Requirement( role = ApplicationStatusSource.class )
    ApplicationStatusSource applicationStatus;

    @Requirement( role = SecurityConfigurationSource.class, hint = "file" )
    private SecurityConfigurationSource securityConfigurationXmlSource;

    @Requirement
    private ConfigurationHelper configHelper;

    @Requirement( hint = "rsa-aes" )
    private PlexusEncryptor plexusEncryptor;

    private static final String DEFAULT_USERNAME = "sonatype_problem_reporting";

    private static final String COMPONENT = "Nexus";

    private static final String ERROR_REPORT_DIR = "error-report-bundles";

    private Set<String> errorHashSet = new HashSet<String>();

    // ==

    protected Logger getLogger()
    {
        return logger;
    }

    // ==

    @Override
    protected void initializeConfiguration()
        throws ConfigurationException
    {
        if ( getApplicationConfiguration().getConfigurationModel() != null )
        {
            configure( getApplicationConfiguration() );
        }
    }

    @Override
    protected ApplicationConfiguration getApplicationConfiguration()
    {
        return nexusConfig;
    }

    @Override
    protected Configurator getConfigurator()
    {
        return null;
    }

    @Override
    protected CErrorReporting getCurrentConfiguration( boolean forWrite )
    {
        return ( (CErrorReportingCoreConfiguration) getCurrentCoreConfiguration() ).getConfiguration( forWrite );
    }

    @Override
    protected CoreConfiguration wrapConfiguration( Object configuration )
        throws ConfigurationException
    {
        if ( configuration instanceof ApplicationConfiguration )
        {
            return new CErrorReportingCoreConfiguration( getApplicationConfiguration() );
        }
        else
        {
            throw new ConfigurationException( "The passed configuration object is of class \""
                + configuration.getClass().getName() + "\" and not the required \""
                + ApplicationConfiguration.class.getName() + "\"!" );
        }
    }

    // ==

    public boolean isEnabled()
    {
        return getCurrentConfiguration( false ).isEnabled();
    }

    public void setEnabled( boolean value )
    {
        getCurrentConfiguration( true ).setEnabled( value );
    }

    public String getJIRAUrl()
    {
        return getCurrentConfiguration( false ).getJiraUrl();
    }

    public void setJIRAUrl( String url )
    {
        getCurrentConfiguration( true ).setJiraUrl( url );
    }

    public String getJIRAUsername()
    {
        return getCurrentConfiguration( false ).getJiraUsername();
    }

    public void setJIRAUsername( String username )
    {
        getCurrentConfiguration( true ).setJiraUsername( username );
    }

    public String getJIRAPassword()
    {
        return getCurrentConfiguration( false ).getJiraPassword();
    }

    public void setJIRAPassword( String password )
    {
        getCurrentConfiguration( true ).setJiraPassword( password );
    }

    public String getJIRAProject()
    {
        return getCurrentConfiguration( false ).getJiraProject();
    }

    public void setJIRAProject( String pkey )
    {
        getCurrentConfiguration( true ).setJiraProject( pkey );
    }

    public boolean isUseGlobalProxy()
    {
        return getCurrentConfiguration( false ).isUseGlobalProxy();
    }

    public void setUseGlobalProxy( boolean val )
    {
        getCurrentConfiguration( true ).setUseGlobalProxy( val );
    }

    // ==

    // FIXME: get rid of CErrorReporirting direct usages in general, use setter/getters!

    public void handleError( ErrorReportRequest request )
        throws IssueSubmissionException, IOException, GeneralSecurityException
    {
        CErrorReporting errorConfig = getCurrentConfiguration( false );

        if ( isEnabled() && shouldHandleReport( request ) )
        {
            IssueSubmissionRequest subRequest = buildRequest( errorConfig, request );
            
            File unencryptedFile = subRequest.getProblemReportBundle();

            encryptRequest( subRequest );
            
            File encryptedFile = subRequest.getProblemReportBundle();
            
            try
            {
                List<Issue> existingIssues = retrieveIssues( errorConfig, subRequest.getSummary() );
    
                if ( existingIssues == null )
                {
                    IssueSubmissionResult result = getIssueSubmitter( errorConfig ).submitIssue( subRequest );
                    renameBundle( unencryptedFile, result.getKey() );
                    getLogger().info( "Generated problem report, ticket " + result.getIssueUrl() + " was created." );
                }
                else
                {
                    renameBundle( unencryptedFile, existingIssues.iterator().next().getKey() );
                    getLogger().info(
                                      "Not reporting problem as it already exists in database: "
                                          + existingIssues.iterator().next().getLink() );
                }
            }
            finally
            {
                if ( encryptedFile != null )
                {
                    encryptedFile.delete();
                }
            }
        }
    }

    private void encryptRequest( IssueSubmissionRequest subRequest )
        throws IOException, GeneralSecurityException
    {
        File encryptedZip = getZipFile( "nexus-error-bundle", "ezip" );

        InputStream publicKey = null;
        try
        {
            publicKey = getClass().getResourceAsStream( "/apr/public-key.txt" );

            plexusEncryptor.encrypt( subRequest.getProblemReportBundle(), encryptedZip, publicKey );
        }
        finally
        {
            IOUtil.close( publicKey );
        }

        subRequest.setProblemReportBundle( encryptedZip );
    }

    protected boolean shouldHandleReport( ErrorReportRequest request )
    {
        if ( request.getThrowable() != null 
            && request.getThrowable().getMessage() != null
            && StringUtils.isNotEmpty( request.getThrowable().getMessage() ) )
        {
            String hash = StringDigester.getSha1Digest( request.getThrowable().getMessage() );
    
            if ( errorHashSet.contains( hash ) )
            {
                getLogger().debug( "Received an exception we already processed, ignoring." );
                return false;
            }
            else
            {
                errorHashSet.add( hash );
                return true;
            }
        }
        else
        {
            getLogger().debug( "Received an empty message in exception, will not handle" );
        }
        
        return false;
    }

    protected List<Issue> retrieveIssues( CErrorReporting errorConfig, String description )
    {
        Jira jira = null;

        try
        {
            jira = new Jira( errorConfig.getJiraUrl() + "/rpc/xmlrpc" );
            jira.login( getJiraUsername( errorConfig ), getJiraPassword( errorConfig ) );

            List<Issue> issues =
                jira.getIssuesFromTextSearchWithProject( Arrays.asList( errorConfig.getJiraProject() ), "\""
                    + description + "\"", 20 );

            if ( !issues.isEmpty() )
            {
                return issues;
            }
        }
        catch ( Exception e )
        {
            getLogger().error( "Unable to query JIRA server to find if error report already exists", e );
        }
        finally
        {
            if ( jira != null )
            {
                try
                {
                    jira.logout();
                }
                catch ( Exception e )
                {
                }
            }
        }

        return null;
    }

    protected void renameBundle( File bundle, String jiraTicket )
        throws IOException
    {
        if ( StringUtils.isNotEmpty( jiraTicket ) )
        {
            String filename = bundle.getAbsolutePath();

            String newfilename = filename.replace( "nexus-error-bundle", "nexus-error-bundle-" + jiraTicket );

            FileUtils.rename( bundle, new File( newfilename ) );
        }
    }

    protected IssueSubmissionRequest buildRequest( CErrorReporting errorConfig, ErrorReportRequest request )
        throws IOException
    {
        String summary = "APR: " + request.getThrowable().getMessage();

        if ( summary.length() > 255 )
        {
            summary = summary.substring( 0, 254 );
        }

        IssueSubmissionRequest subRequest = new IssueSubmissionRequest();

        subRequest.setProjectId( errorConfig.getJiraProject() );
        subRequest.setSummary( summary );
        subRequest.setDescription( "The following exception occurred: " + StringDigester.LINE_SEPERATOR
            + ExceptionUtils.getFullStackTrace( request.getThrowable() ) );
        subRequest.setProblemReportBundle( assembleBundle( request ) );
        subRequest.setReporter( StringUtils.isNotEmpty( errorConfig.getJiraUsername() ) ? errorConfig.getJiraUsername()
                        : DEFAULT_USERNAME );
        subRequest.setComponent( COMPONENT );
        subRequest.setEnvironment( assembleEnvironment( request ) );

        if ( errorConfig.isUseGlobalProxy() )
        {
            subRequest.setProxyConfigurator( new NexusProxyServerConfigurator(
                                                                               nexusConfig.getGlobalRemoteStorageContext(),
                                                                               getLogger() ) );
        }

        return subRequest;
    }

    private String getJiraUsername( CErrorReporting errorConfig )
    {
        String username = DEFAULT_USERNAME;

        if ( StringUtils.isNotEmpty( errorConfig.getJiraUsername() ) )
        {
            username = errorConfig.getJiraUsername();
        }

        return username;
    }

    private String getJiraPassword( CErrorReporting errorConfig )
    {
        String password = DEFAULT_USERNAME;

        if ( StringUtils.isNotEmpty( errorConfig.getJiraPassword() ) )
        {
            password = errorConfig.getJiraPassword();
        }

        return password;
    }

    private String assembleEnvironment( ErrorReportRequest request )
    {
        StringBuffer sb = new StringBuffer();
        sb.append( "Nexus Version: " );
        sb.append( applicationStatus.getSystemStatus().getVersion() );
        sb.append( StringDigester.LINE_SEPERATOR );

        sb.append( "Nexus Edition: " );
        sb.append( applicationStatus.getSystemStatus().getEditionLong() );
        sb.append( StringDigester.LINE_SEPERATOR );

        sb.append( "java.vendor: " );
        sb.append( System.getProperty( "java.vendor" ) );
        sb.append( StringDigester.LINE_SEPERATOR );

        sb.append( "java.version: " );
        sb.append( System.getProperty( "java.version" ) );
        sb.append( StringDigester.LINE_SEPERATOR );

        sb.append( "os.name: " );
        sb.append( System.getProperty( "os.name" ) );
        sb.append( StringDigester.LINE_SEPERATOR );

        sb.append( "os.version: " );
        sb.append( System.getProperty( "os.version" ) );
        sb.append( StringDigester.LINE_SEPERATOR );

        sb.append( "os.arch: " );
        sb.append( System.getProperty( "os.arch" ) );

        return sb.toString();
    }

    private IssueSubmitter getIssueSubmitter( CErrorReporting errorConfig )
        throws IssueSubmissionException
    {
        try
        {
            return new JiraIssueSubmitter( errorConfig.getJiraUrl(),
                                           new DefaultAuthenticationSource( getJiraUsername( errorConfig ),
                                                                            getJiraPassword( errorConfig ) ) );
        }
        catch ( InitializationException e )
        {
            throw new IssueSubmissionException( "Unable to initalized jira issue submitter", e );
        }
    }

    public File assembleBundle( ErrorReportRequest request )
        throws IOException
    {
        File nexusXml = new NexusXmlHandler().getFile( configHelper, nexusConfig );
        File securityXml = new SecurityXmlHandler().getFile( securityXmlSource, nexusConfig );
        File securityConfigurationXml =
            new SecurityConfigurationXmlHandler().getFile( securityConfigurationXmlSource, nexusConfig );
        File fileListing = getFileListing();
        File contextListing = getContextListing( request.getContext() );
        File exceptionListing = getExceptionListing( request.getThrowable() );

        File zipFile = getZipFile( "nexus-error-bundle", "zip" );

        ZipOutputStream zStream = null;

        try
        {
            FileOutputStream fStream = new FileOutputStream( zipFile );
            zStream = new ZipOutputStream( fStream );

            addFileToZip( nexusXml, zStream, "nexus.xml" );
            addFileToZip( securityXml, zStream, "security.xml" );
            addFileToZip( securityConfigurationXml, zStream, "security-configuration.xml" );
            addFileToZip( fileListing, zStream, "fileListing.txt" );
            addFileToZip( contextListing, zStream, "contextListing.txt" );
            addFileToZip( exceptionListing, zStream, "exception.txt" );

            for ( File confFile : getConfigurationFiles() )
            {
                addFileToZip( confFile, zStream, null );
            }

            for ( File logFile : getLogFiles() )
            {
                addFileToZip( logFile, zStream, null );
            }
        }
        finally
        {
            deleteFile( nexusXml );
            deleteFile( securityXml );
            deleteFile( securityConfigurationXml );
            deleteFile( fileListing );
            deleteFile( contextListing );
            deleteFile( exceptionListing );

            IOUtil.close( zStream );
        }

        return zipFile;
    }

    private void addFileToZip( File file, ZipOutputStream zStream, String filename )
        throws IOException
    {
        if ( file != null && file.exists() )
        {
            byte[] buffer = new byte[1024];

            FileInputStream inStream = null;

            try
            {
                inStream = new FileInputStream( file );

                zStream.putNextEntry( new ZipEntry( filename != null ? filename : file.getName() ) );

                int len;
                while ( ( len = inStream.read( buffer ) ) > 0 )
                {
                    zStream.write( buffer, 0, len );
                }

                zStream.closeEntry();
            }
            finally
            {
                if ( inStream != null )
                {
                    inStream.close();
                }
            }
        }
    }

    private Set<File> getLogFiles()
    {
        Set<File> files = new HashSet<File>();

        files.add( new File( nexusConfig.getWorkingDirectory( "logs" ), "nexus.log" ) );

        return files;
    }

    private Set<File> getConfigurationFiles()
    {
        Set<File> files = new HashSet<File>();

        File confDir = nexusConfig.getWorkingDirectory( "conf" );

        File[] confFiles = confDir.listFiles( new FileFilter()
        {
            public boolean accept( File pathname )
            {
                return !pathname.getName().endsWith( ".bak" ) && !pathname.getName().endsWith( "nexus.xml" )
                    && !pathname.getName().endsWith( "security.xml" )
                    && !pathname.getName().endsWith( "security-configuration.xml" );
            }
        } );

        files.addAll( Arrays.asList( confFiles ) );

        return files;
    }

    private File getFileListing()
        throws IOException
    {
        return writeStringToTempFile( FileListingHelper.buildFileListing( nexusConfig.getWorkingDirectory() ),
                                      "fileListing.txt" );
    }

    private File getExceptionListing( Throwable t )
        throws IOException
    {
        return writeStringToTempFile( ExceptionUtils.getFullStackTrace( t ), "exceptionListing.txt" );
    }

    private File getContextListing( Map<String, Object> context )
        throws IOException
    {
        StringBuffer sb = new StringBuffer();

        for ( String key : context.keySet() )
        {
            sb.append( "key: " + key );
            sb.append( FileListingHelper.LINE_SEPERATOR );

            Object o = context.get( key );
            sb.append( "value: " + o == null ? "null" : o.toString() );
            sb.append( FileListingHelper.LINE_SEPERATOR );
            sb.append( FileListingHelper.LINE_SEPERATOR );
        }

        return writeStringToTempFile( sb.toString(), "contextListing.txt" );
    }

    private File writeStringToTempFile( String text, String name )
        throws IOException
    {
        File tempFile = null;

        BufferedWriter bWriter = null;

        try
        {
            tempFile = new File( nexusConfig.getTemporaryDirectory(), name + "." + System.currentTimeMillis() );

            bWriter = new BufferedWriter( new FileWriter( tempFile ) );

            bWriter.write( text );
        }
        finally
        {
            if ( bWriter != null )
            {
                bWriter.close();
            }
        }

        return tempFile;
    }

    private File getZipFile( String prefix, String suffix )
    {
        File zipDir = nexusConfig.getWorkingDirectory( ERROR_REPORT_DIR );

        if ( !zipDir.exists() )
        {
            zipDir.mkdirs();
        }

        return new File( zipDir, prefix + "." + System.currentTimeMillis() + "." + suffix );
    }

    private void deleteFile( File file )
    {
        if ( file != null )
        {
            file.delete();
        }
    }
}
