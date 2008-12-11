/**
 * ﻿Sonatype Nexus (TM) [Open Source Version].
 * Copyright (c) 2008 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at ${thirdpartyurl}.
 *
 * This program is licensed to you under Version 3 only of the GNU General
 * Public License as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * Version 3 for more details.
 *
 * You should have received a copy of the GNU General Public License
 * Version 3 along with this program. If not, see http://www.gnu.org/licenses/.
 */
package org.sonatype.nexus.rest.feeds;

import java.util.List;
import java.util.Map;

import org.codehaus.plexus.component.annotations.Component;
import org.sonatype.nexus.feeds.NexusArtifactEvent;

/**
 * The brokenArtifacts feed.
 * 
 * @author cstamas
 */
@Component( role = FeedSource.class, hint = "brokenArtifacts" )
public class BrokenArtifactsFeedSource
    extends AbstractNexusFeedSource
{
    public static final String CHANNEL_KEY = "brokenArtifacts";

    public String getFeedKey()
    {
        return CHANNEL_KEY;
    }

    public String getFeedName()
    {
        return getDescription();
    }

    @Override
    public String getDescription()
    {
        return "Broken artifacts in all Nexus repositories (checksum errors, wrong POMs, ...).";
    }

    @Override
    public List<NexusArtifactEvent> getEventList( Integer from, Integer count, Map<String, String> params )
    {
        return getNexus().getBrokenArtifacts( from, count, getRepoIdsFromParams( params ) );
    }

    @Override
    public String getTitle()
    {
        return "Broken artifacts";
    }

}
