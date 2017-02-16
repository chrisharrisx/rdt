/* NAME: Christopher Harris  LOGIN: charris */

package rdt;

import java.util.Timer;

/** Represents a UDP segment with added Reliable Data Transfer protocol, i.e. TCP over UDP
 *
 * @author Chris Harris
 */
public class RDTSegment {
	private byte[] data;

	// Package-private access
    Timer timer = new Timer();
    TimeoutHandler timeoutHandler;
    int seqNum;
    int ackNum;
    int flags;
    int checksum;
    int rcvWin;
    int length;  // Number of data bytes (<= MSS)
    static final int SEQ_NUM_OFFSET = 0;
    static final int ACK_NUM_OFFSET = 4;
    static final int FLAGS_OFFSET = 8;
    static final int CHECKSUM_OFFSET = 12;
    static final int RCV_WIN_OFFSET = 16;
    static final int LENGTH_OFFSET = 20;
    static final int HDR_SIZE = 24;
    static final int FLAGS_ACK = 1;
    static final int FLAGS_ACKED = 2;
    static final int FLAGS_FIN = 3;
    static final int FLAGS_FIN_ACK = 4;
    static final int FLAGS_FIN_ACKED = 5;

	RDTSegment() {
		seqNum = 0;
		ackNum = 0;
		flags = 0;
		checksum = 0;
		rcvWin = 0;
        length = 0;
        data = new byte[RDT.MSS];
	}

    /** Sets the data for a segment
     *
     * @param data the byte array to be set
     */
	public void setData(byte[] data) {
	    this.data = data;
    }

    /** Returns the data from a segment
     *
     * @return a byte array
     */
    public byte[] getData() {
	    return this.data;
    }

    /** Checks whether or not this is an ack packet
     *
     * @return a boolean value
     */
	public boolean containsAck() {
		return (flags == FLAGS_ACK);
	}

    /** Checks whether or not this segment contains a data body
     *
     * @return a boolean value
     */
	public boolean containsData() {
		return (length > 0);
	}

    /** Checks whether or not this is a FIN packet
     *
     * @return a boolean value
     */
	public boolean containsFin() { return (flags >= FLAGS_FIN); }

    /** Returns the checksum for this packet
     *
     * @return an integer representation of the checksum value
     */
	public int computeChecksum() {
        int csum = 0;

        csum += (0xff & (((seqNum & 0xff000000) >> 24) +
                ((seqNum & 0x00ff0000) >> 16) +
                ((seqNum & 0x0000ff00) >> 8) +
                (seqNum & 0x000000ff)));
        csum += (0xff & (((ackNum & 0xff000000) >> 24) +
                ((ackNum & 0x00ff0000) >> 16) +
                ((ackNum & 0x0000ff00) >> 8) +
                (ackNum & 0x000000ff)));
        csum += (0xff & (((flags & 0xff000000) >> 24) +
                ((flags & 0x00ff0000) >> 16) +
                ((flags & 0x0000ff00) >> 8) +
                (flags & 0x000000ff)));
        csum += (0xff &(((rcvWin & 0xff000000) >> 24) +
                ((rcvWin & 0x00ff0000) >> 16) +
                ((rcvWin & 0x0000ff00) >> 8) +
                (rcvWin & 0x000000ff)));
        csum += (0xff & (((length & 0xff000000) >> 24) +
                ((length & 0x00ff0000) >> 16) +
                ((length & 0x0000ff00) >> 8) +
                (length & 0x000000ff)));

        for (int i = 0; i < length; i++) {
            csum += (0xff & this.data[i]);
        }

        return (0xff & csum);
	}

    /** Checks whether the received packet has been corrupted or not
     *
     * @return a boolean value
     */
	public boolean isValid() {
        return (this.checksum == computeChecksum());
	}

    /** Prints the header of the packet
     *
     */
	public void printHeader() {
		System.out.print("SeqNum:" + seqNum + " ");
		System.out.print("ackNum:" + ackNum + " ");
		System.out.print("flags:" +  flags + " ");
		System.out.print("checksum:" + checksum + " ");
		System.out.print("rcvWin:" + rcvWin + " ");
		System.out.print("length:" + length + " ");
	}

    /** Prints the body of the packet
     *
     */
	public void printData() {
		System.out.print("Data:[");
		for (int i = 0; i < length; i++) {
            System.out.print(this.data[i]);

            if (i > 10) {
                System.out.print("...");
                break;
            }
        }
		System.out.print("]");
	}

    /** Prints the segment hash code, followed by the header and data
     *
     */
	public void dump() {
		System.out.print(this + " ");
		printHeader();
		printData();
	}

	@Override
	public String toString() {
		return "seg@" + Integer.toHexString(System.identityHashCode(this));
	}
} // end RDTSegment class
