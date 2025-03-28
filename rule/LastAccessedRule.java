package organizer.rule;


import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.io.IOException;
import org.json.JSONObject;



public class LastAccessedRule implements Rule{
    private long timeInMillis;

    public LastAccessedRule(long timeInMillis){
        this.timeInMillis = timeInMillis;
    }

    public LastAccessedRule(JSONObject json){
        this.timeInMillis = json.getLong("Time");
    }

    @Override
    public JSONObject toJSON(){
        JSONObject json = new JSONObject();
        json.put("type","LastAccessedRule");
        json.put("Time", timeInMillis);
        return json;
    }

    @Override
    public boolean matches(Path file){
        try {
            FileTime lastAccess = (FileTime) Files.getAttribute(file, "lastAccessTime");
            return lastAccess.toMillis() > timeInMillis;
        } catch(IOException e) {
            System.err.println("Error accessing file " + file + ": " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean equals(Object obj){
        if(obj instanceof LastAccessedRule other){
            return timeInMillis == other.timeInMillis;
        }
        return false;
    }

    @Override
    public int hashCode(){
        return Long.hashCode(timeInMillis);
    }
}

