/*
 * Nexus: Maven Repository Manager
 * Copyright (C) 2008 Sonatype Inc.                                                                                                                          
 * 
 * This file is part of Nexus.                                                                                                                                  
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 */
package org.sonatype.nexus.rest.schedules;

import java.util.HashMap;
import java.util.Map;

import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.sonatype.nexus.maven.tasks.SnapshotRemoverTask;
import org.sonatype.nexus.rest.AbstractNexusResourceHandler;
import org.sonatype.nexus.tasks.ClearCacheTask;
import org.sonatype.nexus.tasks.PublishIndexesTask;
import org.sonatype.nexus.tasks.RebuildAttributesTask;
import org.sonatype.nexus.tasks.ReindexTask;
import org.sonatype.scheduling.schedules.CronSchedule;
import org.sonatype.scheduling.schedules.DailySchedule;
import org.sonatype.scheduling.schedules.MonthlySchedule;
import org.sonatype.scheduling.schedules.OnceSchedule;
import org.sonatype.scheduling.schedules.Schedule;
import org.sonatype.scheduling.schedules.WeeklySchedule;

public class AbstractScheduledServiceResourceHandler
    extends AbstractNexusResourceHandler
{
    /** Schedule Type Off. */
    public static final String SCHEDULE_TYPE_NONE = "none";

    /** Schedule Type Once. */
    public static final String SCHEDULE_TYPE_ONCE = "once";

    /** Schedule Type Daily. */
    public static final String SCHEDULE_TYPE_DAILY = "daily";

    /** Schedule Type Weekly. */
    public static final String SCHEDULE_TYPE_WEEKLY = "weekly";

    /** Schedule Type Monthly. */
    public static final String SCHEDULE_TYPE_MONTHLY = "monthly";

    /** Schedule Type Advanced. */
    public static final String SCHEDULE_TYPE_ADVANCED = "advanced";

    /**
     * Type property resource: string
     */
    public static final String PROPERTY_TYPE_STRING = "string";

    /**
     * Type property resource: number
     */
    public static final String PROPERTY_TYPE_NUMBER = "number";

    /**
     * Type property resource: date
     */
    public static final String PROPERTY_TYPE_DATE = "date";

    protected Map<String, String> serviceNames = new HashMap<String, String>();
    {
        serviceNames.put( PublishIndexesTask.class.getName(), "Publish Indexes" );
        serviceNames.put( ReindexTask.class.getName(), "Reindex Repositories" );
        serviceNames.put( RebuildAttributesTask.class.getName(), "Rebuild Repository Atributes" );
        serviceNames.put( ClearCacheTask.class.getName(), "Clear Repository Caches" );
        serviceNames.put( SnapshotRemoverTask.class.getName(), "Remove Snapshots From Repository" );
    }

    /**
     * Standard constructor.
     * 
     * @param context
     * @param request
     * @param response
     */
    public AbstractScheduledServiceResourceHandler( Context context, Request request, Response response )
    {
        super( context, request, response );
    }

    protected String getServiceTypeName( String serviceTypeId )
    {
        if ( serviceNames.containsKey( serviceTypeId ) )
        {
            return serviceNames.get( serviceTypeId );
        }
        else
        {
            return serviceTypeId;
        }
    }

    protected String getScheduleShortName( Schedule schedule )
    {
        if ( OnceSchedule.class.isAssignableFrom( schedule.getClass() ) )
        {
            return SCHEDULE_TYPE_ONCE;
        }
        else if ( DailySchedule.class.isAssignableFrom( schedule.getClass() ) )
        {
            return SCHEDULE_TYPE_DAILY;
        }
        else if ( WeeklySchedule.class.isAssignableFrom( schedule.getClass() ) )
        {
            return SCHEDULE_TYPE_WEEKLY;
        }
        else if ( MonthlySchedule.class.isAssignableFrom( schedule.getClass() ) )
        {
            return SCHEDULE_TYPE_MONTHLY;
        }
        else if ( CronSchedule.class.isAssignableFrom( schedule.getClass() ) )
        {
            return SCHEDULE_TYPE_ADVANCED;
        }
        else
        {
            return schedule.getClass().getName();
        }
    }
}
