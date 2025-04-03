package organizer;

import organizer.rule.Rule;
import javafx.scene.Node;

public class RuleVisual {
    private Rule rule;
    private Node visual;

    public RuleVisual(Rule rule, Node visual){
        this.rule = rule;
        this.visual = visual;
    }

    public Rule getRule(){
        return rule;
    }

    public Node getVisual(){
        return visual;
    }
    
}
