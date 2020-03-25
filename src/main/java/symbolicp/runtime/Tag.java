package symbolicp.runtime;

import java.util.Objects;

public class Tag {
    // We store a human-readable name for debugging / reporting purposes only
    private final String name;
    public final int id;

    public Tag(String name, int id) {
        this.name = name;
        this.id = id;
    }

    @Override
    public String toString() {
        return "Tag{" +
            "name='" + name + '\'' +
            ", id=" + id +
            '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Tag tag = (Tag) o;
        return id == tag.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
