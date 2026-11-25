// The harness: spec + framework context -> local model -> one slot file -> the service's own build decides.
// Run with:  java generator/Generator.java --run-id r1 [--temperature 0] [--seed 1] [--attempts 3]
// Exit code: 0 the build passed, 1 it failed after every attempt, 2 not run (the model could not be reached).
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Generator {

    static String arg(String[] a, String name, String dflt) {
        for (int i = 0; i < a.length - 1; i++) if (a[i].equals("--" + name)) return a[i + 1];
        return System.getenv().getOrDefault("WRE_" + name.toUpperCase().replace('-', '_'), dflt);   // e.g. WRE_OLLAMA_URL
    }

    public static void main(String[] args) throws Exception {
        String ollama = arg(args, "ollama-url", "http://localhost:11434");
        String model = arg(args, "model", "local-model");
        String runId = arg(args, "run-id", "run-" + System.currentTimeMillis());
        double temperature = Double.parseDouble(arg(args, "temperature", "0"));
        int seed = Integer.parseInt(arg(args, "seed", "1"));
        int attempts = Integer.parseInt(arg(args, "attempts", "3"));
        Path root = Path.of(arg(args, "root", "."));
        Path service = root.resolve(arg(args, "service", "services/shipping-quote"));
        Path slot = service.resolve("src/main/java/com/enrichmeai/ref/shipping/ShippingQuoteService.java");
        Path template = service.resolve("slots/ShippingQuoteService.java.template");
        String mvnFlags = arg(args, "mvn-flags", "-q -B");
        Path runDir = root.resolve("runs").resolve(runId);
        Files.createDirectories(runDir);

        String context = Files.readString(root.resolve("generator/framework-context.md"));
        String spec = Files.readString(root.resolve("specs/001-shipping-quote/spec.md"));
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", context));
        messages.add(Map.of("role", "user", "content", "Write the body of quote(zone, weightKg) for this spec.\n\n" + spec));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("runId", runId); result.put("model", model);
        result.put("temperature", temperature); result.put("seed", seed);
        List<Map<String, Object>> tries = new ArrayList<>();
        result.put("attempts", tries);
        int code = 1;

        for (int attempt = 1; attempt <= attempts; attempt++) {
            Map<String, Object> t = new LinkedHashMap<>();
            tries.add(t);
            String reply;
            long t0 = System.nanoTime();
            try {
                reply = chat(ollama, model, messages, temperature, seed, t);
            } catch (IOException e) {
                t.put("error", "model not reachable: " + e);
                code = 2;
                break;
            }
            t.put("modelSeconds", round((System.nanoTime() - t0) / 1e9));
            String body = methodBody(extractJava(reply));
            t.put("sha256", sha256(body));
            String java = fill(Files.readString(template), body);
            Files.writeString(slot, java);
            Files.writeString(runDir.resolve("attempt-" + attempt + ".java"), java);

            long b0 = System.nanoTime();
            Build b;
            try {
                b = build(service, mvnFlags);
            } catch (IOException e) {
                t.put("error", "could not start the build: " + e);   // no verdict on the code: NOT RUN, not FAIL
                code = 2;
                break;
            }
            t.put("buildSeconds", round((System.nanoTime() - b0) / 1e9));
            t.put("passed", b.ok);
            Files.writeString(runDir.resolve("attempt-" + attempt + "-build.txt"), b.output);
            System.out.printf("attempt %d: %s (model %.1fs, build %.1fs)%n", attempt, b.ok ? "PASS" : "FAIL",
                    t.get("modelSeconds"), t.get("buildSeconds"));
            if (b.ok) { code = 0; break; }
            String why = failures(service);
            System.out.print(why.isEmpty() ? head(b.output, 1500) + "\n" : why);   // visible in the Job's log too
            messages.add(Map.of("role", "assistant", "content", reply));
            messages.add(Map.of("role", "user", "content",
                    "The build failed. Reply with the corrected method body only.\n\n" + head(b.output, 3500)));
        }
        result.put("passed", code == 0);
        result.put("exit", code);
        Files.writeString(runDir.resolve("result.json"), json(result));
        System.out.println(code == 0 ? "PASS" : code == 2 ? "NOT RUN" : "FAIL");
        System.exit(code);
    }

    static String chat(String url, String model, List<Map<String, String>> messages, double temperature, int seed,
                       Map<String, Object> t) throws IOException, InterruptedException {
        StringBuilder msgs = new StringBuilder("[");
        for (int i = 0; i < messages.size(); i++) {
            if (i > 0) msgs.append(',');
            msgs.append("{\"role\":").append(q(messages.get(i).get("role")))
                .append(",\"content\":").append(q(messages.get(i).get("content"))).append('}');
        }
        msgs.append(']');
        String body = "{\"model\":" + q(model) + ",\"messages\":" + msgs + ",\"stream\":false,\"think\":false,"
                + "\"options\":{\"temperature\":" + temperature + ",\"seed\":" + seed + "}}";
        HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        HttpRequest req = HttpRequest.newBuilder(URI.create(url + "/api/chat"))
                .timeout(Duration.ofMinutes(15)).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build();
        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() != 200) throw new IOException("HTTP " + res.statusCode() + ": " + res.body());
        t.put("promptTokens", number(res.body(), "prompt_eval_count"));
        t.put("outputTokens", number(res.body(), "eval_count"));
        String raw = stringValue(res.body(), "content");
        if (raw == null) throw new IOException("no content in reply");
        return unescape(raw);
    }

    record Build(boolean ok, String output) {}

    static Build build(Path service, String flags) throws IOException, InterruptedException {
        List<String> cmd = new ArrayList<>(List.of("mvn"));
        cmd.addAll(List.of(flags.split(" ")));
        cmd.add("clean");   // never judge a slot by classes left over from an earlier build
        cmd.add("verify");
        Process p = new ProcessBuilder(cmd).directory(service.toFile()).redirectErrorStream(true).start();
        String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        boolean ok = p.waitFor() == 0;
        return new Build(ok, ok ? out : failures(service) + "\n" + out);
    }

    static String extractJava(String reply) {
        Matcher m = Pattern.compile("```(?:java)?\\s*\\n(.*?)```", Pattern.DOTALL | Pattern.CASE_INSENSITIVE).matcher(reply);
        return (m.find() ? m.group(1) : reply).strip() + "\n";
    }

    /** The raw (still escaped) value of the first string field named key: a plain scan, no regex. */
    static String stringValue(String json, String key) {
        int i = json.indexOf("\"" + key + "\":\"");
        if (i < 0) return null;
        int start = i + key.length() + 4;
        for (int j = start; j < json.length(); j++) {
            char c = json.charAt(j);
            if (c == '\\') { j++; continue; }
            if (c == '"') return json.substring(start, j);
        }
        return null;
    }

    /** The failing tests and their messages, read from the Surefire reports: clearer feedback than raw Maven output. */
    static String failures(Path service) throws IOException {
        Path reports = service.resolve("target/surefire-reports");
        if (!Files.isDirectory(reports)) return "";
        StringBuilder b = new StringBuilder();
        try (var files = Files.list(reports)) {
            for (Path f : files.filter(x -> x.getFileName().toString().endsWith(".xml")).sorted().toList()) {
                String xml = Files.readString(f);
                Matcher tc = Pattern.compile("<testcase name=\"([^\"]+)\"[^>]*>(.*?)</testcase>", Pattern.DOTALL).matcher(xml);
                while (tc.find()) {
                    Matcher fail = Pattern.compile("<(failure|error)\\b([^>]*)>").matcher(tc.group(2));
                    if (!fail.find()) continue;
                    Matcher msg = Pattern.compile("message=\"([^\"]*)\"").matcher(fail.group(2));
                    String text = msg.find() ? msg.group(1) : "(no message; see the build output)";
                    b.append("FAILED ").append(tc.group(1)).append(": ").append(text.replace("&#10;", " ")
                            .replace("&quot;", "\"").replace("&apos;", "'").replace("&lt;", "<").replace("&gt;", ">")
                            .replace("&amp;", "&")).append('\n');
                }
            }
        }
        return b.length() == 0 ? "" : "Failing tests (acceptance criteria from the spec, and the platform rules):\n" + b;
    }

    /**
     * Small models often repeat the method (or the whole class) even when asked for the body only.
     * If the reply declares quote(...), keep only what is between that method's braces.
     */
    static String methodBody(String code) {
        String visible = blankStringsAndComments(code);   // same length; braces in strings or comments no longer count
        Matcher sig = Pattern.compile("\\bquote\\s*\\(\\s*String\\b").matcher(visible);
        if (!sig.find()) return code;
        int open = visible.indexOf('{', sig.end());
        if (open < 0) return code;
        int depth = 0;
        for (int i = open; i < visible.length(); i++) {
            char c = visible.charAt(i);
            if (c == '{') depth++;
            else if (c == '}' && --depth == 0) return code.substring(open + 1, i);
        }
        return code;
    }

    /** Replace the contents of string and char literals and of comments with spaces, keeping every position. */
    static String blankStringsAndComments(String code) {
        char[] out = code.toCharArray();
        int i = 0, n = out.length;
        while (i < n) {
            char c = out[i];
            if (c == '/' && i + 1 < n && out[i + 1] == '/') {
                while (i < n && out[i] != '\n') out[i++] = ' ';
            } else if (c == '/' && i + 1 < n && out[i + 1] == '*') {
                out[i++] = ' '; out[i++] = ' ';
                while (i < n && !(out[i] == '*' && i + 1 < n && out[i + 1] == '/')) { if (out[i] != '\n') out[i] = ' '; i++; }
                if (i < n) { out[i++] = ' '; if (i < n) out[i++] = ' '; }
            } else if (c == '"' && i + 2 < n && out[i + 1] == '"' && out[i + 2] == '"') {   // a text block
                i += 3;
                while (i < n && !(out[i] == '"' && i + 2 < n && out[i + 1] == '"' && out[i + 2] == '"')) {
                    if (out[i] == '\\' && i + 1 < n) out[i++] = ' ';
                    if (out[i] != '\n') out[i] = ' ';
                    i++;
                }
                i += 3;
            } else if (c == '"' || c == '\'') {
                char quote = c;
                i++;
                while (i < n && out[i] != quote && out[i] != '\n') {
                    if (out[i] == '\\' && i + 1 < n) { out[i++] = ' '; }
                    out[i++] = ' ';
                }
                i++;
            } else {
                i++;
            }
        }
        return new String(out);
    }

    /** Put the model's statements into the template's slot, indented to match. */
    static String fill(String template, String body) {
        String indented = body.strip().lines().map(l -> l.isBlank() ? "" : "        " + l).reduce((a, b) -> a + "\n" + b).orElse("");
        return template.replace("        // SLOT: quote", indented);
    }

    static String head(String s, int n) { return s.length() <= n ? s : s.substring(0, n); }

    static double round(double d) { return Math.round(d * 10) / 10.0; }

    static long number(String json, String key) {
        Matcher m = Pattern.compile("\"" + key + "\":(\\d+)").matcher(json);
        return m.find() ? Long.parseLong(m.group(1)) : -1;
    }

    static String sha256(String s) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8)));
    }

    static String q(String s) {
        StringBuilder b = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> b.append("\\\"");
                case '\\' -> b.append("\\\\");
                case '\n' -> b.append("\\n");
                case '\r' -> b.append("\\r");
                case '\t' -> b.append("\\t");
                default -> { if (c < 0x20) b.append(String.format("\\u%04x", (int) c)); else b.append(c); }
            }
        }
        return b.append('"').toString();
    }

    static String unescape(String s) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c != '\\' || i + 1 >= s.length()) { b.append(c); continue; }
            char n = s.charAt(++i);
            switch (n) {
                case 'n' -> b.append('\n');
                case 't' -> b.append('\t');
                case 'r' -> b.append('\r');
                case 'b' -> b.append('\b');
                case 'f' -> b.append('\f');
                case 'u' -> {
                    if (i + 4 >= s.length()) { b.append("\\u"); break; }
                    b.append((char) Integer.parseInt(s.substring(i + 1, i + 5), 16));
                    i += 4;
                }
                default -> b.append(n);
            }
        }
        return b.toString();
    }

    static String json(Object o) {
        if (o instanceof Map<?, ?> m) {
            StringBuilder b = new StringBuilder("{");
            m.forEach((k, v) -> b.append(b.length() > 1 ? "," : "").append(q(k.toString())).append(':').append(json(v)));
            return b.append('}').toString();
        }
        if (o instanceof List<?> l) {
            StringBuilder b = new StringBuilder("[");
            for (Object v : l) b.append(b.length() > 1 ? "," : "").append(json(v));
            return b.append(']').toString();
        }
        if (o instanceof String s) return q(s);
        return String.valueOf(o);
    }
}
