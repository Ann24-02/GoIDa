public class LexerTester {
    public static void main(String[] args) {
        String[] testCases = {
            // Test 1
            """
            routine main() is
                print 42
            end
            """,
            
            // Test 2
            """
            -- This is a comment
            routine main() is
                var x : integer is 5
                var y : integer is 3
                var z : integer
                z := x * y + 2
                print z
            end
            """,
            
            // Test 3
            """
            routine main() is
                var a : integer is 10
                if a > 5 then
                    print 1
                else
                    print 0
                end
            end
            """,
            
            // Continue with other tests
        };
        
        for (int i = 0; i < testCases.length; i++) {
            System.out.println("=== Test " + (i + 1) + " ===");
            Lexer lexer = new Lexer(testCases[i]);
            Token token;
            
            do {
                token = lexer.nextToken();
                System.out.println(token);
            } while (token.type != Token.Type.EOF);
            
            System.out.println();
        }
    }
}