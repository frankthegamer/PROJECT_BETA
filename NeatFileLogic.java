package organizer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.io.IOException;
import java.nio.file.*;

public class NeatFileLogic {
    private final Set<Group> groups = Collections.synchronizedSet(new HashSet<>());

    public NeatFileLogic(){
        this.groups = new HashSet<>();
    }

    public boolean addGroup(Group group){

        // checks for conflicting target directories for same rules and watch directories
        for(Group existing : groups){
            if(existing.getRules().equals(group.getRules()) &&
            existing.getWatchDirectories().equals(group.getWatchDirectories()) &&
            !existing.getTargetDirectory().equals(group.getTargetDirectory())){
            
            System.out.println("Error: A group with the same rules and watch directories but different target (" + 
                existing.getTargetDirectory() + " vs. " + group.getTargetDirectory() + ") already exists!");
            return false;
            }
        }

       // checks for EXACT duplicate group
        if(groups.contains(group)){
            System.out.println("Error: A group with the same rules, watch directories, and target already exists!");
            return false;
        }
        groups.add(group);
        return true;
    }
    
    public void processFile(Path file){    
        List<Group> matchingGroups = new ArrayList<>();  // array for storing groups that matching the file (determine conflicts or not)
        for(Group group : groups) {    // check if file is in one of group's watch directories + satisfies all group criteria
            boolean inWatchDir = group.getWatchDirectories().stream().anyMatch(watchDir -> file.startsWith(watchDir));
            if (inWatchDir && group.matches(file)){
                matchingGroups.add(group);     
            }
        }
        if (matchingGroups.isEmpty()){  // no match
            return;
        }
        if (matchingGroups.size() > 1 ) {  // checks if matching groups have same target directory
            Path target = matchingGroups.get(0).getTargetDirectory();
            boolean conflict = matchingGroups.stream().anyMatch(g -> !g.getTargetDirectory().equals(target));
            if (conflict) {
                System.out.println("Conflict: File "+ file + " matches multiple groups with different targets: " + 
                    matchingGroups.stream()
                        .map(g -> g.getTargetDirectory().toString())
                        .collect(Collectors.joining(", ")));
                return; // then don't move file
            }    
        }

        // moves file to target of first matching group                          
        Group group = matchingGroups.get(0);
        Path targetDir = group.getTargetDirectory();
        Path targetFile = targetDir.resolve(file.getFileName());
        try{
            Files.createDirectories(targetDir);  // create if dir doesn't exist
            // Move file
            Files.move(file, targetFile, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Moved " + file + " to " + targetFile);
        } catch (IOException e){
            System.out.println("Failed to move " + file + " to " + targetFile + ": " + e.getMessage());
        }
    }
}


    