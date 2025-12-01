package ast;
import java.util.List;

// ModifiablePrimary - represents a modifiable primary expression (e.g., variable with field/index accesses)
public class ModifiablePrimary extends Expression {
    public final String baseName;
    public final List<Access> accesses; // list of field/index accesses
    
    public ModifiablePrimary(String baseName, List<Access> accesses, int line, int column) {
        super(line, column);
        this.baseName = baseName;
        this.accesses = accesses;
    }
    
    // Access - represents either a field access (.field) or an index access ([index])
    public static class Access extends ASTNode {
        public final boolean isFieldAccess; // true for field access, false for index access
        public final String fieldName; // for field access
        public final Expression index; // for array index access
        
        // Constructor for field access: .fieldName
        public Access(String fieldName, int line, int column) {
            super(line, column);
            this.isFieldAccess = true;
            this.fieldName = fieldName;
            this.index = null;
        }
        
        // Constructor for array index access: [index]
        public Access(Expression index, int line, int column) {
            super(line, column);
            this.isFieldAccess = false;
            this.fieldName = null;
            this.index = index;
        }
    }
}