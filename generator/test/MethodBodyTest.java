import java.lang.reflect.Method;
/** The harness takes the method body out of a reply; braces in strings and comments must not fool it. */
public class MethodBodyTest {
    public static void main(String[] a) throws Exception {
        Class<?> g = Class.forName("Generator");
        Method mb = g.getDeclaredMethod("methodBody", String.class);
        String[][] cases = {
            {"public Quote quote(String zone, int w) {\n  String s = \"}\";\n  return null;\n}", "\n  String s = \"}\";\n  return null;\n"},
            {"public Quote quote(String zone, int w) {\n  String s = \"{\";\n  return null;\n}", "\n  String s = \"{\";\n  return null;\n"},
            {"// called as quote(String zone, ...)\nif (x) { a(); }\nreturn q;", "// called as quote(String zone, ...)\nif (x) { a(); }\nreturn q;"},
            {"return new Quote(\"A\", 1, 600);", "return new Quote(\"A\", 1, 600);"},
            {"public Quote quote(String zone, int w) {\n  String s = \"\"\"\n  }\n  \"\"\";\n  return null;\n}", "\n  String s = \"\"\"\n  }\n  \"\"\";\n  return null;\n"},
        };
        int ok = 0;
        for (String[] c : cases) { String r = (String) mb.invoke(null, c[0]); if (r.equals(c[1])) ok++; else System.out.println("MISMATCH:\n" + r); }
        System.out.println(ok + "/" + cases.length + " methodBody cases");
        if (ok != cases.length) System.exit(1);
    }
}
