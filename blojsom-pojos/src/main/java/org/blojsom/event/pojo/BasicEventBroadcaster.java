/**
 * Copyright (c) 2003-2009, David A. Czarnecki
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this list of conditions and the
 *     following disclaimer.
 * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
 *     following disclaimer in the documentation and/or other materials provided with the distribution.
 * Neither the name of "David A. Czarnecki" and "blojsom" nor the names of its contributors may be used to
 *     endorse or promote products derived from this software without specific prior written permission.
 * Products derived from this software may not be called "blojsom", nor may "blojsom" appear in their name,
 *     without prior written permission of David A. Czarnecki.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
 * CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO
 * EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
 * AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.blojsom.event.pojo;

import java.util.HashMap;
import java.util.Map;
import org.blojsom.event.Event;
import org.blojsom.event.EventBroadcaster;
import org.blojsom.event.Filter;
import org.blojsom.event.Listener;

/**
 *
 * @author owen
 */
public class BasicEventBroadcaster implements EventBroadcaster {

    private static final Filter DEFAULT_FILTER = new BasicFilter();
    private Map<Listener, Filter> _activeListeners = new HashMap<Listener, Filter>();

    @Override
    public void addListener(Listener listener) {
        addListener(listener, DEFAULT_FILTER);
    }

    @Override
    public void addListener(Listener listener, Filter filter) {
        _activeListeners.put(listener, filter);
    }

    @Override
    public void broadcastEvent(Event event) {
        for (Map.Entry<Listener, Filter> entry : _activeListeners.entrySet()) {
            if (entry.getValue().processEvent(event)) {
                entry.getKey().handleEvent(event);
            }
        }
    }

    @Override
    public void removeListener(Listener listener) {
        _activeListeners.remove(listener);
    }

    //TODO Remove method
    @Override
    public void processEvent(Event event) {
        throw new UnsupportedOperationException("Use " + getClass().getSimpleName() + ".broadcastEvent instead.");
    }
}
