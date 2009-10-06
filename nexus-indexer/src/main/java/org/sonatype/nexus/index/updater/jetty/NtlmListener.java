/**
 * Copyright (c) 2007-2008 Sonatype, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 */
package org.sonatype.nexus.index.updater.jetty;

import org.eclipse.jetty.client.HttpDestination;
import org.eclipse.jetty.client.HttpEventListenerWrapper;
import org.eclipse.jetty.client.HttpExchange;
import org.eclipse.jetty.http.HttpHeaders;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.io.Buffer;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * WagonListener
 * 
 * Detect the NTLM authentication scheme and switch to HttpURLConnection
 */
public class NtlmListener
    extends HttpEventListenerWrapper
{
    private static NtlmConnectionHelper _helper;

    private final HttpExchange _exchange;

    private final Set<String> _authTypes;

    private boolean _requestComplete;

    private boolean _responseComplete;

    private boolean _unAuthorized;

    public NtlmListener( final HttpDestination destination, final HttpExchange ex )
    {
        // Start of sending events through to the wrapped listener
        // Next decision point is the onResponseStatus
        super( ex.getEventListener(), true );

        _authTypes = new HashSet<String>();
        _exchange = ex;
    }

    /**
     * scrapes an authentication type from the authString
     * 
     * @param authString
     * @return
     */
    protected String scrapeAuthenticationType( final String authString )
    {
        int idx = authString.indexOf( " " );
        return ( idx < 0 ? authString : authString.substring( 0, idx ) ).trim().toLowerCase();
    }

    @Override
    public void onResponseStatus( final Buffer version, final int status, final Buffer reason )
        throws IOException
    {
        _unAuthorized = ( status == HttpStatus.UNAUTHORIZED_401 );

        if ( _unAuthorized )
        {
            setDelegatingRequests( false );
            setDelegatingResponses( false );
        }

        super.onResponseStatus( version, status, reason );
    }

    @Override
    public void onResponseHeader( final Buffer name, final Buffer value )
        throws IOException
    {
        if ( _unAuthorized )
        {
            int header = HttpHeaders.CACHE.getOrdinal( name );
            switch ( header )
            {
                case HttpHeaders.WWW_AUTHENTICATE_ORDINAL:
                    String authString = value.toString();
                    _authTypes.add( scrapeAuthenticationType( authString ) );
                    break;
            }
        }
        super.onResponseHeader( name, value );
    }

    @Override
    public void onRequestComplete()
        throws IOException
    {
        _requestComplete = true;
        checkExchangeComplete();

        super.onRequestComplete();
    }

    @Override
    public void onResponseComplete()
        throws IOException
    {
        _responseComplete = true;
        checkExchangeComplete();

        super.onResponseComplete();
    }

    public void checkExchangeComplete()
        throws IOException
    {
        if ( _unAuthorized && _requestComplete && _responseComplete )
        {
            setDelegatingRequests( true );
            setDelegatingResponses( true );

            if ( _helper != null && _authTypes.contains( "ntlm" ) && _exchange instanceof ResourceExchange )
            {
                _helper.send( (ResourceExchange) _exchange );
            }
            else
            {
                setDelegationResult( false );
            }
        }
    }

    public static void setHelper( final NtlmConnectionHelper helper )
    {
        _helper = helper;
    }
}
