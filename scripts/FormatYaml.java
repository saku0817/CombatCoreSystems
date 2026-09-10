import java.nio.file.*;
import java.io.*;
import java.util.*;
import org.yaml.snakeyaml.*;
import org.yaml.snakeyaml.nodes.*;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.representer.Representer;

/** Converts YAML presentation to block style, retaining comments and checking values. */
class FormatYaml {
    static void block(Node node) {
        if (node instanceof CollectionNode<?> collection) collection.setFlowStyle(DumperOptions.FlowStyle.BLOCK);
        if (node instanceof MappingNode mapping) for (NodeTuple entry : mapping.getValue()) { block(entry.getKeyNode()); block(entry.getValueNode()); }
        if (node instanceof SequenceNode sequence) for (Node child : sequence.getValue()) block(child);
    }
    public static void main(String[] args) throws Exception {
        LoaderOptions loader = new LoaderOptions(); loader.setProcessComments(true); loader.setAllowDuplicateKeys(false);
        DumperOptions options = new DumperOptions(); options.setProcessComments(true); options.setIndent(2); options.setPrettyFlow(false);
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK); options.setSplitLines(false);
        Yaml yaml = new Yaml(new SafeConstructor(loader), new Representer(options), options, loader);
        try (var paths = Files.list(Path.of(args[0]))) {
            for (Path path : paths.filter(p -> p.toString().endsWith(".yml")).sorted().toList()) {
                String before = Files.readString(path);
                Node node = yaml.compose(new StringReader(before)); block(node);
                StringWriter writer = new StringWriter(); yaml.serialize(node, writer);
                String after = writer.toString();
                if (!Objects.equals(yaml.load(before), yaml.load(after))) throw new IllegalStateException("YAML values changed: " + path);
                Files.writeString(path, after); System.out.println("Formatted with values preserved: " + path);
            }
        }
    }
}
