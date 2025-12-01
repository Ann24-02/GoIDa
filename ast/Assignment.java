package ast;

// Assignment - represents an assignment statement with a target and value
public class Assignment extends Statement {
    public final ModifiablePrimary target;
    public final Expression value;

    public Assignment(ModifiablePrimary target, Expression value, int line, int column) {
        super(line, column);
        this.target = target;
        this.value = value;
    }
}