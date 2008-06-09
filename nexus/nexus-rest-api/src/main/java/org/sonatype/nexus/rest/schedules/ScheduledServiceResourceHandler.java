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

import java.io.IOException;
import java.util.Date;
import java.util.logging.Level;

import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.Variant;
import org.sonatype.nexus.rest.model.RepositoryResourceResponse;
import org.sonatype.nexus.rest.model.ScheduledServiceAdvancedResource;
import org.sonatype.nexus.rest.model.ScheduledServiceBaseResource;
import org.sonatype.nexus.rest.model.ScheduledServiceDailyResource;
import org.sonatype.nexus.rest.model.ScheduledServiceMonthlyResource;
import org.sonatype.nexus.rest.model.ScheduledServiceOnceResource;
import org.sonatype.nexus.rest.model.ScheduledServicePropertyResource;
import org.sonatype.nexus.rest.model.ScheduledServiceResourceResponse;
import org.sonatype.nexus.rest.model.ScheduledServiceWeeklyResource;
import org.sonatype.scheduling.NoSuchTaskException;
import org.sonatype.scheduling.ScheduledTask;
import org.sonatype.scheduling.schedules.CronSchedule;
import org.sonatype.scheduling.schedules.DailySchedule;
import org.sonatype.scheduling.schedules.MonthlySchedule;
import org.sonatype.scheduling.schedules.OnceSchedule;
import org.sonatype.scheduling.schedules.WeeklySchedule;

public class ScheduledServiceResourceHandler
    extends AbstractScheduledServiceResourceHandler
{
    public static final String SCHEDULED_SERVICE_ID_KEY = "scheduledServiceId";

    /** The scheduledService ID */
    private String scheduledServiceId;

    /**
     * The default constructor.
     * 
     * @param context
     * @param request
     * @param response
     */
    public ScheduledServiceResourceHandler( Context context, Request request, Response response )
    {
        super( context, request, response );

        this.scheduledServiceId = getRequest().getAttributes().get( SCHEDULED_SERVICE_ID_KEY ).toString();
    }

    protected String getScheduledServiceId()
    {
        return this.scheduledServiceId;
    }

    /**
     * We are handling HTTP GET's.
     */
    public boolean allowGet()
    {
        return true;
    }

    /**
     * Method constructing and returning the Repository route representation.
     */
    public Representation getRepresentationHandler( Variant variant )
        throws IOException
    {
        try
        {
            ScheduledServiceResourceResponse response = new ScheduledServiceResourceResponse();

            ScheduledTask<?> task = getNexus().getTaskById( getScheduledServiceId() );

            if ( OnceSchedule.class.isAssignableFrom( task.getSchedule().getClass() ) )
            {
                OnceSchedule taskSchedule = (OnceSchedule) task.getSchedule();

                ScheduledServiceOnceResource res = new ScheduledServiceOnceResource();

                res.setId( task.getId() );

                res.setName( task.getId() );

                res.setServiceType( getServiceTypeName( task.getType() ) );

                res.setServiceSchedule( getScheduleShortName( taskSchedule ) );

                res.setServiceProperties( null );

                res.setStartDate( formatDate( taskSchedule.getStartDate() ) );

                res.setStartTime( formatTime( taskSchedule.getStartDate() ) );

                response.setData( res );
            }
            else if ( DailySchedule.class.isAssignableFrom( task.getSchedule().getClass() ) )
            {

            }
            else if ( WeeklySchedule.class.isAssignableFrom( task.getSchedule().getClass() ) )
            {

            }
            else if ( MonthlySchedule.class.isAssignableFrom( task.getSchedule().getClass() ) )
            {

            }
            else if ( CronSchedule.class.isAssignableFrom( task.getSchedule().getClass() ) )
            {

            }
        }
        catch ( NoSuchTaskException e )
        {
            getResponse().setStatus(
                Status.CLIENT_ERROR_NOT_FOUND,
                "There is no task with ID=" + getScheduledServiceId() );

            return null;
        }

        ScheduledServiceBaseResource resource = null;

        if ( "0".equals( getScheduledServiceId() ) )
        {
            resource = new ScheduledServiceBaseResource();
            resource.setServiceSchedule( "none" );
        }
        else if ( "1".equals( getScheduledServiceId() ) )
        {
            resource = new ScheduledServiceOnceResource();
            resource.setServiceSchedule( "once" );
            ( (ScheduledServiceOnceResource) resource ).setStartDate( String.valueOf( new Date().getTime() ) );
            ( (ScheduledServiceOnceResource) resource ).setStartTime( "22:00" );
        }
        else if ( "2".equals( getScheduledServiceId() ) )
        {
            resource = new ScheduledServiceDailyResource();
            resource.setServiceSchedule( "daily" );
            ( (ScheduledServiceDailyResource) resource ).setStartDate( String.valueOf( new Date().getTime() ) );
            ( (ScheduledServiceDailyResource) resource ).setStartTime( "22:00" );
            ( (ScheduledServiceDailyResource) resource ).setRecurringTime( "23:00" );
        }
        else if ( "3".equals( getScheduledServiceId() ) )
        {
            resource = new ScheduledServiceWeeklyResource();
            resource.setServiceSchedule( "weekly" );
            ( (ScheduledServiceWeeklyResource) resource ).setStartDate( String.valueOf( new Date().getTime() ) );
            ( (ScheduledServiceWeeklyResource) resource ).setStartTime( "22:00" );
            ( (ScheduledServiceWeeklyResource) resource ).setRecurringTime( "23:00" );
            ( (ScheduledServiceWeeklyResource) resource ).addRecurringDay( "monday" );
            ( (ScheduledServiceWeeklyResource) resource ).addRecurringDay( "tuesday" );
        }
        else if ( "4".equals( getScheduledServiceId() ) )
        {
            resource = new ScheduledServiceMonthlyResource();
            resource.setServiceSchedule( "monthly" );
            ( (ScheduledServiceMonthlyResource) resource ).setStartDate( String.valueOf( new Date().getTime() ) );
            ( (ScheduledServiceMonthlyResource) resource ).setStartTime( "22:00" );
            ( (ScheduledServiceMonthlyResource) resource ).setRecurringTime( "23:00" );
            ( (ScheduledServiceMonthlyResource) resource ).addRecurringDay( "1" );
            ( (ScheduledServiceMonthlyResource) resource ).addRecurringDay( "7" );
        }
        else if ( "5".equals( getScheduledServiceId() ) )
        {
            resource = new ScheduledServiceAdvancedResource();
            resource.setServiceSchedule( "advanced" );
            ( (ScheduledServiceAdvancedResource) resource ).setCronCommand( "cronCommand" );
        }

        resource.setId( getScheduledServiceId() );
        resource.setName( "name" + getScheduledServiceId() );
        resource.setServiceType( "1" );

        ScheduledServicePropertyResource propResource = new ScheduledServicePropertyResource();
        propResource.setId( "1" );
        propResource.setValue( "some text" );
        resource.addServiceProperty( propResource );

        ScheduledServiceResourceResponse response = new ScheduledServiceResourceResponse();
        response.setData( resource );

        return serialize( variant, response );
    }

    /**
     * This resource allows PUT.
     */
    public boolean allowPut()
    {
        return true;
    }

    /**
     * Update a repository route.
     */
    public void put( Representation representation )
    {
        ScheduledServiceResourceResponse response = (ScheduledServiceResourceResponse) deserialize( new ScheduledServiceResourceResponse() );
        
        if ( response == null )
        {
            return;
        }
        else
        {
            ScheduledServiceBaseResource resource = response.getData();
            
            try
            {
                ScheduledTask<?> task = getNexus().getTaskById( resource.getId() );

                //TODO: ultimately will not want to create new schedule everytime, should just update parameters
                //in existing, unless schedule changes or service type changes
                task.cancel();
                getNexus().schedule( getModelName( resource ), getModelNexusTask( resource ), getModelSchedule( resource ) );
            }
            catch ( NoSuchTaskException e )
            {
                getLogger().log( Level.SEVERE, "Unable to locate task id:" + resource.getId(), e );
                getResponse().setStatus( Status.CLIENT_ERROR_NOT_FOUND, "Scheduled service not found!" );
            }
        }
    }

    /**
     * This resource allows DELETE.
     */
    public boolean allowDelete()
    {
        return true;
    }

    /**
     * Delete a task.
     */
    public void delete()
    {
        try
        {
            getNexus().getTaskById( getScheduledServiceId() ).cancel();
        }
        catch ( NoSuchTaskException e )
        {
            getResponse().setStatus( Status.CLIENT_ERROR_NOT_FOUND, "Scheduled service not found!" );
        }
    }
}
