package com.thoughtworks.selenium.grid.hub.remotecontrol;

import com.thoughtworks.selenium.grid.HttpClient;
import com.thoughtworks.selenium.grid.hub.Environment;
import com.thoughtworks.selenium.grid.hub.EnvironmentManager;
import com.thoughtworks.selenium.grid.hub.NoSuchEnvironmentException;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertSame;
import static junit.framework.Assert.assertTrue;

import com.thoughtworks.selenium.grid.hub.NoSuchSessionException;
import org.jbehave.classmock.UsingClassMock;
import org.jbehave.core.mock.Mock;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;

import java.util.Date;
import java.util.List;


public class GlobalRemoteControlPoolTest extends UsingClassMock {

    @Test
    public void registerAddsTheRemoteControlToTheProvisioner() {
        final Mock provisioner = mock(RemoteControlProvisioner.class);
        final RemoteControlProxy remoteControl = new RemoteControlProxy("", 0, "an environment", null);
        final GlobalRemoteControlPool pool = new GlobalRemoteControlPool();

        pool.getProvisioners().put("an environment", (RemoteControlProvisioner) provisioner.proxy());
        provisioner.expects("add").with(remoteControl);

        pool.register(remoteControl);
        verifyMocks();
    }

    @Test
    public void unregisterReturnsTheResultOfProvisionerRemove() {
        final Mock provisioner = mock(RemoteControlProvisioner.class);
        final RemoteControlProxy remoteControl = new RemoteControlProxy("", 0, "an environment", null);
        final GlobalRemoteControlPool pool = new GlobalRemoteControlPool();

        pool.getProvisioners().put("an environment", (RemoteControlProvisioner) provisioner.proxy());
        provisioner.expects("remove").with(remoteControl).will(returnValue(false));
        assertEquals(false, pool.unregister(remoteControl));
        verifyMocks();
    }


    @Test
    public void unregisterRemovesTheRemoteControlToTheProvisioner() {
        final Mock provisioner = mock(RemoteControlProvisioner.class);
        final RemoteControlProxy remoteControl = new RemoteControlProxy("", 0, "an environment", null);
        final GlobalRemoteControlPool pool = new GlobalRemoteControlPool();

        pool.getProvisioners().put("an environment", (RemoteControlProvisioner) provisioner.proxy());
        provisioner.expects("remove").with(remoteControl).will(returnValue(true));
        assertTrue(pool.unregister(remoteControl));
        verifyMocks();
    }

    @Test
    public void onceUnregisteredARemoteControlIsNotAssociatedWithAnyExistingSession() {
        final RemoteControlProxy remoteControl;
        final GlobalRemoteControlPool pool;

        remoteControl = new RemoteControlProxy("", 0, "", null);
        pool = new GlobalRemoteControlPool();

        pool.register(remoteControl);
        pool.associateWithSession(remoteControl, "a session id");
        pool.unregister(remoteControl);

        try {
            pool.retrieve("a session id");
            fail("did not catch NoSuchSessionException as expected");
        } catch(NoSuchSessionException e) {
            assertEquals("a session id", e.sessionId());
        }

        verifyMocks();
    }

    @Test
    public void reserveReturnsTheRemoteControlReservedByTheProvisioner() {
        final Mock provisioner = mock(RemoteControlProvisioner.class);
        final RemoteControlProxy remoteControl = new RemoteControlProxy("", 0, "an environment", null);
        final GlobalRemoteControlPool pool = new GlobalRemoteControlPool();


        pool.getProvisioners().put("an environment", (RemoteControlProvisioner) provisioner.proxy());
        provisioner.expects("reserve").will(returnValue(remoteControl));
        assertEquals(remoteControl, pool.reserve(new Environment("an environment", "")));
        verifyMocks();
    }

    @Test
    public void reserveRaisesNoSuchEnvironmentExceptionWhenThereIsNoRegisteredRCForThisEnvironment() {
        final GlobalRemoteControlPool pool;

        pool = new GlobalRemoteControlPool() {
            public RemoteControlProvisioner getProvisioner(String environment) {
                assertEquals("an environment", environment);
                return null;
            }
        };

        try {
            pool.reserve(new Environment("an environment", ""));
            fail("did not catch NoSuchEnvironmentException as expected");
        } catch(NoSuchEnvironmentException e) {
            assertEquals("an environment", e.environment());  
        }
    }

    @Test
    public void getRemoteControlRaisesNoSuchSessionExceptionWhenNoRemoteControlHasBeenAssociatedWithTheSessionId() {
        try {
            new GlobalRemoteControlPool().retrieve("unknown session id");
            fail("did not catch NoSuchSessionException as expected");
        } catch(NoSuchSessionException e) {
            assertEquals("unknown session id", e.sessionId());
        }
    }

    @Test
    public void getRemoteControlReturnsTheRemoteControlHasBeenAssociatedWithASpecificSession() {
        final GlobalRemoteControlPool pool = new GlobalRemoteControlPool();
        final RemoteControlProxy remoteControl = new RemoteControlProxy("", 0, "", null);

        pool.associateWithSession(remoteControl, "a session id");
        assertEquals(remoteControl, pool.retrieve("a session id"));
    }

    @Test(expected = IllegalStateException.class)
    public void associateWithSessionThrowsAnIllegalStateExceptionWhenSessionIdIsAlreadyRegistered() {
        final GlobalRemoteControlPool pool = new GlobalRemoteControlPool();

        pool.associateWithSession(new RemoteControlProxy("", 0, "", null), "shared session id");
        pool.associateWithSession(new RemoteControlProxy("", 0, "", null), "shared session id");
    }

    @Test
    public void associateWithSessionKeepsTrackOfEachSessionIndependently() {
        final GlobalRemoteControlPool pool = new GlobalRemoteControlPool();
        final RemoteControlProxy firstRemoteControl = new RemoteControlProxy("", 0, "", null);
        final RemoteControlProxy secondRemoteControl = new RemoteControlProxy("", 0, "", null);

        pool.associateWithSession(firstRemoteControl, "first session id");
        pool.associateWithSession(secondRemoteControl, "second session id");

        assertEquals(firstRemoteControl, pool.retrieve("first session id"));
        assertEquals(secondRemoteControl, pool.retrieve("second session id"));
    }

    @Test
    public void afterReleaseForSessionARemoteControlIsNotAssociatedWithASessionAnymore() {
        final RemoteControlProxy remoteControl;
        final GlobalRemoteControlPool pool;                                                

        remoteControl = new HealthyRemoteControl("", 0, "an environment", new HttpClient());
        pool = new GlobalRemoteControlPool();
        pool.register(remoteControl);
        pool.reserve(new Environment("an environment", ""));
        pool.associateWithSession(remoteControl, "a session id");

        pool.releaseForSession("a session id");

        try {
            pool.retrieve("a session id");
            fail("did not catch NoSuchSessionException as expected");
        } catch(NoSuchSessionException e) {
            assertEquals("a session id", e.sessionId());
        }
    }

    public void releaseForSessionDoesNotThrowsAnExceptionWhenSessionIsNotAssociated() {
        final GlobalRemoteControlPool pool = new GlobalRemoteControlPool();
        pool.releaseForSession("unknown session id");
    }

    @Test
    public void releaseForSessionReleasesTheRemoteControlOnTheProvisioner() {
        final Mock provisioner = mock(RemoteControlProvisioner.class);
        final RemoteControlProxy remoteControl = new RemoteControlProxy("", 0, "an environment", new HttpClient());
        final GlobalRemoteControlPool pool  = new GlobalRemoteControlPool();


        pool.getProvisioners().put("an environment", (RemoteControlProvisioner) provisioner.proxy());
        pool.register(remoteControl);
        pool.associateWithSession(remoteControl, "a session id");

        provisioner.expects("release").with(remoteControl);
        pool.releaseForSession("a session id");

        try {
            pool.retrieve("a session id");
            fail("did not catch NoSuchSessionException as expected");
        } catch(NoSuchSessionException e) {
            assertEquals("a session id", e.sessionId());
        }

        verifyMocks();
    }

    @Test
    public void releaseReleasesTheRemoteControlOnTheProvisioner() {
        final Mock provisioner = mock(RemoteControlProvisioner.class);
        final RemoteControlProxy remoteControl = new RemoteControlProxy("", 0, "an environment", null);
        final GlobalRemoteControlPool pool = new GlobalRemoteControlPool();

        pool.getProvisioners().put("an environment", (RemoteControlProvisioner) provisioner.proxy());
        provisioner.expects("release").with(remoteControl);

        pool.release(remoteControl);

        try {
            pool.retrieve("a session id");
            fail("did not catch NoSuchSessionException as expected");
        } catch(NoSuchSessionException e) {
            assertEquals("a session id", e.sessionId());
        }

        verifyMocks();
    }

    @Test
    public void reserveReturnsTheRemoteControlReservedOnEnvironmentPool() {
        final GlobalRemoteControlPool pool;
        final Environment environment;
        final RemoteControlProxy remoteControl;

        remoteControl = new HealthyRemoteControl("", 0, "an environment", null);
        environment = new Environment("an environment", "*firefox");
        pool = new GlobalRemoteControlPool();
        pool.register(remoteControl);

        assertEquals(remoteControl, pool.reserve(environment));
        verifyMocks();
    }

    @Test
    public void getPoolForSessionReturnsThePoolThatHaveBeenAssociatedWithASession() {
        final RemoteControlProxy remoteControl;
        final GlobalRemoteControlPool pool;

        remoteControl = new RemoteControlProxy("host", 0, "an environment", null);
        pool = new GlobalRemoteControlPool();
        pool.register(remoteControl);
        pool.associateWithSession(remoteControl, "a session id");

        assertSame(remoteControl, pool.getRemoteControlForSession("a session id"));
        verifyMocks();
    }

    @Test
    public void getPoolForSessionRaisesNoSuchSessionExceptionWhenNoPoolHaveBeenAssociatedWithThisSession() {
        try {
            new GlobalRemoteControlPool().getRemoteControlForSession("a session id");
            fail("did not catch NoSuchSessionException as expected");
        } catch(NoSuchSessionException e) {
            assertEquals("a session id", e.sessionId());
        }
    }

    @Test
    public void aRemoteControlIsNotAssociatedWithTheSessionAfterCallingReleaseForSession() {
        final RemoteControlProxy remoteControl;
        final GlobalRemoteControlPool pool;

        remoteControl = new HealthyRemoteControl("host", 0, "an environment", new HttpClient());
        pool = new GlobalRemoteControlPool();

        pool.register(remoteControl);
        pool.reserve(new Environment("an environment", ""));
        pool.associateWithSession(remoteControl, "a session id");
        pool.releaseForSession("a session id");

        try {
            pool.getRemoteControlForSession("a session id");
            fail("did not catch NoSuchSessionException as expected");
        } catch(NoSuchSessionException e) {
            assertEquals("a session id", e.sessionId());
        }
    }

    @Test
    public void releaseDelegatesCallToEnvironmentPoolAndForgetsAboutThisSession() {
        final RemoteControlProxy remoteControl;
        final GlobalRemoteControlPool pool;

        remoteControl = new HealthyRemoteControl("host", 0, "an environment", null);
        pool = new GlobalRemoteControlPool();

        pool.register(remoteControl);
        pool.reserve(new Environment("an environment", ""));
        pool.release(remoteControl);
        assertFalse(remoteControl.sessionInProgress());
    }

    @Test
    public void getReturnsRemoteControlThatHaveBeenAssociatedWithASession() {
        final RemoteControlProxy remoteControl;
        final GlobalRemoteControlPool pool;
        remoteControl = new RemoteControlProxy("host", 0, "an environment", null);
        pool = new GlobalRemoteControlPool();

        pool.register(remoteControl);
        pool.associateWithSession(remoteControl, "a session id");

        assertEquals(remoteControl, pool.retrieve("a session id"));
    }

    @Test
    public void availableRemoteControlsReturnAnEmptyArrayWhenThereIsNoEnvironment() {
        assertTrue(new GlobalRemoteControlPool().availableRemoteControls().isEmpty());
    }

    @Test
    public void availableRemoteControlsReturnAvailableRemoteControlsForAllEnvironments() {
        final HealthyRemoteControl anotherRCForTheSecondEnvironment;
        final HealthyRemoteControl aRCForAnotherEnvironment;
        final List<RemoteControlProxy> availableRemoteControls;
        final EnvironmentManager environmentManager;
        final Environment firstEnvironment;
        final Environment secondEnvironment;
        final GlobalRemoteControlPool pool;
        final HealthyRemoteControl aRC;

        
        environmentManager = new EnvironmentManager();
        firstEnvironment = new Environment("first environment", "*chrome");
        secondEnvironment = new Environment("second environment", "*chrome");
        environmentManager.addEnvironment(firstEnvironment);
        environmentManager.addEnvironment(secondEnvironment);

        pool = new GlobalRemoteControlPool();
        aRC = new HealthyRemoteControl("first host", 0, "first environment", null);
        aRCForAnotherEnvironment = new HealthyRemoteControl("second host", 0, "second environment", null);
        pool.register(aRC);
        pool.register(aRCForAnotherEnvironment);
        anotherRCForTheSecondEnvironment = new HealthyRemoteControl("third host", 0, "second environment", null);
        pool.register(anotherRCForTheSecondEnvironment);

        availableRemoteControls = pool.availableRemoteControls();
        assertEquals(3, availableRemoteControls.size());
        assertTrue(availableRemoteControls.contains(aRC));
        assertTrue(availableRemoteControls.contains(aRCForAnotherEnvironment));
        assertTrue(availableRemoteControls.contains(anotherRCForTheSecondEnvironment));
    }

    @Test
    public void availableRemoteControlsDoNotReturnReservedRemoteControls() {
        final List<RemoteControlProxy> availableRemoteControls;
        final EnvironmentManager environmentManager;
        final Environment anEnvironment;
        final GlobalRemoteControlPool pool;
        final HealthyRemoteControl anotherRC;
        final HealthyRemoteControl aRC;


        environmentManager = new EnvironmentManager();
        anEnvironment = new Environment("an environment", "*chrome");
        environmentManager.addEnvironment(anEnvironment);

        pool = new GlobalRemoteControlPool();
        aRC = new HealthyRemoteControl("first host", 0, "an environment", null);
        anotherRC = new HealthyRemoteControl("second host", 0, "an environment", null);
        pool.register(aRC);
        pool.register(anotherRC);
        pool.reserve(anEnvironment);

        availableRemoteControls = pool.availableRemoteControls();
        assertEquals(1, availableRemoteControls.size());
        assertFalse(availableRemoteControls.contains(aRC));
        assertTrue(availableRemoteControls.contains(anotherRC));
    }

    @Test
    public void availableRemoteControlsIsEmptyWhenAllRemoteControlsAreReserved() {
        final EnvironmentManager environmentManager;
        final Environment anEnvironment;
        final GlobalRemoteControlPool pool;
        final HealthyRemoteControl anotherRC;
        final HealthyRemoteControl aRC;


        environmentManager = new EnvironmentManager();
        anEnvironment = new Environment("an environment", "*chrome");
        environmentManager.addEnvironment(anEnvironment);

        pool = new GlobalRemoteControlPool();
        aRC = new HealthyRemoteControl("first host", 0, "an environment", null);
        anotherRC = new HealthyRemoteControl("second host", 0, "an environment", null);
        pool.register(aRC);
        pool.register(anotherRC);
        pool.reserve(anEnvironment);
        pool.reserve(anEnvironment);

        assertTrue(pool.availableRemoteControls().isEmpty());
    }

    @Test
    public void reservedRemoteControlsReturnAnEmptyArrayWhenThereIsNoEnvironment() {
        assertTrue(new GlobalRemoteControlPool().reservedRemoteControls().isEmpty());
        verifyMocks();
    }

    @Test
    public void reservedRemoteControlsReturnsReservedRemoteControlsForAllEnvironemnt() {
        final HealthyRemoteControl anotherRCForTheSecondEnvironment;
        final HealthyRemoteControl aRCForAnotherEnvironment;
        final List<RemoteControlProxy> reservedRemoteControls;
        final EnvironmentManager environmentManager;
        final Environment firstEnvironment;
        final Environment secondEnvironment;
        final GlobalRemoteControlPool pool;
        final HealthyRemoteControl aRC;


        environmentManager = new EnvironmentManager();
        firstEnvironment = new Environment("first environment", "*chrome");
        secondEnvironment = new Environment("second environment", "*chrome");
        environmentManager.addEnvironment(firstEnvironment);
        environmentManager.addEnvironment(secondEnvironment);

        pool = new GlobalRemoteControlPool();
        aRC = new HealthyRemoteControl("first host", 0, "first environment", null);
        aRCForAnotherEnvironment = new HealthyRemoteControl("second host", 0, "second environment", null);
        pool.register(aRC);
        pool.register(aRCForAnotherEnvironment);
        anotherRCForTheSecondEnvironment = new HealthyRemoteControl("third host", 0, "second environment", null);
        pool.register(anotherRCForTheSecondEnvironment);

        pool.reserve(firstEnvironment);
        pool.reserve(secondEnvironment);
        pool.reserve(secondEnvironment);

        reservedRemoteControls = pool.reservedRemoteControls();
        assertEquals(3, reservedRemoteControls.size());
        assertTrue(reservedRemoteControls.contains(aRC));
        assertTrue(reservedRemoteControls.contains(aRCForAnotherEnvironment));
        assertTrue(reservedRemoteControls.contains(anotherRCForTheSecondEnvironment));
        verifyMocks();
    }

    @Test
    public void reservedRemoteControlsDoNotReturnAvailableRemoteControls() {
        final List<RemoteControlProxy> reservedRemoteControls;
        final EnvironmentManager environmentManager;
        final Environment anEnvironment;
        final GlobalRemoteControlPool pool;
        final HealthyRemoteControl anotherRC;
        final HealthyRemoteControl aRC;


        environmentManager = new EnvironmentManager();
        anEnvironment = new Environment("an environment", "*chrome");
        environmentManager.addEnvironment(anEnvironment);

        pool = new GlobalRemoteControlPool();
        aRC = new HealthyRemoteControl("first host", 0, "an environment", null);
        anotherRC = new HealthyRemoteControl("second host", 0, "an environment", null);
        pool.register(aRC);
        pool.register(anotherRC);
        pool.reserve(anEnvironment);

        reservedRemoteControls = pool.reservedRemoteControls();
        assertEquals(1, reservedRemoteControls.size());
        assertTrue(reservedRemoteControls.contains(aRC));
        assertFalse(reservedRemoteControls.contains(anotherRC));
        verifyMocks();
    }

    @Test
    public void reservedRemoteControlsIsEmptyWhenNoRemoteControlsAreReserved() {
        final EnvironmentManager environmentManager;
        final Environment anEnvironment;
        final GlobalRemoteControlPool pool;
        final HealthyRemoteControl anotherRC;
        final HealthyRemoteControl aRC;


        environmentManager = new EnvironmentManager();
        anEnvironment = new Environment("an environment", "*chrome");
        environmentManager.addEnvironment(anEnvironment);

        pool = new GlobalRemoteControlPool();
        aRC = new HealthyRemoteControl("first host", 0, "an environment", null);
        anotherRC = new HealthyRemoteControl("second host", 0, "an environment", null);
        pool.register(aRC);
        pool.register(anotherRC);

        assertTrue(pool.reservedRemoteControls().isEmpty());
    }

    @Test
    public void allRegisteredRemoteControlsReturnAnEmptyArrayWhenThereIsNoEnvironment() {
        assertTrue(new GlobalRemoteControlPool().allRegisteredRemoteControls().isEmpty());
        verifyMocks();
    }

    @Test
    public void allRegisteredRemoteControlsReturnAllAvailableRemoteControlsForAllEnvironmentsWhenNoneAreReserved() {
        final HealthyRemoteControl anotherRCForTheSecondEnvironment;
        final HealthyRemoteControl aRCForAnotherEnvironment;
        final List<RemoteControlProxy> allRegisteredRemoteControls;
        final EnvironmentManager environmentManager;
        final Environment firstEnvironment;
        final Environment secondEnvironment;
        final GlobalRemoteControlPool pool;
        final HealthyRemoteControl aRC;


        environmentManager = new EnvironmentManager();
        firstEnvironment = new Environment("first environment", "*chrome");
        secondEnvironment = new Environment("second environment", "*chrome");
        environmentManager.addEnvironment(firstEnvironment);
        environmentManager.addEnvironment(secondEnvironment);

        pool = new GlobalRemoteControlPool();
        aRC = new HealthyRemoteControl("first host", 0, "first environment", null);
        aRCForAnotherEnvironment = new HealthyRemoteControl("second host", 0, "second environment", null);
        pool.register(aRC);
        pool.register(aRCForAnotherEnvironment);
        anotherRCForTheSecondEnvironment = new HealthyRemoteControl("third host", 0, "second environment", null);
        pool.register(anotherRCForTheSecondEnvironment);

        allRegisteredRemoteControls = pool.allRegisteredRemoteControls();
        assertEquals(3, allRegisteredRemoteControls.size());
        assertTrue(allRegisteredRemoteControls.contains(aRC));
        assertTrue(allRegisteredRemoteControls.contains(aRCForAnotherEnvironment));
        assertTrue(allRegisteredRemoteControls.contains(anotherRCForTheSecondEnvironment));
    }

    @Test
    public void allRegisteredRemoteControlsReturnsAvailableAndReservedRemoteControls() {
        final List<RemoteControlProxy> allRegisteredRemoteControls;
        final EnvironmentManager environmentManager;
        final Environment anEnvironment;
        final GlobalRemoteControlPool pool;
        final HealthyRemoteControl anotherRC;
        final HealthyRemoteControl aRC;


        environmentManager = new EnvironmentManager();
        anEnvironment = new Environment("an environment", "*chrome");
        environmentManager.addEnvironment(anEnvironment);

        pool = new GlobalRemoteControlPool();
        aRC = new HealthyRemoteControl("first host", 0, "an environment", null);
        anotherRC = new HealthyRemoteControl("second host", 0, "an environment", null);
        pool.register(aRC);
        pool.register(anotherRC);
        pool.reserve(anEnvironment);

        allRegisteredRemoteControls = pool.allRegisteredRemoteControls();
        assertEquals(2, allRegisteredRemoteControls.size());
        assertTrue(allRegisteredRemoteControls.contains(aRC));
        assertTrue(allRegisteredRemoteControls.contains(anotherRC));
    }

    @Test
    public void allRegisteredRemoteControlsReturnsAllReservedRemoteControls() {
        final List<RemoteControlProxy> allRegisteredRemoteControls;
        final EnvironmentManager environmentManager;
        final Environment anEnvironment;
        final GlobalRemoteControlPool pool;
        final HealthyRemoteControl anotherRC;
        final HealthyRemoteControl aRC;


        environmentManager = new EnvironmentManager();
        anEnvironment = new Environment("an environment", "*chrome");
        environmentManager.addEnvironment(anEnvironment);

        pool = new GlobalRemoteControlPool();
        aRC = new HealthyRemoteControl("first host", 0, "an environment", null);
        anotherRC = new HealthyRemoteControl("second host", 0, "an environment", null);
        pool.register(aRC);
        pool.register(anotherRC);
        pool.reserve(anEnvironment);
        pool.reserve(anEnvironment);

        allRegisteredRemoteControls = pool.allRegisteredRemoteControls();
        assertEquals(2, allRegisteredRemoteControls.size());
        assertTrue(allRegisteredRemoteControls.contains(aRC));
        assertTrue(allRegisteredRemoteControls.contains(anotherRC));
    }

    @Test
    public void logSessionMapDoesNotBombWhenThereIsNoSession() {
        final GlobalRemoteControlPool pool;

        pool = new GlobalRemoteControlPool();
        pool.logSessionMap();
    }

    @Test
    public void logSessionMapDoesNotBombWhenThereIsSomeRegisteredSession() {
        final RemoteControlProxy aRemoteControl;
        final RemoteControlProxy anotherRemoteControl;
        final GlobalRemoteControlPool pool;

        aRemoteControl = new HealthyRemoteControl("host", 4444, "an environment", null);
        anotherRemoteControl = new HealthyRemoteControl("host", 4445, "an environment", null);
        pool = new GlobalRemoteControlPool();

        pool.register(aRemoteControl);
        pool.register(anotherRemoteControl);
        pool.reserve(new Environment("an environment", ""));
        pool.reserve(new Environment("an environment", ""));
        pool.associateWithSession(aRemoteControl, "a session id");
        pool.associateWithSession(anotherRemoteControl, "another session id");
        pool.logSessionMap();
    }

    @Test
    public void unregisterRemoteControlIfUnresponsiveDoesNotUnregisterAHealthyRemoteControl() {
        final RemoteControlProxy healthyRC;
        final GlobalRemoteControlPool pool;

        healthyRC = new HealthyRemoteControl("host", 4444, "an environment", null);
        pool = new GlobalRemoteControlPool();
        pool.register(healthyRC);

        pool.unregisterAllUnresponsiveRemoteControls();
        assertTrue(pool.allRegisteredRemoteControls().contains(healthyRC));
    }

    @Test
    public void unregisterRemoteControlIfUnresponsiveUnregistersARemoteControlThatIsUnreliable() {
        final RemoteControlProxy unreliableRC;
        final GlobalRemoteControlPool pool;

        unreliableRC = new UnreliableRemoteControl("host", 4444, "an environment", null);
        pool = new GlobalRemoteControlPool();
        pool.register(unreliableRC);

        pool.unregisterAllUnresponsiveRemoteControls();
        assertTrue(pool.allRegisteredRemoteControls().isEmpty());
    }

    @Test
    public void recycleSessionIfIdleForTooLongDoesReleaseTheSessionWhenIdleForTooLong() {
        final RemoteControlProxy aRC;
        final GlobalRemoteControlPool pool;

        final Mock httpClient = mock(HttpClient.class);
        httpClient.stubs("post");
        aRC = new HealthyRemoteControl("host", 4444, "an environment", (HttpClient) httpClient);

        pool = new GlobalRemoteControlPool();
        pool.register(aRC);
        pool.reserve(new Environment("an environment", "a browser"));
        pool.associateWithSession(aRC, "a session id");

        pool.recycleSessionIfIdleForTooLong(new RemoteControlSession("a session id", aRC), 0);
        assertTrue(pool.availableRemoteControls().contains(aRC));
        assertTrue(pool.allRegisteredRemoteControls().contains(aRC));

        try {
            pool.getRemoteControlForSession("a session id");
            fail("did not catch NoSuchSessionException as expected");
        } catch(NoSuchSessionException e) {
            assertEquals("a session id", e.sessionId());
        }
    }

    @Test
    public void recycleSessionIfIdleForTooLongDoesNotReleaseTheSessionWhenIdleForLessThanTheMaxIdleInterval() {
        final RemoteControlProxy aRC;
        final GlobalRemoteControlPool pool;

        aRC = new HealthyRemoteControl("host", 4444, "an environment", null);
        pool = new GlobalRemoteControlPool();
        pool.register(aRC);
        pool.reserve(new Environment("an environment", "a browser"));
        pool.associateWithSession(aRC, "a session id");

        pool.recycleSessionIfIdleForTooLong(new RemoteControlSession("a session id", aRC), 10);
        assertNotNull(pool.getRemoteControlForSession("a session id"));
        assertFalse(pool.availableRemoteControls().contains(aRC));
        assertTrue(pool.allRegisteredRemoteControls().contains(aRC));
    }

    @Test
    public void recycleSessionIfIdleForTooLongConvertTheMaxIdleIntervalFromSecondsToMilliseconds() {
        final RemoteControlProxy aRC;
        final GlobalRemoteControlPool pool;
        final Mock session;

        final Mock httpClient = mock(HttpClient.class);
        httpClient.stubs("post");
        aRC = new HealthyRemoteControl("host", 4444, "an environment", (HttpClient) httpClient);

        pool = new GlobalRemoteControlPool();
        pool.register(aRC);
        pool.reserve(new Environment("an environment", "a browser"));
        pool.associateWithSession(aRC, "a session id");
        session = mock(RemoteControlSession.class);
        session.stubs("sessionId").will(returnValue("a session id"));
        session.expects("innactiveForMoreThan").with(eq(20000)).will(returnValue(true));

        pool.recycleSessionIfIdleForTooLong((RemoteControlSession) session, 20);

        try {
            pool.getRemoteControlForSession("a session id");
            fail("did not catch NoSuchSessionException as expected");
        } catch(NoSuchSessionException e) {
            assertEquals("a session id", e.sessionId());
        }
    }

    @Test
    public void recycleAllSessionsIdleForTooLongDoesNotAffectRemoteControlsNotAssociatedWithASession() {
        final RemoteControlProxy healthyRC;
        final GlobalRemoteControlPool pool;

        healthyRC = new HealthyRemoteControl("host", 4444, "an environment", null);
        pool = new GlobalRemoteControlPool();
        pool.register(healthyRC);

        pool.recycleAllSessionsIdleForTooLong(0.0);
        assertFalse(pool.reservedRemoteControls().contains(healthyRC));
        assertTrue(pool.allRegisteredRemoteControls().contains(healthyRC));
    }

    @Test
    public void recycleAllSessionsIdleForTooLongDoesReleaseRemoteControlsAssociatedWithASessionAndIdleForTooLong() {
        final RemoteControlProxy rc;
        final GlobalRemoteControlPool pool;

        final Mock httpClient = mock(HttpClient.class);
        httpClient.stubs("post");
        rc = new HealthyRemoteControl("host", 4444, "an environment", (HttpClient) httpClient);

        pool = new GlobalRemoteControlPool();
        pool.register(rc);
        pool.reserve(new Environment("an environment", "a browser"));
        pool.associateWithSession(rc, "a session id");

        pool.recycleAllSessionsIdleForTooLong(0.0);
        assertTrue(pool.allRegisteredRemoteControls().contains(rc));

        try {
            pool.getRemoteControlForSession("a session id");
            fail("did not catch NoSuchSessionException as expected");
        } catch(NoSuchSessionException e) {
            assertEquals("a session id", e.sessionId());
        }
    }

    @Test
    public void unregisterAllUnresponsiveRemoteControlsUnregistersOnlyUnavailableRemoteControls() {
        final RemoteControlProxy anUnreliableRC;
        final RemoteControlProxy anotherUnreliableRC;
        final RemoteControlProxy aHealthyRC;
        final RemoteControlProxy anotherHealthyRC;
        final GlobalRemoteControlPool pool;

        anUnreliableRC = new UnreliableRemoteControl("host", 4444, "an environment", null);
        anotherUnreliableRC = new UnreliableRemoteControl("host", 4445, "another environment", null);
        aHealthyRC = new HealthyRemoteControl("host", 4446, "another environment", null);
        anotherHealthyRC = new HealthyRemoteControl("host", 4447, "another environment", null);
        pool = new GlobalRemoteControlPool();
        pool.register(anUnreliableRC);
        pool.register(anotherUnreliableRC);
        pool.register(aHealthyRC);
        pool.register(anotherHealthyRC);

        pool.unregisterAllUnresponsiveRemoteControls();
        assertEquals(2, pool.allRegisteredRemoteControls().size());
        assertTrue(pool.allRegisteredRemoteControls().contains(aHealthyRC));
        assertTrue(pool.allRegisteredRemoteControls().contains(anotherHealthyRC));
    }

    @Test
    public void updateSessionLastActiveAtUpdatesLastActiveAtOnTheSession() {
        final RemoteControlProxy remoteControl;
        final RemoteControlSession session;
        final GlobalRemoteControlPool pool;
        final long creationTime;
        final long now;

        remoteControl = new HealthyRemoteControl("host", 24, "env", null);
        pool = new GlobalRemoteControlPool();

        pool.register(remoteControl);
        pool.associateWithSession(remoteControl, "a session id");
        now = new Date().getTime();
        creationTime = now - 10;
        pool.getSessions().get("a session id").updateLastActiveAt(creationTime);
        pool.updateSessionLastActiveAt("a session id");
        session = pool.getSessions().get("a session id");
        assertTrue(session.lastActiveAt() > creationTime);
        assertTrue(session.lastActiveAt() >= now);
        assertTrue(session.lastActiveAt() <= now + 10 * 1000);
    }

    @Test
    public void isRegisteredReturnsTrueWhenRemoteControlIsRegistered() {
        final RemoteControlProxy remoteControl;
        final GlobalRemoteControlPool pool;

        remoteControl = new RemoteControlProxy("", 0, "an environment", null);
        pool = new GlobalRemoteControlPool();

        pool.register(remoteControl);
        assertTrue(pool.isRegistered(remoteControl));
    }

    @Test
    public void isRegisteredReturnsTrueWhenRemoteControlHasNeverBeenRegistered() {
        final GlobalRemoteControlPool pool;

        pool = new GlobalRemoteControlPool();
        assertFalse(pool.isRegistered( new RemoteControlProxy("", 0, "an environment", null)));
    }

    @Test
    public void isRegisteredReturnsTrueWhenRemoteControlAreRegisteredWithMultipleEnvironments() {
        final RemoteControlProxy aRemoteControl;
        final RemoteControlProxy anotherRemoteControl;
        final GlobalRemoteControlPool pool;

        aRemoteControl = new RemoteControlProxy("a", 0, "an environment", null);
        anotherRemoteControl = new RemoteControlProxy("b", 0, "another environment", null);
        pool = new GlobalRemoteControlPool();

        pool.register(aRemoteControl);
        pool.register(anotherRemoteControl);
        assertTrue(pool.isRegistered(aRemoteControl));
        assertTrue(pool.isRegistered(anotherRemoteControl));
    }
    
}