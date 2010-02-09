package org.sonatype.nexus.plugins.rrb.parsers;

import java.util.ArrayList;

import org.codehaus.plexus.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.nexus.plugins.rrb.RepositoryDirectory;

public class HtmlRemoteRepositoryParser
    implements RemoteRepositoryParser
{

    private final Logger logger = LoggerFactory.getLogger( HtmlRemoteRepositoryParser.class );

    private static final String[] EXCLUDES =
        { ">Skip to content<", ">Log in<", ">Products<", "Parent Directory", "?", ">../", ">..<", "-logo.png",
            ">Community<", ">Support<", ">Resources<", ">About us<", ">Downloads<", ">Documentation<", ">Resources<",
            ">About This Site<", ">Contact Us<", ">Legal Terms and Privacy Policy<", ">Log out<",
            ">IONA Technologies<", ">Site Index<", ">Skip to content<" };

    private String localUrl;

    private String remoteUrl;

    private String linkStart = "<a ";

    private String linkEnd = "/a>";

    private String href = "href=\"";

    private String id;

    private String baseUrl;

    public HtmlRemoteRepositoryParser( String remoteUrl, String localUrl, String id, String baseUrl )
    {
        this.remoteUrl = remoteUrl;
        this.localUrl = localUrl;
        this.id = id;
        this.baseUrl = baseUrl;
    }

    /**
     * Extracts the links and sets the data in the RepositoryDirectory object.
     * 
     * @param indata
     * @return a list of RepositoryDirectory objects
     */
    public ArrayList<RepositoryDirectory> extractLinks( StringBuilder indata )
    {
        ArrayList<RepositoryDirectory> result = new ArrayList<RepositoryDirectory>();

        if ( indata.indexOf( linkStart.toUpperCase() ) != -1 )
        {
            linkStart = linkStart.toUpperCase();
            linkEnd = linkEnd.toUpperCase();
            href = href.toUpperCase();
        }
        int start = 0;
        int end = 0;
        do
        {
            RepositoryDirectory rp = new RepositoryDirectory();
            StringBuilder temp = new StringBuilder();
            start = indata.indexOf( linkStart, start );
            if ( start < 0 )
            {
                break;
            }

            end = indata.indexOf( linkEnd, start ) + linkEnd.length();
            temp.append( indata.subSequence( start, end ) );
            if ( !exclude( temp ) )
            {
                if ( !getLinkName( temp ).trim().endsWith( "/" ) )
                {
                    rp.setLeaf( true );
                }
                rp.setText( getLinkName( temp ).replace( "/", "" ).trim() );
                if ( !remoteUrl.endsWith( "/" ) )
                {
                    remoteUrl += "/";
                }
                if ( !localUrl.endsWith( "/" ) )
                {
                    localUrl += "/";
                }
                rp.setResourceURI( localUrl + getLinkUrl( temp ) );
                rp.setRelativePath( remoteUrl.replace( baseUrl, "" ) + getLinkUrl( temp ) );
                if ( !rp.getRelativePath().startsWith( "/" ) )
                {
                    rp.setRelativePath( "/" + rp.getRelativePath() );
                }

                if ( StringUtils.isNotEmpty( rp.getText() ) )
                {
                    result.add( rp );
                }
                logger.debug( "addning {} to result", rp.toString() );
            }
            start = end + 1;
        }
        while ( start > 0 );

        return result;
    }

    /**
     * Extracts the link name.
     */
    String getLinkName( StringBuilder temp )
    {
        int start = temp.indexOf( ">" ) + 1;
        int end = temp.indexOf( "</" );
        return cleanup( temp.substring( start, end ) );
    }

    String cleanup( String value )
    {
        int start = value.indexOf( '<' );
        int end = value.indexOf( '>' );
        if ( start != -1 && start < end )
        {
            CharSequence seq = value.substring( start, end + 1 );
            value = value.replace( seq, "" );
            cleanup( value );
        }
        return value.trim();
    }

    /**
     * Extracts the link url.
     */
    String getLinkUrl( StringBuilder temp )
    {
        int start = temp.indexOf( href ) + href.length();
        int end = temp.indexOf( "\"", start + 1 );
        return temp.substring( start, end );
    }

    /**
     * Excludes links that are not relevant for the listing.
     */
    boolean exclude( StringBuilder value )
    {
        for ( String s : EXCLUDES )
        {
            if ( value.indexOf( s ) > 0 )
            {
                logger.debug( "{} is in EXCLUDES array", value );
                return true;
            }
        }
        return false;
    }
}
