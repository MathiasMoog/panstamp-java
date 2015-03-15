package me.legrange.panstamp.definition;

import me.legrange.panstamp.GatewayException;

/**
 * Thrown when there is a problem with panStamp device definitions.
 * 
 * @since 1.0
 * @author Gideon le Grange https://github.com/GideonLeGrange *
 */
public abstract class DefinitionException extends GatewayException {

    public DefinitionException(String message) {
        super(message);
    }

    public DefinitionException(String message, Throwable cause) {
        super(message, cause);
    }
    
}