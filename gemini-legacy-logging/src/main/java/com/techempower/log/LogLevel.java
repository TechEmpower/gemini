package com.techempower.log;

public interface LogLevel
{
  /**
   * Notification constant 1 (value=0).  This is lowest notification level.
   */
  int     MINIMUM                 = 0;

  /**
   * Notification constant 2 (value=10).
   */
  int     DEBUG                   = 10;

  /**
   * Notification constant 3 (value=30).
   */
  int     NOTICE                  = 30;

  /**
   * Notification constant 4 (value=50).
   */
  int     NORMAL                  = 50;

  /**
   * Notification constant 5 (value=70).
   */
  int     ALERT                   = 70;

  /**
   * Notification constant 6 (value=90).
   */
  int     CRITICAL                = 90;

  /**
   * Notification constant 7 (value=100).
   */
  int     MAXIMUM                 = 100;
}
