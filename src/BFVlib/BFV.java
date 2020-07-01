package BFVlib;

import java.util.*;

public class BFV {
    /*
        int: Converted value = Integer value
        Boolean: Converted value = FALSE if Integer value = 0, and TRUE otherwise
        double: Converted value = Integer value * Factor
    --> int_offset: Converted value = Integer value + Factor
     */
    public static final int TYPE_INT = 0;
    public static final int TYPE_DOUBLE = 1;
    public static final int TYPE_INTOFFSET = 2;
    // where is 3 ?
    public static final int TYPE_BOOLEAN = 4;
    public static final int TYPE_INTLIST = 5;

    private double qnh;

    private double batteryFromDevice;
    private double temperatureFromDevice;
    private double rawAltitudeFromDevice;
    private String hardwareVersionFromDevice;

    private TreeMap<String, Command> commands = new TreeMap<>();
    private TreeMap<String, Command> parameters = new TreeMap<>();

    private HashMap<String, String> parameterCodeToName = new HashMap<>();

    private boolean hasParameterKeysFromDevice = false;
    private String[] parameterKeysFromDevice;
    private boolean hasParameterValuesFromDevice = false;
    private int[] parameterValuesFromDevice;

    private boolean changedRawAltitudeFromDevice = false;
    private boolean changedBatteryFromDevice = false;
    private boolean changedTemperatureFromDevice = false;
    private boolean changedHardwareVersionFromDevice = false;
    private boolean changedParametersFromDevice = false;



    public BFV() {
        this.hardwareVersionFromDevice = "n/a";
        this.rawAltitudeFromDevice = Double.NaN;
        this.temperatureFromDevice = Double.NaN;
        this.batteryFromDevice = Double.NaN;
        this.qnh = 101325.0; // sea level pressure "101.325 kPa 1013.25hPa"

        // bfv commands
        commands.put("volumeUp", Command.Builder("BVU", "Volume Up (x2)")
                .build());

        commands.put("volumeDown", Command.Builder("BVD", "Volume Down (/2)")
                .build());

        commands.put("getSettings", Command.Builder("BST", "Get Settings")
                .build());

        commands.put("getTemp", Command.Builder("TMP", "Get Temperature")
                .build());

        commands.put("reset", Command.Builder("RST", "Simple Reset")
                .build());

        commands.put("restoreDefaults", Command.Builder("RSX", "Reset and restore default settings")
                .build());

        commands.put("sleep", Command.Builder("SLP", "Go To Sleep")
                .build());

        commands.put("sleepNoWake", Command.Builder("SLX", "Sleep - No UART wake")
                .build());

        commands.put("simulateButton", Command.Builder("BTN", "Simulate Button Press")
                .build());

        commands.put("playSound", Command.Builder("BSD", "Play Sound")
                .setDefaultArguments("800 500 400 500")
                .build());


        // pmtk commands
        commands.put( "eraseLocus", Command.Builder("PMTK184,1","Erase Locus")
                .build());

        commands.put( "queryLocus", Command.Builder("PMTK183","Query Locus")
                .build());

        commands.put( "queryLocusData", Command.Builder("PMTK622,0","Query Locus Data")
                .build());


        // bluetooth commands
        commands.put( "setBluetoothName", Command.Builder("RNC SN,","Set bluetooth name(max 16 characters)")
                .setDefaultArguments("BlueFly-")
                .build());


        // bfv parameters
        parameters.put("useAudioWhenConnected", Command.Builder("BAC", "Check to enable hardware audio when connected.")
                .setParameters(TYPE_BOOLEAN, 6,0, 1, 1.0)
                .setDefaultValue(0)
                .build());

        parameters.put("useAudioWhenDisconnected", Command.Builder("BAD", "Check to enable hardware audio when disconnected.")
                .setParameters(TYPE_BOOLEAN, 6, 0, 1, 1.0)
                .setDefaultValue(1)
                .build());

        parameters.put("positionNoise", Command.Builder("BFK", "Kalman filter position noise.")
                .setParameters(TYPE_DOUBLE, 6, 10, 10000, 1000.0)
                .setDefaultValue(100)
                .build());

        parameters.put("liftThreshold", Command.Builder("BFL", "The value in m/s of lift when the audio beeping will start.")
                .setParameters(TYPE_DOUBLE, 6, 0, 1000, 100.0)
                .setDefaultValue(20)
                .build());

        parameters.put("liftOffThreshold", Command.Builder("BOL", "The value in m/s of lift when the audio beeping will stop.")
                .setParameters(TYPE_DOUBLE, 6, 0, 1000, 100.0)
                .setDefaultValue(5)
                .build());

        parameters.put("liftFreqBase", Command.Builder("BFQ", "The audio frequency for lift beeps in Hz of 0 m/s.")
                .setParameters(TYPE_INT, 6, 500, 2000, 1.0)
                .setDefaultValue(1000)
                .build());

        parameters.put("liftFreqIncrement", Command.Builder("BFI", "The increase in audio frequency for lift beeps in Hz for each 1 m/s.")
                .setParameters(TYPE_INT, 6, 0, 1000, 1.0)
                .setDefaultValue(100)
                .build());

        parameters.put("sinkThreshold", Command.Builder("BFS", "The value in -m/s of sink when the sink tone will start.")
                .setParameters(TYPE_DOUBLE, 6, 0, 1000, 100.0)
                .setDefaultValue(20)
                .build());

        parameters.put("sinkOffThreshold", Command.Builder("BOS", "The value in -m/s of sink when the sink tone will stop.")
                .setParameters(TYPE_DOUBLE, 6, 0, 1000, 100.0)
                .setDefaultValue(5)
                .build());

        parameters.put("sinkFreqBase", Command.Builder("BSQ", "The audio frequency for the sink tone in Hz of 0 m/s.")
                .setParameters(TYPE_INT, 6, 250, 1000, 1.0)
                .setDefaultValue(400)
                .build());

        parameters.put("sinkFreqIncrement", Command.Builder("BSI", "The decrease in audio frequency for sink tone in Hz for each -1 m/s.")
                .setParameters(TYPE_INT, 6, 0, 1000, 1.0)
                .setDefaultValue(100)
                .build());

        parameters.put("secondsBluetoothWait", Command.Builder("BTH", "The time that the hardware will be allow establishment of a bluetooth connection for when turned on.")
                .setParameters(TYPE_INT, 6, 0, 10000, 1.0)
                .setDefaultValue(180)
                .build());

        parameters.put("rateMultiplier", Command.Builder("BRM", "The lift beep cadence -> 0.5 = beeping twice as fast as normal.")
                .setParameters(TYPE_DOUBLE, 6, 10, 1000, 100.0)
                .setDefaultValue(100)
                .build());

        parameters.put("speedMultiplier", Command.Builder("BSM", "The sensitivity of cadence to vertical speed -> 2.0 = cadence changes slower than normal.")
                .setParameters(TYPE_DOUBLE, 10, 10, 1000, 100.0)
                .setDefaultValue(100)
                .build());

        parameters.put("volume", Command.Builder("BVL", "The volume of beeps ->  0.1 is only about 1/2 as loud as 1.0.")
                .setParameters(TYPE_DOUBLE, 6, 1, 1000, 1000.0)
                .setDefaultValue(1000)
                .build());

        parameters.put("outputMode", Command.Builder("BOM", "The output mode -> 0-BlueFlyVario(default), 1-LK8EX1, 2-LX, 3-FlyNet, 4-None, 5-BFVlib, 6-BFX, 7-OpenVario")
                .setParameters(TYPE_INT, 7, 0, 7, 1.0)
                .setDefaultValue(0)
                .build());

        parameters.put("outputFrequency", Command.Builder("BOF", "The output frequency divisor -> 1-every 20ms ... 50-every 20msx50=1000ms")
                .setParameters(TYPE_INT, 7, 1, 50, 1.0)
                .setDefaultValue(1)
                .build());

        parameters.put("outputQNH", Command.Builder("BQH", "QNH (in Pascals), used for hardware output alt for some output modes - (default 101325)")
                .setParameters(TYPE_INTOFFSET, 7, 0, 65535, 80000.0)
                .setDefaultValue(21325)
                .build());

        parameters.put("uart1BRG", Command.Builder("BRB", "BRG setting for UART1, baud = 2000000/(BRG-1) (default of 207 = approx 9600 baud)")
                .setParameters(TYPE_INT, 8, 0, 65535, 1.0)
                .setDefaultValue(207)
                .build());

        parameters.put("uart2BRG", Command.Builder("BR2", "BRG setting for UART1, baud = 2000000/(BRG-1) (default of 34 = approx 57.6k baud)")
                .setParameters(TYPE_INT, 9, 0, 65535, 1.0)
                .setDefaultValue(16)
                .build());

        parameters.put("heightSensitivityDm", Command.Builder("BHV", "How far you have to move in dm to reset the idle timeout")
                .setParameters(TYPE_INT, 10, 0, 65535, 1.0)
                .setDefaultValue(20)
                .build());

        parameters.put("heightSeconds", Command.Builder("BHT", "Idle timeout")
                .setParameters(TYPE_INT, 10, 0, 65535, 1.0)
                .setDefaultValue(600)
                .build());

        parameters.put("uartPassthrough", Command.Builder("BPT", "Check to pass data received by U2 into U1")
                .setParameters(TYPE_BOOLEAN, 9, 0, 1, 1.0)
                .setDefaultValue(1)
                .build());

        parameters.put("uart1Raw", Command.Builder("BUR", "Check to make U1 data transferred raw instead of line by line")
                .setParameters(TYPE_BOOLEAN, 9, 0, 1, 1.0)
                .setDefaultValue(0)
                .build());

        parameters.put("greenLED", Command.Builder("BLD", "Check to make green LED flash with beep")
                .setParameters(TYPE_BOOLEAN, 9, 0, 1, 1.0)
                .setDefaultValue(1)
                .build());

        parameters.put("useAudioBuzzer", Command.Builder("BBZ", "Check to use the experimental audio buzzer")
                .setParameters(TYPE_BOOLEAN, 10, 0, 1, 1.0)
                .setDefaultValue(0)
                .build());

        parameters.put("buzzerThreshold", Command.Builder("BZT", "The value in m/s below the liftThreshold when the buzzer will start.")
                .setParameters(TYPE_DOUBLE, 10, 0, 1000, 100.0)
                .setDefaultValue(40)
                .build());

        parameters.put("usePitot", Command.Builder("BUP", "Check to use the experimental MS4525DO pitot connected via I2C")
                .setParameters(TYPE_BOOLEAN, 11, 0, 1, 1.0)
                .setDefaultValue(0)
                .build());

        parameters.put("toggleThreshold", Command.Builder("BTT", "The value in m/s below or above which will auto turn the button audio toggle off")
                .setParameters(TYPE_DOUBLE, 11, 0, 1000, 100.0)
                .setDefaultValue(100)
                .build());

        parameters.put("startDelayMS", Command.Builder("BDM", "The delay ms at start")
                .setParameters(TYPE_INT, 12, 0, 65535, 1.0)
                .setDefaultValue(0)
                .build());

        parameters.put("quietStart", Command.Builder("BQS", "Check to quiet the startup beeps")
                .setParameters(TYPE_BOOLEAN, 12, 0, 1, 1.0)
                .setDefaultValue(0)
                .build());

        parameters.put("gpsLogInterval", Command.Builder("BGL", "GPS Log for XA1110")
                .setParameters(TYPE_INT, 12, 0, 65535, 1.0)
                .setDefaultValue(10)
                .build());

        // set min hw version to 99 so it doesnt appear in UI depending on the implementation of UI
        parameters.put("isPrintPressure", Command.Builder("BFP", "Controls if the output is printed. It is equivalent to outputMode=4 (or at least it was in some earlier version of the firmware)")
                .setParameters(TYPE_BOOLEAN, 99, 0, 1, 1.0)
                .setDefaultValue(1)
                .build());

        // make dict command_code -> name
        for (String name: parameters.keySet()) {
            parameterCodeToName.put(parameters.get(name).getCommandCode(), name);
        }
    }

    private void updateParameterKeysFromDevice(String[] splitted) {
        parameterKeysFromDevice = splitted;
        hasParameterKeysFromDevice = true;
    }

    private void updateParameterValuesFromDevice(int[] splitted) {
        if(!Arrays.equals(parameterValuesFromDevice, splitted)) {
            parameterValuesFromDevice = splitted;
            hasParameterValuesFromDevice = true;
            updateAllUserValues();
            changedParametersFromDevice = true;
        }
    }

    private void updateAllUserValues() {
        if(hasParameterKeysFromDevice && hasParameterValuesFromDevice &&
                (parameterKeysFromDevice.length == parameterValuesFromDevice.length)) {
            for (int i = 0; i < parameterKeysFromDevice.length && i < parameterValuesFromDevice.length; i++) {
                parameters.get(getParameterNameFromCommandCode(parameterKeysFromDevice[i])).setUserValue(parameterValuesFromDevice[i]);
            }
        }
    }

    private void resetAllUserValues() {
        for (Command command: this.parameters.values()) {
            command.resetUserValue();
        }
    }

    private String getParameterNameFromCommandCode(String code) {
        return parameterCodeToName.get(code);
    }

    private static String getHwVersionFromRawData(String rawData) {
        String[] split = rawData.split(" ");
        String version;
        if (split.length > 2) {
            version = split[1] + "." + split[2];
        } else {
            version = split[1];
        }
        return version;
    }

    private static Double getTemperatureFromRawData(String rawData) {
        String value = rawData.split(" ")[1];
        return Double.parseDouble(value) / 10.0;
    }

    private static Double getBatteryFromRawData(String rawData) {
        String value = rawData.split(" ")[1];
        return Integer.parseInt(value, 16) / 1000.0;
    }

    private static Double getRawAltitudeFromRawData(String rawData, Double qnh) {
        String[] split = rawData.split(" ");
        if(split.length > 1) {
            int pressure = Integer.parseInt(split[1], 16);
            return 44330.0 * (1 - Math.pow((pressure / qnh), 0.190295));
        }
        return Double.NaN;
    }

    public Map<String, Command> getAllCommands() {
        return commands;
    }

    public Map<String, Command> getAllParameters() {
        return parameters;
    }

    /**
     * @param rawData raw data from serial port
     * @return true if parameter values have been updated, false otherwise
     */
    public void parseRawDataFromDevice(String rawData) {
        // PMTK commands are comma separated
        if(rawData.startsWith("$PMTK")){
            new PMTKParser(rawData);
        }

        // BFV one are space separated
        // 0=COMMAND_code
        // 1=VALUE
        String[] split = rawData.split(" ");
        if (split.length > 1) {
            switch(split[0]) {
                case "PRS":
                    setAltitudeFomDevice(rawData);
                    break;
                case "BFV":
                    setHardwareVersionFromDevice(rawData);
                    break;
                case "TMP":
                    setTemperatureFromDevice(rawData);
                    break;
                case "BAT":
                    setBatteryFromDevice(rawData);
                    break;

                /*
                BFV [VersionNumber] \r\n
                BST [followed by a space separated list of each of the settings codes]
                SET [0(skip) followed by a space separated list of each of the settings Integer Values]
                 */
                case "BST":
                    updateParameterKeysFromDevice(Arrays.copyOfRange(split, 1, split.length));
                    break;
                case "SET":
                    // it's possible that BST wasn't received, therefor not populated
                    if(hasParameterKeysFromDevice) {
                    /*
                    We skip the first value:
                        The first address is a flag to reset the other addresses.
                        $RSX* sets it to 1, then restarts the vario,
                        which then uses that to reset all of the other values to their default settings.
                     */
                        updateParameterValuesFromDevice(Arrays.stream(split).skip(2).mapToInt(Integer::parseInt).toArray());
                    }
                    break;
                case "MS5611":
                    /*
                    As seen in https://www.te.com/commerce/DocumentDelivery/DDEController?Action=showdoc&DocId=Data+Sheet%7FMS5611-01BA03%7FB3%7Fpdf%7FEnglish%7FENG_DS_MS5611-01BA03_B3.pdf%7FCAT-BLPS0036
                        Variable | Description/Equation                                  | Variable Type   | Size[bit] | Min | Max      | Example/Typical
                        -----------------------------------------------------------------------------------------------------------------------------
                        C1       | Pressure sensitivity - SENST1                         | unsigned int 16 | 16        | 0   | 65535    | 40127
                        C2       | Pressure offset - OFFT1                               | unsigned int 16 | 16        | 0   | 65535    | 36924
                        C3       | Temperature coefficient of pressure sensitivity - TCS | unsigned int 16 | 16        | 0   | 65535    | 23317
                        C4       | Temperature coefficient of pressure offset - TCO      | unsigned int 16 | 16        | 0   | 65535    | 23282
                        C5       | Reference temperature - TREF                          | unsigned int 16 | 16        | 0   | 65535    | 33464
                        C6       | Temperature coefficient of the temperature - TEMPSENS | unsigned int 16 | 16        | 0   | 65535    | 28312
                        D1       | Digital pressure value                                | unsigned int 32 | 24        | 0   | 16777216 | 9085466
                        D2       | Digital temperature value                             | unsigned int 32 | 24        | 0   | 16777216 | 8569150
                     */
                    break;
                case "Batt":
                    // battery value in Volts -> Volts = Batt / 1000
                    break;
                case "No":
                    // No movement ie 'No movement from 101.7m'
                    break;
                case "Shutdown...": // never gonna reach here, documentation only
                    // vario disconnected
                    break;
            }
        }
    }

    private void setBatteryFromDevice(String rawData) {
        this.changedBatteryFromDevice = true;
        this.batteryFromDevice = getBatteryFromRawData(rawData);
    }

    private void setTemperatureFromDevice(String rawData) {
        this.changedTemperatureFromDevice = true;
        this.temperatureFromDevice = getTemperatureFromRawData(rawData);
    }

    private void setHardwareVersionFromDevice(String rawData) {
        this.changedHardwareVersionFromDevice = true;
        this.hardwareVersionFromDevice = getHwVersionFromRawData(rawData);
    }

    private void setAltitudeFomDevice(String rawData) {
        Double rawAlt = getRawAltitudeFromRawData(rawData, this.qnh);
        if(this.rawAltitudeFromDevice != rawAlt) {
            this.rawAltitudeFromDevice = rawAlt;
            this.changedRawAltitudeFromDevice = true;
        }
    }

    public void setQnh(Double value) {
        this.qnh = value;
    }

    public void resetAllValuesFromDevice() {
        this.parameterKeysFromDevice = new String[0];
        this.parameterValuesFromDevice = new int[0];
        this.hasParameterValuesFromDevice = false;
        this.hasParameterKeysFromDevice = false;
        this.hardwareVersionFromDevice = "n/a";
        this.rawAltitudeFromDevice = Double.NaN;
        this.batteryFromDevice = Double.NaN;
        this.temperatureFromDevice = Double.NaN;
        resetAllUserValues();
    }

    public String getHwVersion() {
        this.changedHardwareVersionFromDevice = false;
        return this.hardwareVersionFromDevice;
    }

    public Double getRawAltitude() {
        this.changedRawAltitudeFromDevice = false;
        return this.rawAltitudeFromDevice;
    }

    public Double getTemperature() {
        this.changedTemperatureFromDevice = false;
        return this.temperatureFromDevice;
    }

    public Double getBattery() {
        this.changedBatteryFromDevice = false;
        return this.batteryFromDevice;
    }

    public boolean isChangedRawAltitudeFromDevice() {
        return changedRawAltitudeFromDevice;
    }

    public boolean isChangedBatteryFromDevice() {
        return changedBatteryFromDevice;
    }

    public boolean isChangedTemperatureFromDevice() {
        return changedTemperatureFromDevice;
    }

    public boolean isChangedHardwareVersionFromDevice() {
        return changedHardwareVersionFromDevice;
    }

    public boolean isChangedParametersFromDevice() {
        return changedParametersFromDevice;
    }

    public void readParametersFromDevice() {
        this.changedParametersFromDevice = false;
    }

    public static void main(String[] args){
        BFV device = new BFV();

        System.out.println("ParameterNames(" + device.getAllParameters().keySet().size() + "): " + device.getAllParameters().keySet());
        ArrayList<String> parameters = new ArrayList<>();
        for (String command : device.getAllParameters().keySet()) {
            parameters.add(device.getAllParameters().get(command).getCommandCode());
        }
        System.out.println("ParameterCodes(" + parameters.size() + "): " + parameters);


        ArrayList<Integer> values = new ArrayList<>();
        for (String command : device.getAllParameters().keySet()) {
            values.add(device.getAllParameters().get(command).getDefaultValue());
        }
        System.out.println("DefaultParameterValues(" + values.size() + "): " + values);


        System.out.println("Commands(" + device.getAllCommands().keySet().size() + "): " + device.getAllCommands().keySet());
        ArrayList<String> commands = new ArrayList<>();
        for (String command : device.getAllCommands().keySet()) {
            commands.add(device.getAllCommands().get(command).getCommandCode());
        }

        System.out.println("CommandCodes(" + commands.size() + "): " + commands);

        System.out.println(device.parameterCodeToName.toString());
    }
}

