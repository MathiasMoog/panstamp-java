package me.legrange.panstamp.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Level;
import java.util.logging.Logger;
import me.legrange.panstamp.Gateway;
import me.legrange.panstamp.GatewayException;
import me.legrange.panstamp.GatewayListener;
import me.legrange.panstamp.NodeNotFoundException;
import me.legrange.panstamp.PanStamp;
import me.legrange.panstamp.def.ClassLoaderLibrary;
import me.legrange.panstamp.def.Device;
import me.legrange.panstamp.def.DeviceLibrary;
import me.legrange.swap.SwapMessage;
import me.legrange.swap.MessageListener;
import me.legrange.swap.Registers;
import me.legrange.swap.SWAPException;
import me.legrange.swap.SWAPModem;
import me.legrange.swap.UserMessage;

/**
 * A gateway connecting a PanStampImpl network to your code using the
 * PanStampImpl modem connected to the local serial port.
 *
 * @author gideon
 */
public final class SerialGateway extends Gateway {

    public SerialGateway(SWAPModem modem) throws ModemException {
        lib = new ClassLoaderLibrary();
        this.modem = modem;
        receiver = new Receiver();
        modem.addListener(receiver);
    }

    /**
     * Disconnect from the modem and close the gateway
     *
     * @throws me.legrange.panstamp.impl.ModemException
     */
    @Override
    public void close() throws ModemException {
        try {
            modem.close();
        } catch (SWAPException ex) {
            throw new ModemException(ex.getMessage(), ex);

        }
        devices.clear();
    }

    /**
     * use to check if a device with the given address is known
     *
     * @param address Address of the device we're looking for.
     * @return True if a device with the given address is known
     */
    @Override
    public boolean hasDevice(int address) {
        synchronized (devices) {
            return devices.get(address) != null;
        }
    }

    /**
     * return the device with the given name
     *
     * @param address Address of device to fins
     * @return The device
     * @throws me.legrange.panstamp.NodeNotFoundException
     */
    @Override
    public PanStamp getDevice(int address) throws NodeNotFoundException {
        synchronized (devices) {
            PanStampImpl dev = devices.get(address);
            if (dev == null) {
                throw new NodeNotFoundException(String.format("No device found for address %02x", address));
            }
            return dev;
        }
    }

    /**
     * return the devices associated with this gateway
     *
     * @return The collection of devices
     */
    @Override
    public List<PanStamp> getDevices() {
        List<PanStamp> res = new ArrayList<>();
        res.addAll(devices.values());
        return res;
    }

    @Override
    public void addListener(GatewayListener l) {
        listeners.add(l);
    }

    @Override
    public void removeListener(GatewayListener l) {
        listeners.remove(l);
    }

    @Override
    public SWAPModem getSWAPModem() {
        return modem;
    }

    /**
     * send a command message to a remote device
     */
    void sendCommandMessage(PanStampImpl dev, int register, byte[] value) throws ModemException {
        UserMessage msg = new UserMessage(SwapMessage.Type.COMMAND, 0xFF, dev.getAddress(), register, value);
        msg.setRegisterAddress(dev.getAddress());
        send(msg);
    }

    /**
     * send a query message to a remote device
     */
    void sendQueryMessage(PanStampImpl dev, int register) throws ModemException {
        UserMessage msg = new UserMessage(SwapMessage.Type.QUERY, 0xFF, dev.getAddress(), register, new byte[]{});
          msg.setRegisterAddress(dev.getAddress());
        send(msg);
    }

    Device getDeviceDefinition(int manId, int prodId) throws GatewayException {
        return lib.findDevice(manId, prodId);
    }

    /**
     * send a message to a mote
     */
    void send(SwapMessage msg) throws ModemException {
        try {
            modem.send(msg);
        } catch (SWAPException ex) {
            throw new ModemException(ex.getMessage(), ex);
        }
    }

    /**
     * update the network based on a received message
     */
    private void updateNetwork(SwapMessage msg) throws ModemException {
        int address = msg.getSender();
        boolean isNew = false;
        PanStampImpl dev;
        synchronized (devices) {
            dev = devices.get(address);
            if (dev == null) {
                dev = new PanStampImpl(this, address);
                devices.put(address, dev);
                isNew = true;
            }
        }
        if (!dev.getRegister(Registers.Register.PRODUCT_CODE.position()).hasValue()) {
            dev.sendQueryMessage(Registers.Register.PRODUCT_CODE.position());
        }
        if (isNew) {
            fireEvent(dev);
        }
    }

    private void fireEvent(PanStamp ps) {
        for (GatewayListener l : listeners) {
            pool.submit(new ListenerTask(l, ps));
        }
    }

    /**
     * process a status message received from the modem
     */
    private void processStatusMessage(SwapMessage msg) {
        try {
            updateNetwork(msg);
            PanStampImpl dev = (PanStampImpl) getDevice(msg.getRegisterAddress());
            dev.statusMessageReceived(msg.getRegisterID(), msg.getRegisterValue());
        } catch (GatewayException ex) {
            java.util.logging.Logger.getLogger(SerialGateway.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    private final SWAPModem modem;
    private final Receiver receiver;
    private final Map<Integer, PanStampImpl> devices = new HashMap<>();
    private final List<GatewayListener> listeners = new LinkedList<>();
    private static final Logger logger = Logger.getLogger(SerialGateway.class.getName());
    private final DeviceLibrary lib;
    private final ExecutorService pool = Executors.newCachedThreadPool(new ThreadFactory() {

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "SerialGateway Task");
            t.setDaemon(true);
            return t;
        }
    });

    /**
     * A receiver for incoming messages
     */
    private class Receiver implements MessageListener {

        @Override
        public void messageReceived(SwapMessage msg) {
            switch (msg.getType()) {
                case COMMAND:
                case QUERY:
                    try {
                        updateNetwork(msg);
                    } catch (ModemException ex) {
                        Logger.getLogger(SerialGateway.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    break;
                case STATUS:
                    processStatusMessage(msg);
                    break;
            }

        }

        @Override
        public void messageSent(SwapMessage msg) {
        }
    }

    /**
     * A runnable task that sends a panStamp to a listener
     */
    private class ListenerTask implements Runnable {

        private ListenerTask(GatewayListener l, PanStamp ps) {
            this.l = l;
            this.ps = ps;
        }

        @Override
        public void run() {
            try {
                l.deviceDetected(ps);
            } catch (Throwable e) {
                Logger.getLogger(SerialGateway.class.getName()).log(Level.SEVERE, null, e);

            }
        }

        private final GatewayListener l;
        private final PanStamp ps;
    }
}
