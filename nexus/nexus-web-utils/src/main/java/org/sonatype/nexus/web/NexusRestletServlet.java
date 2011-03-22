package org.sonatype.nexus.web;

import java.util.Enumeration;

import javax.inject.Singleton;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

import org.sonatype.plexus.rest.PlexusServerServlet;

/**
 * An {@link PlexusServerServlet} that has an hardcoded name of "nexus" as required by plexus init param lookup. Guice
 * servlet extension does not allow servlet name setup while binding.
 * 
 * @author adreghiciu
 */
@Singleton
class NexusRestletServlet
    extends PlexusServerServlet
{

    private static final long serialVersionUID = -840934203229475592L;

    /**
     * Original servlet context delegate.
     */
    private DelegatingServletConfig servletConfig;

    NexusRestletServlet()
    {
        servletConfig = new DelegatingServletConfig();
    }

    @Override
    public ServletConfig getServletConfig()
    {
        return servletConfig;
    }

    /**
     * An {@link ServletConfig} delegate that has an hardcoded servlet name.
     */
    private class DelegatingServletConfig
        implements ServletConfig
    {

        public String getServletName()
        {
            return "nexus";
        }

        public ServletContext getServletContext()
        {
            return NexusRestletServlet.super.getServletConfig().getServletContext();
        }

        public String getInitParameter( String name )
        {
            return NexusRestletServlet.super.getServletConfig().getInitParameter( name );
        }

        @SuppressWarnings( "rawtypes" )
        public Enumeration getInitParameterNames()
        {
            return NexusRestletServlet.super.getServletConfig().getInitParameterNames();
        }
    }

}
