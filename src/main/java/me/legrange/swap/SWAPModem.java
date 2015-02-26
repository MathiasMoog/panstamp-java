package me.legrange.swap;

/**
 * A virtual modem that provides access to a SWAP transport. Currently we
 * implement two kinds, serial and TCP/IP.
 *
 * @author gideon
 */
public interface SWAPModem {

    /** Type of SWAP modem. Currently only serial and TCP_IP are supported, and it's unlikely to change. */
    public enum Type {

        SERIAL, TCP_IP
    };
    
    /** 
     * connect to the modem. 
     * @throws me.legrange.swap.SWAPException Thrown if there is a problem opening the modem.
     */
    void open() throws SWAPException;

    /**
     * disconnect and close the modem
     *
     * @throws me.legrange.swap.SWAPException
     */
    void close() throws SWAPException;
    
    /** 
     * Check if the modem is open (connected to it's implementation device) 
     * @return true if the modem is connected
     * 
     */
    boolean isOpen();

    /**
     * send a message out onto the network
     *
     * @param msg Message to send.
     * @throws me.legrange.swap.SWAPException
     */
    void send(SwapMessage msg) throws SWAPException;

    /**
     * add a message listener to receive messages
     *
     * @param l listener to add.
     */
    void addListener(MessageListener l);

    /**
     * remove a listener
     *
     * @param l listener to remove
     */
    void removeListener(MessageListener l);

    /**
     * get the network setup
     *
     * @return The setup data
     * @throws me.legrange.swap.SWAPException Thrown if there is a problem
     * retrieving the setup
     */
    ModemSetup getSetup() throws SWAPException;

    /**
     * set the network setup
     *
     * @param setup The modem setup to apply
     * @throws me.legrange.swap.SWAPException Thrown if there is a problem
     * applying the setup
     */
    void setSetup(ModemSetup setup) throws SWAPException;

    /**
     * determine the type of virtual modem
     *
     * @return The type of the modem
     */
    Type getType();

}
