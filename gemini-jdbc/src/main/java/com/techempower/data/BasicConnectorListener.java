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

package com.techempower.data;

import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.Date;

import com.techempower.data.jdbc.*;
import com.techempower.gemini.*;
import com.techempower.gemini.notification.*;
import com.techempower.helper.*;
import com.techempower.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple DatabaseConnectorListener for Gemini applications. Generates
 * administrative notifications in the event of database connectivity
 * problems.
 * <p>
 * Configuration options:
 * <ul>
 * <li>ConnectorListener.NotificationFrequency - The minimum interval between
 * notifications, expressed in seconds.</li>
 * <li>ConnectorListener.MaximumAlertsPerNotification - A maximum number of
 * alerts to include in a notification, as an anti-flooding measure.</li>
 * <li>ConnectorListener.AlertLogFile - A log file to which to write alerts.
 * Leaving this empty will turn off the log file option.</li>
 * <li>ConnectorListener.RetriesOnDisconnect - How many attempts to reconnect
 * should be made on a disconnect? Defaults to 1.</li>
 * </ul>
 */
public class BasicConnectorListener
  implements DatabaseConnectionListener
{

  //
  // Constants.
  //

  public static final String        DEFAULT_PROPERTY_PREFIX      = "ConnectorListener.";
  public static final int           DEFAULT_MAX_ALERTS_PER       = 10;

  //
  // Member variables.
  //

  private final GeminiApplication application;
  private final Logger            log = LoggerFactory.getLogger(getClass());
  private final List<String>      currentAlerts;
  private final String            propertyPrefix;
  
  private String                  alertLogFile                 = "";
  private long                    notificationFrequency        = UtilityConstants.HOUR;
  private int                     maximumAlertsPerNotification = DEFAULT_MAX_ALERTS_PER;
  private long                    lastNotification             = 0L;
  private int                     currentOverflow;
  private int                     retriesOnDisconnect          = 1;

  //
  // Member methods.
  //

  /**
   * Constructor.
   */
  public BasicConnectorListener(GeminiApplication application,
      String propertyPrefix)
  {
    this.application = application;
    this.currentAlerts = new ArrayList<>();

    // Changed to use default prefix if an empty String is provided.
    if (StringHelper.isNonEmpty(propertyPrefix))
    {
      this.propertyPrefix = propertyPrefix;
    }
    else
    {
      this.propertyPrefix = DEFAULT_PROPERTY_PREFIX;
    }
  }

  /**
   * Simplified constructor.
   */
  public BasicConnectorListener(GeminiApplication application)
  {
    this(application, DEFAULT_PROPERTY_PREFIX);
  }

  /**
   * Configure this component.
   */
  @Override
  public void configure(EnhancedProperties props)
  {
    // Support EmailFrequency for legacy configuration files.
    this.notificationFrequency = UtilityConstants.SECOND
        * props.getInt(this.propertyPrefix + "EmailFrequency",
            (int)(this.notificationFrequency / UtilityConstants.SECOND));
    this.notificationFrequency = UtilityConstants.SECOND
        * props.getInt(this.propertyPrefix
            + "NotificationFrequency",
            (int)(this.notificationFrequency / UtilityConstants.SECOND));

    // Support MaximumAlertsPerMail for legacy configuration files.
    this.maximumAlertsPerNotification = props.getInt(
        this.propertyPrefix + "MaximumAlertsPerMail",
        this.maximumAlertsPerNotification);
    this.maximumAlertsPerNotification = props.getInt(
        this.propertyPrefix + "MaximumAlertsPerNotification",
        this.maximumAlertsPerNotification);

    this.alertLogFile = props.get(this.propertyPrefix
        + "AlertLogFile", this.alertLogFile);
    this.retriesOnDisconnect = props.getInt(this.propertyPrefix
        + "RetriesOnDisconnect", this.retriesOnDisconnect);

    if (StringHelper.isNonEmpty(this.alertLogFile))
    {
      this.log.info("Alert file: {}", this.alertLogFile);
    }
    else
    {
      this.log.info("No dbconn listener alert log file specified.");
    }
  }

  /**
   * Processes an alert
   */
  protected synchronized void processAlert(String alert)
  {
    this.log.debug("Processing: {}", alert);

    logAlert(alert);

    // We'll hold on to a copy of the Alert if there's room.
    if (this.currentAlerts.size() < this.maximumAlertsPerNotification)
    {
      this.currentAlerts.add(alert);
    }
    else
    {
      // Can't keep this alert around, let's add to the overflow.
      this.currentOverflow++;
    }

    // Can we send a notification now?
    if (System.currentTimeMillis() > (this.lastNotification + this.notificationFrequency))
    {
      processNotification();
      this.currentAlerts.clear();
    }
  }

  /**
   * Log an alert to the log file.
   */
  protected void logAlert(String alert)
  {
    if (StringHelper.isNonEmpty(this.alertLogFile))
    {
      try (
          FileWriter writer = new FileWriter(this.alertLogFile, true)
          )
      {
        writer.write(alert + UtilityConstants.CRLF);
      }
      catch (IOException ioexc)
      {
        this.log.info("IOException while writing alert: ", ioexc);
      }
    }
  }

  /**
   * Process the administrative notification.
   */
  protected void processNotification()
  {
    StringList alerts = new StringList("\n\n");

    // Append together all of the alerts.
    for (String alert : this.currentAlerts)
    {
      alerts.add(alert);
    }

    String synopsis = this.currentAlerts.size()
        + " database connectivity alert"
        + StringHelper.pluralize(this.currentAlerts.size());

    // Build the body.
    StringBuilder details = new StringBuilder();
    details.append(this.currentAlerts.size())
           .append(" database connectivity alert")
           .append(StringHelper.pluralize(this.currentAlerts.size()));
    details.append(" (")
           .append(this.currentOverflow)
           .append(" overflow; ")
           .append(this.notificationFrequency)
           .append(" minute frequency)\n\n");
    details.append(alerts.toString());

    // Send the notification.
    BasicNotification notification = new BasicNotification("db", synopsis,
        details.toString(), Notification.Severity.HIGH);
    this.application.getNotifier().addNotification(notification);

    // Record the time of this notification.
    this.lastNotification = System.currentTimeMillis();
  }

  /**
   * Overload this method to selectively ignore certain errors before they are
   * processed. Return true to process, false to ignore. The default
   * implementation ignores nothing, so it always returns true.
   * 
   * @param sqlexc A SQLException that was raised by a query.
   * @param conn the JdbcConnector responsible for the exception.
   */
  protected boolean shouldProcess(SQLException sqlexc, JdbcConnector conn)
  {
    return true;
  }

  /**
   * ConnectorListener method indicating an exception in runQuery.
   */
  @Override
  public int exceptionInRunQuery(SQLException exc, JdbcConnector conn)
  {
    if (shouldProcess(exc, conn))
    {
      String alert = "Exception in runQuery" + UtilityConstants.CRLF
          + "Time: " + DateHelper.format(new Date()) + UtilityConstants.CRLF
          + "Query: " + conn.getQuery() + UtilityConstants.CRLF
          + "Exception: " + exc.toString() + UtilityConstants.CRLF
          + "SQLState: " + exc.getSQLState() + UtilityConstants.CRLF
          + "Error code: " + exc.getErrorCode() + UtilityConstants.CRLF;

      processAlert(alert);

      // Determine if we should instruct the connector to automatically
      // retry.
      if ((this.retriesOnDisconnect >= conn.getTryCount())
          && (isConnectionException(exc)))
      {
        return INSTRUCT_RETRY;
      }
    }

    // Do nothing by default.
    return INSTRUCT_DO_NOTHING;
  }

  /**
   * ConnectorListener method indicating an exception in runUpdateQuery.
   */
  @Override
  public int exceptionInRunUpdateQuery(SQLException exc, JdbcConnector conn)
  {
    if (shouldProcess(exc, conn))
    {
      String alert = "Exception in runUpdateQuery" + UtilityConstants.CRLF
          + "Time: " + DateHelper.format(new Date()) + UtilityConstants.CRLF
          + "Query: " + conn.getQuery() + UtilityConstants.CRLF
          + "Exception: " + exc.toString() + UtilityConstants.CRLF
          + "SQLState: " + exc.getSQLState() + UtilityConstants.CRLF
          + "Error code: " + exc.getErrorCode() + UtilityConstants.CRLF;

      processAlert(alert);

      // Determine if we should instruct the connector to automatically
      // retry.
      if ((this.retriesOnDisconnect >= conn.getTryCount())
          && (isConnectionException(exc)))
      {
        return INSTRUCT_RETRY;
      }
    }

    // Do nothing by default.
    return INSTRUCT_DO_NOTHING;
  }

  /**
   * ConnectorListener method indicating an exception in executeBatch.
   */
  @Override
  public int exceptionInExecuteBatch(SQLException exc, JdbcConnector conn)
  {
    if (shouldProcess(exc, conn))
    {
      String alert = "Exception in executeBatch" + UtilityConstants.CRLF
          + "Time: " + DateHelper.format(new Date()) + UtilityConstants.CRLF
          + "Query: " + conn.getQuery() + UtilityConstants.CRLF
          + "Exception: " + exc.toString() + UtilityConstants.CRLF
          + "SQLState: " + exc.getSQLState() + UtilityConstants.CRLF
          + "Error code: " + exc.getErrorCode() + UtilityConstants.CRLF;

      processAlert(alert);

      // Determine if we should instruct the connector to automatically
      // retry.
      if ((this.retriesOnDisconnect >= conn.getTryCount())
          && (isConnectionException(exc)))
      {
        return INSTRUCT_RETRY;
      }
    }

    // Do nothing by default.
    return INSTRUCT_DO_NOTHING;
  }

  @Override
  public void queryStarting()
  {
    // Does nothing here.
  }

  @Override
  public void queryCompleting()
  {
    // Does nothing here.
  }

  /**
   * Attempt to determine if the exception is indicating a connection problem.
   */
  public boolean isConnectionException(SQLException sqlexc)
  {
    // According to the SQL and JDBC specs, a SQLState starting with "08"
    // indicates a connection error.
    return (sqlexc.getSQLState().startsWith("08"));
  }

} // End BasicConnectorListener.
