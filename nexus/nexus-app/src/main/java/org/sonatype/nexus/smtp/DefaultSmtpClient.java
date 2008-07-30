/*
 * Nexus: Maven Repository Manager
 * Copyright (C) 2008 Sonatype Inc.                                                                                                                          
 * 
 * This file is part of Nexus.                                                                                                                                  
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 */
package org.sonatype.nexus.smtp;

import java.util.ArrayList;
import java.util.List;

import org.codehaus.plexus.mailsender.MailMessage;
import org.codehaus.plexus.mailsender.MailSender;
import org.codehaus.plexus.mailsender.MailSenderException;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.sonatype.nexus.configuration.ConfigurationChangeEvent;
import org.sonatype.nexus.configuration.ConfigurationChangeListener;
import org.sonatype.nexus.configuration.application.NexusConfiguration;
import org.sonatype.nexus.configuration.model.CSmtpConfiguration;

/**
 * @plexus.component
 */
public class DefaultSmtpClient
    implements
        SmtpClient, Initializable, ConfigurationChangeListener
{
    /**
     * @plexus.requirement
     */
    private MailSender sender;
    
    /**
     * The nexus configuration.
     * 
     * @plexus.requirement
     */
    private NexusConfiguration nexusConfiguration;
    
    private boolean initialized = false;
    
    public void initialize()
        throws InitializationException
    {
        nexusConfiguration.addConfigurationChangeListener( this );
    }
    
    public void onConfigurationChange( ConfigurationChangeEvent evt )
    {
        initialize( true );
    }
    
    public void initialize( boolean force )
    {
        if ( force || !initialized )
        {
            CSmtpConfiguration config = nexusConfiguration.getConfiguration().getSmtpConfiguration();
            
            sender.setSmtpHost( config.getHost() );
            sender.setSmtpPort( config.getPort() );
            sender.setSslMode( config.isSslEnabled(), config.isTlsEnabled() );
            sender.setUsername( config.getUsername() );
            sender.setPassword( config.getPassword() );
            sender.setDebugMode( config.isDebugMode() );
            initialized = true;
        }
    }
    
    public void sendEmail( String to, String from, String subject, String body )
        throws SmtpClientException
    {
        List<String> toList = new ArrayList<String>();
        toList.add( to );
        
        sendEmail( toList, from, subject, body );
    }

    public void sendEmail( List<String> toList, String from, String subject, String body )
        throws SmtpClientException
    {   
        try
        {
            initialize( false );
            
            MailMessage message = new MailMessage();
            
            for ( String to : toList )
            {
                message.addTo( to, null );
            }
            
            message.setFrom( from == null ? nexusConfiguration.getConfiguration().getSmtpConfiguration().getSystemEmailAddress() : from , null );
            message.setSubject( subject );
            message.setContent( body );
    
            sender.send( message );
        }
        catch ( MailSenderException e )
        {
            throw new SmtpClientException( "Error handling smtp request", e );
        }
    }
}
