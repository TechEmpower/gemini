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

package com.techempower.gemini.handler;

import java.io.*;
import java.lang.management.*;
import java.util.*;

import com.techempower.asynchronous.*;
import com.techempower.gemini.*;
import com.techempower.gemini.configuration.*;
import com.techempower.gemini.path.*;
import com.techempower.gemini.path.annotation.*;
import com.techempower.helper.*;
import com.techempower.text.*;
import com.techempower.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Thread Dump Handler facilitates debugging of production servers through
 * the ability to get a "dump" of the current stack trace for all running
 * Threads. This Handler uses the new "getAllStackTraces" method provided in
 * JDK 1.5's Thread class. This Handler will not function on any platform
 * prior to Java 1.5.
 * <p>
 * ThreadDumpHandler can run in its "classic" mode where
 * Thread.getAllThreadDumps is used or in an improved mode that uses
 * java.lang.management ("JMX").
 * <p>
 * To use ThreadDumpHandler, make sure that the following parameters are set
 * in your application's .conf file:
 * <ul>
 * <li>ThreadDump.Passphrase - A required configuration parameter that
 * specifies a passphrase that must be present in the URL parameters to view
 * the thread dump. This class will throw ConfigurationError during
 * configuration if this parameter is missing or empty.
 * <li>ThreadDump.AuthorizedIP - An IP address that is authorized to request a
 * thread dump. The special option of "any" means requests from any IP address
 * will be accepted. If additional security is desired, overload the
 * authorized method in this Handler.
 * <li>ThreadDump.DumpOnStopLocation - An optional file system location to
 * write text thread dumps when the application is stopped (such as when it is
 * reloaded or when the application server shuts down). If empty, no such
 * files will be written.
 * <li>ThreadDump.UseJmx - Enabled by default. The JMX mode allows additional
 * useful information such as lock/monitor details. Disable this mode to use
 * the "classic" approach of calling Thread.getAllStackTraces.
 * </ul>
 * <p>
 * Add the ThreadDumpHandler to your application's Dispatcher.
 * <p>
 * To invoke the ThreadDumpHandler, issue a command of this form to your
 * application in the form of an HTTP request: /threaddump
 */
public class ThreadDumpHandler<C extends Context>
  extends    MethodSegmentHandler<C>
  implements Configurable,
             UriAware,
             Asynchronous
{
  
  //
  // Constants.
  //

  public static final String DEFAULT_PROPS_PREFIX = "ThreadDump.";
  public static final int    MEGABYTE             = 1024 * 1024;
  public static final String DEFAULT_ROLE         = "threaddump";
  public static final String ANY_IP               = "any";
  
  private static ThreadDumpHandler<?> INSTANCE;

  //
  // Member variables.
  //

  private String            propsPrefix  = DEFAULT_PROPS_PREFIX;
  private String            passphrase   = null;
  private String            authorizedIP = ANY_IP;
  private final SynchronizedSimpleDateFormat dateFormatter = new SynchronizedSimpleDateFormat();
  private String            dumpOnStopLocation = "";
  private boolean           useJmx       = true;
  private final Logger      log          = LoggerFactory.getLogger(getClass());

  //
  // Member methods.
  //

  /**
   * Constructor.  Sets up references.  Subclasses' constructors should
   * call super(application).
   * 
   * @param application The application reference
   * @param propsPrefix Optional properties file attribute name prefix; if
   *        null, the default is "ThreadDump." (including the period.)
   */
  public ThreadDumpHandler(GeminiApplication application, String propsPrefix)
  {
    super(application);
    
    if (propsPrefix != null)
    {
      this.propsPrefix = propsPrefix;
    }
    
    // Make sure that we are going to be configured by the application's
    // Configurator.
    application.getConfigurator().addConfigurable(this);
    
    INSTANCE = this;
  }

  /**
   * Constructor.
   * 
   * @param application The application reference
   */
  public ThreadDumpHandler(GeminiApplication application)
  {
    this (application, null);
  }
  
  /**
   * Gets the instance.
   */
  public static ThreadDumpHandler<?> getInstance()
  {
    return INSTANCE;
  }
  
  /**
   * Is the viewer requesting JMX data?
   */
  protected boolean isJmx(Context context)
  {
    boolean jmx = this.useJmx;
    if (  (jmx)
       && (!query().getBooleanLenient("jmx", jmx))
       )
    {
      jmx = false;
    }
    return jmx;
  }
  
  /**
   * Overload this method to provide an authorization check based off of
   * the Context.  The default implementation just checks to see if the
   * client's IP address matches the IP address specified in the configuration
   * file (ThreadDump.AuthorizedIP).  Also, the passphrase must have been
   * configured to a non-empty value, and that passphrase must be present as
   * a request parameter.
   *   <p>
   * If the AuthorizedIP is set to "any", a request from any IP address will
   * be accepted.
   */
  protected boolean isAuthorized(Context context)
  {
    return this.passphrase != null
        && query().get(this.passphrase) != null
        && (this.authorizedIP.equalsIgnoreCase(ANY_IP)
            || context.getClientId().equals(this.authorizedIP));
  }
  
  /**
   * A simple embedded class used to capture the details of a Thread.
   */
  public static class ThreadDescriptor
    implements Comparable<ThreadDescriptor>
  {
    private final Thread thread;
    private final ThreadInfo info;
    private final StackTraceElement[] stack;
    
    public ThreadDescriptor(Thread thread, ThreadInfo info, StackTraceElement[] stack)
    {
      this.thread = thread;
      this.info = info;
      this.stack = stack;
    }
    
    /**
     * Gets the thread.
     */
    public Thread getThread()
    {
      return this.thread;
    }      
    
    /**
     * Gets the ThreadInfo.
     */
    public ThreadInfo getThreadInfo()
    {
      return this.info;
    }
    
    /**
     * Gets the Stack Trace Elements.
     */
    public StackTraceElement[] getStack()
    {
      return this.stack;
    }
    
    @Override
    public int compareTo(ThreadDescriptor other)
    {
      return this.thread.getName().compareTo(other.thread.getName());
    }
  }
  
  /**
   * Gets a sorted Thread map.
   */
  protected static List<ThreadDescriptor> getThreads(boolean isUsingJmx)
  {
    // Call our regular getAllStackTraces.  We need to do this even if
    // we're using JMX since the JMX stuff doesn't get us information like
    // priority for some reason.
    final Map<Thread, StackTraceElement[]> currentThreads = Thread.getAllStackTraces();

    // If we're using JMX, let's grab JMX information first.
    ThreadInfo[] allInfos = null;
    if (isUsingJmx)
    {
      allInfos = ManagementFactory.getThreadMXBean().dumpAllThreads(true, true);
    }
    
    final List<ThreadDescriptor> toReturn = new ArrayList<>();

    ThreadDescriptor desc;
    Thread thread;
    ThreadInfo info = null;

    for (Map.Entry<Thread, StackTraceElement[]> entry : currentThreads.entrySet())
    {
      thread = entry.getKey();
      
      // If we're using JMX, find the matching ThreadInfo.
      if (allInfos != null)
      {
        info = null;
        for (ThreadInfo allInfo : allInfos)
        {
          if (allInfo.getThreadId() == thread.getId())
          {
            info = allInfo;
            break;
          }
        }
      }
      
      desc = new ThreadDescriptor(thread, info, entry.getValue());
      toReturn.add(desc);
    }

    Collections.sort(toReturn);
    
    return toReturn;
  }
  
  /**
   * Executes a thread dump.
   */
  @PathDefault
  public boolean threadDump(Context context)
  {
    final boolean jmx = isJmx(context);
    final long startTime = System.currentTimeMillis();

    log.info("Thread dump requested.");
    
    context.setContentType("text/html");

    writeHeader(context);

    final List<ThreadDescriptor> threadDescs = getThreads(jmx);
    int half = threadDescs.size() / 2 + threadDescs.size() % 2;
    int position = 0;
    
    context.print("<table border=\"0\" cellspacing=\"0\" cellpadding=\"0\" width=\"100%\">");
    context.print("<tr><td width=\"50%\">");
    
    Iterator<ThreadDescriptor> threads = threadDescs.iterator();
    ThreadDescriptor desc;
    
    while (threads.hasNext())
    {
      if (position == half)
      {
        context.print("</td><td>&nbsp;&nbsp;</td><td width=\"50%\">");
      }
      position++;
      
      desc = threads.next();
      
      int priority = desc.thread.getPriority();
      
      context.print("<div class=\"thread\">");
      context.print("<div class=\"threadname\">");
      context.print(desc.thread.getName() + " [ID: " + desc.thread.getId() + "; Priority: " + priority + "]");
      context.print("</div>");
      context.print("<div class=\"threaddesc\">");
      context.print(desc.thread.toString());
      context.print("</div>");
      context.print("<div class=\"stackblock\">");
      
      StackTraceElement[] stack = desc.stack;
      if ( (stack != null)
        && (stack.length > 0)
        )
      {
        String stackfirstStyle;
        String stackStyle;
        
        if (priority < Thread.NORM_PRIORITY)
        {
          stackfirstStyle = "stacklowfirst";
          stackStyle = "stacklow";
        }
        else if (priority > Thread.NORM_PRIORITY)
        {
          stackfirstStyle = "stackhighfirst";
          stackStyle = "stackhigh";
        }
        else
        {
          stackfirstStyle = "stackfirst";
          stackStyle = "stack";
        }
        
        for (int i = 0; i < stack.length; i++)
        {
          if (i == 0)
          {
            context.print("<div class=\"" + stackfirstStyle + "\">" + stack[i].toString() + "</div>");
          }
          else
          {
            context.print("<div class=\"" + stackStyle + "\">" + stack[i].toString() + "</div>");
          }
        }
      }
      else
      {
        context.print("<div class=\"stackunavailable\">No stack trace available.</div>");
      }
      
      // Do we have additional information from JMX?
      if (desc.info != null)
      {
        LockInfo[] locks = desc.info.getLockedMonitors();
        for (int i = 0; i < locks.length; i++)
        {
          context.print("<div class=\"ownlock" + (i==0?"first":"") + "\">Owns lock on instance " + Integer.toHexString(locks[i].getIdentityHashCode()) + " of " + locks[i].getClassName() + "</div>");
        }
        
        long owner = desc.info.getLockOwnerId();
        if (desc.info.getLockInfo() != null)
        {
          context.print("<div class=\"block\">Blocked on lock " + Integer.toHexString(desc.info.getLockInfo().getIdentityHashCode()) + " of " + desc.info.getLockInfo().getClassName() + (owner >= 0 ? " owned by thread " + owner + " (" + desc.info.getLockOwnerName() + ")" : "") + "</div>");
        }
      }
      
      context.print("</div>");
      context.print("</div>");
    }
    
    context.print("</td></tr></table>");
    
    final long msTaken = (System.currentTimeMillis() - startTime);
    log.info("Thread dump complete, took {} ms.", msTaken);
    writeFooter(context, msTaken);
  
    return true;
  }

  /**
   * Executes a thread dump.
   */
  @PathSegment("plain")
  public boolean threadDumpPlainText(Context context)
  {
    final boolean jmx = isJmx(context);
    final long startTime = System.currentTimeMillis();

    log.info("Thread dump requested.");
    
    context.setContentType("text/plain");
    context.print("Gemini Thread Dump");
    context.print("");
    long uptime = app().getUptime();
    context.print(this.dateFormatter.format(new Date()) + " - "
      + DateHelper.getHumanDuration(uptime, 2) + " uptime ("
      + uptime + " millis) - "
      + (Runtime.getRuntime().freeMemory() / MEGABYTE) + " MiB free; "
      + (Runtime.getRuntime().totalMemory() / MEGABYTE) + " MiB allocated - "
      + app().getVersion().getVerboseDescription());
    context.print("");
    
    List<ThreadDescriptor> threadDescs = getThreads(jmx);
    Iterator<ThreadDescriptor> threads = threadDescs.iterator();
    
    ThreadDescriptor desc;
    while (threads.hasNext())
    {
      desc = threads.next();
      
      int priority = desc.thread.getPriority();
      
      context.print(desc.thread.getName() + " [Priority: " + priority + "]");
      context.print(desc.thread.toString());
      
      StackTraceElement[] stack = desc.stack;

      if ( (stack != null)
        && (stack.length > 0)
        )
      {
        for (StackTraceElement aStack : stack)
        {
          context.print("  " + aStack.toString());
        }
      }
      else
      {
        context.print("No stack trace available.");
      }

      // Do we have additional information from JMX?
      if (desc.info != null)
      {
        LockInfo[] locks = desc.info.getLockedMonitors();
        for (LockInfo lock : locks)
        {
          context.print("  + Owns lock on instance " + Integer.toHexString(
              lock.getIdentityHashCode()) + " of " + lock.getClassName());
        }
        
        long owner = desc.info.getLockOwnerId();
        if (desc.info.getLockInfo() != null)
        {
          context.print("  - Blocked on lock " + Integer.toHexString(desc.info.getLockInfo().getIdentityHashCode()) + " of " + desc.info.getLockInfo().getClassName() + (owner >= 0 ? " owned by thread " + owner + " (" + desc.info.getLockOwnerName() + ")" : ""));
        }
      }

      context.print("");
    }
    
    final long msTaken = (System.currentTimeMillis() - startTime);
    log.info("Thread dump complete, took {} ms.", msTaken);
  
    return true;
  }

  /**
   * Writes out an HTML header for the thread dump.
   */
  protected void writeHeader(Context context)
  {
    context.print("<html>");
    context.print("<head>");
    context.print("<title>Gemini Thread Dump</title>");
    context.print("<style>");
    context.print("BODY { background-color: white; color: black; margin: 10px 10px 10px 10px; }");
    context.print("P, TD, DIV { font-family: Tahoma, Arial, Helvetica, Sans-serif; font-size: 10px; color: black; }");
    context.print("TD { vertical-align: top; }");
    context.print("H1 { font-family: Tahoma, Arial, Helvetica, Sans-serif; font-size: 13px; font-weight: Bold; color: #203040; }");
    context.print("H2 { font-family: Tahoma, Arial, Helvetica, Sans-serif; font-size: 12px; font-weight: Bold; color: #304050; }");
    context.print(".thread { border: 1px solid #405060; margin: 10px 0px 10px 0px; }");
    context.print(".threadname { font-weight: Bold; font-size: 12px; background-color: #D0E0F0; padding: 2px 5px 0px 5px; }");
    context.print(".threaddesc { border-bottom: 1px solid #8090A0; font-size: 9px; background-color: #D0E0F0; padding: 0px 5px 2px 5px; }");
    context.print(".stackblock { padding: 5px 5px 5px 5px; }");
    context.print(".stackunavailable { border-top: 1px solid #707070; border-left: 1px solid #707070; border-right: 1px solid #707070; border-bottom: 1px solid #707070; background-color: #E8E8E8; padding: 0px 2px 0px 2px; color: #707070 }");
    context.print(".stackfirst { border-top: 1px solid #8090A0; border-left: 1px solid #8090A0; border-right: 1px solid #8090A0; border-bottom: 1px solid #8090A0; background-color: #E0F0FF; padding: 0px 2px 0px 2px; }");
    context.print(".stack { border-left: 1px solid #8090A0; border-right: 1px solid #8090A0; border-bottom: 1px solid #8090A0; background-color: #E0F0FF; padding: 0px 2px 0px 2px; }");
    context.print(".stacklowfirst { border-top: 1px solid #80A090; border-left: 1px solid #80A090; border-right: 1px solid #80A090; border-bottom: 1px solid #80A090; background-color: #E0FFF0; padding: 0px 2px 0px 2px; }");
    context.print(".stacklow { border-left: 1px solid #80A090; border-right: 1px solid #80A090; border-bottom: 1px solid #80A090; background-color: #E0FFF0; padding: 0px 2px 0px 2px; }");
    context.print(".stackhighfirst { border-top: 1px solid #A09090; border-left: 1px solid #A09090; border-right: 1px solid #A09090; border-bottom: 1px solid #A09090; background-color: #FFE8E8; padding: 0px 2px 0px 2px; }");
    context.print(".stackhigh { border-left: 1px solid #A09080; border-right: 1px solid #A09090; border-bottom: 1px solid #A09090; background-color: #FFE8E8; padding: 0px 2px 0px 2px; }");
    context.print(".ownlockfirst { border-top: 1px solid #D4E400; border-left: 1px solid #D4E400; border-right: 1px solid #D4E400; border-bottom: 1px solid #D4E400; background-color: #F8FFCF; padding: 0px 2px 0px 2px; margin-top: 4px}");
    context.print(".ownlock { border-left: 1px solid #D4E400; border-right: 1px solid #D4E400; border-bottom: 1px solid #D4E400; background-color: #F8FFCF; padding: 0px 2px 0px 2px; }");
    context.print(".block { border-top: 1px solid #C8B000; border-left: 1px solid #C8B000; border-right: 1px solid #C8B000; border-bottom: 1px solid #C8B000; background-color: #FFE0BF; padding: 0px 2px 0px 2px; margin-top: 4px}");
    context.print("</style>");
    context.print("</head>");
    context.print("<body>");
    context.print("<h1>Gemini Thread Dump</h1>");
    
    long uptime = app().getUptime();
    context.print("<h2>" + this.dateFormatter.format(new Date()) + " - " 
      + DateHelper.getHumanDuration(uptime, 2) + " uptime ("
      + uptime + " millis) - "
      + (Runtime.getRuntime().freeMemory() / MEGABYTE) + " MiB free; "
      + (Runtime.getRuntime().totalMemory() / MEGABYTE) + " MiB allocated - "
      + app().getVersion().getVerboseDescription() + "</h2>");
  }
  
  /**
   * Writes out an HTML header for the thread dump.
   */
  protected void writeFooter(Context context, long msTaken)
  {
    context.print("<p>Operation took " + msTaken + " ms.  <a href='" + getBaseUri() + "/plain'>Plaintext version</a></p>");
    context.print("</body>");
    context.print("</html>");
  }
  
  /**
   * Writes a thread dump file to the specified directory.
   */
  protected void writeDumpFile(String location)
  {
    final String filename = location + "thddmp-" + DateHelper.STANDARD_FILENAME_FORMAT.format(new Date()) + ".txt";

    log.info("Thread dump: {}", filename);

    try (PrintWriter pw = new PrintWriter(filename))
    {
      pw.println("Gemini Stop-time Thread Dump");
      pw.println("");
      long uptime = app().getUptime();
      pw.println(this.dateFormatter.format(new Date()) + " - "
        + DateHelper.getHumanDuration(uptime, 2) + " uptime ("
        + uptime + " millis) - "
        + (Runtime.getRuntime().freeMemory() / MEGABYTE) + " MiB free; "
        + (Runtime.getRuntime().totalMemory() / MEGABYTE) + " MiB allocated - "
        + app().getVersion().getVerboseDescription());
      pw.println("");
      
      List<ThreadDescriptor> threadDescs = getThreads(this.useJmx);
      Iterator<ThreadDescriptor> threads = threadDescs.iterator();
      
      ThreadDescriptor desc;
      while (threads.hasNext())
      {
        desc = threads.next();
        
        int priority = desc.thread.getPriority();
        
        pw.println(desc.thread.getName() + " [Priority: " + priority + "]");
        pw.println(desc.thread.toString());
        
        StackTraceElement[] stack = desc.stack;
        
        if ( (stack != null)
          && (stack.length > 0)
          )
        {
          for (StackTraceElement aStack : stack)
          {
            pw.println("  " + aStack.toString());
          }
        }
        else
        {
          pw.println("No stack trace available.");
        }
        
        // Do we have additional information from JMX?
        if (desc.info != null)
        {
          LockInfo[] locks = desc.info.getLockedMonitors();
          for (LockInfo lock : locks)
          {
            pw.println("  + Owns lock on instance " + Integer.toHexString(
                lock.getIdentityHashCode()) + " of " + lock.getClassName());
          }
          
          long owner = desc.info.getLockOwnerId();
          if (desc.info.getLockInfo() != null)
          {
            pw.println("  - Blocked on lock " + Integer.toHexString(desc.info.getLockInfo().getIdentityHashCode()) + " of " + desc.info.getLockInfo().getClassName() + (owner >= 0 ? " owned by thread " + owner + " (" + desc.info.getLockOwnerName() + ")" : ""));
          }
        }

        pw.println("");
        pw.flush();
      }
    }
    catch (Exception exc)
    {
      log.info("Exception while writing thread dump file: ", exc);
    }
  }
  
  /**
   * Configures this component.
   */
  @Override
  public void configure(EnhancedProperties props)
  {
    this.passphrase = props.get(this.propsPrefix + "Passphrase", this.passphrase);
    this.authorizedIP = props.get(this.propsPrefix + "AuthorizedIP", this.authorizedIP);
    this.useJmx = props.getBoolean(this.propsPrefix + "UseJmx", this.useJmx);
    this.dumpOnStopLocation = props.get(this.propsPrefix + "DumpOnStopLocation", this.dumpOnStopLocation);

    if (StringHelper.isEmptyTrimmed(this.passphrase))
    {
      this.passphrase = null;
      throw new ConfigurationError("Required configuration parameter '"
          + this.propsPrefix + "Passphrase' was missing.  This must be "
          + "specified and must have a non-empty value.");
    }
    
    if (StringHelper.isNonEmpty(this.dumpOnStopLocation))
    {
      app().addAsynchronous(this);
    }
    else
    {
      app().removeAsynchronous(this);
    }
  }

  @Override
  public void begin()
  {
    // Does nothing on begin.
  }

  @Override
  public void end()
  {
    // If the dump-on-stop location is specified, write a dump file to the
    // specified location.
    
    if (StringHelper.isNonEmpty(this.dumpOnStopLocation))
    {
      writeDumpFile(this.dumpOnStopLocation);
    }
  }

}  // End ThreadDumpHandler.
