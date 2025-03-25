package organizer.rule;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.SAXException;
import java.util.regex.Pattern;

public class StringContainedRule implements Rule{
    private String input;
    private boolean caseSensitive;
    private boolean useRegex;

    public StringContainedRule(String input, boolean caseSensitive, boolean useRegex){
        this.input = input;
        this.caseSensitive = caseSensitive;
        this.useRegex = useRegex;
    }

    @Override
    public boolean matches(Path file){
        try {
            String text = extractText(file);
            if (text == null) {
                return false;
            }
            if (useRegex) {
                Pattern pattern = caseSensitive ? Pattern.compile(input) : Pattern.compile(input, Pattern.CASE_INSENSITIVE);
                return pattern.matcher(text).find();
            }
            String compareText = caseSensitive ? text : text.toLowerCase();
            String compareInput = caseSensitive ? input : input.toLowerCase();
            return compareText.contains(compareInput);
         } catch (IOException | SAXException | TikaException e) {
            System.err.println("Error processing file" + file + ": " + e.getMessage());
            return false;
        }
    }

    private String extractText(Path file) throws IOException, SAXException, TikaException {   // extracts text from different file types
        BodyContentHandler handler = new BodyContentHandler(-1);
        AutoDetectParser parser = new AutoDetectParser();
        Metadata metadata = new Metadata();
        try (var stream = Files.newInputStream(file)) {
            parser.parse(stream, handler, metadata);
            return handler.toString();
        }
    }


    @Override
    public boolean equals(Object obj) {
        if (obj instanceof StringContainedRule other) {
            return input.equals(other.input) && caseSensitive == other.caseSensitive && useRegex == other.useRegex;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return input.hashCode() * 31 + Boolean.hashCode(caseSensitive) * 17 + Boolean.hashCode(useRegex);
    }

    }

