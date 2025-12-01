package ast;
import java.util.List;

// RecordType - represents a record type with a list of field declarations
public class RecordType extends Type {
    public final List<VariableDeclaration> fields;

    public RecordType(List<VariableDeclaration> fields, int line, int column) {
        super(line, column);
        this.fields = fields;
    }
}