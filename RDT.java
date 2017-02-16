/* NAME: Christopher Harris  LOGIN: charris */

package rdt;

import java.io.*;
import java.net.*;
import java.util.*;

import static rdt.RDTSegment.FLAGS_FIN;

/** Driver class for creating a Reliable Data Transfer protocol over UDP
 *
 * @author Chris Harris
 */
public class RDT {
	public static int MSS = 100; // Max segment size in bytes
	public static final int RTO = 500; // Retransmission Timeout in msec
    public static final int TTO = 10000; // Timeout for teardown in msec
	public static final int ERROR = -1;
	public static final int MAX_BUF_SIZE = 3;
	public static final int GBN = 1;   // Go back N protocol
	public static final int SR = 2;    // Selective Repeat
	public static int protocol = GBN;
	
	public static double lossRate = 0.0;
	public static Random random = new Random();
	public static Timer timer = new Timer();

	private DatagramSocket socket; 
	private InetAddress dst_ip;
	private int dst_port;
	private int local_port;
	
	private ReceiverThread rcvThread;

	public int sequence_number = 0;

	// Package-private access
    RDTBuffer sndBuf;
    RDTBuffer rcvBuf;

    static final String ANSI_RESET = "\u001B[0m";
    static final String ANSI_RED = "\u001B[31m";
    static final String ANSI_GREEN = "\u001B[32m";
    static final String ANSI_YELLOW = "\u001B[33m";
    static final String ANSI_CYAN = "\u001B[36m";
    static final String ANSI_PURPLE = "\u001B[35m";

    /**
     *
     * @param dst_hostname_ hostname of the recipient
     * @param dst_port_ port number of the recipient
     * @param local_port_ port number of the sender
     */
	RDT (String dst_hostname_, int dst_port_, int local_port_) {
		local_port = local_port_;
		dst_port = dst_port_;

		try {
			 socket = new DatagramSocket(local_port);
			 dst_ip = InetAddress.getByName(dst_hostname_);
		 } catch (IOException e) {
			 System.out.println("RDT constructor: " + e);
		 }

		sndBuf = new RDTBuffer(MAX_BUF_SIZE);

//		if (protocol == GBN) {
//            rcvBuf = new RDTBuffer(1);
//        }
//		else {
//            rcvBuf = new RDTBuffer(MAX_BUF_SIZE);
//        }

		rcvThread = new ReceiverThread(rcvBuf, sndBuf, socket, dst_ip, dst_port);
		rcvThread.start();
	}

    /**
     *
     * @param dst_hostname_ hostname of the recipient
     * @param dst_port_ port number of the recipient
     * @param local_port_ port number of the sender
     * @param sndBufSize size of the send buffer
     * @param rcvBufSize size of the receive buffer
     */
	RDT (String dst_hostname_, int dst_port_, int local_port_, int sndBufSize, int rcvBufSize) {
		local_port = local_port_;
		dst_port = dst_port_;

		 try {
			 socket = new DatagramSocket(local_port);
			 dst_ip = InetAddress.getByName(dst_hostname_);
		 } catch (IOException e) {
			 System.out.println("RDT constructor: " + e);
		 }

		sndBuf = new RDTBuffer(sndBufSize);
        rcvBuf = new RDTBuffer(rcvBufSize);

		rcvThread = new ReceiverThread(rcvBuf, sndBuf, socket, dst_ip, dst_port);
		rcvThread.start();

	}

    /** Sets the simulated rate at which packets are lost over the network
     *
     * @param rate percentage of packets lost
     */
	public static void setLossRate(double rate) {
	    lossRate = rate;
	}

    /** Sets the maximum size of body data in an RDT segment
     *
     * @param maxSize maximum segment size in bytes
     */
	public static void setMSS(int maxSize) {
	    MSS = maxSize;
    }

    /** Creates and places segments into a send buffer (for later processing, i.e. tracking of acks), and sends
     * segments over the network using udp_send().
     *
     * @param data array of data to be sent to the receiver
     * @param size size of the data array
     * @return total number of sent bytes
     */
	public int send(byte[] data, int size) {

	    /*------------------------------------------------------------------------

                                     GO-BACK-N

        ------------------------------------------------------------------------*/
		if (protocol == GBN) {

            if (size > MSS) {
                double numSegments = Math.ceil((double) size / MSS);

                for (int i = 0; i < numSegments; i++) {
                    RDTSegment seg = new RDTSegment();
                    byte segData[] = new byte[MSS];

                    for (int j = MSS * i, k = 0; j < (MSS * i) + MSS; j++, k++) {
                        if (j < size) {
                            segData[k] = data[j];
                        }
                        else {
                            segData[k] = 0;  // Padding out last segment with zeros
                        }
                    }

                    seg.seqNum = sequence_number++;
                    seg.length = MSS;
                    seg.setData(segData);
                    seg.checksum = seg.computeChecksum();

                    sndBuf.putNext(seg); // Put segment into send buffer

                    Utility.udp_send(seg, socket, dst_ip, dst_port, false);

                    if (sndBuf.base == sndBuf.nextSeqNum) {
                        System.out.println(System.currentTimeMillis() + ":" + ANSI_CYAN + " START TIMER: " + ANSI_RESET +
                                "base=" + sndBuf.base + " nextSeqNum=" + sndBuf.nextSeqNum);
                        sndBuf.runTimerTask(socket, dst_ip, dst_port);
                    }

                    sndBuf.nextSeqNum++;
                }
            }
            else {
                RDTSegment seg = new RDTSegment();
                seg.seqNum = sequence_number++;

                byte segData[] = new byte[size];
                System.arraycopy(data, 0, segData, 0, size);

                seg.length = size;
                seg.setData(segData);
                seg.checksum = seg.computeChecksum();

                sndBuf.putNext(seg); // Put segment into send buffer

                Utility.udp_send(seg, socket, dst_ip, dst_port, false);

                if (sndBuf.base == sndBuf.nextSeqNum) {
                    System.out.println(System.currentTimeMillis() + ":" + ANSI_CYAN + " START TIMER: " + ANSI_RESET +
                            "base=" + sndBuf.base + " nextSeqNum=" + sndBuf.nextSeqNum);
                    sndBuf.runTimerTask(socket, dst_ip, dst_port);
                }

                sndBuf.nextSeqNum++;
            }
        }

        /*------------------------------------------------------------------------

                                SELECTIVE REPEAT

        ------------------------------------------------------------------------*/
        if (protocol == SR) {
            RDTSegment seg = new RDTSegment();
            seg.seqNum = sequence_number++;

            byte segData[] = new byte[size];
            System.arraycopy(data, 0, segData, 0, size);

            seg.length = size;
            seg.setData(segData);
            seg.checksum = seg.computeChecksum();

            sndBuf.putNext(seg); // Put segment into send buffer

            Utility.udp_send(seg, socket, dst_ip, dst_port, false);

            TimeoutHandler timeoutHandler = new TimeoutHandler(sndBuf, seg, socket, dst_ip, dst_port);
            seg.timeoutHandler = timeoutHandler;
            seg.timer.schedule(timeoutHandler, RTO, RTO); // Timer will be cancelled when ack is received

            sndBuf.nextSeqNum++;
        }

		return size;
	}

    /** Takes packets placed into the receive buffer by the receiver thread, and delivers them to the upper layer
     *
     * @param buf buffer into which the received data should be copied
     * @param size length of data received
     * @return number of bytes copied in buf
     */
	public int receive (byte[] buf, int size) {
        RDTSegment seg = rcvBuf.getNext();

        if (seg != null && seg.containsData()) {
            System.arraycopy(seg.getData(), 0, buf, 0, seg.length);
            String segData = Utility.dataToString(seg.getData());

            if (protocol == GBN) {
                System.out.println(System.currentTimeMillis() + ":" + ANSI_YELLOW + " RECEIVED SEGMENT: " + ANSI_RESET +
                        "SeqNum=" + seg.seqNum + " Checksum=" + seg.checksum + " Data=" + segData);
            }
            else {
                System.out.println(System.currentTimeMillis() + ":" + ANSI_PURPLE + " SEGMENT DELIVERED TO UPPER LAYER: " +
                        ANSI_RESET + "SeqNum=" + seg.seqNum + " Checksum=" + seg.checksum + " Data=" + segData);
            }
        }
        else {
            return 0;
        }

		return seg.length;
	}

    /** Closes the connection gracefully, using TCP teardown
     *
     */
	public void close() {
        // OPTIONAL: close the connection gracefully
		// you can use TCP-style connection termination process

        // Add teardown before this exit call
        // System.exit(0);


        // NEED TO MAKE SURE SNDBUF IS DONE BEFORE SENDING THIS


//        RDTSegment finSeg = new RDTSegment();
//        finSeg.flags = FLAGS_FIN;
//
//        Utility.udp_send(finSeg, this.socket, this.dst_ip, this.dst_port, false);
//
//        System.out.println(System.currentTimeMillis() + ":" + ANSI_PURPLE + "Client is done" + ANSI_RESET);
//        System.exit(0);
    }

}  // end RDT class
