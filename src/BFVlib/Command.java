package BFVlib;

/**
 * Command object is used to hold BlueFlyVario command codes and
 * provides serialization of said codes with or without user provided values / arguments
 *
 */
public class Command {
    private final String code;
    private final String description;

    private static final String PREFIX = "$";
    private static final String SUFFIX = "*";

    private boolean hasDefaultValue = false;
    private int defaultValue = -1;

    private boolean hasValue = false;
    private int userValue = -1;

    private boolean acceptsArguments = false;
    private String defaultArguments;

    private boolean hasArguments = false;
    private String userArguments;

    private boolean hasMinHWVersion = false;
    private int minHWVersion;

    private boolean hasParameters = false;
    private int type;
    private int minVal;
    private int maxVal;
    private double factor;

    private boolean isPmtk = false;

    /**
     * Constructor of builder that is used to create BlueFlyVario device commands
     *
     * @param code command code
     * @param description command description
     * @return Builder with provided code and description
     */
    protected static Builder Builder(String code, String description) {
        return new Builder(code, description);
    }

    /**
     * Builder that provides methods for setting command fields
     *
     */
    protected static final class Builder {
        private final Command cmd;

        Builder(String code, String description) {
            assert ( ! code.isEmpty() && ! description.isEmpty());
            cmd = new Command(code, description);

            if (code.startsWith("PMTK")){
                cmd.isPmtk = true;
            }
        }

        /**
         * Sets acceptsArguments to provided value
         *
         * @param value to set acceptsArguments to
         */
        Builder setAcceptsArguments(Boolean value) {
            cmd.acceptsArguments = value;
            return this;
        }

        /**
         * Sets defaultArguments to provided value
         *
         * @param value to set defaultArguments to
         */
        Builder setDefaultArguments(String value) {
            cmd.defaultArguments = value;
            return this;
        }

        /**
         * Sets minHWVersion to provided value and hasMinHWVersion to true
         *
         * @param value to set minHWVersion to
         */
        Builder setMinHwVersion(int value) {
            cmd.minHWVersion = value;
            cmd.hasMinHWVersion = true;
            return this;
        }

        /**
         * Sets parameters(type, minVal, maxVal, factor) to provided values
         * and hasParameters to true
         *
         * @param type of parameter
         * @param minVal min value of the parameter, must be >= 0
         * @param maxVal max value of the parameter, must be <= 65535
         * @param factor of the parameter value
         */
        Builder setParameters(int type, int minVal, int maxVal, double factor) {
            assert (type >= BFV.TYPE_INT && type <=BFV.TYPE_INTLIST);
            cmd.type = type;

            assert (minVal >= 0);
            cmd.minVal = minVal;

            assert (maxVal <= 65535);
            cmd.maxVal = maxVal;

            cmd.factor = factor;
            cmd.hasParameters = true;
            return this;
        }

        /**
         * Sets defaultValue to provided values and hasDefaultValue to true
         *
         * @param value to set defaultValue to
         */
        Builder setDefaultValue(int value) {
            assert (value >= cmd.getMinVal() && value <= cmd.getMaxVal());
            cmd.defaultValue = value;
            cmd.hasDefaultValue = true;
            return this;
        }

        /**
         * Builds the Command
         *
         * @return built Command
         */
        Command build() {
            return cmd;
        }
    }

    /**
     * Sets userValue to -1 and hasValue to false
     *
     */
    protected void resetValue() {
        this.userValue = -1;
        this.hasValue = false;
    }

    /**
     * Sets userValue from provided int used in parser to set parameter values from device
     * where all values are already in proper form so we dont need to convert them
     *
     * @param iValue value to set
     * @return true if value was set, false otherwise
     */
    protected boolean setFromParsed(int iValue) {
        if(iValue >= 0) {
            userValue = iValue;
            hasValue = true;
            return true;
        }
        else {
            return false;
        }
    }

    /**
     * Serializes command with arguments provided in format that
     * BlueFlyVario device accepts via serial / bluetooth
     *
     * @param arguments to add to command
     * @return serialized command with arguments
     */
    public final String serializeCommand(String arguments) {
        this.userArguments = arguments;
        this.hasArguments = true;
        return serializeCommand();
    }

    /**
     * Serializes command in format that BlueFlyVario device accepts via serial / bluetooth
     *
     * @return serialized command
     */
    public final String serializeCommand() {
        // create plain _code
        String _code = this.code;

        // compile PMTK _code
        if(_code.startsWith("PMTK")) {
            _code = PREFIX +
                    _code +
                    SUFFIX +
                    Integer.toHexString(pmtkChecksum(_code)).toUpperCase() +
                    "\r\n";
        }
        else {
            // add arguments
            if(this.acceptsArguments) {
                _code = _code + " ";

                if(hasArguments) {
                    _code = _code + userArguments;
                }
                else {
                    _code = _code + defaultArguments;
                }
            }

            // add parameters
            else if(this.hasParameters && this.hasValue) {
                _code = _code + " " + this.userValue;
            }

            // add prefix and suffix
            _code = PREFIX + _code + SUFFIX;
        }

        return _code;
    }

    /**
     * @return true if Command accepts arguments, false otherwise
     */
    public final boolean acceptsArguments(){
        return acceptsArguments;
    }

    /**
     * @return true if Parameter has minimal required hardware version, false otherwise
     */
    public final boolean hasMinHWVersion(){
        return hasMinHWVersion;
    }

    /**
     * @return true if Command has Parameters, false otherwise
     */
    public final boolean hasParameters(){
        return hasParameters;
    }

    /**
     * @return true if Parameter has default value, false otherwise
     */
    public final boolean hasDefaultValue(){
        return hasDefaultValue;
    }

    /**
     * @return true if Parameter has user value, false otherwise
     */
    public final boolean hasValue() {
        return hasValue;
    }

    /**
     * One of: TYPE_INT, TYPE_DOUBLE, TYPE_INTOFFSET, TYPE_BOOLEAN, TYPE_INTLIST
     *
     * @return type of the Parameter
     */
    public final int getType(){
        return type;
    }

    /**
     * @return code of the Command
     */
    public final String getCommandCode(){
        return code;
    }

    /**
     * @return description of the Command
     */
    public final String getDescription(){
        return description;
    }

    /**
     * @return Commands minimal supported hardware wersion
     */
    public int getMinHWVersion() {
        return minHWVersion;
    }

    /**
     * @return Commands default arguments
     */
    public final String getDefaultArguments(){
        return defaultArguments;
    }

    /**
     * @return defaultValue formatted for sending to the device
     */
    public final int getDefaultValue(){
        return defaultValue;
    }

    /**
     * @return userValue formatted for sending to the device
     */
    public final int getValue() {
        return userValue;
    }

    /**
     * Converts user value to string
     *
     * @return userValue as string if value is set, null otherwise
     */
    public String getValueAsString() {
        if (this.userValue != -1) {
            return valueToString(this.userValue);
        }
        else {
            return null;
        }
    }

    /**
     * Converts default value to string
     *
     * @return defaultValue as string if value is set, null otherwise
     */
    public String getDefaultValueAsString() {
        if (this.defaultValue != -1) {
            return valueToString(this.defaultValue);
        }
        else {
            return null;
        }
    }

    /**
     * @return minimum allowed command value
     */
    public int getMinVal() {
        return minVal;
    }

    /**
     * @return maximum allowed command value
     */
    public int getMaxVal() {
        return maxVal;
    }

    /**
     * @return factor of command value
     */
    public double getFactor() {
        return factor;
    }

    /**
     * @return true if command is pmtk command, false otherwise
     */
    public boolean isPmtk() {
        return isPmtk;
    }

    /**
     * Sets userValue from provided int if it converts to this Commands type of value
     *
     * @param iValue value to set
     * @return true if value was set, false otherwise
     */
    public boolean setValue(int iValue) {
        return setValueFromUncheckedUserProvidedValue((double) iValue);
    }

    /**
     * Sets userValue from provided Double if it converts to this Commands type of value
     *
     * @param dValue value to set
     * @return true if value was set, false otherwise
     */
    public boolean setValue(Double dValue) {
        return setValueFromUncheckedUserProvidedValue(dValue);
    }

    /**
     * Sets userValue from provided boolean if it converts to this Commands type of value
     *
     * @param bValue value to set
     * @return true if value was set, false otherwise
     */
    public boolean setValue(boolean bValue) {
        return setValueFromUncheckedUserProvidedValue(new Double(String.valueOf(bValue)));
    }

    /**
     * Sets userValue from provided String if it converts to this Commands type of value
     *
     * @param sValue value to set
     * @return true if value was set, false otherwise
     */
    public boolean setValue(String sValue) {
        return setValueFromUncheckedUserProvidedValue(Double.valueOf(sValue));
    }

    /**
     * Check if dValue is valid for this Command
     *
     * @param dValue user provided value
     * @return proper int value if value combined with factor is in range
     * "min <= value <= max", -1 otherwise
     */
    private int toIntValueChecked(Double dValue) {
        // filter only double and int
        switch(this.getType()) {

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
        if(isInRange(dValue.intValue())) {
            return dValue.intValue();
        } else {
            return -1;
        }
    }

    /**
     * Sets command code and description
     *
     * @param code of command
     * @param description of command
     */
    private Command(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * Check if provided value is in this commands range of accepted values
     *
     * @param value to check
     * @return true if Command.minVal <= value <= Command.maxVal
     */
    private boolean isInRange(int value) {
        return (this.getMinVal() <= value && value <= this.getMaxVal());
    }

    /**
     * Converts value to string base on this Commands type
     *
     * @param value to convert
     * @return converted value
     */
    private String valueToString(int value) {
        String ret = null;
        switch(this.getType()){
            case BFV.TYPE_INT:
                ret = String.valueOf(value);
                break;
            case BFV.TYPE_DOUBLE:
                ret = String.valueOf(value / this.getFactor());
                break;
            case BFV.TYPE_INTOFFSET:
                ret = String.valueOf((int) (value + this.getFactor()));
                break;
            case BFV.TYPE_BOOLEAN:
                ret = String.valueOf(value != 0);
                break;
        }
        return ret;
    }

    /**
     * Sets value to user provided value if it converts to this Commands type of value
     *
     * @param value to set
     * @return true if value was set, false otherwise
     */
    private boolean setValueFromUncheckedUserProvidedValue(Double value) {
        int converted = toIntValueChecked(value);
        if(converted >= 0) {
            userValue = converted;
            hasValue = true;
            return true;
        }
        else {
            return false;
        }
    }

    /**
     * Calculates checksum for pmtk Command
     *
     * @param value String to calculate checksum for
     * @return calculated checksum
     */
    private static int pmtkChecksum(String value) {
        char[] msgArray = value.toCharArray();

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
}
