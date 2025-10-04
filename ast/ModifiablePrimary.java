package ast;

/**
 * Изменяемый первичный элемент (левая часть присваивания): x, arr[i], rec.field
 */
public class ModifiablePrimary extends Expression {
    public final String baseName;
    public final List<Access> accesses; // Доступы к полям/элементам
    
    public ModifiablePrimary(String baseName, List<Access> accesses, int line, int column) {
        super(line, column);
        this.baseName = baseName;
        this.accesses = accesses;
    }
    
    /**
     * Элемент доступа: поле или индекс массива
     */
    public static class Access extends ASTNode {
        public final boolean isFieldAccess; // true для .field, false для [index]
        public final String fieldName;      // для доступа к полю
        public final Expression index;      // для доступа к массиву
        
        // Конструктор для доступа к полю: .fieldName
        public Access(String fieldName, int line, int column) {
            super(line, column);
            this.isFieldAccess = true;
            this.fieldName = fieldName;
            this.index = null;
        }
        
        // Конструктор для доступа к массиву: [index]
        public Access(Expression index, int line, int column) {
            super(line, column);
            this.isFieldAccess = false;
            this.fieldName = null;
            this.index = index;
        }
    }
}