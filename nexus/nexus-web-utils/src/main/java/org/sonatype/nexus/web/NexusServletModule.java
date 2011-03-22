package org.sonatype.nexus.web;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Named;
import javax.inject.Singleton;

import com.google.inject.servlet.ServletModule;

/**
 * Guice module for binding nexus servlets.
 * 
 * @author adreghiciu
 */
@Named
@Singleton
public class NexusServletModule
    extends ServletModule
{
    @Override
    protected void configureServlets()
    {
        requestStaticInjection( NexusGuiceFilter.class );

        serve( "/*" ).with( NexusRestletServlet.class, nexusRestletServletInitParams() );
    }

    private Map<String, String> nexusRestletServletInitParams()
    {
        Map<String, String> params = new HashMap<String, String>();
        params.put( "nexus.role", "org.restlet.Application" );
        params.put( "nexus.roleHint", "nexus" );
        params.put( "nexus.org.restlet.clients", "FILE CLAP" );
        params.put( "plexus.discoverResources", "true" );
        return params;
    }
}
