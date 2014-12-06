package me.legrange.panstamp.impl;

import me.legrange.panstamp.def.RegisterDef;
import me.legrange.swap.Registers;
import me.legrange.swap.Registers.Register;

/**
 *
 * @author gideon
 */
public class StandardRegister extends RegisterDef {
    
    public static final StandardRegister PRODUCT_CODE = new StandardRegister(Register.PRODUCT_CODE);
    public static final StandardRegister HARDWARE_VERSION = new StandardRegister(Register.HARDWARE_VERSION);
    public static final StandardRegister FIRMWARE_VERSION = new StandardRegister(Register.FIRMWARE_VERSION);
    public static final StandardRegister SYSTEM_STATE = new StandardRegister(Register.SYSTEM_STATE);
    public static final StandardRegister FREQUENCY_CHANNEL = new StandardRegister(Register.FREQUENCY_CHANNEL);
    public static final StandardRegister SECURITY_OPTION = new StandardRegister(Register.SECURITY_OPTION);
    public static final StandardRegister SECURITY_PASSWORD = new StandardRegister(Register.SECURITY_PASSWORD);
    public static final StandardRegister SECURITY_NONCE = new StandardRegister(Register.SECURITY_NONCE);
    public static final StandardRegister NETWORK_ID = new StandardRegister(Register.NETWORK_ID);
    public static final StandardRegister DEVICE_ADDRESS = new StandardRegister(Register.DEVICE_ADDRESS);
    public static final StandardRegister PERIODIC_TX_INTERVAL = new StandardRegister(Register.PERIODIC_TX_INTERVAL);
            
     StandardRegister(Registers.Register reg) {
        super(reg.position(), reg.name());
    }
    
}