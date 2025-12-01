package ast;

// UserType - represents a user-defined type by its name
public class UserType extends Type {
    public final String typeName;

    public UserType(String typeName, int line, int column) {
        super(line, column);
        this.typeName = typeName;
    }
}