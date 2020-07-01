package BFVlib;


public class Command {
    private String command;
    private String description;

    private static final String PREFIX = "$";
    private static final String SUFFIX = "*";

    private boolean hasUserValue = false;
    private int userValue;

    private boolean hasArguments = false;
    private String defaultArguments;

    private boolean hasUserArguments = false;
    private String userArguments;

    private boolean hasDefaultValue = false;
    private int defaultValue;

    private boolean hasParameters = false;
    private int type;
    private int minHWVersion;
    private int minVal;
    private int maxVal;
    private double factor;

    private boolean isPmtk = false;


    private Command(String command, String description) {
        this.command = command;
        this.description = description;
    }

    static Builder Builder(String command, String description) {
        return new Builder(command, description);
    }

    static final class Builder {
        private Command cmd;

        Builder(String command, String description) {
            assert (!command.isEmpty() && !description.isEmpty());
            cmd = new Command(command, description);

            if (command.startsWith("PMTK")){
                cmd.isPmtk = true;
            }
        }

        Builder setDefaultArguments(String val) {
            cmd.defaultArguments = val;
            cmd.hasArguments = true;
            return this;
        }

        Builder setParameters(int type, int minHWVersion, int minVal, int maxVal, double factor) {
            assert (type >= BFV.TYPE_INT && type <=BFV.TYPE_INTLIST);
            cmd.type = type;
            cmd.minHWVersion = minHWVersion;

            assert (minVal >= 0);
            cmd.minVal = minVal;

            assert (maxVal <= 65535);
            cmd.maxVal = maxVal;

            cmd.factor = factor;
            cmd.hasParameters = true;
            return this;
        }

        Builder setDefaultValue(int value) {
            assert (value >= cmd.getMinVal() && value <= cmd.getMaxVal());
            cmd.defaultValue = value;
            cmd.hasDefaultValue = true;
            return this;
        }

        Command build() {
            return cmd;
        }
    }

    private static int pmtkChecksum(String s) {
        char[] msgArray = s.toCharArray();

        int checksum = 0;
        for ( char c : msgArray )
        {
            if ( (c == '!') || (c == '$')){
                return -1;
            }
            if (c == '*')
                break;

            checksum ^= c;
        }
        return checksum;
    }

    public final boolean hasParameters(){
        return hasParameters;
    }

    public final boolean hasArguments(){
        return hasArguments;
    }

    public final boolean hasDefaultValue(){
        return hasDefaultValue;
    }

    public final boolean hasUserValue() {
        return hasUserValue;
    }

    public final int getType(){
        return type;
    }

    public final int getDefaultValue(){
        return defaultValue;
    }

    public final String getCommandCode(){
        return command;
    }

    public final String getCompiledCommand(String arguments) {
        this.userArguments = arguments;
        this.hasUserArguments = true;
        return getCompiledCommand();
    }

    public final String getCompiledCommand() {
        // create plain command
        String command = this.command;

        // compile PMTK command
        if(command.startsWith("PMTK")) {
            command = PREFIX +
                    command +
                    SUFFIX +
                    Integer.toHexString(pmtkChecksum(command)).toUpperCase() +
                    "\r\n";
        }
        else {
            // add arguments
            if(this.hasArguments) {
                command = command + " ";

                if(hasUserArguments) {
                    command = command + userArguments;
                }
                else {
                    command = command + defaultArguments;
                }
            }

            // add parameters
            else if(this.hasParameters && this.hasUserValue) {
                command = command + " " + this.userValue;
            }

            // add prefix and suffix
            command = PREFIX + command + SUFFIX;
        }

        return command;
    }

    public final String getDescription(){
        return description;
    }

    public final String getDefaultArguments(){
        return defaultArguments;
    }

    public final int getUserValue() {
        return userValue;
    }

    public int getMinHWVersion() {
        return minHWVersion;
    }

    public int getMinVal() {
        return minVal;
    }

    public int getMaxVal() {
        return maxVal;
    }

    public double getFactor() {
        return factor;
    }

    public boolean isPmtk() {
        return isPmtk;
    }

    /**
     * @param value >= 0
     * @return true if value was set, false otherwise
     */
    public boolean setUserValue(int value) {
        if(value >= 0) {
            userValue = value;
            hasUserValue = true;
            return true;
        }
        else {
            return false;
        }
    }

    private String convertValueToString(int value) {
        String converted = null;
        switch(this.getType()){
            case BFV.TYPE_INT:
                converted = String.valueOf(value);
                break;
            case BFV.TYPE_DOUBLE:
                converted = String.valueOf(value / this.getFactor());
                break;
            case BFV.TYPE_INTOFFSET:
                converted = String.valueOf((int) (value + this.getFactor()));
                break;
            case BFV.TYPE_BOOLEAN:
                converted = String.valueOf(value != 0);
                break;
        }
        return converted;
    }

    public String convertUserValueToString() {
        return convertValueToString(this.userValue);
    }

    public String convertDefaultValueToString() {
        return convertValueToString(this.defaultValue);
    }

    private boolean isInRange(Double value) {
        return (this.getMinVal() <= value && value <= this.getMaxVal());
    }

    /**
     * @param value user provided value
     * @return converted int value if value converts to number and is in range "min <= value <= max", -1 otherwise
     * @throws NullPointerException– when the string parsed is null
     * @throws NumberFormatException– when the string parsed does not contain a parsable number
     */
    public int convertValueToInt(String value) {
        int converted = -1;
        double dValue = Double.parseDouble(value);

        // filter only double and int
        switch(this.getType()){

            // if type int do nothing
            case BFV.TYPE_INT:
                break;

            // if type double convert to double * factor
            case BFV.TYPE_DOUBLE:
                dValue = dValue * this.factor;
                break;

            // if type int_offset convert to int - factor
            case BFV.TYPE_INTOFFSET:
                dValue = dValue - this.factor;
                break;

            // if type bool convert to 0 or 1
            case BFV.TYPE_BOOLEAN:
                dValue = dValue == 0.0 ? 0.0 : 1.0;
                break;
        }

        // check if its in range
        if(isInRange(dValue)) {
            converted = (int) dValue;
        }

        return converted;
    }

    void resetUserValue() {
        this.userValue = -1;
        this.hasUserValue = false;
    }

}
