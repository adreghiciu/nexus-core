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
package org.sonatype.nexus.proxy.repository;

import org.sonatype.nexus.configuration.ConfigurationException;
import org.sonatype.nexus.configuration.application.ApplicationConfiguration;
import org.sonatype.nexus.configuration.model.CRepository;

/**
 * A component responsible for "apply" (config -> repo) and "prepare" (repo -> config) steps.
 * 
 * @author cstamas
 */
public interface RepositoryConfigurator
{
    /**
     * Validates the repoConfig.
     * 
     * @param configuration
     * @param repoConfig
     * @throws ConfigurationException on validation problem.
     */
    public void validate( ApplicationConfiguration configuration, CRepository repoConfig )
        throws ConfigurationException;

    /**
     * Will apply the configuration is parameter repo to the repository.
     * 
     * @param repository
     * @param configuration
     * @param repoConfig
     * @throws ConfigurationException
     */
    void applyConfiguration( Repository repository, ApplicationConfiguration configuration, CRepository repoConfig )
        throws ConfigurationException;

    /**
     * Will prepare repoConfig for save, by syncing it with repository state.
     * 
     * @param repository
     * @param configuration
     * @param repoConfig
     * @throws ConfigurationException
     */
    void prepareForSave( Repository repository, ApplicationConfiguration configuration, CRepository repoConfig );

    /**
     * Returns the external configuration object, if any. Null otherwise.
     * 
     * @return
     */
    Object getExternalConfiguration( Repository repository );
}
