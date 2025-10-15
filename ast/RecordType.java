package ast;
import java.util.List;
import parser.Token;

public class RecordType extends Type {
    public final List<VariableDeclaration> fields;

    public RecordType(List<VariableDeclaration> fields, int line, int column) {
        super(line, column);
        this.fields = fields;
    }
}
