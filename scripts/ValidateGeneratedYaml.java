import java.nio.file.*;
import java.util.*;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.constructor.SafeConstructor;

/** Independent parser check of the files downloaded by the browser test. */
class ValidateGeneratedYaml {
    public static void main(String[] args) throws Exception {
        LoaderOptions options = new LoaderOptions(); options.setAllowDuplicateKeys(false);
        Yaml yaml = new Yaml(new SafeConstructor(options));
        Path directory = Path.of("server/v142-web-evidence/generated");
        int count = 0;
        try (var files = Files.list(directory)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".yml")).toList()) {
                Map<?, ?> parsed = yaml.load(Files.readString(file));
                if (!Integer.valueOf(1).equals(parsed.get("data-version"))) throw new AssertionError(file + " data-version");
                if (file.getFileName().toString().equals("weapon-weapons.yml")) {
                    Map<?, ?> weapon = (Map<?, ?>) ((Map<?, ?>) parsed.get("weapons")).get("new_weapon");
                    if (!"HP".equals(((Map<?, ?>) weapon.get("skill")).get("reference"))) throw new AssertionError("skill reference");
                    if (((List<?>) weapon.get("lore")).size() != 2) throw new AssertionError("multiline lore");
                    if (!weapon.get("name").toString().contains("\" と :")) throw new AssertionError("quoted name");
                }
                if (file.getFileName().toString().equals("nested.yml")) {
                    var components = (List<?>) parsed.get("components");
                    if (!(components.getFirst() instanceof Map<?, ?> first) || !Integer.valueOf(2).equals(first.get("multiplier"))) throw new AssertionError("nested YAML objects");
                }
                count++; System.out.println("PASS parsed YAML: " + file.getFileName());
            }
        }
        if (count != 10) throw new AssertionError("Expected 10 generated fixtures, found " + count);
    }
}
