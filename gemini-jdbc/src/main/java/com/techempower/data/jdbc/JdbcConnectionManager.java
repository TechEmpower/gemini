/*******************************************************************************
 * Copyright (c) 2018, TechEmpower, Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name TechEmpower, Inc. nor the names of its
 *       contributors may be used to endorse or promote products derived from
 *       this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL TECHEMPOWER, INC. BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY
 * OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *******************************************************************************/

package com.techempower.data.jdbc;

import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import com.techempower.*;
import com.techempower.asynchronous.*;
import com.techempower.data.*;
import com.techempower.helper.*;
import com.techempower.log.*;
import com.techempower.thread.*;
import com.techempower.util.*;

/**
 * Manages a list of JdbcConnectionProfile objects.
 *   <p>
 * Applications do not typically interact directly with this class, but
 * rather interact with BasicConnectorFactory to get connectors and 
 * JdbcConnector to execute queries.
 *
 * @see JdbcConnector
 * @see BasicConnectorFactory
 */
public class JdbcConnectionManager
  implements Asynchronous
{
  //
  // Constants.
  //
  
  public static final long POOL_SHRINK_PERIODICITY = UtilityConstants.MINUTE;

  //
  // Member variables.
  //

  private final JdbcConnectionAttributes     attributes;
  private final AtomicInteger                callCount        = new AtomicInteger(0);
  private final AtomicInteger                profileCounter   = new AtomicInteger(0);
  private final DatabaseConnectionListener   listener;
  private final List<JdbcConnectionProfile>  profiles;
  private final ThreadLocal<JdbcConnectionProfile> profilesForThreads;
  private final TechEmpowerApplication       application;
  private final ComponentLog                 log;
  private final JdbcConnectionManagerThread  thread;
  private final AtomicInteger                profileIndexScanOffset = new AtomicInteger(0);        
  
  private transient long nextCheckSizeTime = System.currentTimeMillis() + POOL_SHRINK_PERIODICITY;

  //
  // Public methods.
  //

  /**
   * Constructor.
   * 
   * @param attributes JdbcConnectionAttributes object specifying attributes
   *               of the Connections to establish to the database.
   */
  protected JdbcConnectionManager(JdbcConnectionAttributes attributes)
  {
    this.profiles    = new CopyOnWriteArrayList<>();
    this.attributes  = attributes;
    this.listener    = attributes.getListener();
    this.application = attributes.getApplication();
    this.log         = attributes.getLog();
    
    // The thread's sleep interval is the configured test interval divided by
    // the minimum number of Connections in the pool.  That is, on average, 
    // each Connection will be evaluated once per configured test interval.
    this.thread = new JdbcConnectionManagerThread(
        this.attributes.getTestInterval() / this.attributes.getMinimumPoolSize());
    
    this.profilesForThreads = new ThreadLocal<>();
  }
  
  /**
   * The Connection Manager Thread periodically checks the size of the pool
   * and runs keep-alive queries on each of the active Connection profiles.
   */
  class        JdbcConnectionManagerThread 
    extends    EndableThread
  {
    private int index = 0;
    
    public JdbcConnectionManagerThread(long sleepInterval)
    {
      // Disallow a sleep period less than one second regardless of how many
      // connections we maintain (which can cause the sleepInterval parameter
      // to become quite small).
      super("JDBC Connection Manager (" 
          + JdbcConnectionManager.this.application.getVersion().getProductName()
          + ", "
          + JdbcConnectionManager.this.attributes.getDisplayName()
          + ")",
          NumberHelper.boundInteger((int)sleepInterval, (int)UtilityConstants.SECOND, Integer.MAX_VALUE));
    }
    
    @Override
    public void run()
    {
      log("JDBC Connection Manager thread started (sleep period " + getSleepPeriod() + "ms).");

      // Check the pool size immediately on start.
      checkSize();

      while (checkPause())
      {
        simpleSleep();

        if (checkPause())
        {
          //debug("Maintaining JDBC connections.");
          
          // Check the pool size.
          checkSize();
          
          // Reduce size periodically.
          compactSize();
          
          // Bypass keep-alive if connection tests are disabled.
          if (JdbcConnectionManager.this.attributes.isTestEnabled())
          {
            // Keep a connection alive.
            keepAlive(index++);
            if (index == Integer.MAX_VALUE)
            {
              index = 0;
            }
          }
        }
      }
      
      log("JDBC Connection Manager thread ending.");
    }
    
    private void log(String debug)
    {
      JdbcConnectionManager.this.log.log(debug, LogLevel.NORMAL);
    }
  }
  
  @Override
  public void begin()
  {
    // Start thread.
    thread.begin();
  }
  
  @Override
  public void end()
  {
    // Shut down thread.
    thread.end();
    
    // Drop and disconnect all profiles.
    dropAllProfiles();
  }
  
  /**
   * Adds a new connected profile.  If a connection cannot be established,
   * this method returns null and does not add a profile to the manager.
   */
  protected JdbcConnectionProfile addNewProfile(boolean addWhenFull)
  {
    JdbcConnectionProfile profile = createConnectedProfile();

    // If a Connection was established, it will be non-null.
    if (profile.getConnection() != null)
    {
      // Add to the profiles list if we have room or were told to always
      // add even if the list is full.
      if (  (profiles.size() < attributes.getMaximumPoolSize())
         || (addWhenFull)
         )
      {
        synchronized (this)
        {
          profiles.add(profile);
        }
      }
      // Otherwise, close the profile once the client releases it.
      else 
      {
        profile.setCloseOnRelease(true);
      }
      
      return profile;
    }

    // If we got here, we must have not got a good connection.
    return null;
  }
  
  /**
   * Gets a detached profile (that is, one that is not part of the connection
   * pool).  This should not be used regularly but only for special, possibly
   * very high-cost, database operations.
   */
  protected JdbcConnectionProfile getDetachedProfile()
  {
    return createConnectedProfile();
  }
  
  /**
   * Checks the size of the pool.
   */
  private void checkSize()
  {
    final JdbcConnectionAttributes connectionAttributes = attributes;
    int size = profiles.size();
    
    // Add connection profiles until the size of the pool is above the 
    // minimum boundary.
    if (size < connectionAttributes.getMinimumPoolSize())
    {
      // Add one connection in this thread to confirm that connectivity is
      // presently good.
      if (addNewProfile(true) != null)
      {
        // If the connection was added successfully, add additional 
        // connections in a worker thread.
        size = profiles.size();
        if (size < connectionAttributes.getMinimumPoolSize())
        {
          ThreadHelper.submit(() -> {
            // Only tolerate 10 connection errors and then give up.
            int errors = 0;
            while (  (profiles.size() < connectionAttributes.getMinimumPoolSize())
                  && (errors < 10)
                  )
            {
              if (addNewProfile(true) == null) 
              {
                errors++;
              }
            }
          });
        }
      }
      else
      {
        log.log("Cannot establish connection to populate pool.", LogLevel.ALERT);
      }
    }
    
    // If necessary, remove profiles until the size is below the maximum.
    while (profiles.size() > connectionAttributes.getMaximumPoolSize())
    {
      dropProfile();
    }
  }
  
  /**
   * Periodically reduces the size of the connection pool if it's greater than
   * the minimum pool size.  This is called only by the manager thread.
   */
  private void compactSize()
  {
    final long now = System.currentTimeMillis();
    final int size = profiles.size();
    final JdbcConnectionAttributes connectionAttributes = attributes;
    
    // Periodically shrink the pool if Connections are no longer needed.
    // We run this on every 5000th call to checkSize.
    if (  (size > connectionAttributes.getMinimumPoolSize())
       && (nextCheckSizeTime < now)
       )
    {
      final long staleConnectionTime = 
        System.currentTimeMillis() - connectionAttributes.getStaleTimeout();
      final long abortConnectionTime =
        System.currentTimeMillis() - connectionAttributes.getAbortTimeout();
      
      // Create a copy of the profiles array for iteration.
      List<JdbcConnectionProfile> profileArray = new ArrayList<>(profiles);
      
      synchronized (this)
      {
        nextCheckSizeTime = now + POOL_SHRINK_PERIODICITY;
        
        for (JdbcConnectionProfile current : profileArray)
        {
          // If a Connection is stale (hasn't been used in a long time
          // and isn't currently in use), close and remove it.  Also remove
          // connections that have been in use longer than the abort-timeout
          // period (1 hour by default).
          if (  (current.getLastUse() < abortConnectionTime)
             || (  (current.getLastUse() < staleConnectionTime)
                && (!current.isInUse())
                )
             )
          {
            current.close(true);   // Close after a 10-second delay.
            profiles.remove(current);
          }
          
          // We only want to keep removing until we're back down to the
          // minimum pool size.
          if (profiles.size() <= connectionAttributes.getMinimumPoolSize())
          {
            break;
          }
        }
      }
      
      // Reduce the scan offset back to zero if it's over a billion.
      if (profileIndexScanOffset.get() > 1000000000)
      {
        profileIndexScanOffset.set(0);
      }
    }
  }
  
  /**
   * Keep a connection alive, selecting a connection from the pool based on
   * the provided index.  The actual index selected is (index modulo size). 
   */
  protected void keepAlive(int index)
  {
    JdbcConnectionProfile profile = null;
    synchronized (this)
    {
      if (this.profiles.size() > 0)
      {
        profile = this.profiles.get(index % this.profiles.size());
      }
    }
    
    if (profile != null)
    {
      //this.log.debug("Keeping alive connection " + profile.getId() + ".");
      profile.keepAlive();
    }
  }
  
  /**
   * Gets a profile for use by the current thread.  If the thread has a
   * preferred connection available in the ThreadLocal map, attempt to claim
   * that first.  If that fails, find an available connection by scanning the
   * list of connections.
   */
  protected JdbcConnectionProfile getProfile()
  {
    callCount.incrementAndGet();
    
    JdbcConnectionProfile threadProfile = profilesForThreads.get();
    
    // If the thread has a preferred profile and its connection is available,
    // attempt to claim it for use.
    if (  (threadProfile != null)
       && (threadProfile.isConnectionAvailable())
       )
    {
      if (threadProfile.claim())
      {
        return threadProfile;
      }
      /*
      else
      {
        System.out.println("%%%% Thread " + Thread.currentThread().getId() + " was unable to claim its reserved profile " + threadProfile.getId());
      }
      */
    }
    /*
    else
    {
      System.out.println("#### Thread " + Thread.currentThread().getId() + " did not yet have a reserved profile. Making one.");
    }
    */
    
    // Either the thread has no preferred profile or it has been claimed by
    // another thread since its last use by the current thread.
    threadProfile = getAnyAvailableProfile();
    profilesForThreads.set(threadProfile);
    return threadProfile;
  }

  /**
   * Gets an available profile from the connection pool, creating one if 
   * necessary.  Returns null if no connection profiles can be made 
   * available.
   */
  private JdbcConnectionProfile getAnyAvailableProfile()
  {
    // A reference to an available connection profile.
    JdbcConnectionProfile available = null;

    int size = profiles.size();
    if (size > 0)
    {
      int scanIteration = profileIndexScanOffset.getAndIncrement();
      int scanIndex;
      final int startingScanCycle = scanIteration / size;
      int currentScanCycle = startingScanCycle;
      while (currentScanCycle - startingScanCycle < 2)
      {
        scanIndex = scanIteration % size;
        try
        {
          final JdbcConnectionProfile current = profiles.get(scanIndex);
          if (current.claim())
          {
            available = current;
            break;
          }
        }
        catch (IndexOutOfBoundsException ioobexc) 
        {
          // Do nothing.  This can happen if the profiles list changes size
          // while we're in this loop.  It just means the list shrunk while
          // we were iterating, which is fine.  We'll proceed to add a profile
          // below.
          
          // Reset the size variable.
          size = profiles.size();
        }
  
        scanIteration = profileIndexScanOffset.getAndIncrement();
        currentScanCycle = scanIteration / size;
      }
    }

    // If no connections are available, create a new connection and, if the
    // current number of open connections is below the maximum pool size,
    // add the new connection to the pool.
    //
    // Note that with high thread contention, there is a possibility that the
    // size of the pool may go beyond the maximum size for a moment due to
    // the unsynchronized check on the pool's size.  However, the pool will 
    // shrink when the next call to checkSize occurs.
    if (available == null)
    {
      available = addNewProfile(false);
    }

    // If the available connection is closed for some reason, let's try to 
    // reopen it. If that fails, return the open reference if we've got one.
    if (available.isClosed())
    {
      available.establishDatabaseConnection();
    }

    // If the available connection is -still- closed, drop it from the pool
    // and return null. 
    if (available.isClosed())
    {
      // Drop the profile from the pool.
      dropProfile(available);
      available = null;
    }

    // Return the reference.  This will return null if there are NO UNUSED
    // profiles and a new profile cannot be connected to the database.  
    return available;
  }
  
  /**
   * Claims a ConnectionProfile and then returns that profile's 
   * ConnectionMonitor.
   */
  protected ConnectionMonitor getConnectionMonitor()
    throws SQLException
  {
    final JdbcConnectionProfile profile = getProfile();
    if (profile != null)
    {
      return profile.getMonitor();
    }
    else
    {
      throw new SQLException("No JDBC connection profiles available.");
    }
  }
  
  /**
   * Drops a connection profile.  Drops the last one in the collection.
   * Before dropping the profile, this method closes the profile's 
   * connection to the database.
   */
  protected synchronized void dropProfile()
  {
    dropProfile(profiles.size() - 1);
  }

  /**
   * Drops a specific connection profile.
   *
   * @param whichProfile The index of the profile to drop.
   */
  protected synchronized void dropProfile(int whichProfile)
  {
    if ( (whichProfile >= 0)
      && (whichProfile < profiles.size() )
      )
    {
      // Close the profile if requested.
      final JdbcConnectionProfile profile = profiles.get(whichProfile);
      dropProfile(profile);
    }
  }

  /**
   * Drops a specific connection profile.
   *
   * @param profile The profile to drop.
   */
  protected synchronized void dropProfile(JdbcConnectionProfile profile)
  {
    if (profile != null)
    {
      // Close on a new thread.
      profile.close(true);

      // Remove it.
      profiles.remove(profile);
    }
  }

  /**
   * Drops and disconnect <b>all</b> of the connection profiles being managed
   * by this connection manager.
   */
  protected synchronized void dropAllProfiles()
  {
    while (profiles.size() > 0)
    {
      dropProfile(0);
    }
  }
  
  /**
   * Gets the call count.  This is the number of times a call to getProfile
   * has been made.  It is roughly analogous to the total number of queries
   * that have been executed, assuming a single query is executed on each
   * Connector.
   */
  public int getCallCount()
  {
    return callCount.get();
  }
  
  /**
   * Gets the Connection Attributes.
   */
  protected JdbcConnectionAttributes getAttributes()
  {
    return attributes;
  }
  
  /**
   * Gets the ComponentLog.
   */
  protected ComponentLog getLog()
  {
    return log;
  }

  /**
   * Creates a new JdbcConnectionProfile.
   */
  protected JdbcConnectionProfile createConnectedProfile()
  {
    int newProfileId = profileCounter.incrementAndGet();
    JdbcConnectionProfile profile = new JdbcConnectionProfile(newProfileId, 
        this);
    profile.establishDatabaseConnection();

    return profile;
  }
  
  /**
   * Returns a list of the profiles.
   */
  public synchronized List<JdbcConnectionProfile> getProfiles()
  {
    return new ArrayList<>(profiles);
  }
  
  /**
   * Returns the DatabaseConnectionListener used for this manager. 
   */
  protected DatabaseConnectionListener getListener()
  {
    return listener;
  }

}  // End JdbcConnectionManager.
