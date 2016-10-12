/**
 * Copyright (C) 2016 Axel Faust / Markus Joos
 * Copyright (C) 2016 Order of the Bee
 *
 * This file is part of Community Support Tools
 *
 * Community Support Tools is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * Community Support Tools is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Community Support Tools. If not, see
 * <http://www.gnu.org/licenses/>.
 */
package org.orderofthebee.addons.support.tools.repo;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.alfresco.util.ParameterCheck;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;

/**
 * @author Axel Faust, <a href="http://acosix.de">Acosix GmbH</a>
 */
public class LimitedListAppender extends AppenderSkeleton
{

    // 20 minutes after last retrieval we assume tail is no longer active and automatically deregister this appender
    private static final long AUTO_DEREGISTRATION_TIMEOUT = 1000 * 60 * 20;

    protected final List<LoggingEvent> storedEvents = new LinkedList<>();

    protected final List<Logger> appendedToLoggers = new ArrayList<>();

    protected final int eventCountLimit;

    protected long lastRetrievalTimestamp = System.currentTimeMillis();

    public LimitedListAppender(final String uuid, final int eventCountLimit)
    {
        ParameterCheck.mandatory("uuid", uuid);
        this.setName(uuid);

        if (eventCountLimit <= 0)
        {
            throw new IllegalArgumentException("eventCountLimit must be a positive integer");
        }

        this.eventCountLimit = eventCountLimit;
    }

    public void registerAsAppender(final Logger logger)
    {
        logger.addAppender(this);
        this.appendedToLoggers.add(logger);
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void close()
    {
        // NO-OP
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public boolean requiresLayout()
    {
        return false;
    }

    public List<LoggingEvent> retrieveLogEvents()
    {
        List<LoggingEvent> retrievedEvents;
        synchronized (this.storedEvents)
        {
            retrievedEvents = new ArrayList<>(this.storedEvents);
            this.storedEvents.clear();
            this.lastRetrievalTimestamp = System.currentTimeMillis();
        }

        return retrievedEvents;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    protected void append(final LoggingEvent event)
    {
        boolean deregister;

        synchronized (this.storedEvents)
        {
            this.storedEvents.add(event);

            while (this.storedEvents.size() > this.eventCountLimit)
            {
                this.storedEvents.remove(0);
            }

            deregister = System.currentTimeMillis() - this.lastRetrievalTimestamp > AUTO_DEREGISTRATION_TIMEOUT;
        }

        if (deregister)
        {
            for (final Logger logger : this.appendedToLoggers)
            {
                logger.removeAppender(this);
            }
        }
    }
}
