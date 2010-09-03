/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.blojsom.event.pojo;

import org.blojsom.event.Event;
import org.blojsom.event.EventBroadcaster;
import org.blojsom.event.Filter;
import org.blojsom.event.Listener;
import org.testng.annotations.Test;

/**
 *
 * @author owen
 */
@Test
public class BasicEventBroadcasterTest {

    public void testAddListener() {
        Listener l = new BasicListener();
        EventBroadcaster eb = new BasicEventBroadcaster();
        eb.addListener(l);
    }

    public void testAddListenerNegative() {
        EventBroadcaster eb = new BasicEventBroadcaster();
        eb.addListener(null);
    }

    public void testOverloadAddListener() {
        Listener l = new BasicListener();
        Filter f = new BasicFilter();
        EventBroadcaster eb = new BasicEventBroadcaster();
        eb.addListener(l, f);
    }

    @Test(expectedExceptions = {UnsupportedOperationException.class})
    public void testProcessMessage() {
        EventBroadcaster eb = new BasicEventBroadcaster();
        eb.processEvent(null);
    }

    public void testBroadcastMessage() {
        Event e = new BasicEvent(this);
        Listener l = new BasicListener();
        EventBroadcaster eb = new BasicEventBroadcaster();
        eb.addListener(l);
        eb.broadcastEvent(e);
        // TODO Implement mocking to ensure that e.handleEvent was called
    }

    public void testRemoveListener() {
        Event e = new BasicEvent(this);
        Listener l = new BasicListener();
        EventBroadcaster eb = new BasicEventBroadcaster();
        eb.addListener(l);
        eb.removeListener(l);
        // TODO Implement mocking to ensure that e.handleEvent was _NOT_ called
    }

    public void testRemoveListenerNegative() {
        Listener l = new BasicListener();
        EventBroadcaster eb = new BasicEventBroadcaster();
        eb.removeListener(l);
        eb.removeListener(null);
    }
}
