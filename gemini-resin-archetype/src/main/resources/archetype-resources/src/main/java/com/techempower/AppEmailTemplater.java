package com.techempower;

import com.techempower.gemini.*;
import com.techempower.gemini.email.outbound.*;
import com.techempower.util.*;

/**
 * An EmailTemplater.
 */
public class AppEmailTemplater
        extends ResinMustacheEmailTemplater {

    //
    // Member variables.
    //

    private String fromEmailAddress = "noone@techempower.com";

    //
    // Member methods.
    //

    /**
     * Constructor.
     */
    public AppEmailTemplater(GeminiApplication application) {
        super(application);
    }

    /**
     * Configures this component.  Overload this method to load emails.
     */
    @Override
    public void configure(EnhancedProperties props) {
        super.configure(props);

        this.fromEmailAddress = props.get("FromEmailAddress", this.fromEmailAddress);
        getLog().log("E-mail author: " + this.fromEmailAddress);
    }

    /**
     * Gets the author e-mail address.
     */
    public String getEmailAuthor() {
        return this.fromEmailAddress;
    }

}
