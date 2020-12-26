import BFVLib.BFV;
import BFVLib.Command;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Map;


public class BFVTest {
    /**
     * Test if we get all expected commands names and commands codes,
     * If you are adding new commands or editing default values make sure to
     * take appropriate actions so test can pass
     */
    @Test
    void getAllCommands() {
        BFV device = new BFV();
        Map<String, Command> COMMANDS = device.getAllCommands();

        String[] expected_commands_names = {
                "eraseLocus", "getSettings", "getTemp", "playSound", "queryLocus", "queryLocusData", "reset",
                "restoreDefaults", "setBluetoothName", "simulateButton", "sleep", "sleepNoWake", "volumeDown",
                "volumeUp" };

        String[] expected_parameter_codes = {
                "PMTK184,1", "BST", "TMP", "BSD", "PMTK183", "PMTK622,0", "RST", "RSX",
                "RNC SN,", "BTN", "SLP", "SLX", "BVD", "BVU" };

        ArrayList<String> commandCodes = new ArrayList<>();
        for (String command : device.getAllCommands().keySet()) {
            commandCodes.add(device.getAllCommands().get(command).getCommandCode());
        }

        Assertions.assertArrayEquals(COMMANDS.keySet().toArray(), expected_commands_names);
        Assertions.assertArrayEquals(commandCodes.toArray(), expected_parameter_codes);
    }

    /**
     * Test if we get all expected parameters names, codes, and default values
     * If you are adding new parameters make sure to include them here so test can pass
     * if you are modi
     */
    @Test
    void getAllParameters() {
        BFV device = new BFV();
        Map<String, Command> PARAMETERS = device.getAllParameters();

        String[] expected_parameter_names = {
                "buzzerThreshold", "gpsLogInterval", "greenLED", "heightSeconds", "heightSensitivityDm",
                "isPrintPressure", "liftFreqBase", "liftFreqIncrement", "liftOffThreshold", "liftThreshold",
                "outputFrequency", "outputMode", "outputQNH", "positionNoise", "quietStart", "rateMultiplier",
                "secondsBluetoothWait", "sinkFreqBase", "sinkFreqIncrement", "sinkOffThreshold", "sinkThreshold",
                "speedMultiplier", "startDelayMS", "toggleThreshold", "uart1BRG", "uart1Raw", "uart2BRG",
                "uartPassthrough", "useAudioBuzzer", "useAudioWhenConnected", "useAudioWhenDisconnected",
                "usePitot", "volume"};

        String[] expected_parameter_codes = {
                "BZT", "BGL", "BLD", "BHT", "BHV", "BFP", "BFQ", "BFI", "BOL", "BFL", "BOF",
                "BOM", "BQH", "BFK", "BQS", "BRM", "BTH", "BSQ", "BSI", "BOS", "BFS", "BSM",
                "BDM", "BTT", "BRB", "BUR", "BR2", "BPT", "BBZ", "BAC", "BAD", "BUP", "BVL" };

        Integer[] expected_parameter_default_values = {
                40, 10, 1, 600, 20, 1, 1000, 100, 5, 20, 1, 0, 21325, 100, 0, 100,
                180, 400, 100, 5, 20, 100, 0, 100, 207, 0, 16, 1, 0, 0, 1, 0, 1000};

        ArrayList<String> parameterCodes = new ArrayList<>();
        for (String command : PARAMETERS.keySet()) {
            parameterCodes.add(PARAMETERS.get(command).getCommandCode());
        }

        ArrayList<Integer> parameterDefaultValues = new ArrayList<>();
        for (String command : PARAMETERS.keySet()) {
            parameterDefaultValues.add(PARAMETERS.get(command).getDefaultValue());
        }

        Assertions.assertArrayEquals(PARAMETERS.keySet().toArray(), expected_parameter_names);
        Assertions.assertArrayEquals(parameterCodes.toArray(), expected_parameter_codes);
        Assertions.assertArrayEquals(parameterDefaultValues.toArray(), expected_parameter_default_values);
    }

    @Test
    void setQnh() {
        BFV device = new BFV();

        // By default is NaN
        Assertions.assertEquals(device.getAltitude(), Double.NaN);

        // Parse with default value
        device.parseLine("PRS 18BCD");  // hex 101325
        Assertions.assertEquals(device.getAltitude(), 0.0);

        // Actual value change tests
        device.setQnh(0.0);
        device.parseLine("PRS 1");
        Assertions.assertTrue(device.getAltitude().isInfinite());

        device.setQnh(1.0);
        device.parseLine("PRS 1");
        Assertions.assertEquals(device.getAltitude(), 0.0);
    }

    @Test
    void resetAllValues() {
        BFV device = new BFV();
        Map<String, Command> PARAMETERS = device.getAllParameters();

        device.parseLine("BFV 0");
        device.parseLine("BAT 0");
        device.parseLine("PRS 0");
        device.parseLine("TMP 0");

        device.resetAllValues();

        Assertions.assertEquals(device.getAltitude(), Double.NaN);
        Assertions.assertEquals(device.getBattery(), Double.NaN);
        Assertions.assertEquals(device.getTemperature(), Double.NaN);
        Assertions.assertEquals(device.getHwVersion(), "");

        ArrayList<Integer> parameterUserValues = new ArrayList<>();
        for (String command : PARAMETERS.keySet()) {
            parameterUserValues.add(PARAMETERS.get(command).getValue());
        }

        for (int value : parameterUserValues) {
            Assertions.assertEquals(value, -1);
        }
    }

    @Test
    void getHwVersion() {
        BFV device = new BFV();

        Assertions.assertEquals(device.getHwVersion(), "");

        device.parseLine("BFV 0");
        Assertions.assertEquals(device.getHwVersion(), "0");

        device.parseLine("BFV 0.0");
        Assertions.assertEquals(device.getHwVersion(), "0.0");
    }

    @Test
    void getAltitude() {
        BFV device = new BFV();

        Assertions.assertEquals(device.getAltitude(), Double.NaN);

        device.parseLine("PRS 0");
        Assertions.assertEquals(device.getAltitude(), 44330.0);

        device.parseLine("PRS 18BCD");  // hex 101325
        Assertions.assertEquals(device.getAltitude(), 0.0);
    }

    @Test
    void getTemperature() {
        BFV device = new BFV();

        Assertions.assertEquals(device.getTemperature(), Double.NaN);

        device.parseLine("TMP 0");
        Assertions.assertEquals(device.getTemperature(), 0);

        device.parseLine("TMP 100");
        Assertions.assertEquals(device.getTemperature(), 10);
    }

    @Test
    void getBattery() {
        BFV device = new BFV();

        Assertions.assertEquals(device.getBattery(), Double.NaN);

        device.parseLine("BAT 0");
        Assertions.assertEquals(device.getBattery(), 0);

        device.parseLine("BAT 3E8");  // hex 1000
        Assertions.assertEquals(device.getBattery(), 1);
    }

    @Test
    void isUpdatedHardwareVersion() {
        BFV device = new BFV();

        Assertions.assertFalse(device.isUpdatedHardwareVersion());

        device.parseLine("BFV 0");
        Assertions.assertTrue(device.isUpdatedHardwareVersion());
        device.getHwVersion();
        Assertions.assertFalse(device.isUpdatedHardwareVersion());
    }

    @Test
    void isUpdatedAltitude() {
        BFV device = new BFV();

        Assertions.assertFalse(device.isUpdatedAltitude());

        device.parseLine("PRS 0");
        Assertions.assertTrue(device.isUpdatedAltitude());
        device.getAltitude();
        Assertions.assertFalse(device.isUpdatedAltitude());
    }

    @Test
    void isUpdatedTemperature() {
        BFV device = new BFV();

        Assertions.assertFalse(device.isUpdatedTemperature());

        device.parseLine("TMP 0");
        Assertions.assertTrue(device.isUpdatedTemperature());
        device.getTemperature();
        Assertions.assertFalse(device.isUpdatedTemperature());
    }

    @Test
    void isUpdatedBattery() {
        BFV device = new BFV();

        Assertions.assertFalse(device.isUpdatedBattery());

        device.parseLine("BAT 0");
        Assertions.assertTrue(device.isUpdatedBattery());
        device.getBattery();
        Assertions.assertFalse(device.isUpdatedBattery());
    }

    @Test
    void checkUpdatedValues() {
        BFV device = new BFV();

        Assertions.assertFalse(device.checkUpdatedValues());

        device.parseLine("");
        Assertions.assertFalse(device.checkUpdatedValues());

        device.parseLine( "BST BFK BFL BFP BAC BAD BTH BFQ BFI BSQ BSI BFS BOL BOS BRM BVL BOM" +
                        " BOF BQH BRB BPT BUR BLD BR2 BHV BHT BBZ BZT BSM BUP BTT BDM BQS BGL");
        device.parseLine("SET 0 100 35 1 1 1 180 1000 100 400 100 300 20 190 100 1000 0 1 21325" +
                        " 207 1 0 0 16 20 1200 0 60 100 0 2000 0 0 10");
        Assertions.assertTrue(device.checkUpdatedValues());
        Assertions.assertFalse(device.checkUpdatedValues());
    }
}