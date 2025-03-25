package organizer.rule;

import java.nio.file.Path;

public interface Rule {

    boolean matches(Path file);
    boolean equals(Object obj);
    int hashCode();
}



