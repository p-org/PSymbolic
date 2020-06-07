package symbolicp.vs;

import symbolicp.util.NotImplementedException;
import symbolicp.bdd.Bdd;
import symbolicp.runtime.EventTag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EventVS {
    private PrimVS<EventTag> tag;
    private Map<EventTag, Object> payloads;

    public EventVS(PrimVS<EventTag> tag, Map<EventTag, Object> payloads) {
        this.tag = tag;
        this.payloads = payloads;
    }

    public EventVS(Bdd pc, EventTag tag, Object payload) {
        this.tag = VSOps.guard(new PrimVS<>(tag), pc);
        this.payloads = new HashMap<>();
        this.payloads.put(tag, payload);
    }

    public PrimVS<EventTag> getTag() {
        return tag;
    }

    public Object getPayload(EventTag eventTag) {
        return payloads.get(eventTag);
    }

    @Override
    public String toString() {
        return tag.guardedValues.toString();
    }
}
