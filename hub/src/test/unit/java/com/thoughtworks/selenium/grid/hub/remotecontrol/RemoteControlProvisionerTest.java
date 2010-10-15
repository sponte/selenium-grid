package com.thoughtworks.selenium.grid.hub.remotecontrol;

import com.thoughtworks.selenium.grid.hub.ConcurrentAction;
import com.thoughtworks.selenium.grid.hub.HubRegistry;
import org.junit.Test;

import static junit.framework.Assert.*;
import static org.junit.Assert.assertSame;

public class RemoteControlProvisionerTest {

    @Test
    public void onceAddedARemoteControlIsPartOfTheAvailableList() {
        final RemoteControlProvisioner provisioner = new RemoteControlProvisioner();
        final RemoteControlProxy remoteControl = new RemoteControlProxy("a", 0, "", null);

        provisioner.add(remoteControl);
        assertEquals(1, provisioner.availableRemoteControls().size());
        assertTrue(provisioner.availableRemoteControls().contains(remoteControl));
    }

    @Test
    public void whenAddingTwoRemoteControlsThatAreEqualsTheFirstIsReplacedByTheSecond() {
        final RemoteControlProvisioner provisioner;
        final IRemoteControlProxy availableRc;

        provisioner = new RemoteControlProvisioner();

        RemoteControlProxy oldRC = new RemoteControlProxy("a", 0, "", null);
        RemoteControlProxy newRC = new RemoteControlProxy("a", 0, "", null);

        provisioner.add(oldRC);
        provisioner.add(newRC);

        availableRc = provisioner.findNextAvailableRemoteControl();
        assertSame(newRC, availableRc);
    }

    @Test
    public void whenReplacingRemoteControlFirstRemoveAllRegisteredSessions() {
        final RemoteControlProvisioner provisioner;

        provisioner = new RemoteControlProvisioner();
        RemoteControlProxy oldRC = new RemoteControlProxy("a", 0, "", null);
        RemoteControlProxy newRC = new RemoteControlProxy("a", 0, "", null);

        oldRC.registerNewSession();

        assertTrue(oldRC.sessionInProgress());

        provisioner.add(oldRC);
        provisioner.add(newRC);

        assertEquals(0, provisioner.reservedRemoteControls().size());
        assertEquals(1, provisioner.availableRemoteControls().size());
        assertEquals(newRC,
                provisioner.availableRemoteControls().get(0));
        assertFalse(newRC.sessionInProgress());
    }

    @Test
    public void multipleRemoteControlsCanBeAdded() {
        final RemoteControlProvisioner provisioner;
        final IRemoteControlProxy firstRemoteControl;
        final IRemoteControlProxy secondRemoteControl;

        provisioner = new RemoteControlProvisioner();
        firstRemoteControl = new RemoteControlProxy("a", 0, "", null);
        secondRemoteControl = new RemoteControlProxy("b", 0, "", null);

        provisioner.add(firstRemoteControl);
        provisioner.add(secondRemoteControl);
        assertEquals(2, provisioner.availableRemoteControls().size());
        assertTrue(provisioner.availableRemoteControls().contains(firstRemoteControl));
        assertTrue(provisioner.availableRemoteControls().contains(secondRemoteControl));
    }

    @Test
    public void onceRemovedARemoteControlIsNotPartOfTheAvailableList() {
        final RemoteControlProvisioner provisioner = new RemoteControlProvisioner();
        final IRemoteControlProxy remoteControl = new RemoteControlProxy("", 0, "", null);
        provisioner.add(remoteControl);

        provisioner.remove(remoteControl);
        assertTrue(provisioner.availableRemoteControls().isEmpty());
    }

    @Test
    public void removeOnlyRemovesASpecificRemoteControl() {
        final RemoteControlProvisioner provisioner = new RemoteControlProvisioner();
        final IRemoteControlProxy firstRemoteControl = new HealthyRemoteControl("a", 0, "", null);
        final IRemoteControlProxy secondRemoteControl = new HealthyRemoteControl("b", 0, "", null);
        provisioner.add(firstRemoteControl);
        provisioner.add(secondRemoteControl);

        provisioner.remove(firstRemoteControl);
        assertFalse(provisioner.availableRemoteControls().contains(firstRemoteControl));
        assertTrue(provisioner.availableRemoteControls().contains(secondRemoteControl));
    }

    @Test
    public void removeReturnsFalseForARemoteControlThanHasNeverBeenAdded() {
        final RemoteControlProvisioner provisioner = new RemoteControlProvisioner();
        assertFalse(provisioner.remove(new RemoteControlProxy("", 0, "", null)));
    }

    @Test
    public void removeReturnsTrueForAnAvailableRemoteControl() {
        final RemoteControlProvisioner provisioner = new RemoteControlProvisioner();
        final IRemoteControlProxy remoteControl = new RemoteControlProxy("", 0, "", null);
        provisioner.add(remoteControl);

        assertTrue(provisioner.remove(remoteControl));
    }

    @Test
    public void removeReturnsTrueForAReservedRemoteControl() {
        final RemoteControlProvisioner provisioner = new RemoteControlProvisioner();
        final IRemoteControlProxy remoteControl = new HealthyRemoteControl("", 0, "", null);
        provisioner.add(remoteControl);
        provisioner.reserve();

        assertTrue(provisioner.remove(remoteControl));
    }

    @Test
    public void containsReturnsFalseWhenNoRemoteControlHasBeenAdded() {
        final RemoteControlProvisioner provisioner = new RemoteControlProvisioner();

        assertFalse(provisioner.contains(new RemoteControlProxy("a", 0, "", null)));
    }

    @Test
    public void containsReturnsTrueWhenRemoteControlHasBeenAdded() {
        final RemoteControlProvisioner provisioner = new RemoteControlProvisioner();
        final RemoteControlProxy remoteControl = new RemoteControlProxy("a", 0, "", null);

        provisioner.add(remoteControl);
        assertTrue(provisioner.contains(remoteControl));
    }

    @Test
    public void containsReturnsFalseWhenARemoteControlHasNotBeenAdded() {
        final RemoteControlProvisioner provisioner = new RemoteControlProvisioner();
        final RemoteControlProxy remoteControl = new RemoteControlProxy("a", 0, "", null);

        provisioner.add(remoteControl);
        assertFalse(provisioner.contains(new RemoteControlProxy("b", 0, "", null)));
    }

    @Test
    public void reserveReturnsNullWhenThereIsNoRegisteredRemoteControlToAvoidDeadlocks() {
        assertNull(new RemoteControlProvisioner().reserve());
    }

    @Test
    public void reserveReturnsTheFirstAvailableRemoteControl() {
        final RemoteControlProvisioner provisioner = new RemoteControlProvisioner();
        final RemoteControlProxy firstRemoteControl = new HealthyRemoteControl("a", 0, "", null);
        final RemoteControlProxy secondRemoteControl = new HealthyRemoteControl("b", 0, "", null);
        provisioner.add(firstRemoteControl);
        provisioner.add(secondRemoteControl);

        assertEquals(firstRemoteControl, provisioner.reserve());
    }

    @Test
    public void reserveReturnsAvailableRemoteControlInOrderWhileThenCanHandleNewSessions() {
        final RemoteControlProvisioner provisioner = new RemoteControlProvisioner();
        final RemoteControlProxy firstRemoteControl = new HealthyRemoteControl("a", 0, "", null);
        final RemoteControlProxy secondRemoteControl = new HealthyRemoteControl("b", 0, "", null);
        provisioner.add(firstRemoteControl);
        provisioner.add(secondRemoteControl);

        assertEquals(firstRemoteControl, provisioner.reserve());
        assertEquals(secondRemoteControl, provisioner.reserve());
    }

    @Test
    public void reserveBlocksUntilARemoteControlIsReleasedWhenThereIsAtLeastOneRegisteredRemoteControl() {
        final RemoteControlProvisioner provisioner = new RemoteControlProvisioner();
        final RemoteControlProxy remoteControl = new HealthyRemoteControl("a", 0, "", null);

        provisioner.add(remoteControl);
        provisioner.reserve();

        new ConcurrentAction() {
            public void execute() {
                provisioner.release(remoteControl);
            }
        };
        assertEquals(remoteControl, provisioner.reserve());
    }

    @Test
    public void reserveBlocksUntilARemoteControlIsAddedWhenThereIsAtLeastOneRegisteredRemoteControl() {
        final RemoteControlProvisioner provisioner;
        final RemoteControlProxy firstRemoteControl;
        final RemoteControlProxy secondRemoteControl;

        provisioner = new RemoteControlProvisioner();
        firstRemoteControl = new HealthyRemoteControl("a", 0, "", null);
        secondRemoteControl = new HealthyRemoteControl("b", 0, "", null);

        provisioner.add(firstRemoteControl);
        provisioner.reserve();

        new ConcurrentAction() {
            public void execute() {
                provisioner.add(secondRemoteControl);
            }
        };
        assertEquals(secondRemoteControl, provisioner.reserve());
    }

    @Test
    public void reserveTimesOutIfARemoteControlIsNotAddedOrReleased() {
        final RemoteControlProvisioner provisioner = new RemoteControlProvisioner();
        final RemoteControlProxy remoteControl = new HealthyRemoteControl("a", 0, "", null);

        HubRegistry.registry().gridConfiguration().getHub().setNewSessionMaxWaitTimeInSeconds(2.0);

        provisioner.add(remoteControl);
        provisioner.reserve();

        assertEquals(null, provisioner.reserve());
    }

    @Test
    public void findNextAvailableRemoteControlReturnNullWhenThereIsNoRegisteredRemoteControl() {
        assertNull(new RemoteControlProvisioner().findNextAvailableRemoteControl());
    }

    @Test
    public void reserveAutoRemoveRemoteControlsThatAreNotReliables() {
        final RemoteControlProxy firstRemoteControl;
        final RemoteControlProxy secondRemoteControl;
        final RemoteControlProvisioner provisioner;

        provisioner = new RemoteControlProvisioner();
        firstRemoteControl = new UnreliableRemoteControl("a", 0, "", null);
        secondRemoteControl = new HealthyRemoteControl("b", 0, "", null);
        provisioner.add(firstRemoteControl);
        provisioner.add(secondRemoteControl);

        assertEquals(secondRemoteControl, provisioner.reserve());
        assertFalse(provisioner.availableRemoteControls().contains(firstRemoteControl));
        assertFalse(provisioner.reservedRemoteControls().contains(firstRemoteControl));
    }

    @Test
    public void reserveAutoRemoveAllRemoteControlsThatAreDetectedAsNotReliables() {
        final RemoteControlProxy firstRemoteControl;
        final RemoteControlProxy secondRemoteControl;
        final RemoteControlProxy thirdRemoteControl;
        final RemoteControlProvisioner provisioner;

        provisioner = new RemoteControlProvisioner();
        firstRemoteControl = new UnreliableRemoteControl("a", 0, "", null);
        secondRemoteControl = new UnreliableRemoteControl("b", 0, "", null);
        thirdRemoteControl = new HealthyRemoteControl("c", 0, "", null);
        provisioner.add(firstRemoteControl);
        provisioner.add(secondRemoteControl);
        provisioner.add(thirdRemoteControl);

        assertEquals(thirdRemoteControl, provisioner.reserve());
        assertFalse(provisioner.availableRemoteControls().contains(firstRemoteControl));
        assertFalse(provisioner.reservedRemoteControls().contains(firstRemoteControl));
        assertFalse(provisioner.availableRemoteControls().contains(secondRemoteControl));
        assertFalse(provisioner.reservedRemoteControls().contains(secondRemoteControl));
    }

    @Test
    public void reserveReturnsNullWhenAllRemoteControlsAreDetectedAsNotReliables() {
        final RemoteControlProxy firstRemoteControl;
        final RemoteControlProxy secondRemoteControl;
        final RemoteControlProvisioner provisioner;

        provisioner = new RemoteControlProvisioner();
        firstRemoteControl = new UnreliableRemoteControl("a", 0, "", null);
        secondRemoteControl = new UnreliableRemoteControl("b", 0, "", null);
        provisioner.add(firstRemoteControl);
        provisioner.add(secondRemoteControl);

        assertNull(provisioner.reserve());
        assertFalse(provisioner.availableRemoteControls().contains(firstRemoteControl));
        assertFalse(provisioner.reservedRemoteControls().contains(firstRemoteControl));
        assertFalse(provisioner.availableRemoteControls().contains(secondRemoteControl));
        assertFalse(provisioner.reservedRemoteControls().contains(secondRemoteControl));
    }

    @Test
    public void findNextAvailableRemoteControlReturnTheFirstAvailableRemoteControlWhenThereIsOne() {
        final RemoteControlProvisioner provisioner;
        final RemoteControlProxy firstRemoteControl;
        final RemoteControlProxy secondRemoteControl;

        provisioner = new RemoteControlProvisioner();
        firstRemoteControl = new RemoteControlProxy("a", 0, "", null);
        secondRemoteControl = new RemoteControlProxy("b", 0, "", null);
        provisioner.add(firstRemoteControl);
        provisioner.add(secondRemoteControl);

        assertEquals(firstRemoteControl, provisioner.findNextAvailableRemoteControl());
    }

    @Test
    public void findNextAvailableRemoteControlCyclesUntilItFindsAnAvailableRemoteControlWhenThereIsOne() {
        final RemoteControlProvisioner provisioner;
        final RemoteControlProxy firstRemoteControl;
        final RemoteControlProxy secondRemoteControl;

        provisioner = new RemoteControlProvisioner();
        firstRemoteControl = new HealthyRemoteControl("a", 0, "", null);
        secondRemoteControl = new HealthyRemoteControl("b", 0, "", null);
        provisioner.add(firstRemoteControl);
        provisioner.add(secondRemoteControl);

        provisioner.reserve();
        assertEquals(secondRemoteControl, provisioner.findNextAvailableRemoteControl());
    }

    @Test
    public void findNextAvailableRemoteControlReturnsNullWhenThereIsNoAvailableRemoteControlAtAll() {
        final RemoteControlProvisioner provisioner;
        final RemoteControlProxy firstRemoteControl;
        final RemoteControlProxy secondRemoteControl;

        provisioner = new RemoteControlProvisioner();
        firstRemoteControl = new HealthyRemoteControl("a", 0, "", null);
        secondRemoteControl = new HealthyRemoteControl("b", 0, "", null);
        provisioner.add(firstRemoteControl);
        provisioner.add(secondRemoteControl);

        provisioner.reserve();
        provisioner.reserve();
        assertNull(provisioner.findNextAvailableRemoteControl());
    }

    @Test
    public void onceReservedARemoteControlIsNotAvailableAnymore() {
        final RemoteControlProvisioner provisioner = new RemoteControlProvisioner();
        final RemoteControlProxy remoteControl = new RemoteControlProxy("", 0, "", null);
        provisioner.add(remoteControl);

        provisioner.reserve();
        assertNull(provisioner.findNextAvailableRemoteControl());
    }

    @Test
    public void releaseMakesAReservedRemoteControlAvailable() {
        final RemoteControlProvisioner provisioner = new RemoteControlProvisioner();
        final RemoteControlProxy remoteControl = new HealthyRemoteControl("", 0, "", null);
        provisioner.add(remoteControl);

        provisioner.reserve();
        provisioner.release(remoteControl);
        assertTrue(provisioner.availableRemoteControls().contains(remoteControl));
    }

    @Test
    public void reservedRemoteControlsReturnsAnEmptyArrayWhenNoneHaveBeenAdded() {
        assertTrue(new RemoteControlProvisioner().reservedRemoteControls().isEmpty());
    }

    @Test
    public void reservedRemoteControlsReturnsAnEmptyArrayWhenRemoteControlHasBeenAddedButNotReserved() {
        final RemoteControlProvisioner provisioner = new RemoteControlProvisioner();
        final RemoteControlProxy remoteControl = new RemoteControlProxy("", 0, "", null);

        provisioner.add(remoteControl);
        assertTrue(provisioner.reservedRemoteControls().isEmpty());
    }

    @Test
    public void reservedRemoteControlsReturnsARemoteControlThatHaveBeenReserved() {
        final RemoteControlProvisioner provisioner = new RemoteControlProvisioner();
        final RemoteControlProxy remoteControl = new HealthyRemoteControl("", 0, "", null);

        provisioner.add(remoteControl);
        provisioner.reserve();
        assertEquals(1, provisioner.reservedRemoteControls().size());
        assertTrue(provisioner.reservedRemoteControls().contains(remoteControl));
    }

    @Test
    public void availableRemoteControlsReturnsAnEmptyArrayWhenNoneHaveBeenAdded() {
        assertTrue(new RemoteControlProvisioner().availableRemoteControls().isEmpty());
    }

    @Test
    public void availableRemoteControlsReturnsARemoteControlThanHasBeenAdded() {
        final RemoteControlProvisioner provisioner = new RemoteControlProvisioner();
        final RemoteControlProxy remoteControl = new RemoteControlProxy("", 0, "", null);

        provisioner.add(remoteControl);
        assertEquals(1, provisioner.availableRemoteControls().size());
        assertTrue(provisioner.availableRemoteControls().contains(remoteControl));
    }

    @Test
    public void allRemoteControlsReturnsAnEmptyArrayWhenNoneHaveBeenAdded() {
        assertTrue(new RemoteControlProvisioner().allRemoteControls().isEmpty());
    }

    @Test
    public void allRemoteControlsReturnsAvailableRemoteControls() {
        final RemoteControlProvisioner provisioner = new RemoteControlProvisioner();
        final RemoteControlProxy remoteControl = new HealthyRemoteControl("", 0, "", null);

        provisioner.add(remoteControl);
        assertEquals(1, provisioner.allRemoteControls().size());
        assertTrue(provisioner.allRemoteControls().contains(remoteControl));
    }

    @Test
    public void allRemoteControlsReturnsReservedRemoteControls() {
        final RemoteControlProvisioner provisioner = new RemoteControlProvisioner();
        final RemoteControlProxy remoteControl = new HealthyRemoteControl("", 0, "", null);

        provisioner.add(remoteControl);
        provisioner.reserve();
        assertEquals(1, provisioner.allRemoteControls().size());
        assertTrue(provisioner.allRemoteControls().contains(remoteControl));
    }

}
