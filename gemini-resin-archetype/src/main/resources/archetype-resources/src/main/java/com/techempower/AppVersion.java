package com.techempower;

import com.techempower.gemini.*;
import com.techempower.helper.*;

/**
 * Provides a name, client, and version number for the current build of
 * the application.
 */
public class AppVersion
        extends Version {

    //
    // Member methods.
    //

    /**
     * Constructor.  This builds the version string.
     */
    public AppVersion() {
        this.setVersionString(getMajorVersion()
                + "." + StringHelper.padZero(getMinorVersion(), 2)
                + "." + StringHelper.padZero(getMicroVersion(), 2)
                + " (Gemini " + GeminiConstants.GEMINI_VERSION + ")");
    }

    /**
     * Get the version levels.
     */
    @Override
    public int getMajorVersion() {
        return 0;
    }

    @Override
    public int getMinorVersion() {
        return 1;
    }

    @Override
    public int getMicroVersion() {
        return 0;
    }

    /**
     * Gets the product code.
     */
    @Override
    public String getProductCode() {
        return "";
    }

    /**
     * Gets the product name.
     */
    @Override
    public String getProductName() {
        return "App";
    }

    /**
     * Gets the client's name.
     */
    @Override
    public String getClientName() {
        return "TechEmpower, Inc.";
    }

    /**
     * Gets the developer's name.
     */
    @Override
    public String getDeveloperName() {
        return "";
    }

    /**
     * Gets the copyright years.
     */
    @Override
    public String getCopyrightYears() {
        return "2016";
    }

}

