/* NAME: Christopher Harris  LOGIN: charris */

package rdt;

import java.net.*;

import static rdt.RDT.*;

/** Handles creation of packets, int/byte data conversion and simulation of packet loss over the network
 *
 * @author Chris Harris
 */
public class Utility {

	private static final int MAX_NETWORK_DELAY = 200; // msec

    /**
     *
     * @param seg RDT segment to be sent over the network
     * @param socket datagram socket used for transmission
     * @param ip IP address of the recipient
     * @param port port number of the recipient
     * @param resend boolean value representing whether or not this is the first transmission of this segment
     */
	public static void udp_send (RDTSegment seg, DatagramSocket socket, InetAddress ip, int port, boolean resend) {
        // Simulate network loss
		double d = RDT.random.nextDouble();
		if ( d < RDT.lossRate) {
            if (seg.containsData()) {
                System.out.println(System.currentTimeMillis() + ":" + ANSI_RED + " LOST SEGMENT:" + ANSI_RESET + " SeqNum=" +
                        seg.seqNum);
            }
            else {
                System.out.println(System.currentTimeMillis() + ":" + ANSI_RED + " LOST ACK:" + ANSI_RESET + " AckNum=" +
                        seg.ackNum);
            }

			System.out.flush();
	        return;
	    }

		// Prepare UDP payload
		int payloadSize = seg.length + RDTSegment.HDR_SIZE;
		byte[] payload = new byte[payloadSize];
		makePayload(seg, payload);

		// Send over UDP
		// Simulate random network delay
		int delay = RDT.random.nextInt(MAX_NETWORK_DELAY);
		try {
			Thread.sleep(delay);
			socket.send(new DatagramPacket(payload, payloadSize, ip, port));
		} catch (Exception e) {
			System.out.println("udp_send: " + e);
		}

		// Print information about the transmission to stdout
		String segData = dataToString(seg.getData());
		String status;

		if (resend) {
		    status = "RESEND: ";
        }
        else {
		    status = "SEND: ";
        }

		if (seg.containsAck()) {
            System.out.println(System.currentTimeMillis() + ": " + ANSI_GREEN + status + ANSI_RESET + "SeqNum="
                    + seg.seqNum + ANSI_GREEN + " AckNum=" + seg.ackNum + ANSI_RESET + " Delay=" + delay);
        }
        else if (seg.containsFin()) {
            System.out.println(System.currentTimeMillis() + ": " + ANSI_PURPLE + "SEND FIN:" + ANSI_RESET + " Delay=" + delay);
        }
        else {
            System.out.println(System.currentTimeMillis() + ": " + ANSI_GREEN + status + ANSI_RESET + "Segment <" + seg + "> " + ANSI_GREEN + "SeqNum="
                    + seg.seqNum + ANSI_RESET + " AckNum=" + seg.ackNum + " Delay=" + delay + " Checksum=" + seg.checksum + " Data=" + segData + "");
        }

        System.out.flush();
		// end print

		return;
	}

    /** Converts int to byte and stores value in the segments data array
     *
     * @param intValue integer to convert
     * @param data byte array for storing the converted value
     * @param idx index of shifted bits
     */
	public static void intToByte(int intValue, byte[] data, int idx) {
		data[idx++] = (byte) ((intValue & 0xFF000000) >> 24);
		data[idx++] = (byte) ((intValue & 0x00FF0000) >> 16);
		data[idx++] = (byte) ((intValue & 0x0000FF00) >> 8);
		data[idx]   = (byte) (intValue & 0x000000FF);	
	}

    /** Converts short to byte and stores value in the segments data array
     *
     * @param shortValue short to convert
     * @param data byte array for storing the converted value
     * @param idx index of shifted bits
     */
	public static void shortToByte(short shortValue, byte[] data, int idx) {
		data[idx++] = (byte) ((shortValue & 0xFF00) >> 8);
		data[idx]   = (byte) (shortValue & 0x00FF);	
	}

    /** Converts byte to int
     *
     * @param data data array of a given segment
     * @param idx index of shifted bits
     * @return an integer value
     */
    public static int byteToInt(byte[] data, int idx) {
        int intValue = 0, intTmp = 0;
		
		if ( ((int) data[idx]) < 0 ) { //leftmost bit (8th bit) is 1
			intTmp = 0x0000007F & ( (int) data[idx]);
			intTmp += 128;  // add the value of the masked bit: 2^7
		} else
			intTmp = 0x000000FF & ((int) data[idx]);
		idx++;
		intValue = intTmp; 
		intValue <<= 8;
				
		if ( ((int) data[idx]) < 0 ) { //leftmost bit (8th bit) is 1
			intTmp = 0x0000007F & ( (int) data[idx]);
			intTmp += 128;  // add the value of the masked bit: 2^7
		} else
			intTmp = 0x000000FF & ((int) data[idx]);
		idx++;
		intValue |= intTmp;
		intValue <<= 8 ; 	
			
		if ( ((int) data[idx]) < 0 ) { //leftmost bit (8th bit) is 1
			intTmp = 0x0000007F & ( (int) data[idx]);
			intTmp += 128;  // add the value of the masked bit: 2^7
		} else
			intTmp = 0x000000FF & ((int) data[idx]);
		idx++;
		intValue |= intTmp;
		intValue <<= 8;
			
		if ( ((int) data[idx]) < 0 ) { //leftmost bit (8th bit) is 1
			intTmp = 0x0000007F & ( (int) data[idx]);
			intTmp += 128;  // add the value of the masked bit: 2^7
		} else
			intTmp = 0x000000FF & ((int) data[idx]);
		intValue |= intTmp;
		//System.out.println(" byteToInt: " + intValue + "  " + intTmp);
		return intValue;
	}

    /** Converts this seg to a series of bytes
     *
     * @param seg RDT segment to be prepared for transmission
     * @param payload array of data to be added to the segments header and body
     */
    public static void makePayload(RDTSegment seg, byte[] payload) {
        // Add header
        intToByte(seg.seqNum, payload, seg.SEQ_NUM_OFFSET);
        intToByte(seg.ackNum, payload, seg.ACK_NUM_OFFSET);
        intToByte(seg.flags, payload, seg.FLAGS_OFFSET);
        intToByte(seg.checksum, payload, seg.CHECKSUM_OFFSET);
        intToByte(seg.rcvWin, payload, seg.RCV_WIN_OFFSET);
        intToByte(seg.length, payload, seg.LENGTH_OFFSET);

        // Add data
        byte segData[] = seg.getData();

        for (int i = 0; i < seg.length; i++) {
            payload[i + seg.HDR_SIZE] = segData[i];
        }
    }

    /** Converts a segments data array to a string, for printing to stdout
     *
     * @param data array of segment data
     * @return a string representation of the array
     */
    public static String dataToString(byte[] data) {
	    String str = "[";
        for (int i = 0; i < data.length; i++) {
            str += data[i];

            if (i > 10) {
                str += "...";
                break;
            }
        }
        str += "]";

        return str;
    }
}
