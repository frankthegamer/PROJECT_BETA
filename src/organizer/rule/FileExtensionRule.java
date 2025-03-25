package organizer.rule;

import java.nio.file.Path;
import java.util.Set;
import java.util.HashSet;

public class FileExtensionRule implements Rule{
    private Set<String> extensions;

    public FileExtensionRule(Set<String> extensions){
        this.extensions = new HashSet<>();
        for(String ext : extensions){
            if (ext.startsWith(".")) {
                this.extensions.add(ext);
            } else {
                this.extensions.add("." + ext);
            }
        }
    }

    @Override
    public boolean matches(Path file) {
        String fileName = file.toString().toLowerCase();
        return extensions.stream().anyMatch(fileName::endsWith);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof FileExtensionRule other) {
            return extensions.equals(other.extensions);
        }
        return false;
    }

    @Override
    public int hashCode(){
        return extensions.hashCode();
    }

    
}
