package ast;

/**
 * Базовый класс для типов
 */
public abstract class Type extends ASTNode {
    public Type(int line, int column) {
        super(line, column);
    }
}

/**
 * Примитивный тип: integer, real, boolean
 */
class PrimitiveType extends Type {
    public final String typeName; // "integer", "real", "boolean"
    
    public PrimitiveType(String typeName, int line, int column) {
        super(line, column);
        this.typeName = typeName;
    }
}

/**
 * Массив: array [10] integer
 */
class ArrayType extends Type {
    public final Expression size; // null для массивов без размера (параметры)
    public final Type elementType;
    
    public ArrayType(Expression size, Type elementType, int line, int column) {
        super(line, column);
        this.size = size;
        this.elementType = elementType;
    }
}

/**
 * Запись: record ... end
 */
class RecordType extends Type {
    public final List<VariableDeclaration> fields;
    
    public RecordType(List<VariableDeclaration> fields, int line, int column) {
        super(line, column);
        this.fields = fields;
    }
}

/**
 * Пользовательский тип (алиас): type MyInt is integer
 */
class UserType extends Type {
    public final String typeName;
    
    public UserType(String typeName, int line, int column) {
        super(line, column);
        this.typeName = typeName;
    }
}