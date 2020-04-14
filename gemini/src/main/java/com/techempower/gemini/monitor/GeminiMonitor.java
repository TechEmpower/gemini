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

package com.techempower.gemini.monitor;

import java.lang.management.*;
import java.util.*;
import java.util.concurrent.atomic.*;

import com.techempower.asynchronous.*;
import com.techempower.gemini.*;
import com.techempower.gemini.feature.*;
import com.techempower.gemini.monitor.cpupercentage.*;
import com.techempower.gemini.monitor.health.*;
import com.techempower.gemini.monitor.session.*;
import com.techempower.gemini.session.Session;
import com.techempower.helper.*;
import com.techempower.log.*;
import com.techempower.thread.*;
import com.techempower.util.*;

/**
 * The main class for Gemini application-monitoring functionality.  
 * Applications should instantiate an instance of Monitor and then attach
 * the provided MonitorListener as a DatabaseConnectionListener and Dispatch
 * Listener.
 *   <p>
 * The Monitor has four sub-components:
 *   <ol>
 * <li>Performance monitoring, the main component, observes the execution
 *     of requests to trend the performance of each type of request over
 *     time.</li>
 * <li>Health monitoring, an optional component, observes the total amount
 *     of memory used, the number of threads, and other macro-level concerns
 *     to evaluate the health of the application.</li>
 * <li>CPU Usage Percentage monitoring, an optional component, uses JMX to
 *     observe the CPU time of Java threads and provide a rough usage
 *     percentage per thread in 1-second real-time samples.</li>
 * <li>Web session monitoring, an optional component, that counts and 
 *     optionally maintains a set of active web sessions.</li>
 *   </ol>
 * Configurable options:
 *   <ul>
 * <li>Feature.monitor - Is the Gemini Monitoring component enabled as a
 *     whole?  Defaults to yes.</li>
 * <li>Feature.monitor.health - Is the Health Monitoring sub-component 
 *     enabled?  Defaults to yes.</li>
 * <li>Feature.monitor.cpu - Is the CPU Usage Percentage sub-component 
 *     enabled?  Defaults to yes.</li>
 * <li>Feature.monitor.session - Is the Session Monitoring sub-component 
 *     enabled?</li>
 * <li>GeminiMonitor.HealthSnapshotCount - The number of health snapshots to
 *     retain in memory.  The default is 120.  Cannot be lower than 2 or
 *     greater than 30000.</li>
 * <li>GeminiMonitor.HealthSnapshotInterval - The number of milliseconds 
 *     between snapshots.  The default is 300000 (5 minutes).  Cannot be set 
 *     below 500ms or greater than 1 year.</li>
 * <li>GeminiMonitor.SessionSnapshotCount - The number of session snapshots to
 *     retain in memory.  The defaults are the same as Health snapshots.</li>
 * <li>GeminiMonitor.SessionSnapshotInterval - The number of milliseconds 
 *     between snapshots.  Defaults same as for health.</li>
 * <li>GeminiMonitor.SessionTracking - If true, active sessions will be 
 *     tracked by the session monitor to allow for listing active sessions.</li>
 * </ul>
 *   <p>
 * Note that some of the operations executed by the health snapshot are non
 * trivial (e.g., 10-20 milliseconds).  Setting a very low snapshot interval
 * such as 500ms would mean that every 500ms, you may be consuming about
 * 25ms of CPU time to take a snapshot.  An interval of 1 minute should be
 * suitable for most applications.
 */
public abstract class GeminiMonitor
  implements Configurable,
             Feature,
             Asynchronous
{
  
  //
  // Constants.
  //
  
  public static final String COMPONENT_CODE = "Moni";
  public static final String PROPS_PREFIX = "GeminiMonitor.";
  public static final int    DEFAULT_SNAPSHOT_HISTORY = 120;
  public static final long   DEFAULT_SNAPSHOT_INTERVAL = 5 * UtilityConstants.MINUTE;
  public static final long   MINIMUM_SNAPSHOT_INTERVAL = 500L;
  public static final int    MAXIMUM_SNAPSHOT_COUNT = 30000;
  public static final long   MINIMUM_PERFORMANCE_INTERVAL = 5 * UtilityConstants.SECOND;
  public static final long   MAXIMUM_PERFORMANCE_INTERVAL = UtilityConstants.YEAR;
    
  //
  // Member variables.
  //
  
  protected final ComponentLog      log;
  private final GeminiApplication app;
  private final FeatureManager    fm;
  private final MonitorListener   listener;          // Monitors other components such as the Dispatcher.
  private final Map<String, MonitoredCommand> commands;
  private final Map<Long, MonitorSample> currentRequests;
  private final ThreadMXBean      threadBean;
  private final boolean           cpuTimeSupported;
  private final AtomicInteger     concurrentDispatches = new AtomicInteger(0);
  private final AtomicInteger     concurrentPages = new AtomicInteger(0);
  private final AtomicInteger     concurrentQueries = new AtomicInteger(0);
  
  private final GeminiMonitorThread thread;
  private GeminiMonitorListener[] monitorListeners;  // Components listening to the GeminiMonitor.

  // Performance
  private long              perfIntervalStart = 0L;
  private long              perfIntervalEnd   = 0L;
  private long              perfIntervalLength = UtilityConstants.HOUR;
  private final PercentageMonitorThread percentageThread;
  private PercentageEvaluator[] percEvaluators = new PercentageEvaluator[0];
  
  // Health
  private int               healthSnapshotCount = DEFAULT_SNAPSHOT_HISTORY;
  private long              healthIntervalLength = DEFAULT_SNAPSHOT_INTERVAL;
  private long              healthIntervalEnd = 0L;
  private HealthSnapshot[]  healthSnapshots = new HealthSnapshot[this.healthSnapshotCount];
  private HealthSnapshot    currentHealth = null;
  private HealthEvaluator[] healthEvaluators = new HealthEvaluator[0];
  private ThreadGroup       rootThreadGroup;
  
  // Sessions
  //protected final SessionState sessionState;
  private int               sessionSnapshotCount = DEFAULT_SNAPSHOT_HISTORY;
  private long              sessionIntervalLength = DEFAULT_SNAPSHOT_INTERVAL;
  private long              sessionIntervalEnd = 0L;
  private SessionSnapshot[] sessionSnapshots = new SessionSnapshot[this.sessionSnapshotCount];

  //
  // Member methods.
  //
  
  /**
   * Constructor.
   */
  public GeminiMonitor(GeminiApplication app)
  {
    this.app = app;
    this.log = app.getLog(COMPONENT_CODE);
    app.getConfigurator().addConfigurable(this);
    this.fm = app.getFeatureManager();
    enrollFeature(this.fm);
    this.listener = new MonitorListener(this);
    this.monitorListeners = new GeminiMonitorListener[0];
    this.commands = new HashMap<>();
    this.currentRequests = new HashMap<>();
    app.addAsynchronous(this);
    
    // Create the health snapshots.
    this.currentHealth = new HealthSnapshot(this.healthIntervalLength);
    this.healthSnapshots[0] = this.currentHealth;
    
    // Get a reference to the JMX Thread Bean
    this.threadBean = ManagementFactory.getThreadMXBean();
    this.cpuTimeSupported = this.threadBean.isCurrentThreadCpuTimeSupported();
    
    // Get a reference to the root Thread Group.
    this.rootThreadGroup = Thread.currentThread().getThreadGroup();
    while (this.rootThreadGroup.getParent() != null)
    {
      try
      {
        this.rootThreadGroup = this.rootThreadGroup.getParent();
      }
      catch (SecurityException sec)
      {
        break;
      }
    }
    
    // Create the threads.
    this.thread = new GeminiMonitorThread();
    this.percentageThread = new PercentageMonitorThread(this);
  }
  
  /**
   * Gets a reference to the application.
   */
  public GeminiApplication getApplication()
  {
    return app;
  }
  
  /**
   * Configure the Monitor.
   */
  @Override
  public void configure(EnhancedProperties props)
  {
    final EnhancedProperties.Focus focus = props.focus(PROPS_PREFIX);
    
    synchronized (this)
    {
      // Enabling/disabling the monitor using these configuration options is
      // now deprecated.  Use FeatureManager settings instead.
      if (focus.get("Enabled") != null)
      {
        log.log(focus.name("Enabled") + " is deprecated.  Use Feature.monitor instead.");

        fm.set("monitor", focus.getBoolean("Enabled", true));
        if (!isEnabled())
        {
          log.log("Gemini Monitor disabled.");
        }
      }
      if (focus.get("HealthEnabled") != null)
      {
        log.log(focus.name("HealthEnabled") + " is deprecated.  Use Feature.monitor.health instead.");

        fm.set("health", focus.getBoolean("HealthEnabled", true));
        if (!isHealthEnabled())
        {
          log.log("Health Monitoring sub-component disabled.");
        }
      }

      // Configure health snapshot intervals.
      healthSnapshotCount = focus.getInt("HealthSnapshotCount",
          DEFAULT_SNAPSHOT_HISTORY, 2, MAXIMUM_SNAPSHOT_COUNT);
      healthIntervalLength = focus.getLong("HealthSnapshotInterval",
          DEFAULT_SNAPSHOT_INTERVAL, MINIMUM_SNAPSHOT_INTERVAL, UtilityConstants.YEAR);
      if (isEnabled())
      {
        log.log("Health snapshots: " + healthSnapshotCount + " at " 
            + healthIntervalLength + "ms intervals.");
      }
      
      // Create a new snapshots array if the size has been changed.
      if (healthSnapshots.length != healthSnapshotCount)
      {
        HealthSnapshot[] newArray = new HealthSnapshot[healthSnapshotCount];
        int toCopy = Math.min(newArray.length, healthSnapshots.length);
        System.arraycopy(healthSnapshots, 0, newArray, 0, toCopy);
        healthSnapshots = newArray;
      }
      
      // If the current health interval's end is further in the future than 
      // the new interval length; adjust it.
      long temp = System.currentTimeMillis() + healthIntervalLength;
      if (temp < healthIntervalEnd)
      {
        healthIntervalEnd = temp;
        currentHealth.setEndTime(temp);
      }
      
      // -------------------------------------------------------------------
      // Configure session snapshot intervals.
      sessionSnapshotCount = focus.getInt("SessionSnapshotCount", 
          DEFAULT_SNAPSHOT_HISTORY, 2, MAXIMUM_SNAPSHOT_COUNT);
      sessionIntervalLength = focus.getLong("SessionSnapshotInterval",
          DEFAULT_SNAPSHOT_INTERVAL, MINIMUM_SNAPSHOT_INTERVAL, UtilityConstants.YEAR);
      if (isEnabled())
      {
        log.log("Session snapshots: " + sessionSnapshotCount + " at " 
            + sessionIntervalLength + "ms intervals.");
      }
      
      // Create a new snapshots array if the size has been changed.
      if (sessionSnapshots.length != sessionSnapshotCount)
      {
        SessionSnapshot[] newArray = new SessionSnapshot[sessionSnapshotCount];
        int toCopy = Math.min(newArray.length, sessionSnapshots.length);
        System.arraycopy(sessionSnapshots, 0, newArray, 0, toCopy);
        sessionSnapshots = newArray;
      }
      
      // If the current session interval's end is further in the future than 
      // the new interval length; adjust it.
      temp = System.currentTimeMillis() + sessionIntervalLength;
      if (temp < sessionIntervalEnd)
      {
        sessionIntervalEnd = temp;
      }
    }
    
    // Pre-calculate the current performance interval's start and end time.
    evaluateIntervals();
  }

  /**
   * Enroll this Feature with the feature manager.
   */
  public void enrollFeature(FeatureManager manager)
  {
    manager.add("monitor", "Gemini Monitor")                      // Main feature.
           .add("health", "Gemini Monitor's health monitoring")   // Health sub-component.
           .add("session", "Gemini Monitor's web session monitoring")
           .add("cpu", "Gemini Monitor's CPU utilization monitoring"); // CPU percentage sub-component.
  }

  /**
   * Is the Gemini Monitor enabled as a whole?
   */
  public boolean isEnabled()
  {
    return fm.on("monitor");
  }
  
  /**
   * Is the Health-monitoring sub-component enabled?  Returns false if either
   * the Health component is disabled -or- the Gemini Monitor as a whole is
   * disabled.
   */
  public boolean isHealthEnabled()
  {
    return fm.on("health");
  }
  
  /**
   * Is the CPU usage percentage monitoring sub-component enabled?  This
   * returns false if either the sub-component or the overall Gemini Monitor
   * are disabled.  
   */
  public boolean isCpuPercentageEnabled()
  {
    return fm.on("cpu");
  }
  
  /**
   * Is the Session-monitoring sub-component enabled?  This returns false if
   * either the sub-component or the overall Gemini Monitor are disabled.
   */
  public boolean isSessionEnabled()
  {
    return fm.on("session");
  }
  
  /**
   * Processes a request sample at the completion of a request.
   */
  public void process(MonitorSample sample, BasicContext context)
  {
    if (isEnabled())
    {
      synchronized (this)
      {
        // Get the MonitoredCommand.
        MonitoredCommand command = commands.get(sample.getDispatchCommand());
    
        try
        {
          //log.debug("" + sample);
          evaluateIntervals();
          
          if (command == null)
          {
            command = new MonitoredCommand(this, sample.getDispatchCommand());
            commands.put(sample.getDispatchCommand(), command);
          }
          
          // Add this sample to the MonitoredCommand.
          command.process(sample, context);
        }
        finally
        {
          if (command != null)
          {
            // Reduce the concurrent load on the MonitoredCommand.
            command.adjustLoad(-(sample.getRequestLoad()));
          }
        }
      }
    }
  }
  
  /**
   * Adds a Sample to the Current Requests set. 
   */
  public void addRequest(MonitorSample sample)
  {
    if (isEnabled())
    {
      synchronized (this)
      {
        currentRequests.put(sample.getThreadID(), sample);
      }
    }
  }
  
  /**
   * Removes a Sample from the Current Requests set.
   */
  public void removeRequest(MonitorSample sample)
  {
    if (isEnabled())
    {
      synchronized (this)
      {
        currentRequests.remove(sample.getThreadID());
      }
    }
  }
  
  /**
   * Fetches a MonitorSample associated with a given Thread ID.  Null if
   * no matching MonitorSample exists for the provided Thread ID.
   */
  public MonitorSample getRequest(long threadID)
  {
    return currentRequests.get(threadID);
  }
  
  /**
   * Gets a collection of Current Requests.
   */
  public synchronized List<MonitorSample> getCurrentRequests()
  {
    return new ArrayList<>(currentRequests.values());
  }
  
  /**
   * Gets a count of the current requests.
   */
  public int getCurrentRequestCount()
  {
    return currentRequests.size();
  }
  
  /**
   * Adds a GeminiMonitorListener, a component interested in listening to
   * events raised by the GeminiMonitor.  Generally, these are created and
   * added at startup time.
   */
  public synchronized void addGeminiMonitorListener(GeminiMonitorListener gmListener)
  {
    final GeminiMonitorListener[] newListeners = 
        new GeminiMonitorListener[monitorListeners.length + 1];
    System.arraycopy(monitorListeners, 0, newListeners, 0, monitorListeners.length);
    newListeners[monitorListeners.length] = gmListener;
    monitorListeners = newListeners;
  }
  
  /**
   * Adds a HealthEvaluator for evaluating health snapshots.  This function
   * should be called only at application start-up; it is not intended for
   * dynamic addition of evaluators at runtime. 
   */
  public synchronized void addHealthEvaluator(HealthEvaluator evaluator)
  {
    final HealthEvaluator[] newList = 
        new HealthEvaluator[healthEvaluators.length + 1];
    newList[0] = evaluator;
    if (healthEvaluators.length > 0)
    {
      System.arraycopy(healthEvaluators, 0, newList, 1, healthEvaluators.length);
    }    
    healthEvaluators = newList;
  }
  
  /**
   * Adds a PercentageEvaluator for evaluating CPU utilization intervals.
   * This function should be called only at application start-up; it is not 
   * intended for dynamic addition of evaluators at runtime. 
   */
  public synchronized void addPercentageEvaluator(PercentageEvaluator evaluator)
  {
    final PercentageEvaluator[] newList = 
        new PercentageEvaluator[percEvaluators.length + 1];
    newList[0] = evaluator;
    if (percEvaluators.length > 0)
    {
      System.arraycopy(percEvaluators, 0, newList, 1, percEvaluators.length);
    }
    percEvaluators = newList;
  }
  
  /**
   * Ask the HealthEvaluators to evaluate a HealthSnapshot.
   */
  public String evaluateHealthSnapshot(HealthSnapshot snapshot)
  {
    String evaluation = null;
    if (isHealthEnabled())
    {
      for (HealthEvaluator healthEvaluator : healthEvaluators)
      {
        try
        {
          evaluation = healthEvaluator.isExceptional(snapshot, this);
          if (evaluation != null)
          {
            break;
          }
        }
        catch (Exception exc)
        {
          log.log("Exception while evaluating health: " + exc);
        }
      }
    }
    return evaluation;
  }
  
  /**
   * Ask the PercentageEvaluators to evaluate a PercentageInterval.
   */
  public String evaluatePercentage(PercentageInterval interval)
  {
    String evaluation = null;
    final PercentageInterval fInterval = interval;
    if (isCpuPercentageEnabled())
    {
      for (PercentageEvaluator percEvaluator : percEvaluators)
      {
        try
        {
          evaluation = percEvaluator.isExceptional(interval, this);
          if (evaluation != null)
          {
            // Notify listeners on a separate thread.
            Runnable r = new Runnable() {
              @Override
              public void run()
              {
                notifyListenersExceptionalCpuUtilization(fInterval);
              }
            };
            ThreadHelper.submit(r);
            break;
          }
        }
        catch (Exception exc)
        {
          log.log("Exception while evaluating CPU percentages: " + exc);
        }
      }
    }
    return evaluation;
  }
  
  /**
   * Gets a reference to the listener.
   */
  public MonitorListener getListener()
  {
    return listener;
  }
  
  /**
   * Gets the health interval length in milliseconds.
   */
  public synchronized long getHealthIntervalLength()
  {
    return healthIntervalLength;
  }
  
  /**
   * Gets the session interval length in milliseconds.
   */
  public synchronized long getSessionIntervalLength()
  {
    return sessionIntervalLength;
  }
  
  /**
   * Gets the performance interval length in milliseconds.
   */
  public long getPerfIntervalLength()
  {
    return perfIntervalLength;
  }

  /**
   * Sets the length of the performance interval in milliseconds.  This should
   * only be called once per application execution at initialization time.
   * The default performance interval is 1 hour.  Interval lengths shorter 
   * than 5 seconds are not permitted.
   */
  public void setPerfIntervalLength(long intervalLength)
  {
    perfIntervalLength = NumberHelper.boundLong(intervalLength, 
        MINIMUM_PERFORMANCE_INTERVAL, 
        MAXIMUM_PERFORMANCE_INTERVAL);
  }

  /**
   * Evaluate the interval, pushing the next snapshots/samples into the
   * data structures as needed.
   */
  protected void evaluateIntervals()
  {
    // Don't do anything if we're disabled.
    if (isEnabled())
    {
      HealthSnapshot justCompletedHealthReference = null;
      
      // Check to see if it's time to push the intervals for Performance
      // and Health forward.  Do this in a synchronized block so that we
      // don't end up pushing when another request's sample is being 
      // captured.
      synchronized (this)
      {
        final long current = System.currentTimeMillis();
        
        // Are we at the end of the current performance interval?
        if (current > perfIntervalEnd)
        {
          // Compute the new interval.
          long absoluteHour = current / getPerfIntervalLength();
          perfIntervalStart = absoluteHour * getPerfIntervalLength();
          perfIntervalEnd = perfIntervalStart + getPerfIntervalLength() - 1L;
          
          // Push all MonitoredCommands.
          for (MonitoredCommand monitoredCommand : commands.values())
          {
            monitoredCommand.push();
          }
        }
        
        // Are we at the end of the current health interval?
        if (  (current > healthIntervalEnd)
           && (isHealthEnabled())
           )
        {
          justCompletedHealthReference = pushHealthArray(current + getHealthIntervalLength());
        }
        
        // Are we at the end of the current session interval?
        if (  (current > sessionIntervalEnd)
           && (isSessionEnabled())
           )
        {
          pushSessionArray(current + getSessionIntervalLength());
        }
      }
      
      // Notify listeners if the health snapshot that just completed was 
      // exceptional, notify the listeners.  Do so outside of the 
      // synchronization block in case the listeners need to do something 
      // expensive.
      if (  (justCompletedHealthReference != null)
         && (justCompletedHealthReference.isExceptional())
         )
      {
        notifyListenersExceptionalHealth(justCompletedHealthReference);
      }
    }
  }
  
  /**
   * Push the health array, returning the just-completed snapshot for
   * convenience.
   */
  protected HealthSnapshot pushHealthArray(long newEnd)
  {
    // Keep a reference to the health snapshot that is just now 
    // completing.
    final HealthSnapshot justCompletedHealthReference = currentHealth;

    // Complete the snapshot.
    justCompletedHealthReference.complete(this);
    
    // Create a new HealthSnapshot object.
    currentHealth = new HealthSnapshot(getHealthIntervalLength());
    
    // Push it into the snapshot array.
    System.arraycopy(healthSnapshots, 0, healthSnapshots, 1, healthSnapshots.length - 1);
    healthSnapshots[0] = currentHealth;
    
    // Set the new interval end.
    healthIntervalEnd = newEnd;
    
    return justCompletedHealthReference;
  }
  
  /**
   * Push the session array.
   */
  protected void pushSessionArray(long newEnd)
  {
    // Create a snapshot for the interval that just completed.
    final SessionSnapshot justCompleted = new SessionSnapshot(getSessionState());
    
    // Push the array.
    System.arraycopy(sessionSnapshots, 0, sessionSnapshots, 1, sessionSnapshots.length - 1);
    sessionSnapshots[0] = justCompleted;
    
    sessionIntervalEnd = newEnd;
  }
  
  /**
   * Gets a set of current Sessions.
   */
  public Set<Session> getSessions()
  {
    if (isSessionEnabled())
    {
      return getSessionState().getSessions();
    }
    else
    {
      // Session monitoring is not enabled.
      return null;
    }
  }
  
  /**
   * Gets the session snapshots.
   */
  public SessionSnapshot[] getSessionSnapshots()
  {
    final SessionSnapshot[] toReturn;
    synchronized (this)
    {
      toReturn = new SessionSnapshot[sessionSnapshots.length];
      System.arraycopy(sessionSnapshots, 0, toReturn, 0, sessionSnapshots.length);
    }
    return toReturn;
  }
  
  /**
   * Gets the current health snapshot.
   */
  public synchronized HealthSnapshot getCurrentHealth()
  {
    return currentHealth;
  }
  
  /**
   * Gets a copy of the health snapshots.
   */
  public HealthSnapshot[] getHealthSnapshots()
  {
    final HealthSnapshot[] toReturn;
    synchronized (this)
    {
      toReturn = new HealthSnapshot[healthSnapshots.length];
      System.arraycopy(healthSnapshots, 0, toReturn, 0, healthSnapshots.length);
    }
    return toReturn;
  }
  
  /**
   * Gets a list of monitored commands.
   */
  public List<MonitoredCommand> getMonitoredCommands()
  {
    final ArrayList<MonitoredCommand> toReturn = new ArrayList<>(commands.values());
    Collections.sort(toReturn, MonitoredCommand.BY_COMMAND);
    return toReturn;
  }
  
  /**
   * Gets a reference to a specific MonitoredCommand.
   */
  public MonitoredCommand getMonitoredCommand(String command)
  {
    return commands.get(command);
  }
  
  /**
   * Gets the load on a monitored command.  If the command has not yet been
   * observed by the monitor, zero will be returned.
   */
  public int getCurrentLoad(String command)
  {
    final MonitoredCommand mc = getMonitoredCommand(command);
    if (mc != null)
    {
      return mc.getCurrentLoad();
    }
    else
    {
      return 0;
    }
  }
  
  /**
   * Gets the current performance interval start.
   */
  protected synchronized long getPerfIntervalStart()
  {
    return perfIntervalStart;
  }
  
  /**
   * Gets the current performance interval end.
   */
  protected synchronized long getPerfIntervalEnd()
  {
    return perfIntervalEnd;
  }
  
  /** Health: Increase OVERALL dispatch load. */
  public void dispatchStarting(MonitorSample sample, String command)
  {
    if (isEnabled())
    {
      synchronized (this)
      {
        if (isHealthEnabled())
        {
          concurrentDispatches.incrementAndGet();
          currentHealth.incrementDispatchCount();
        }
        
        // Increase the load for the specific command.  If we have not yet
        // fully processed a request for this command, we won't track the load
        // just yet.
        final MonitoredCommand monitoredCommand = commands.get(command);
        if (monitoredCommand != null)
        {
          // If we have already captured this command, let's increase the
          // current request load normally.
          monitoredCommand.adjustLoad(1);
        }
        else
        { 
          // If this is the "first" time we've seen this command (or there
          // are several requests processing but none have yet completed so
          // that we've captured the command), let's set the request's load
          // to zero so that when the request completes, we don't end up with
          // a negative current load.
          sample.setRequestLoad(0);
        }
      }
    }
  }
  
  /** Health: Decrease OVERALL dispatch load. */
  public void dispatchComplete()
  {
    if (isHealthEnabled())
    {
      concurrentDispatches.decrementAndGet();
    }
  }
  
  /** Health: Increase OVERALL page load. */
  public void jspIncluded()
  {
    if (isHealthEnabled())
    {
      concurrentPages.incrementAndGet();
      currentHealth.incrementPageRenderCount();
    }
  }
  
  /** Health: Decrease OVERALL page load. */
  public void jspComplete()
  {
    if (isHealthEnabled())
    {
      concurrentPages.decrementAndGet();
    }
  }
  
  /** Health: Increase OVERALL query load. */
  public void queryStarting()
  {
    if (isHealthEnabled())
    {
      concurrentQueries.incrementAndGet();
      currentHealth.incrementQueryCount();
    }
  }
  
  /** Health: Increase OVERALL query load. */
  public void queryCompleting()
  {
    if (isHealthEnabled())
    {
      concurrentQueries.decrementAndGet();
    }
  }
  
  /**
   * Gets the current OVERALL dispatch load.
   */
  public int getDispatchLoad()
  {
    return concurrentDispatches.get();
  }
  
  /**
   * Gets the current OVERALL page render load.
   */
  public int getPageRenderLoad()
  {
    return concurrentPages.get();
  }
  
  /**
   * Gets the current OVERALL page render load.
   */
  public int getQueryLoad()
  {
    return concurrentQueries.get();
  }
  
  /**
   * Gets the application's count of total number of threads since start time.
   */
  public long getRequestCount()
  {
    return app.getRequestNumber();
  }
  
  /**
   * Gets an array of Threads suitable for counting but not getting stack
   * traces (see getThreadBasics for stack traces).
   */
  public Thread[] getThreadArray()
  {
    int threadCount = rootThreadGroup.activeCount();
    Thread[] toReturn = new Thread[threadCount];
    rootThreadGroup.enumerate(toReturn);
    return toReturn;
  }
  
  /**
   * Gets Thread basics using the Thread API.
   */
  public Thread[] getThreadBasics()
  {
    Map<Thread,StackTraceElement[]> map = Thread.getAllStackTraces();
    Thread[] toReturn = new Thread[map.size()];
    return map.keySet().toArray(toReturn);
  }
  
  /**
   * Gets a snapshot of all running threads.
   */
  public ThreadInfo[] getThreadDetails()
  {
    return threadBean.dumpAllThreads(true, true);
  }
  
  /**
   * Gets the current thread's CPU time, if such a measurement is supported.
   * If not supported, returns 0.
   */
  public long getCurrentThreadCpuTime()
  {
    if (cpuTimeSupported)
    {
      return threadBean.getCurrentThreadCpuTime();
    }
    else
    {
      return 0L;
    }
  }
  
  /**
   * Gets a list of CPU PercentageSamples from the CPU Percentage Usage
   * Monitoring sub-component.  Returns null if that sub-component is not
   * enabled.
   */
  public List<PercentageSample> getCpuUsagePercentages()
  {
    if (  (isCpuPercentageEnabled())
       && (percentageThread != null)
       )
    {
      return percentageThread.getCurrent();
    }
    // Not enabled, return null.
    else
    {
      return null;
    }
  }

  protected abstract void addSessionListener();

  public abstract SessionState getSessionState();
  
  @Override
  public void begin()
  {
    addSessionListener();
    
    // We'll start the thread even if the monitor is disabled because a
    // reconfiguration could enable monitoring and we might as well have
    // the thread ready.  When disabled, the thread won't do anything 
    // because every call to evaluateIntervals will immediately return. 
    thread.begin();
    
    // Start the percentage monitoring thread.
    percentageThread.begin();
  }

  // @see com.techempower.Asynchronous#end()
  @Override
  public void end()
  {
    // End the main monitoring thread.
    thread.end();

    // End the percentage monitoring thread.
    percentageThread.end();
  }
  
  //
  // Notifications out to our listeners.
  //
  
  /**
   * Notify the Gemini Monitor listeners that the most recent health snapshot
   * was exceptional.
   */
  protected void notifyListenersExceptionalHealth(HealthSnapshot snapshot)
  {
    if (  (isHealthEnabled())
       && (monitorListeners.length > 0)
       )
    {
      for (GeminiMonitorListener l : monitorListeners)
      {
        l.healthSnapshotExceptional(snapshot);
      }
    }
  }
  
  /**
   * Notify the Gemini Monitor listeners that the most recent CPU utilization
   * interval was exceptional.
   */
  protected void notifyListenersExceptionalCpuUtilization(PercentageInterval interval)
  {
    if (  (isCpuPercentageEnabled())
       && (monitorListeners.length > 0)
       )
    {
      for (GeminiMonitorListener l : monitorListeners)
      {
        l.cpuUtilizationIntervalExceptional(interval);
      }
    }
  }
  
  //
  // GeminiMonitorThread
  //

  /**
   * A thread to push intervals periodically.
   */
  class     GeminiMonitorThread
    extends EndableThread
  {
    public GeminiMonitorThread()
    {
      super("Gemini Monitor thread (" 
          + GeminiMonitor.this.app.getVersion().getProductName() + ")", 
          (int)MINIMUM_SNAPSHOT_INTERVAL);
      setPriority(MIN_PRIORITY);
    }

    @Override
    public void run()
    {
      GeminiMonitor.this.log.log("Gemini Monitor thread started.");
      while (checkPause())
      {
        evaluateIntervals();
        simpleSleep();
      }
      GeminiMonitor.this.log.log("Gemini Monitor thread stopped.");
    }
    
    @Override
    public String toString()
    {
      return getName() + (isEnabled() ? "" : " (monitoring disabled)");
    }
  }

}
