package com.github.saku0817.combatcoresystems.model.trigger;

import com.github.saku0817.combatcoresystems.model.Element;
import java.util.*;
import static com.github.saku0817.combatcoresystems.model.trigger.TriggerDefinition.*;

/** Combat-only modifiers never enter a player's persistent StatKey values. */
public final class CombatModifiers {
    public enum CriticalMode { DEFAULT, DISABLED, ENABLED, FORCED }
    private static final Set<String> DOMAINS=Set.of("damage-dealt","damage-taken","healing-dealt","healing-received","crit-rate","crit-damage","element","critical-mode");
    private static final Set<String> CATEGORIES;
    static {
        Set<String> names=new HashSet<>();for(SourceKind kind:SourceKind.values())names.add(kind.name().toLowerCase(Locale.ROOT).replace('_','-'));
        for(Element element:Element.values())names.add(element.name().toLowerCase(Locale.ROOT));CATEGORIES=Set.copyOf(names);
    }
    public record Rule(String category,String domain,String mode,Object value,Map<String,Object> scope,int priority) {}
    private static final class Scalar {double flat,percent;Double override;}
    private final Map<String,Scalar> values=new LinkedHashMap<>();
    private record Applied(Rule rule,int stacks) {}
    private final List<Applied> applied=new ArrayList<>();
    private Element evaluatedElement;
    private Element element;
    private CriticalMode criticalMode=CriticalMode.DEFAULT;
    public Element element(Element fallback) {return element==null?fallback:element;}
    public CriticalMode criticalMode() {return criticalMode;}

    public static List<Rule> parse(Map<String,Object> map) {
        List<Rule> result=new ArrayList<>();parse(map,"",Map.of(),0,result);return List.copyOf(result);
    }
    private static void parse(Map<String,Object> config,String category,Map<String,Object> inheritedScope,int inheritedPriority,List<Rule> result) {
        Map<String,Object> scope=config.containsKey("scope")?child(config,"scope"):inheritedScope;validateScope(scope);
        int priority=config.containsKey("priority")?signedInteger(config,"priority"):inheritedPriority;
        // Within one definition, category-specific overrides follow global overrides,
        // independent of YAML map order. Priority and later definitions still win.
        var entries=new ArrayList<>(config.entrySet());
        if(category.isEmpty())entries.sort(Comparator.comparingInt(entry->CATEGORIES.contains(entry.getKey().toLowerCase(Locale.ROOT).replace('_','-'))?1:0));
        for(var entry:entries) {
            String domain=entry.getKey().toLowerCase(Locale.ROOT).replace('_','-');
            if(domain.equals("scope")||domain.equals("priority"))continue;
            if(category.isEmpty()&&CATEGORIES.contains(domain)) {parse(map(entry.getValue()),domain,scope,priority,result);continue;}
            if(!DOMAINS.contains(domain))throw new IllegalArgumentException("Unknown combat modifier: "+entry.getKey());
            if(Element.parse(category).isPresent()&&!domain.equals("damage-taken"))
                throw new IllegalArgumentException("Element categories support damage-taken only; use scope.element for other domains");
            if(domain.equals("critical-mode")) {
                result.add(new Rule(category,domain,"override",CriticalMode.valueOf(String.valueOf(entry.getValue()).toUpperCase(Locale.ROOT)),scope,priority));continue;
            }
            Map<String,Object> modes=map(entry.getValue());
            if(modes.isEmpty())throw new IllegalArgumentException("Empty combat modifier: "+domain);
            for(var mode:modes.entrySet()) {
                if(!Set.of("flat","percent","override").contains(mode.getKey()))throw new IllegalArgumentException("Unknown combat modifier mode: "+mode.getKey());
                Object value;
                if(domain.equals("element")) {
                    if(!mode.getKey().equals("override"))throw new IllegalArgumentException("Element supports override only");
                    value=Element.parse(String.valueOf(mode.getValue())).orElseThrow(()->new IllegalArgumentException("Unknown element"));
                } else {
                    if(mode.getKey().equals("flat")&&!domain.startsWith("crit-"))throw new IllegalArgumentException(domain+" does not support flat");
                    value=number(modes,mode.getKey(),0);
                }
                result.add(new Rule(category,domain,mode.getKey(),value,scope,priority));
            }
        }
    }
    private static int signedInteger(Map<String,Object> m,String key) {
        double n=range(m,key,0,Integer.MIN_VALUE,Integer.MAX_VALUE);if(n!=Math.rint(n))throw new IllegalArgumentException("Expected integer "+key);return(int)n;
    }
    private static void validateScope(Map<String,Object> scope) {
        for(String key:scope.keySet())switch(key) {
            case "source-kind" -> names(scope.get(key)).forEach(value->SourceKind.valueOf(value.toUpperCase(Locale.ROOT)));
            case "source-id" -> {if(!(scope.get(key) instanceof String s)||s.isBlank())throw new IllegalArgumentException("Invalid source-id");}
            case "element" -> {if(Element.parse(text(scope,key,"")).isEmpty())throw new IllegalArgumentException("Unknown scope element");}
            case "tags" -> names(scope.get(key));
            default -> throw new IllegalArgumentException("Unknown modifier scope: "+key);
        }
    }
    public static List<String> names(Object value) {
        if(value instanceof String s&&!s.isBlank())return List.of(s);
        if(value instanceof List<?> list&&!list.isEmpty()&&list.stream().allMatch(v->v instanceof String s&&!s.isBlank()))return list.stream().map(String::valueOf).toList();
        throw new IllegalArgumentException("Expected string or nonempty string list");
    }
    public static List<Rule> action(Map<String,Object> action) {
        List<Rule> result=new ArrayList<>(parse(child(action,"modifiers")));
        Map<String,Object> critical=child(action,"critical");
        for(String key:critical.keySet())if(!Set.of("mode","rate","damage").contains(key))throw new IllegalArgumentException("Unknown critical key: "+key);
        Map<String,Object> translated=new LinkedHashMap<>();
        if(critical.containsKey("mode"))translated.put("critical-mode",critical.get("mode"));
        if(critical.containsKey("rate"))translated.put("crit-rate",critical.get("rate"));
        if(critical.containsKey("damage"))translated.put("crit-damage",critical.get("damage"));
        if(action.containsKey("element"))translated.put("element",action.get("element"));
        result.addAll(parse(translated));return List.copyOf(result);
    }
    public static List<Rule> event(Map<String,Object> action) {
        List<Rule> rules=new ArrayList<>(action(action));Map<String,Object> translated=new LinkedHashMap<>();
        for(String section:List.of("damage","healing"))for(var entry:child(action,section).entrySet()) {
            String suffix=entry.getKey();
            if(!Set.of("dealt",section.equals("damage")?"taken":"received").contains(suffix))throw new IllegalArgumentException("Unknown "+section+" modifier: "+suffix);
            translated.put(section+"-"+suffix,entry.getValue());
        }
        rules.addAll(parse(translated));return List.copyOf(rules);
    }
    public void apply(List<Rule> rules,EventContext context,int stacks) {
        if(evaluatedElement!=context.attribute)rebuild(context);
        rules.stream().sorted(Comparator.comparingInt(Rule::priority)).forEach(rule->{
            var entry=new Applied(rule,stacks);applied.add(entry);applyOne(entry,context);
        });
    }
    private void rebuild(EventContext context) {
        values.clear();element=null;criticalMode=CriticalMode.DEFAULT;evaluatedElement=context.attribute;
        applied.forEach(entry->applyOne(entry,context));
    }
    private void applyOne(Applied entry,EventContext context) {
            Rule rule=entry.rule();int stacks=entry.stacks();
            String kind=context.sourceKind.name().toLowerCase(Locale.ROOT).replace('_','-');
            if(!rule.category().isEmpty()&&!rule.category().equals(kind)&&!rule.category().equals(context.attribute.name().toLowerCase(Locale.ROOT)))return;
            Map<String,Object> scope=rule.scope();
            if(scope.containsKey("source-kind")&&names(scope.get("source-kind")).stream().noneMatch(v->v.equalsIgnoreCase(context.sourceKind.name())))return;
            if(scope.containsKey("source-id")&&!scope.get("source-id").equals(context.sourceId))return;
            if(scope.containsKey("element")&&Element.parse(text(scope,"element","")).orElse(null)!=context.attribute)return;
            if(scope.containsKey("tags")&&!context.tags.containsAll(names(scope.get("tags"))))return;
            if(rule.domain().equals("element")){element=(Element)rule.value();return;}
            if(rule.domain().equals("critical-mode")){criticalMode=(CriticalMode)rule.value();return;}
            Scalar scalar=values.computeIfAbsent(rule.category()+":"+rule.domain(),k->new Scalar());
            double n=((Number)rule.value()).doubleValue();
            switch(rule.mode()) {case "flat"->scalar.flat+=n*stacks;case "percent"->scalar.percent+=n*stacks;case "override"->scalar.override=n;default->throw new IllegalArgumentException("mode");}
    }
    private double value(String category,String domain,double base) {
        Scalar scalar=values.get(category+":"+domain);return scalar==null?base:scalar.override!=null?scalar.override:base+scalar.flat+scalar.percent;
    }
    public double factor(String domain,EventContext context) {
        if(evaluatedElement!=context.attribute)rebuild(context);
        String category=context.sourceKind.name().toLowerCase(Locale.ROOT).replace('_','-');
        double result=Math.max(0,1+value("",domain,0))*Math.max(0,1+value(category,domain,0));
        if(domain.equals("damage-taken"))result*=Math.max(0,1+value(context.attribute.name().toLowerCase(Locale.ROOT),domain,0));
        return result;
    }
    public double critical(String domain,double base,EventContext context) {
        if(evaluatedElement!=context.attribute)rebuild(context);
        return value(context.sourceKind.name().toLowerCase(Locale.ROOT).replace('_','-'),domain,value("",domain,base));
    }
}
