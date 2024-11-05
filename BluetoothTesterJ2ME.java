import javax.microedition.midlet.*;
import javax.microedition.lcdui.*;
import javax.microedition.io.*;
import javax.bluetooth.*;
import java.io.*;

public class BluetoothTesterJ2ME extends MIDlet implements CommandListener {
    private Display display;
    private Form mainForm;
    private boolean lightOn = false;
    private StreamConnectionNotifier notifier;
    private StreamConnection connection;
    private DataInputStream inputStream;
    private DataOutputStream outputStream;
    
    public void startApp() {
        display = Display.getDisplay(this);
        mainForm = new Form("Bluetooth Connection Tester");
        updateLight();
        
        mainForm.setCommandListener(this);
        display.setCurrent(mainForm);

        new Thread(new Runnable() {
            public void run() {
                try {
                    // Set up the Bluetooth server to listen for connections
                    LocalDevice localDevice = LocalDevice.getLocalDevice();
                    localDevice.setDiscoverable(DiscoveryAgent.GIAC);
                    String url = "btspp://localhost:" + new UUID(80087355).toString() + ";name=BluetoothTesterJ2ME";
                    notifier = (StreamConnectionNotifier) Connector.open(url);
                    
                    // Wait for a connection from the Android app
                    connection = notifier.acceptAndOpen();
                    inputStream = connection.openDataInputStream();
                    outputStream = connection.openDataOutputStream();

                    while (true) {
                        int signal = inputStream.readInt();  // Read signal from Android app
                        if (signal == 1) {                   // Signal received to activate light
                            lightOn = !lightOn;
                            updateLight();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    protected void updateLight() {
        mainForm.deleteAll();
        mainForm.append("Signal Light: " + (lightOn ? "ON" : "OFF"));
    }

    protected void sendSignal() {
        try {
            outputStream.writeInt(1);  // Send signal to the Android app
            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    protected void pauseApp() {}
    protected void destroyApp(boolean unconditional) {}
    
    public void commandAction(Command c, Displayable d) {
        if (c.getLabel().equals("5")) {
            sendSignal();
        }
    }
}