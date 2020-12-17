package BFVlib;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * UNTESTED!!!! as provided by BlueFlyVario flying instrument creator mr. Alistair Dickie
 *
 */
public class PMTKParser {
    private PrintWriter printWriterMTK;
    private PrintWriter printWriterCSV;

    /**
     * UNTESTED!!!! as provided by BlueFlyVario flying instrument creator mr. Alistair Dickie
     *
     * @param line UNTESTED!!!! as provided by BlueFlyVario flying instrument creator mr. Alistair Dickie
     */
    public PMTKParser(String line){
        String[] mainSplit = line.split("\\*");
        String[] split = mainSplit[0].split(",");

        if(split[0].equals("$PMTKLOX")){
            if(split[1].equals("0")){
                try {
                    printWriterMTK = new PrintWriter(new FileWriter("locus_record.mtk"));
                    printWriterCSV = new PrintWriter(new FileWriter("locus_record.csv"));

                    printWriterMTK.println(line);
                }
                catch (IOException e){
                    // e.printStackTrace();
                }

            }
            if(split[1].equals("1")){
                int fourByteNum = 0;
                StringBuilder record = new StringBuilder();
                for(int b = 3; b < split.length; b++){
                    record.append(split[b]);
                    fourByteNum++;
                    if(fourByteNum == 4){
                        fourByteNum = 0;
                        byte[] bytes = hexStringToByteArray(record.toString());

                        byte[] timeBytes = new byte[4];
                        timeBytes[0] = bytes[0];
                        timeBytes[1] = bytes[1];
                        timeBytes[2] = bytes[2];
                        timeBytes[3] = bytes[3];
                        int timeStamp = ByteBuffer.wrap(timeBytes).order(ByteOrder.LITTLE_ENDIAN).getInt();

//                        int timeStamp = ((bytes[3] & 0xFF) << 24)|((bytes[2] & 0xFF) << 16)|((bytes[1] & 0xFF) << 8 )|((bytes[0] & 0xFF) << 0 );
                        Date time = new java.util.Date((long)timeStamp*1000);

                        int fix = bytes[4];

                        byte[] latBytes = new byte[4];
                        latBytes[0] = bytes[5];
                        latBytes[1] = bytes[6];
                        latBytes[2] = bytes[7];
                        latBytes[3] = bytes[8];
                        float latitude = ByteBuffer.wrap(latBytes).order(ByteOrder.LITTLE_ENDIAN).getFloat();

                        byte[] longBytes = new byte[4];
                        longBytes[0] = bytes[9];
                        longBytes[1] = bytes[10];
                        longBytes[2] = bytes[11];
                        longBytes[3] = bytes[12];
                        float longitude = ByteBuffer.wrap(longBytes).order(ByteOrder.LITTLE_ENDIAN).getFloat();

                        byte[] heightBytes = new byte[4];
                        heightBytes[0] = bytes[13];
                        heightBytes[1] = bytes[14];
                        heightBytes[2] = 0;
                        heightBytes[3] = 0;
                        int height = ByteBuffer.wrap(heightBytes).order(ByteOrder.LITTLE_ENDIAN).getInt();

                        if(fix == 2){
                            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-DD'T'HH:mm:ss'Z'");
                            dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                            String CSVLine = timeStamp + "," + dateFormat.format(time) + "," + fix + "," + latitude + "," + longitude + "," + height;
                            printWriterCSV.println(CSVLine);
                        }
                        record = new StringBuilder();
                        printWriterMTK.println(line);
                    }
                }
            }

            if(split[1].equals("2")){
                printWriterMTK.println(line);
                printWriterMTK.close();
                printWriterCSV.close();

            }
        }
    }

    /**
     * UNTESTED!!!! as provided by BlueFlyVario flying instrument creator mr. Alistair Dickie
     *
     */
    private static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        assert len % 2 == 1;
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

}
