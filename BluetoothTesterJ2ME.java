import javax.microedition.midlet.*;
import javax.microedition.lcdui.*;
import javax.microedition.io.*;
import javax.bluetooth.*;
import java.io.*;

public class BluetoothTesterJ2ME extends MIDlet implements CommandListener {
    private Display display;
    private Form mainForm;
    private boolean lightOn = false;
    private boolean isConnected = false;
    private StreamConnection connection;
    private DataInputStream inputStream;
    private DataOutputStream outputStream;
    private DiscoveryAgent discoveryAgent;
    private String serverUrl;
    private Command toggleLightCommand = new Command("Toggle Light", Command.ITEM, 1);
    private StringItem connectionStatus = new StringItem("Status: ", "Disconnected");

    public void startApp() {
        display = Display.getDisplay(this);
        mainForm = new Form("Bluetooth Connection Tester");
        mainForm.append(connectionStatus);
        updateLight();

        mainForm.addCommand(toggleLightCommand);
        mainForm.setCommandListener(this);
        display.setCurrent(mainForm);

        new Thread(new Runnable() {
            public void run() {
                try {
                    LocalDevice localDevice = LocalDevice.getLocalDevice();
                    discoveryAgent = localDevice.getDiscoveryAgent();

                    // Attempt to connect to a known, paired device
                    connectToPairedDevice();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void connectToPairedDevice() throws IOException {
        RemoteDevice[] pairedDevices = discoveryAgent.retrieveDevices(DiscoveryAgent.PREKNOWN);

        // Letting the user know it's searching
        connectionStatus.setText("Searching...");
        updateLight();

        if (pairedDevices != null) {
            for (int i = 0; i < pairedDevices.length; i++) {
                RemoteDevice device = pairedDevices[i];
                try {
                    String deviceName = device.getFriendlyName(false);
                    if (deviceName.equals("Notonk")) { // Replace with Android device name if needed
                        // Device found, start service search on the paired device
                        serverUrl = discoveryAgent.selectService(
                                new UUID("0000110100001000800000805F9B34FB", false),
                                ServiceRecord.NOAUTHENTICATE_NOENCRYPT, false);
                        if (serverUrl != null) {
                            establishConnection(serverUrl);
                            return;
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        // If no paired device found, fall back to device discovery
        discoverAndConnect();
    }

    private void establishConnection(String url) {
        try {
            connection = (StreamConnection) Connector.open(url);
            inputStream = connection.openDataInputStream();
            outputStream = connection.openDataOutputStream();
            isConnected = true;
            updateConnectionStatus();
        } catch (IOException e) {
            e.printStackTrace();
            isConnected = false;
            updateConnectionStatus();
        }
    }

    private void discoverAndConnect() {
        try {
            discoveryAgent.startInquiry(DiscoveryAgent.GIAC, new DiscoveryListener() {
                public void deviceDiscovered(RemoteDevice btDevice, DeviceClass cod) {
                    try {
                        String deviceName = btDevice.getFriendlyName(false);
                        if (deviceName.equals("Notonk")) {
                            serverUrl = discoveryAgent.selectService(
                                    new UUID("0000110100001000800000805F9B34FB", false),
                                    ServiceRecord.NOAUTHENTICATE_NOENCRYPT, false);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                public void servicesDiscovered(int transID, ServiceRecord[] servRecord) {}

                public void inquiryCompleted(int discType) {
                    if (serverUrl != null) {
                        establishConnection(serverUrl);
                    } else {
                        isConnected = false;
                        updateConnectionStatus();
                    }
                }

                public void serviceSearchCompleted(int transID, int respCode) {}
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void updateLight() {
        mainForm.deleteAll();
        mainForm.append(connectionStatus);
        mainForm.append("Signal Light: " + (lightOn ? "ON" : "OFF"));
    }

    protected void updateConnectionStatus() {
        connectionStatus.setText(isConnected ? "Connected" : "Disconnected");
        updateLight();
    }

    protected void toggleLight() {
        lightOn = !lightOn;
        updateLight();
        sendSignal();
    }

    protected void sendSignal() {
        try {
            if (isConnected) {
                outputStream.writeInt(lightOn ? 1 : 0);
                outputStream.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void pauseApp() {}
    protected void destroyApp(boolean unconditional) {}

    public void commandAction(Command c, Displayable d) {
        if (c == toggleLightCommand) {
            toggleLight();
        }
    }
}