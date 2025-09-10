import java.nio.file.Files;
import java.nio.file.Path;

public class Main {
    public static void main(String[] args) throws Exception {
        String src;
        if (args.length > 0) {
            src = Files.readString(Path.of(args[0]));
        } else {
            // fallback sample
            src = """
                  -- sample
                  routine main() is
                      var x : integer is 5
                      var y : integer is 3
                      print x + y
                  end
                  """;
        }

        Lexer lexer = new Lexer(src);
        Token t;
        do {
            t = lexer.nextToken();
            System.out.println(t);
        } while (t.type != Token.Type.EOF);
    }
}
