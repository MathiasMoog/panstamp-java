package me.legrange.panstamp.core;

import me.legrange.panstamp.GatewayException;
import me.legrange.panstamp.def.Param;

/**
 *
 * @author gideon
 */
class StringParameter extends AbstractParameter<String> {

    public StringParameter(RegisterImpl reg, Param epDef) {
        super(reg, epDef);
        
    }

    @Override
    public Type getType() {
        return Type.STRING;
    }
    
   
    @Override
    public String getValue() throws GatewayException {
        byte bytes[]  = reg.getValue();
        byte keep[] = new byte[par.getSize().getBytes()];
        System.arraycopy(bytes, par.getPosition().getBytePos(), keep, 0, par.getSize().getBytes());
        return new String(keep);
    }
  
    @Override
    public void setValue(String value) throws GatewayException {
        int len = par.getSize().getBytes();
        if (value.length() > len) {
            value = value.substring(0, len -1);
        } 
        else {
        }
        byte bytes[] = new byte[len];
        System.arraycopy(value.getBytes(), 0, bytes, par.getPosition().getBytePos(), value.length());
    }

    @Override
    public String getDefault() {
        return par.getDefault();
    }
    
    
    
}