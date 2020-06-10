package symbolicp.runtime;

import symbolicp.vs.BoolUtils;
import symbolicp.vs.ValueSummary;

public class Event {
    public final EventName name;
    public final ValueSummary payload; // TODO: dynamic type checking

    public Event(EventName name, ValueSummary payload) {
        this.name = name;
        this.payload = payload;
    }

    public Event(EventName name) {
        this.name = name;
        this.payload = null;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Event) {
            Event e = (Event) obj;
            return this.name.equals(e.name) && ((this.payload == null && e.payload == null) ||
                    (e.payload.getUniverse().implies(this.payload.getUniverse()).isConstTrue() &&
                            this.payload.getUniverse()
                            .implies(BoolUtils.trueCond(this.payload.symbolicEquals(e.payload, payload.getUniverse())))
                            .isConstTrue()));
        }
        return false;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return this.name.toString();
    }
}