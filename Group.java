package organizer;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import organizer.rule.Rule;

public class Group {
    private Set<Rule> rules;
    private Set<Path> watchDirectories;
    private Path targetDirectory;

    public Group(Set<Path> watchDirectories, Path targetDirectory){
        this.rules = new HashSet<>();
        this.watchDirectories = new HashSet<>(watchDirectories);
        this.targetDirectory = targetDirectory;
    }

    public void addRule(Rule rule){
        rules.add(rule);  
    }

    public void addWatchDirectory(Path directory){
        watchDirectories.add(directory);
    }

    public boolean matches(Path file){
        return rules.stream().allMatch(rule -> rule.matches(file));
    }

    public Set<Rule> getRules(){
        return new HashSet<>(rules);
    }

    public Set<Path> getWatchDirectories(){
        return new HashSet<>(watchDirectories);
    }

    public Path getTargetDirectory(){
        return targetDirectory;
    }

    @Override
    public boolean equals(Object obj){    // ensures distinct group rules, watch directories or target directory
        if(obj instanceof Group other){
            return rules.equals(other.rules) &&
                watchDirectories.equals(other.watchDirectories) &&
                targetDirectory.equals(other.targetDirectory);
        }
        return false;
    }

    @Override
    public int hashCode(){
        int result = rules.hashCode();
        result = 31 * result + watchDirectories.hashCode();
        result = 31 * result + targetDirectory.hashCode();
        return result;
    }
}


