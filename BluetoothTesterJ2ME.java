import javax.microedition.midlet.*;
import javax.microedition.lcdui.*;
import javax.microedition.io.*;
import javax.bluetooth.*;
import java.io.*;

public class BluetoothTesterJ2ME extends MIDlet implements CommandListener {
    private Display display;
    private Form mainForm;
    private boolean lightOn = false;
    private StreamConnection connection;
    private DataInputStream inputStream;
    private DataOutputStream outputStream;
    private DiscoveryAgent discoveryAgent;
    private String serverUrl;
    private Command toggleLightCommand = new Command("Toggle Light", Command.ITEM, 1);

    public void startApp() {
        display = Display.getDisplay(this);
        mainForm = new Form("Bluetooth Connection Tester");
        updateLight();

        mainForm.addCommand(toggleLightCommand); // Add the "Toggle Light" command
        mainForm.setCommandListener(this);
        display.setCurrent(mainForm);

        new Thread(new Runnable() {
            public void run() {
                try {
                    LocalDevice localDevice = LocalDevice.getLocalDevice();
                    discoveryAgent = localDevice.getDiscoveryAgent();
                    
                    // Start device discovery and connect to the server
                    discoverAndConnect();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void discoverAndConnect() throws IOException {
        // Ensure the MIDlet is discoverable and find Bluetooth devices
        discoveryAgent.startInquiry(DiscoveryAgent.GIAC, new DiscoveryListener() {
            public void deviceDiscovered(RemoteDevice btDevice, DeviceClass cod) {
                try {
                    String deviceName = btDevice.getFriendlyName(false);
                    if (deviceName.equals("BluetoothTesterApp")) { // Replace with Android device name if needed
                        serverUrl = discoveryAgent.selectService(
                                new UUID("0000110100001000800000805F9B34FB", false),  // Same UUID as Android server
                                ServiceRecord.NOAUTHENTICATE_NOENCRYPT, false);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            public void servicesDiscovered(int transID, ServiceRecord[] servRecord) {}
            public void inquiryCompleted(int discType) {
                if (serverUrl != null) {
                    try {
                        connection = (StreamConnection) Connector.open(serverUrl);
                        inputStream = connection.openDataInputStream();
                        outputStream = connection.openDataOutputStream();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            public void serviceSearchCompleted(int transID, int respCode) {}
        });
    }

    protected void updateLight() {
        mainForm.deleteAll();
        mainForm.append("Signal Light: " + (lightOn ? "ON" : "OFF"));
    }

    protected void toggleLight() {
        // Toggle light on and off
        lightOn = !lightOn;
        updateLight();
        
        // Send the toggle signal to the Android app
        sendSignal();
    }

    protected void sendSignal() {
        try {
            outputStream.writeInt(lightOn ? 1 : 0);  // Send the current light state to the Android app
            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void pauseApp() {}
    protected void destroyApp(boolean unconditional) {}

    public void commandAction(Command c, Displayable d) {
        if (c == toggleLightCommand) {
            toggleLight();  // Toggle light when "Toggle Light" command is triggered
        }
    }
}