/*******************************************************************************
 * Copyright (c) 2007-2008 Sonatype Inc
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Eugene Kuleshov (Sonatype)
 *    Tamás Cservenák (Sonatype)
 *    Brian Fox (Sonatype)
 *    Jason Van Zyl (Sonatype)
 *******************************************************************************/
package org.sonatype.nexus.index.search;

import java.io.IOException;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.sonatype.nexus.index.ArtifactInfo;
import org.sonatype.nexus.index.ArtifactInfoGroup;
import org.sonatype.nexus.index.Grouping;
import org.sonatype.nexus.index.context.IndexContextInInconsistentStateException;
import org.sonatype.nexus.index.context.IndexingContext;

/**
 * @author Eugene Kuleshov
 * @author cstamas
 * @plexus.component
 */
public class DefaultSearchEngine
    extends AbstractLogEnabled
    implements SearchEngine
{
    // ====================================
    // Inner methods doing the work

    protected void searchFlat( Collection<ArtifactInfo> result, IndexingContext indexingContext, Query query, int from,
        int aiCount )
        throws IOException,
            IndexContextInInconsistentStateException
    {
        Hits hits = indexingContext.getIndexSearcher().search(
            query,
            new Sort( new SortField( ArtifactInfo.UINFO, SortField.STRING ) ) );

        if ( hits != null && hits.length() != 0 )
        {
            int start = from == UNDEFINED ? 0 : from;

            int end = aiCount == UNDEFINED ? hits.length() : Math.min( hits.length(), from + aiCount );

            for ( int i = start; i < end; i++ )
            {
                Document doc = hits.doc( i );

                ArtifactInfo artifactInfo = indexingContext.constructArtifactInfo( indexingContext, doc );

                if ( artifactInfo != null )
                {
                    artifactInfo.repository = indexingContext.getRepositoryId();

                    artifactInfo.context = indexingContext.getId();

                    result.add( artifactInfo );
                }
            }
        }
    }

    protected void searchGrouped( Map<String, ArtifactInfoGroup> result, Grouping grouping,
        Comparator<String> groupKeyComparator, IndexingContext indexingContext, Query query )
        throws IOException,
            IndexContextInInconsistentStateException
    {
        Hits hits = indexingContext.getIndexSearcher().search(
            query,
            new Sort( new SortField( ArtifactInfo.UINFO, SortField.STRING ) ) );

        if ( hits != null && hits.length() != 0 )
        {
            for ( int i = 0; i < hits.length(); i++ )
            {
                ArtifactInfo artifactInfo = indexingContext.constructArtifactInfo( indexingContext, hits.doc( i ) );

                if ( artifactInfo != null )
                {
                    artifactInfo.repository = indexingContext.getRepositoryId();

                    artifactInfo.context = indexingContext.getId();

                    grouping.addArtifactInfo( result, artifactInfo );
                }
            }
        }
    }

    // ====================================
    // Public impls

    public Set<ArtifactInfo> searchFlat( Comparator<ArtifactInfo> artifactInfoComparator,
        IndexingContext indexingContext, Query query )
        throws IOException,
            IndexContextInInconsistentStateException
    {
        return searchFlatPaged( artifactInfoComparator, indexingContext, query, UNDEFINED, UNDEFINED );
    }

    public Set<ArtifactInfo> searchFlat( Comparator<ArtifactInfo> artifactInfoComparator,
        Collection<IndexingContext> indexingContexts, Query query )
        throws IOException,
            IndexContextInInconsistentStateException
    {
        return searchFlatPaged( artifactInfoComparator, indexingContexts, query, UNDEFINED, UNDEFINED );
    }

    public Set<ArtifactInfo> searchFlatPaged( Comparator<ArtifactInfo> artifactInfoComparator,
        IndexingContext indexingContext, Query query, int from, int aiCount )
        throws IOException,
            IndexContextInInconsistentStateException
    {
        TreeSet<ArtifactInfo> result = new TreeSet<ArtifactInfo>( artifactInfoComparator );

        searchFlat( result, indexingContext, query, from, aiCount );

        return result;
    }

    public Set<ArtifactInfo> searchFlatPaged( Comparator<ArtifactInfo> artifactInfoComparator,
        Collection<IndexingContext> indexingContexts, Query query, int from, int aiCount )
        throws IOException,
            IndexContextInInconsistentStateException
    {

        TreeSet<ArtifactInfo> result = new TreeSet<ArtifactInfo>( artifactInfoComparator );

        for ( IndexingContext ctx : indexingContexts )
        {
            if ( ctx.isSearchable() )
            {
                searchFlat( result, ctx, query, from, aiCount );
            }
        }

        return result;
    }

    public Map<String, ArtifactInfoGroup> searchGrouped( Grouping grouping, Comparator<String> groupKeyComparator,
        IndexingContext indexingContext, Query query )
        throws IOException,
            IndexContextInInconsistentStateException
    {
        TreeMap<String, ArtifactInfoGroup> result = new TreeMap<String, ArtifactInfoGroup>( groupKeyComparator );

        searchGrouped( result, grouping, groupKeyComparator, indexingContext, query );

        return result;
    }

    public Map<String, ArtifactInfoGroup> searchGrouped( Grouping grouping, Comparator<String> groupKeyComparator,
        Collection<IndexingContext> indexingContexts, Query query )
        throws IOException,
            IndexContextInInconsistentStateException
    {
        TreeMap<String, ArtifactInfoGroup> result = new TreeMap<String, ArtifactInfoGroup>( groupKeyComparator );

        for ( IndexingContext ctx : indexingContexts )
        {
            if ( ctx.isSearchable() )
            {
                searchGrouped( result, grouping, groupKeyComparator, ctx, query );
            }
        }

        return result;
    }

}
