package me.legrange.swap;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An interface providing access through the serial port to the SWAP modem.
 *
 * @since 1.0
 * @author Gideon le Grange https://github.com/GideonLeGrange *
 */
public final class SerialModem implements SwapModem {

    public SerialModem(String port, int baud) {
        this.port = port;
        this.baud = baud;
    }

    @Override
    public void open() throws SwapException {
        com = ComPort.open(port, baud);
        running = true;
        reader = new Reader();
        reader.setDaemon(true);
        reader.setName(String.format("%s Reader Thread", getClass().getSimpleName()));
        reader.start();
        if (setup != null) {
            setSetup(setup);
        }
    }

    @Override
    public void close() throws SerialException {
        running = false;
        com.close();
    }

    @Override
    public boolean isOpen() {
        return running;
    }

    @Override
    public synchronized void send(SwapMessage msg) throws SerialException {
        send(msg.getText() + "\r");
        fireEvent(msg, ReceiveTask.Direction.OUT);
    }

    @Override
    public void addListener(MessageListener l) {
        synchronized (listeners) {
            listeners.add(l);
        }
    }

    @Override
    public void removeListener(MessageListener l) {
        synchronized (listeners) {
            listeners.remove(l);
        }
    }

    @Override
    public ModemSetup getSetup() throws SerialException {
        if (setup == null) {
            if (running) {
                synchronized (this) {
                    enterCommandMode();
                    setup = new ModemSetup(readATasInt("ATCH?"), readATasHex("ATSW?"),
                            readATasHex("ATDA?"));
                    leaveCommandMode();
                }
            } else {
                setup = new ModemSetup(0, 0, 0);
            }
        }
        return setup;
    }

    @Override
    public void setSetup(ModemSetup setup) throws SerialException {
        synchronized (this) {
            if (running) {
                enterCommandMode();
                sendATCommand(String.format("ATCH=%2d", setup.getChannel()));
                sendATCommand(String.format("ATSW=%4h", setup.getNetworkID()));
                sendATCommand(String.format("ATDA=%2d", setup.getDeviceAddress()));
                leaveCommandMode();
            }
            this.setup = setup;
        }
    }

    @Override
    public Type getType() {
        return Type.SERIAL;
    }

    /**
     * Get the name of the serial port used by this modem.
     *
     * @return The name of the serial port
     */
    public String getPort() {
        return port;
    }

    /**
     * Get the serial speed this modem connects at
     *
     * @return The serial speed
     */
    public int getBaud() {
        return baud;
    }

    private int readATasInt(String cmd) throws SerialException {
        String res = readAT(cmd);
        try {
            return Integer.parseInt(res);
        } catch (NumberFormatException e) {
            throw new SerialException(String.format("Malformed integer response '%s' (%s) to %s commamnd", res, asHex(res), cmd), e);
        }
    }

    private int readATasHex(String cmd) throws SerialException {
        String res = readAT(cmd);
        try {
            return Integer.parseInt(res, 16);
        } catch (NumberFormatException e) {
            throw new SerialException(String.format("Malformed hex response '%s' (%s) to %s commamnd", res, asHex(res), cmd), e);
        }
    }

    private String readAT(String cmd) throws SerialException {
        String res = sendATCommand(cmd);
        switch (res) {
            case "ERROR":
                throw new SerialException(String.format("Error received on %s command", cmd));
            case "OK":
                throw new SerialException(String.format("Unexpected OK in %s command", cmd));
            default:
                return res;
        }
    }

    private void enterCommandMode() throws SerialException {
        if (mode == Mode.COMMAND) {
            return;
        }
        while (mode == Mode.INIT) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
               logger.log(Level.SEVERE, null, ex);
            }
        }
        int count = 3;
        while (count > 0) {
            if (tryCommandMode()) {
                return;
            }
            count--;
        }
        throw new SerialException("Timed out waiting for command mode");
    }

    private boolean tryCommandMode() throws SerialException {
        int count = 0;
        send("+++");
        while ((mode != Mode.COMMAND) && (count < 15)) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
                if (!running) {
                    throw new SerialException("Modem stopped while entering command mode");
                }

            }
            count++;
        }
        return (mode == Mode.COMMAND);
    }

    private void leaveCommandMode() throws SerialException {
        if (mode == Mode.DATA) {
            return;
        }
        send("ATO\r");
        while (mode != Mode.DATA) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
            }

        }
    }

    private String sendATCommand(String cmd) throws SerialException {
        send(cmd + "\r");
        try {
            return results.take();
        } catch (InterruptedException ex) {
            throw new SerialException("Interruped waiting for AT response");
        }
    }

    private void send(String cmd) throws SerialException {
        log("SEND: '" + cmd.replace("\r", "") + "'");
        com.send(cmd);
    }

    private String read() throws SerialException {
        String in = com.read();
        log("RECV: '" + in + "'");
        return in.trim();
    }

    private void log(String msg) {
        logger.finest(msg);
    }

    /**
     * send the received message to listeners
     */
    private void fireEvent(SwapMessage msg, ReceiveTask.Direction dir) {
        synchronized (listeners) {
            for (MessageListener l : listeners) {
                pool.submit(new ReceiveTask(l, msg, dir));
            }
        }
    }

    private enum Mode {

        INIT, DATA, COMMAND
    };

    private ComPort com;
    private Mode mode = Mode.DATA;
    private ModemSetup setup;
    private final BlockingQueue<String> results = new LinkedBlockingQueue<>();
    private Reader reader;
    private boolean running;
    private final List<MessageListener> listeners = new CopyOnWriteArrayList<>();
    private final int baud;
    private final String port;
    private final ExecutorService pool = Executors.newCachedThreadPool(new ThreadFactory() {

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "SWAP Listener Notification");
            t.setDaemon(true);
            return t;
        }
    });
    
    private static final Logger logger = Logger.getLogger(SerialModem.class.getName());

    /**
     * The reader thread that receives data from the modem, unpacks it into
     * messages and send the messages to the listeners.
     */
    private class Reader extends Thread {

        private static final String OK_COMMAND = "OK-Command mode";
        private static final String OK_DATA = "OK-Data mode";
        private static final String MODEM_READY = "Modem ready!";

        private boolean isSwapMessage(String in) {
            return !in.isEmpty() && (in.charAt(0) == '(') && in.length() >= 12;
        }

        @Override
        public void run() {
            while (running) {
                try {
                    String in = read();
                    if (in.isEmpty()) {
                        continue; // discard empty lines
                    }
                    if (isSwapMessage(in)) {
                        mode = Mode.DATA;
                        fireEvent(new SerialMessage(in), ReceiveTask.Direction.IN);
                    } else {
                        switch (in) {
                            case OK_COMMAND:
                                mode = Mode.COMMAND;
                                break;
                            case MODEM_READY:
                            case OK_DATA:
                                mode = Mode.DATA;
                                break;
                            default:
                                if (mode == Mode.COMMAND) {
                                    results.add(in);
                                }
                        }
                    }
                } catch (SerialException | DecodingException ex) {
                    logger.log(Level.SEVERE, null, ex);
                } catch (Throwable ex) {
                    logger.log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    private String asHex(String text) {
        byte[] bytes = text.getBytes();
        StringBuilder buf = new StringBuilder();
        for (byte b : bytes) {
            buf.append(String.format("%2x", b));
        }
        return buf.toString();
    }

    private static class ReceiveTask implements Runnable {

        private enum Direction {

            IN, OUT;
        }

        private ReceiveTask(MessageListener listener, SwapMessage msg, Direction dir) {
            this.msg = msg;
            this.l = listener;
            this.dir = dir;
        }

        @Override
        public void run() {
            try {
                if (dir == Direction.IN) {
                    l.messageReceived(msg);
                } else {
                    l.messageSent(msg);
                }
            } catch (Throwable e) {
                logger.log(Level.SEVERE, null, e);
            }
        }

        private final SwapMessage msg;
        private final MessageListener l;
        private final Direction dir;
    }
}
