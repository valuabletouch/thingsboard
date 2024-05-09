/*
* Ahmet Ertuğrul KAYA
*/
package org.thingsboard.server.vsensor.update.service;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.event.*;
import org.thingsboard.server.dao.event.EventDao;
import org.thingsboard.server.dao.service.DataValidator;

import java.util.function.BiConsumer;
import java.util.function.Function;

@Service
@Slf4j
public class UpgradeEventService implements EventService {

    @Value("${sql.ttl.events.events_ttl:0}")
    private long ttlInSec;
    @Value("${sql.ttl.events.debug_events_ttl:604800}")
    private long debugTtlInSec;

    @Value("${event.debug.max-symbols:4096}")
    private int maxDebugEventSymbols;

    @Autowired
    public EventDao eventDao;

    @Autowired
    private DataValidator<Event> eventValidator;

    @Override
    public ListenableFuture<Void> saveAsync(Event event) {
        eventValidator.validate(event, Event::getTenantId);
        checkAndTruncateDebugEvent(event);
        return eventDao.saveAsync(event);
    }

    private void checkAndTruncateDebugEvent(Event event) {
        switch (event.getType()) {
            case DEBUG_RULE_NODE:
                RuleNodeDebugEvent rnEvent = (RuleNodeDebugEvent) event;
                truncateField(rnEvent, RuleNodeDebugEvent::getData, RuleNodeDebugEvent::setData);
                truncateField(rnEvent, RuleNodeDebugEvent::getMetadata, RuleNodeDebugEvent::setMetadata);
                truncateField(rnEvent, RuleNodeDebugEvent::getError, RuleNodeDebugEvent::setError);
                break;
            case DEBUG_RULE_CHAIN:
                RuleChainDebugEvent rcEvent = (RuleChainDebugEvent) event;
                truncateField(rcEvent, RuleChainDebugEvent::getMessage, RuleChainDebugEvent::setMessage);
                truncateField(rcEvent, RuleChainDebugEvent::getError, RuleChainDebugEvent::setError);
                break;
            case LC_EVENT:
                LifecycleEvent lcEvent = (LifecycleEvent) event;
                truncateField(lcEvent, LifecycleEvent::getError, LifecycleEvent::setError);
                break;
            case ERROR:
                ErrorEvent eEvent = (ErrorEvent) event;
                truncateField(eEvent, ErrorEvent::getError, ErrorEvent::setError);
                break;
        }
    }

    private <T extends Event> void truncateField(T event, Function<T, String> getter, BiConsumer<T, String> setter) {
        var str = getter.apply(event);
        str = StringUtils.truncate(str, maxDebugEventSymbols);
        setter.accept(event, str);
    }

    @Override
    public void migrateEvents() {
        eventDao.migrateEvents(ttlInSec > 0 ? (System.currentTimeMillis() - ttlInSec * 1000) : 0, debugTtlInSec > 0 ? (System.currentTimeMillis() - debugTtlInSec * 1000) : 0);
    }
}
