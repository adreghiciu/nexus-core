package org.sonatype.nexus.plugins.plugin.console.api.test.util;

import org.sonatype.nexus.plugins.plugin.console.api.dto.PluginInfoDTO;
import org.sonatype.nexus.plugins.plugin.console.api.dto.PluginInfoListResponseDTO;
import org.sonatype.plexus.rest.xstream.AliasingListConverter;

import com.thoughtworks.xstream.XStream;

public class XStreamFactory
{
    public static XStream getXmlXStream()
    {
        XStream xs = org.sonatype.nexus.test.utils.XStreamFactory.getXmlXStream();
        configureXStream( xs );
        return xs;
    }

    public static XStream getJsonXStream()
    {
        XStream xs = org.sonatype.nexus.test.utils.XStreamFactory.getJsonXStream();
        configureXStream( xs );
        return xs;
    }

    private static void configureXStream( XStream xstream )
    {
        xstream.processAnnotations( PluginInfoDTO.class );
        xstream.processAnnotations( PluginInfoListResponseDTO.class );

        xstream.registerLocalConverter( PluginInfoListResponseDTO.class, "data", new AliasingListConverter(
            PluginInfoDTO.class,
            "pluginInfo" ) );
    }
}
