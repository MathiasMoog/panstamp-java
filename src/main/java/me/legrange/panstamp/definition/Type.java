package me.legrange.panstamp.definition;

/**
 * The data types defined for endpoint and parameter data. 
 * 
 * @since 1.0
 * @author Gideon le Grange https://github.com/GideonLeGrange *
 */
public enum Type {
    BINARY("bin"), NUMBER("num"), STRING("str"), INTEGER("int"), BSTRING("btr");

    public static Type forTag(String tag) {
        for (Type type : Type.values()) {
            if (type.tag.equals(tag)) {
                return type;
            }
        }
        return null;
    }

    private Type(String tag) {
        this.tag = tag;
    }
    private final String tag;
    
}
