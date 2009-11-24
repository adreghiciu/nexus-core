package org.sonatype.sample.wrapper;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.LinkedList;

import org.codehaus.plexus.util.StringUtils;

public class DefaultWrapperConfWrapper
    implements WrapperConfWrapper
{
    private final File wrapperConfFile;

    private LinkedList<String> lines;

    public DefaultWrapperConfWrapper( File file )
        throws IOException
    {
        this.wrapperConfFile = file;

        reset();
    }

    public void reset()
        throws IOException
    {
        // clear the buffer that holds the changes
        lines = null;

        // reload
        load();
    }

    public void save()
        throws IOException
    {
        save( wrapperConfFile );
    }

    public void save( File target )
        throws IOException
    {
        BufferedOutputStream bout = new BufferedOutputStream( new FileOutputStream( target ) );

        try
        {
            PrintWriter writer = new PrintWriter( bout );

            java.util.Iterator<String> i = getLines().iterator();

            while ( i.hasNext() )
            {
                writer.println( i.next() );
            }

            writer.flush();
        }
        finally
        {
            bout.close();
        }
    }

    public String getProperty( String key, String defaultValue )
    {
        int lineIndex = getLineIndexWithKey( key );

        if ( lineIndex > -1 )
        {
            return getValueFromLine( getLines().get( lineIndex ), defaultValue );
        }
        else
        {
            return defaultValue;
        }
    }

    public String getProperty( String key )
    {
        return getProperty( key, null );
    }

    public int getIntegerProperty( String key, int defaultValue )
    {
        try
        {
            return Integer.valueOf( getProperty( key, String.valueOf( defaultValue ) ) );
        }
        catch ( NumberFormatException e )
        {
            return defaultValue;
        }
    }

    public void setProperty( String key, String value )
    {
        int lineToChange = getLineIndexWithKey( key );

        String newLine = key + "=" + value;

        if ( lineToChange > -1 )
        {
            getLines().remove( lineToChange );

            getLines().add( lineToChange, newLine );
        }
        else
        {
            getLines().add( newLine );
        }
    }

    public void setIntegerProperty( String key, int value )
    {
        setProperty( key, String.valueOf( value ) );
    }

    public boolean removeProperty( String key )
    {
        int lineIndex = getLineIndexWithKey( key );

        if ( lineIndex > -1 )
        {
            getLines().remove( lineIndex );

            return true;
        }
        else
        {
            return false;
        }
    }

    public String[] getPropertyList( String key )
    {
        // TODO Auto-generated method stub
        return null;
    }

    public void setPropertyList( String key, String[] values )
    {
        // TODO Auto-generated method stub

    }

    // ==

    protected LinkedList<String> getLines()
    {
        if ( lines == null )
        {
            lines = new LinkedList<String>();
        }

        return lines;
    }

    protected void load()
        throws IOException
    {
        InputStream in = new FileInputStream( wrapperConfFile );

        LinkedList<String> lines = getLines();

        try
        {
            BufferedReader reader = new BufferedReader( new InputStreamReader( in ) );

            String line;

            while ( ( line = reader.readLine() ) != null )
            {
                lines.add( line );
            }
        }
        finally
        {
            in.close();
        }
    }

    protected int getLineIndexWithKey( String key )
    {
        LinkedList<String> lines = getLines();

        for ( int idx = 0; idx < lines.size(); idx++ )
        {
            String line = lines.get( idx );

            if ( StringUtils.equals( key, getKeyFromLine( line ) ) )
            {
                return idx;
            }
        }

        return -1;
    }

    protected String getKeyFromLine( String line )
    {
        String[] elems = explodeLine( line );

        if ( elems.length == 2 )
        {
            return elems[0];
        }

        return null;
    }

    protected String getValueFromLine( String line, String defaultValue )
    {
        String[] elems = explodeLine( line );

        if ( elems.length == 2 )
        {
            return elems[1];
        }

        return defaultValue;
    }

    protected String[] explodeLine( String line )
    {
        return line.split( "\\s=\\s|\\s=|=\\s|\\s" );
    }
}
