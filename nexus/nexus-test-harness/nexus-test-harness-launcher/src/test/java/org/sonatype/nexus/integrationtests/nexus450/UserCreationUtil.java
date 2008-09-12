package org.sonatype.nexus.integrationtests.nexus450;

import java.io.IOException;

import org.restlet.data.Status;
import org.sonatype.nexus.integrationtests.RequestFacade;
import org.sonatype.nexus.rest.xstream.XStreamInitializer;

import com.thoughtworks.xstream.XStream;

public class UserCreationUtil
{
    private static XStream xstream;

    static
    {
        xstream = XStreamInitializer.initialize( new XStream() );
        XStreamInitializer.initialize( xstream );
    }

    public static Status login()
        throws IOException
    {
        String serviceURI = "service/local/authentication/login";

        return RequestFacade.doGetRequest( serviceURI ).getStatus();
    }

}
