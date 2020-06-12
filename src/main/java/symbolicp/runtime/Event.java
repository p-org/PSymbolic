package symbolicp.runtime;

import symbolicp.bdd.Bdd;
import symbolicp.vs.BoolUtils;
import symbolicp.vs.ValueSummary;

public class Event implements SymbolicQueue.canGuard<Event> {
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

    public boolean matches(Event e) {
        return this.name.equals(e.name) && ((this.payload == null && e.payload == null) ||
                this.payload.getUniverse().and(e.payload.getUniverse()).isConstFalse());
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
        return this.name.toString() + ", payload: " + payload;
    }

    @Override
    public Event guard(Bdd pc) {
        if (payload == null) return this;
        return new Event(name, payload.guard(pc));
    }

}
