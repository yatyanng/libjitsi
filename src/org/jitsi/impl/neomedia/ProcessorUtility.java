/*
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jitsi.impl.neomedia;

import javax.media.*;

import org.jitsi.util.*;

/**
 * A utility class that provides utility functions when working with processors.
 *
 * @author Emil Ivov
 * @author Ken Larson
 * @author Lyubomir Marinov
 */
public class ProcessorUtility
    implements ControllerListener
{

    /**
     * The <tt>Logger</tt> used by the <tt>ProcessorUtility</tt> class and its
     * instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(ProcessorUtility.class);

    /**
     * The <tt>Object</tt> used for syncing when waiting for a processor to
     * enter a specific state.
     */
    private final Object stateLock = new Object();

    /**
     * The indicator which determines whether the waiting of this instance on a
     * processor for it to enter a specific state has failed.
     */
    private boolean failed = false;

    /**
     * Initializes a new <tt>ProcessorUtility</tt> instance.
     */
    public ProcessorUtility()
    {
    }

    /**
     * Gets the <tt>Object</tt> to use for syncing when waiting for a processor
     * to enter a specific state.
     *
     * @return the <tt>Object</tt> to use for syncing when waiting for a
     * processor to enter a specific state
     */
    private Object getStateLock()
    {
        return stateLock;
    }

    /**
     * Specifies whether the wait operation has failed or completed with
     * success.
     *
     * @param failed <tt>true</tt> if waiting has failed; <tt>false</tt>,
     * otherwise
     */
    private void setFailed(boolean failed)
    {
        this.failed = failed;
    }

    /**
     * This method is called when an event is generated by a
     * <code>Controller</code> that this listener is registered with. We use
     * the event to notify all waiting on our lock and record success or
     * failure.
     *
     * @param ce The event generated.
     */
    public void controllerUpdate(ControllerEvent ce)
    {
        // If there was an error during configure or
        // realize, the processor will be closed
        if (ce instanceof ControllerClosedEvent)
        {
            if (ce instanceof ControllerErrorEvent)
                logger.warn("ControllerErrorEvent: " + ce);
            else
                if (logger.isDebugEnabled())
                    logger.debug("ControllerClosedEvent: " + ce);

            setFailed(true);

            // All controller events, send a notification
            // to the waiting thread in waitForState method.
        }

        Object stateLock = getStateLock();

        synchronized (stateLock)
        {
            stateLock.notifyAll();
        }
    }

    /**
     * Waits until <tt>processor</tt> enters state and returns a boolean
     * indicating success or failure of the operation.
     *
     * @param processor Processor
     * @param state one of the Processor.XXXed state vars
     * @return <tt>true</tt> if the state has been reached; <tt>false</tt>,
     * otherwise
     */
    public synchronized boolean waitForState(Processor processor, int state)
    {
        processor.addControllerListener(this);
        setFailed(false);

        logger.debug(processor.getClass().getName()+" "+processor.hashCode()
        	+" is waiting for state "+state+"!");
        
        // Call the required method on the processor
        if (state == Processor.Configured)
            processor.configure();
        else if (state == Processor.Realized)
            processor.realize();

        boolean interrupted = false;

        // Wait until we get an event that confirms the
        // success of the method, or a failure event.
        // See StateListener inner class
        while ((processor.getState() < state) && !failed)
        {
        	logger.trace(processor.getClass().getName()+" "+processor.hashCode()
        		+" is in state "+processor.getState()+" and is waiting for state "+state+"!");
        	
            Object stateLock = getStateLock();

            synchronized (stateLock)
            {
                try
                {
                    stateLock.wait();
                }
                catch (InterruptedException ie)
                {
                    logger.warn(
                            "Interrupted while waiting on Processor "
                                + processor
                                + " for state "
                                + state,
                            ie);
                    /*
                     * XXX It is not really clear what we should do. It seems
                     * that an InterruptedException may be thrown and the
                     * Processor will still work fine. Consequently, we cannot
                     * fail here. Besides, if the Processor fails, it will tell
                     * us with a ControllerEvent anyway and we will get out of
                     * the loop.
                     */
                    interrupted = true;
//                    processor.removeControllerListener(this);
//                    return false;
                }
            }
            
            logger.trace(processor.getClass().getName()+" "+processor.hashCode()
    			+" is now in state "+processor.getState()+"!");
        }
        if (interrupted)
            Thread.currentThread().interrupt();

        processor.removeControllerListener(this);
        if (failed) {
        	logger.trace(processor.getClass().getSimpleName()+" "+processor.hashCode()
				+" has failed waiting for state "+state+ "!");
        }
        return !failed;
    }
}
