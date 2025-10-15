package ast;
import parser.Token;

public class UserType extends Type {
    public final String typeName;

    public UserType(String typeName, int line, int column) {
        super(line, column);
        this.typeName = typeName;
    }
}
