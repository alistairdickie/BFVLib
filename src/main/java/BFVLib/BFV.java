package BFVLib;

import java.util.*;

/**
 * BFV object contains methods to parse and decode data from BlueFlyVario device
 *
 */
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

    private double battery;
    private double temperature;
    private double altitude;
    private String hardwareVersion;

    private final TreeMap<String, Command> COMMANDS = new TreeMap<>();
    private final TreeMap<String, Command> PARAMETERS = new TreeMap<>();

    private final HashMap<String, String> parameterCodeToName = new HashMap<>();

    private boolean hasParameterKeys = false;
    private String[] parameterKeys;
    private boolean hasValues = false;
    private int[] userValues;

    private boolean updatedAltitude = false;
    private boolean updatedBattery = false;
    private boolean updatedTemperature = false;
    private boolean updatedHardwareVersion = false;
    private boolean updatedValues = false;


    public BFV() {
        this.hardwareVersion = "";
        this.altitude = Double.NaN;
        this.temperature = Double.NaN;
        this.battery = Double.NaN;
        this.qnh = 101325.0; // sea level pressure "101.325 kPa 1013.25hPa"

        // bfv commands
        COMMANDS.put("volumeUp", Command.Builder("BVU", "Volume Up (x2)")
                .build());

        COMMANDS.put("volumeDown", Command.Builder("BVD", "Volume Down (/2)")
                .build());

        COMMANDS.put("getSettings", Command.Builder("BST", "Get Settings")
                .build());

        COMMANDS.put("getTemp", Command.Builder("TMP", "Get Temperature")
                .build());

        COMMANDS.put("reset", Command.Builder("RST", "Simple Reset")
                .build());

        COMMANDS.put("restoreDefaults", Command.Builder("RSX", "Reset and restore default settings")
                .build());

        COMMANDS.put("sleep", Command.Builder("SLP", "Go To Sleep")
                .build());

        COMMANDS.put("sleepNoWake", Command.Builder("SLX", "Sleep - No UART wake")
                .build());

        COMMANDS.put("simulateButton", Command.Builder("BTN", "Simulate Button Press")
                .build());

        COMMANDS.put("playSound", Command.Builder("BSD", "Play Sound")
                .setAcceptsArguments(true)
                .setDefaultArguments("800 500 400 500")
                .build());


        // pmtk commands
        COMMANDS.put( "eraseLocus", Command.Builder("PMTK184,1","Erase Locus")
                .build());

        COMMANDS.put( "queryLocus", Command.Builder("PMTK183","Query Locus")
                .build());

        COMMANDS.put( "queryLocusData", Command.Builder("PMTK622,0","Query Locus Data")
                .build());


        // bluetooth commands
        COMMANDS.put( "setBluetoothName", Command.Builder("RNC SN,","Set bluetooth name(max 16 characters)")
                .setMinHwVersion(12)
                .setAcceptsArguments(true)
                .setDefaultArguments("BlueFly-")
                .build());


        // bfv parameters
        PARAMETERS.put("useAudioWhenConnected", Command.Builder("BAC", "Enable hardware audio when connected.")
                .setParameters(TYPE_BOOLEAN, 0, 1, 1.0)
                .setMinHwVersion(6)
                .setDefaultValue(0)
                .build());

        PARAMETERS.put("useAudioWhenDisconnected", Command.Builder("BAD", "Enable hardware audio when disconnected.")
                .setParameters(TYPE_BOOLEAN, 0, 1, 1.0)
                .setMinHwVersion(6)
                .setDefaultValue(1)
                .build());

        PARAMETERS.put("positionNoise", Command.Builder("BFK", "Kalman filter position noise.")
                .setParameters(TYPE_DOUBLE, 10, 10000, 1000.0)
                .setMinHwVersion(6)
                .setDefaultValue(100)
                .build());

        PARAMETERS.put("liftThreshold", Command.Builder("BFL", "Value in m/s of lift when the audio beeping will start.")
                .setParameters(TYPE_DOUBLE, 0, 1000, 100.0)
                .setMinHwVersion(6)
                .setDefaultValue(20)
                .build());

        PARAMETERS.put("liftOffThreshold", Command.Builder("BOL", "Value in m/s of lift when the audio beeping will stop.")
                .setParameters(TYPE_DOUBLE, 0, 1000, 100.0)
                .setMinHwVersion(6)
                .setDefaultValue(5)
                .build());

        PARAMETERS.put("liftFreqBase", Command.Builder("BFQ", "Audio frequency for lift beeps in Hz of 0 m/s.")
                .setParameters(TYPE_INT, 500, 2000, 1.0)
                .setMinHwVersion(6)
                .setDefaultValue(1000)
                .build());

        PARAMETERS.put("liftFreqIncrement", Command.Builder("BFI", "Increase in audio frequency for lift beeps in Hz for each 1 m/s.")
                .setParameters(TYPE_INT, 0, 1000, 1.0)
                .setMinHwVersion(6)
                .setDefaultValue(100)
                .build());

        PARAMETERS.put("sinkThreshold", Command.Builder("BFS", "Value in -m/s of sink when the sink tone will start.")
                .setParameters(TYPE_DOUBLE, 0, 1000, 100.0)
                .setMinHwVersion(6)
                .setDefaultValue(20)
                .build());

        PARAMETERS.put("sinkOffThreshold", Command.Builder("BOS", "Value in -m/s of sink when the sink tone will stop.")
                .setParameters(TYPE_DOUBLE, 0, 1000, 100.0)
                .setMinHwVersion(6)
                .setDefaultValue(5)
                .build());

        PARAMETERS.put("sinkFreqBase", Command.Builder("BSQ", "Audio frequency for the sink tone in Hz of 0 m/s.")
                .setParameters(TYPE_INT, 250, 1000, 1.0)
                .setMinHwVersion(6)
                .setDefaultValue(400)
                .build());

        PARAMETERS.put("sinkFreqIncrement", Command.Builder("BSI", "Decrease in audio frequency for sink tone in Hz for each -1 m/s.")
                .setParameters(TYPE_INT, 0, 1000, 1.0)
                .setMinHwVersion(6)
                .setDefaultValue(100)
                .build());

        PARAMETERS.put("secondsBluetoothWait", Command.Builder("BTH", "Time that the hardware will be allow establishment of a bluetooth connection for when turned on.")
                .setParameters(TYPE_INT, 0, 10000, 1.0)
                .setMinHwVersion(6)
                .setDefaultValue(180)
                .build());

        PARAMETERS.put("rateMultiplier", Command.Builder("BRM", "Lift beep cadence -> 0.5 = beeping twice as fast as normal.")
                .setParameters(TYPE_DOUBLE, 10, 1000, 100.0)
                .setMinHwVersion(6)
                .setDefaultValue(100)
                .build());

        PARAMETERS.put("speedMultiplier", Command.Builder("BSM", "Sensitivity of cadence to vertical speed -> 2.0 = cadence changes slower than normal.")
                .setParameters(TYPE_DOUBLE, 10, 1000, 100.0)
                .setMinHwVersion(10)
                .setDefaultValue(100)
                .build());

        PARAMETERS.put("volume", Command.Builder("BVL", "Volume of beeps ->  0.1 is only about 1/2 as loud as 1.0.")
                .setParameters(TYPE_DOUBLE, 1, 1000, 1000.0)
                .setMinHwVersion(6)
                .setDefaultValue(1000)
                .build());

        PARAMETERS.put("outputMode", Command.Builder("BOM", "Output mode -> 0-BlueFlyVario(default), 1-LK8EX1, 2-LX, 3-FlyNet, 4-None, 5-BFVlib, 6-BFX, 7-OpenVario")
                .setParameters(TYPE_INT, 0, 7, 1.0)
                .setMinHwVersion(7)
                .setDefaultValue(0)
                .build());

        PARAMETERS.put("outputFrequency", Command.Builder("BOF", "Output frequency divisor -> 1-every 20ms ... 50-every 20ms*50=1000ms")
                .setParameters(TYPE_INT, 1, 50, 1.0)
                .setMinHwVersion(7)
                .setDefaultValue(1)
                .build());

        PARAMETERS.put("outputQNH", Command.Builder("BQH", "QNH (in Pascals), used for hardware output alt for some output modes - (default 101325)")
                .setParameters(TYPE_INTOFFSET, 0, 65535, 80000.0)
                .setMinHwVersion(7)
                .setDefaultValue(21325)
                .build());

        PARAMETERS.put("uart1BRG", Command.Builder("BRB", "BRG setting for UART1, baud = 2000000/(BRG-1) (default of 207 = approx 9600 baud)")
                .setParameters(TYPE_INT, 0, 65535, 1.0)
                .setMinHwVersion(8)
                .setDefaultValue(207)
                .build());

        PARAMETERS.put("uart2BRG", Command.Builder("BR2", "BRG setting for UART1, baud = 2000000/(BRG-1) (default of 34 = approx 57.6k baud)")
                .setParameters(TYPE_INT, 0, 65535, 1.0)
                .setMinHwVersion(9)
                .setDefaultValue(16)
                .build());

        PARAMETERS.put("heightSensitivityDm", Command.Builder("BHV", "How far you have to move in dm to reset the idle timeout")
                .setParameters(TYPE_INT, 0, 65535, 1.0)
                .setMinHwVersion(10)
                .setDefaultValue(20)
                .build());

        PARAMETERS.put("heightSeconds", Command.Builder("BHT", "Idle timeout")
                .setParameters(TYPE_INT, 0, 65535, 1.0)
                .setMinHwVersion(10)
                .setDefaultValue(600)
                .build());

        PARAMETERS.put("uartPassthrough", Command.Builder("BPT", "Pass data received by U2 into U1")
                .setParameters(TYPE_BOOLEAN, 0, 1, 1.0)
                .setMinHwVersion(9)
                .setDefaultValue(1)
                .build());

        PARAMETERS.put("uart1Raw", Command.Builder("BUR", "Make U1 data transferred raw instead of line by line")
                .setParameters(TYPE_BOOLEAN, 0, 1, 1.0)
                .setMinHwVersion(9)
                .setDefaultValue(0)
                .build());

        PARAMETERS.put("greenLED", Command.Builder("BLD", "Make green LED flash with beep")
                .setParameters(TYPE_BOOLEAN, 0, 1, 1.0)
                .setMinHwVersion(9)
                .setDefaultValue(1)
                .build());

        PARAMETERS.put("useAudioBuzzer", Command.Builder("BBZ", "Use the experimental audio buzzer")
                .setParameters(TYPE_BOOLEAN, 0, 1, 1.0)
                .setMinHwVersion(10)
                .setDefaultValue(0)
                .build());

        PARAMETERS.put("buzzerThreshold", Command.Builder("BZT", "Value in m/s below the liftThreshold when the buzzer will start.")
                .setParameters(TYPE_DOUBLE, 0, 1000, 100.0)
                .setMinHwVersion(10)
                .setDefaultValue(40)
                .build());

        PARAMETERS.put("usePitot", Command.Builder("BUP", "Use the experimental MS4525DO pitot connected via I2C")
                .setParameters(TYPE_BOOLEAN, 0, 1, 1.0)
                .setMinHwVersion(11)
                .setDefaultValue(0)
                .build());

        PARAMETERS.put("toggleThreshold", Command.Builder("BTT", "Value in m/s below or above which will auto turn the button audio toggle off")
                .setParameters(TYPE_DOUBLE, 0, 1000, 100.0)
                .setMinHwVersion(11)
                .setDefaultValue(100)
                .build());

        PARAMETERS.put("startDelayMS", Command.Builder("BDM", "Delay ms at start")
                .setParameters(TYPE_INT, 0, 65535, 1.0)
                .setMinHwVersion(12)
                .setDefaultValue(0)
                .build());

        PARAMETERS.put("quietStart", Command.Builder("BQS", "Quiet the startup beeps")
                .setParameters(TYPE_BOOLEAN, 0, 1, 1.0)
                .setMinHwVersion(12)
                .setDefaultValue(0)
                .build());

        PARAMETERS.put("gpsLogInterval", Command.Builder("BGL", "GPS Log for XA1110")
                .setParameters(TYPE_INT, 0, 65535, 1.0)
                .setMinHwVersion(12)
                .setDefaultValue(10)
                .build());

        // set min hw version to 99 so it doesnt appear in UI depending on the implementation of UI
        PARAMETERS.put("isPrintPressure", Command.Builder("BFP", "Controls if the output is printed. It is equivalent to outputMode=4 (or at least it was in some earlier version of the firmware)")
                .setParameters(TYPE_BOOLEAN, 0, 1, 1.0)
                .setMinHwVersion(99)
                .setDefaultValue(1)
                .build());

        // make dict command_code -> name
        for (String name: PARAMETERS.keySet()) {
            parameterCodeToName.put(PARAMETERS.get(name).getCommandCode(), name);
        }
    }

    /**
     * Returns map of 'commandName' -> command object
     *
     * @return BFV.COMMANDS
     */
    public Map<String, Command> getAllCommands() {
        return COMMANDS;
    }

    /**
     * Returns map of 'parameterName' -> command object
     *
     * @return BFV.PARAMETERS
     */
    public Map<String, Command> getAllParameters() {
        return PARAMETERS;
    }

    /**
     * Parses provided line for BlueFlyVario device codes and tries to decode them
     * On success updates relevant field in BFV and sets relevant is"Name"Updated field to true
     *
     * @param line from serial port to parse
     */
    public void parseLine(String line) {
        // PMTK lines are comma(',') separated
        if(line.startsWith("$PMTK")){
            new PMTKParser(line);
        }

        // BFV lines are space(' ') separated
        // split[0] = COMMAND_code
        // split[1] = VALUE
        String[] split = line.split(" ");
        if (split.length > 1) {
            switch(split[0]) {
                case "PRS":
                    setAltitudeFomDevice(line);
                    break;
                case "BFV":
                    setHardwareVersion(line);
                    break;
                case "TMP":
                    setTemperature(line);
                    break;
                case "BAT":
                    setBattery(line);
                    break;

                /*
                BFV [VersionNumber] \r\n
                BST [followed by a space separated list of each of the settings codes]
                SET [0(skip) followed by a space separated list of each of the settings Integer Values]
                 */
                case "BST":
                    updateParameterKeys(Arrays.copyOfRange(split, 1, split.length));
                    break;
                case "SET":
                    // it's possible that BST wasn't received, therefor not populated
                    if(hasParameterKeys) {
                    /*
                    We skip the first value:
                        The first address is a flag to reset the other addresses.
                        $RSX* sets it to 1, then restarts the vario,
                        which then uses that to reset all of the other values to their default settings.
                     */
                        updateValues(Arrays.stream(split).skip(2).mapToInt(Integer::parseInt).toArray());
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
                case "Audio":
                    // ie 'Audio and Buzzer Toggle Off'
                    break;
                case "Bluetooth":
                    // ie 'Bluetooth Connected'
                    break;
                case "Shutdown...": // never gonna reach here, documentation only
                    // vario disconnected
                    break;
            }
        }
    }

    /**
     * Sets BFV.qnh to provided value used in calculating device altitude
     * @link https://en.wikipedia.org/wiki/Altimeter_setting
     *
     * @param value to set BFV.qnh to
     */
    public void setQnh(Double value) {
        this.qnh = value;
    }

    /**
     * Resets all BFV fields and all userValue fields for parameter in PARAMETERS
     *
     */
    public void resetAllValues() {
        this.parameterKeys = new String[0];
        this.userValues = new int[0];
        this.hasValues = false;
        this.hasParameterKeys = false;
        this.hardwareVersion = "";
        this.altitude = Double.NaN;
        this.battery = Double.NaN;
        this.temperature = Double.NaN;
        resetAllParameterValues();
    }

    /**
     * Returns BFV.hardwareVersion and resets BFV.updatedHardwareVersion to false.
     * Should only be called after isUpdatedHardwareVersion() return true,
     * otherwise it will return default value -> Double.NaN
     *
     * @return BFV.hardwareVersion
     */
    public String getHwVersion() {
        this.updatedHardwareVersion = false;
        return this.hardwareVersion;
    }

    /**
     * Returns BFV.altitude and resets BFV.updatedAltitude to false.
     * Should only be called after isUpdatedAltitude() return true,
     * otherwise it will return default value -> Double.NaN
     *
     * @return BFV.altitude
     */
    public Double getAltitude() {
        this.updatedAltitude = false;
        return this.altitude;
    }

    /**
     * Returns BFV.temperature and resets BFV.updatedTemperature to false.
     * Should only be called after isUpdatedTemperature() return true,
     * otherwise it will return default value -> Double.NaN
     *
     * @return BFV.temperature
     */
    public Double getTemperature() {
        this.updatedTemperature = false;
        return this.temperature;
    }

    /**
     * Returns BFV.battery and resets BFV.updatedBattery to false.
     * Should only be called after isUpdatedBattery() return true,
     * otherwise it will return default value -> Double.NaN
     *
     * @return BFV.battery
     */
    public Double getBattery() {
        this.updatedBattery = false;
        return this.battery;
    }

    /**
     * Check if BFV.hardwareVersion has been updated since last getHwVersion() call
     *
     * @return true if hw version has been updated, false otherwise
     */
    public boolean isUpdatedHardwareVersion() {
        return updatedHardwareVersion;
    }

    /**
     * Check if BFV.altitude has been updated since last getAltitude() call
     *
     * @return true if BFV.altitude has been updated, false otherwise
     */
    public boolean isUpdatedAltitude() {
        return updatedAltitude;
    }

    /**
     * Check if BFV.temperature has been updated since last getTemperature() call
     *
     * @return true if BFV.temperature has been updated, false otherwise
     */
    public boolean isUpdatedTemperature() {
        return updatedTemperature;
    }

    /**
     * Check if BFV.battery has been updated since last getTemperature() call
     *
     * @return true if battery has been updated, false otherwise
     */
    public boolean isUpdatedBattery() {
        return updatedBattery;
    }

    /**
     * Checks if values have been modified since last calling this method
     * and resets BFV.updatedValues to false
     *
     * @return true if values have been modified since last check, false othewise
     */
    public boolean checkUpdatedValues() {
        boolean ret = updatedValues;
        this.updatedValues = false;
        return ret;
    }

    /**
     * Updates BFV.parameterKeys with keys provided ands sets hasParameterKeys to true
     *
     * @param keys to replace parameterKeys
     */
    private void updateParameterKeys(String[] keys) {
        parameterKeys = keys;
        hasParameterKeys = true;
    }

    /**
     * Updates BFV.userValues with values provided, sets hasValues to true
     * and calls updateAllValues to update all parameters values.
     *
     * @param values to replace userValues
     */
    private void updateValues(int[] values) {
        userValues = values;
        hasValues = true;
        updateAllValues();
    }

    /**
     * Updates all parameter.userValues for each parameter in PARAMETERS if
     * we have keys and values previously set and they are of the same length.
     * Assumes that value at position n belongs to key at position n!
     *
     */
    private void updateAllValues() {
        if(hasParameterKeys && hasValues &&
                (parameterKeys.length == userValues.length)) {
            for (int i = 0; i < parameterKeys.length && i < userValues.length; i++) {
                PARAMETERS.get(getParameterName(parameterKeys[i])).setFromParsed(userValues[i]);
            }
            updatedValues = true;
        }
    }

    /**
     * Resets all parameter.userValue in PARAMETERS
     *
     */
    private void resetAllParameterValues() {
        for (Command command: this.PARAMETERS.values()) {
            command.resetValue();
        }
    }

    /**
     * Convenient way to get parameter name from parameter code ie. 'BVU' -> 'volumeUp'
     *
     * @param code to get name from
     * @return name of provided code
     */
    private String getParameterName(String code) {
        return parameterCodeToName.get(code);
    }

    /**
     * Returns decoded hw version from line
     *
     * @param line to decode hw version from
     * @return decoded hw version
     */
    private static String getHwVersionFromLine(String line) {
        String[] split = line.split(" ");
        String version;
        if (split.length > 2) {
            version = split[1] + "." + split[2];
        } else {
            version = split[1];
        }
        return version;
    }

    /**
     * Returns decoded temperature from line
     *
     * @param line to decode temperature from
     * @return decoded temperature
     */
    private static Double getTemperatureFromLine(String line) {
        String value = line.split(" ")[1];
        return Double.parseDouble(value) / 10.0;
    }

    /**
     * Returns decoded battery from line
     *
     * @param line to decode battery from
     * @return decoded battery
     */
    private static Double getBatteryFromLine(String line) {
        String value = line.split(" ")[1];
        return Integer.parseInt(value, 16) / 1000.0;
    }

    /**
     * Returns decoded altitude from line
     *
     * @param line to decode altitude from
     * @return decoded altitude
     */
    private static Double getAltitudeFromLine(String line, Double qnh) {
        String[] split = line.split(" ");
        if(split.length > 1) {
            int pressure = Integer.parseInt(split[1], 16);
            return 44330.0 * (1 - Math.pow((pressure / qnh), 0.190295));
        }
        return Double.NaN;
    }

    /**
     * Updates BFV.hardwareVersion from provided line
     *
     * @param line to decode hw version from
     */
    private void setHardwareVersion(String line) {
        this.updatedHardwareVersion = true;
        this.hardwareVersion = getHwVersionFromLine(line);
    }

    /**
     * Updates BFV.temperature from provided line
     *
     * @param line to decode temperature from
     */
    private void setTemperature(String line) {
        this.updatedTemperature = true;
        this.temperature = getTemperatureFromLine(line);
    }

    /**
     * Updates BFV.battery from provided line
     *
     * @param line to decode battery from
     */
    private void setBattery(String line) {
        this.updatedBattery = true;
        this.battery = getBatteryFromLine(line);
    }

    /**
     * Updates BFV.altitude from provided line, if provided altitude is same as
     * last altitude - does nothing
     *
     * @param line to decode altitude from
     */
    private void setAltitudeFomDevice(String line) {
        Double altitude = getAltitudeFromLine(line, this.qnh);
        if(this.altitude != altitude) {
            this.altitude = altitude;
            this.updatedAltitude = true;
        }
    }


    public static void main(String[] args){
        BFV device = new BFV();

        System.out.println("ParameterNames(" + device.getAllParameters().keySet().size() + "): " + device.getAllParameters().keySet());
        ArrayList<String> parameterNames = new ArrayList<>();
        for (String command : device.getAllParameters().keySet()) {
            parameterNames.add(device.getAllParameters().get(command).getCommandCode());
        }
        System.out.println("ParameterCodes(" + parameterNames.size() + "): " + parameterNames);

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

        device.updatedValues = true;
        System.out.println(device.checkUpdatedValues());
        System.out.println(device.checkUpdatedValues());

        Map<String, Command> parameters = device.getAllParameters();

        System.out.println("PARAMETERS: buzzerThreshold");
        System.out.println(parameters.get("buzzerThreshold").hasValue());
        parameters.get("buzzerThreshold").setValue(8.0);
        System.out.println(parameters.get("buzzerThreshold").hasValue());
        parameters.get("buzzerThreshold").setValue(8);
        System.out.println(parameters.get("buzzerThreshold").hasValue());
        parameters.get("buzzerThreshold").setValue("8");
        System.out.println(parameters.get("buzzerThreshold").hasValue());
        parameters.get("buzzerThreshold").setValue("8.0");
        System.out.println(parameters.get("buzzerThreshold").hasValue());

        System.out.println("PARAMETERS: outputQNH");
        System.out.println(parameters.get("outputQNH").hasValue());
        parameters.get("outputQNH").setValue(80000.0);
        System.out.println(parameters.get("outputQNH").hasValue());
        parameters.get("outputQNH").setValue(80000);
        System.out.println(parameters.get("outputQNH").hasValue());
        parameters.get("outputQNH").setValue("80000");
        System.out.println(parameters.get("outputQNH").hasValue());
        parameters.get("outputQNH").setValue("80000.0");
        System.out.println(parameters.get("outputQNH").hasValue());


        Command cmd = Command.Builder("", "").build();
        System.out.println("Result: " + (0 > cmd.getMinVal()));
        System.out.println("Result: " + cmd.getValueAsString());
        System.out.println("Result: " + cmd.getDefaultValueAsString());

        // TODO: add tests
    }
}
