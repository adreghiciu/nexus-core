/**
 * Sonatype Nexus (TM) [Open Source Version].
 * Copyright (c) 2008 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at ${thirdPartyUrl}.
 *
 * This program is licensed to you under Version 3 only of the GNU
 * General Public License as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License Version 3 for more details.
 *
 * You should have received a copy of the GNU General Public License
 * Version 3 along with this program. If not, see http://www.gnu.org/licenses/.
 */
package org.sonatype.nexus;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Appender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.sonatype.nexus.log.LogConfiguration;
import org.sonatype.nexus.log.SimpleLog4jConfig;

/**
 * Log4J file manager.
 * 
 * @author cstamas
 */
@Component( role = LogFileManager.class )
public class Log4jLogFileManager
    extends AbstractLogEnabled
    implements LogFileManager
{
    @Requirement
    private LogConfiguration<Properties> logConfiguration;

    public Log4jLogFileManager()
    {
        createLogDirectory();
    }

    public File getLogFile( String filename )
    {
        Logger logger = Logger.getRootLogger();

        @SuppressWarnings( "unchecked" )
        Enumeration<Appender> appenders = logger.getAllAppenders();

        while ( appenders.hasMoreElements() )
        {
            Appender appender = appenders.nextElement();

            if ( FileAppender.class.isAssignableFrom( appender.getClass() ) )
            {
                File logfile = new File( ( (FileAppender) appender ).getFile() );

                if ( logfile.getName().equals( filename ) )
                {
                    return logfile;
                }
            }
        }

        return null;
    }

    public Set<File> getLogFiles()
    {

        Logger logger = Logger.getRootLogger();

        @SuppressWarnings( "unchecked" )
        Enumeration<Appender> appenders = logger.getAllAppenders();

        HashSet<File> files = new HashSet<File>();

        while ( appenders.hasMoreElements() )
        {
            Appender appender = appenders.nextElement();

            if ( FileAppender.class.isAssignableFrom( appender.getClass() ) )
            {
                files.add( new File( ( (FileAppender) appender ).getFile() ) );
            }
        }

        return files;
    }

    public void createLogDirectory()
    {
        for ( File file : getLogFiles() )
        {
            File parent = file.getParentFile();

            if ( parent != null && !parent.exists() )
            {
                parent.mkdirs();
            }
        }
    }

    public SimpleLog4jConfig getLogConfig()
        throws IOException
    {
        logConfiguration.load();

        return new SimpleLog4jConfig( logConfiguration.getConfig() );
    }

    public void setLogConfig( SimpleLog4jConfig simpleLog4jConfig )
        throws IOException
    {
        Properties config = logConfiguration.getConfig();

        config.putAll( simpleLog4jConfig.toMap() );

        logConfiguration.apply();

        logConfiguration.save();
    }

}
