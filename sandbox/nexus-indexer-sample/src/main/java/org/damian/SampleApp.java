package org.damian;

import java.io.IOException;
import java.util.Set;

import org.sonatype.nexus.index.ArtifactInfo;

public interface SampleApp
{
    void index() 
        throws IOException;
    
    Set<ArtifactInfo> searchIndex( String field, String value ) 
        throws IOException;
}
