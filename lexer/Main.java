public class Main {
    public static void main(String[] args) {
        Lexer lexer = new Lexer("var x := 42; print(x);");
        Token t;
        do {
            t = lexer.nextToken();
            System.out.println(t);
        } while (t.type != Token.Type.EOF);
    }
}
