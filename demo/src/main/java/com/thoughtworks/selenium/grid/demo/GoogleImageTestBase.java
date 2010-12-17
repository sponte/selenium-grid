package com.thoughtworks.selenium.grid.demo;

import static com.thoughtworks.selenium.grid.tools.ThreadSafeSeleniumSessionStorage.session;
import static org.testng.AssertJUnit.assertTrue;

/**
 * Base class for Amazon Web Acceptance tests
 */
public abstract class GoogleImageTestBase {

    public static final String TIMEOUT = "120000";


    protected void runFlickrScenario(String searchString) {
        session().setTimeout(TIMEOUT);
        session().open("/");
        assertTrue(session().getLocation(), session().getLocation().contains("images.google.com"));
        session().type("q", searchString);
        session().click("btnG");
        session().waitForPageToLoad(TIMEOUT);
        session().click("isz_l");
        session().click("itp_photo");
    }

}
