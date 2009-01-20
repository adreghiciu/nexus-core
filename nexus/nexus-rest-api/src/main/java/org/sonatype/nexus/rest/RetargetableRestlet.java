/**
 * Sonatype Nexus (TM) Open Source Version.
 * Copyright (c) 2008 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://nexus.sonatype.org/dev/attributions.html
 * This program is licensed to you under Version 3 only of the GNU General Public License as published by the Free Software Foundation.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License Version 3 for more details.
 * You should have received a copy of the GNU General Public License Version 3 along with this program.
 * If not, see http://www.gnu.org/licenses/.
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc.
 * "Sonatype" and "Sonatype Nexus" are trademarks of Sonatype, Inc.
 */
package org.sonatype.nexus.rest;

import org.restlet.Context;
import org.restlet.Restlet;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;

/**
 * A simple restlet that is returned as root, while allowing to recreate roots in applications per application request.
 * 
 * @author cstamas
 */
public class RetargetableRestlet
    extends Restlet
{
    private Restlet root;

    public RetargetableRestlet( Context context )
    {
        super( context );
    }

    @Override
    public void handle( Request request, Response response )
    {
        super.handle( request, response );

        Restlet next = getRoot();

        if ( next != null )
        {
            next.handle( request, response );
        }
        else
        {
            response.setStatus( Status.CLIENT_ERROR_NOT_FOUND );
        }
    }

    public Restlet getRoot()
    {
        return root;
    }

    public void setRoot( Restlet root )
    {
        this.root = root;
    }
}
