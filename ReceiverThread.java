/* NAME: Christopher Harris  LOGIN: charris */

package rdt;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import static rdt.RDT.*;
import static rdt.RDT.ANSI_RESET;
import static rdt.RDTSegment.FLAGS_ACK;
import static rdt.RDTSegment.FLAGS_ACKED;
import static rdt.RDTSegment.HDR_SIZE;

/** Receives packets from the network, and responds appropriately by either acking the packets,
 * or delivering them to the upper layer. Contains a buffer exclusively for sending packets, and
 * another buffer exclusively for receiving packets
 *
 * @author Chris Harris
 */
class ReceiverThread extends Thread {
    private RDTBuffer rcvBuf, sndBuf;
    private DatagramSocket socket;
    private InetAddress dst_ip;
    private int dst_port;
    private int expectedSeqNum = 0;
    private int lastRecvdSeqNum = 0;

    ReceiverThread (RDTBuffer rcv_buf, RDTBuffer snd_buf, DatagramSocket s,
                    InetAddress dst_ip_, int dst_port_) {
        rcvBuf = rcv_buf;
        sndBuf = snd_buf;
        socket = s;
        dst_ip = dst_ip_;
        dst_port = dst_port_;
    }

    /** Starts the thread
     *
     */
    @Override
    public void run() {

        while (true) {

            // Receive a packet from the socket, and convert it into an RDTSegment
            byte[] buffer = new byte[MSS + HDR_SIZE];
            DatagramPacket pkt = new DatagramPacket(buffer, MSS + HDR_SIZE);

            try {
                socket.receive(pkt);
            } catch (IOException e) {
                e.printStackTrace();
            }

            RDTSegment seg = new RDTSegment();
            makeSegment(seg, pkt.getData());

            /*------------------------------------------------------------------------

                                            GO-BACK-N

            ------------------------------------------------------------------------*/
            if (protocol == GBN) {
                if (seg.containsFin()) {
                    System.out.println(System.currentTimeMillis() + ":" + ANSI_PURPLE + " RECEIVED FIN" + ANSI_RESET);
                }

                if (seg.containsAck()) {

                    // AckNum = -1 indicates that the receiver has not received the first expected sequence number (i.e. 0)
                    if (seg.ackNum == -1) {
                        System.out.println(System.currentTimeMillis() + ":" + ANSI_YELLOW + " RECEIVED ACK: " + ANSI_RESET +
                                "Segment number " + seg.ackNum);
                        timer.cancel();
                        sndBuf.runTimerTask(socket, dst_ip, dst_port);
                        continue;
                    }

                    // Set the ACKED flag for the buffer segment with matching sequence number
                    if (sndBuf.buf[seg.ackNum % sndBuf.size].flags < FLAGS_ACKED) {
                        sndBuf.buf[seg.ackNum % sndBuf.size].flags = FLAGS_ACKED;
                    }

                    System.out.println(System.currentTimeMillis() + ":" + ANSI_YELLOW + " RECEIVED ACK: " + ANSI_RESET +
                            "Segment number " + seg.ackNum);

                    if (sndBuf.size > 1) {
                        if (seg.ackNum == sndBuf.base) {
                            sndBuf.base = seg.ackNum + 1;
                            sndBuf.semEmpty.release(); // Notify that an empty slot is available, so putNext() can take more data
                        }
                        else if (seg.ackNum > sndBuf.base) {
                            // Acks are cumulative, so check how far the window is sliding and release that many empty slots
                            int numSlots = seg.ackNum - sndBuf.base;

                            for (int i = 0; i < numSlots; i++) {
                                sndBuf.base++;
                                sndBuf.semEmpty.release(); // Notify that an empty slot is available, so putNext() can take more data
                            }
                        }
                    }
                    else {
                        sndBuf.base = seg.ackNum + 1;
                        sndBuf.semEmpty.release(); // Notify that an empty slot is available, so putNext() can take more data
                    }

                    if (sndBuf.base == sndBuf.nextSeqNum) { // All packets in the pipeline have been acked, so stop the timer
                        try {
                            timer.cancel();
                            System.out.println(System.currentTimeMillis() + ":" + ANSI_CYAN + " TIMER CANCELLED" + ANSI_RESET);
                        }
                        catch (IllegalStateException e) {
                            System.out.println(e);
                        }
                    }
                    else { // Acks are being received, but there are still unacknowledged packets in the pipeline, so restart the timer
                        timer.cancel();
                        sndBuf.runTimerTask(socket, dst_ip, dst_port);
                    }
                }

                if (seg.containsData()) {

                    // Ensure that segment received is the next in-order segment
                    RDTSegment ack_seg = new RDTSegment();
                    boolean resend = false;

                    if (seg.seqNum == expectedSeqNum) {
                        ack_seg.ackNum = seg.seqNum;
                        expectedSeqNum++;
                        lastRecvdSeqNum = seg.seqNum;
                        rcvBuf.putNext(seg);
                        rcvBuf.receivedFirst = true;
                    }
                    else {
                        // Drop the packet (implicitly) and ack the last received packet
                        if (rcvBuf.receivedFirst) {
                            ack_seg.ackNum = lastRecvdSeqNum;
                        }
                        else {
                            ack_seg.ackNum = -1;
                        }

                        System.out.println(System.currentTimeMillis() + ":" + ANSI_RED + " RECEIVED OUT OF ORDER PACKET: " +
                                ANSI_RESET + "SeqNum=" + seg.seqNum);
                        resend = true;
                    }

                    ack_seg.flags = FLAGS_ACK;
                    Utility.udp_send(ack_seg, socket, dst_ip, dst_port, resend);
                }
            }

            /*------------------------------------------------------------------------

                                      SELECTIVE REPEAT

            ------------------------------------------------------------------------*/
            if (protocol == SR) {
                if (seg.containsAck()) {
                    if (seg.ackNum < sndBuf.base) {
                        continue;
                    }

                    try {
                        sndBuf.semMutex.acquire(); // Acquire exclusive lock

                        sndBuf.buf[seg.ackNum % sndBuf.size].flags = FLAGS_ACKED;
                        sndBuf.buf[seg.ackNum % sndBuf.size].timer.cancel();
                        System.out.println(System.currentTimeMillis() + ":" + ANSI_YELLOW + " RECEIVED ACK: " + ANSI_RESET +
                                "Segment number " + seg.ackNum);

                        if (seg.ackNum == sndBuf.base) {
                            // Increment base and notify that an empty slot is available
                            sndBuf.base++;
                            sndBuf.semEmpty.release();

                            // Check if there are contiguous acknowledged packets in the buffer – if so, increment
                            // base, cancel the timer and notify that additional empty slots are available
                            for (int i = sndBuf.base % sndBuf.size; i < sndBuf.size; i++) {
                                if (sndBuf.buf[i] != null && sndBuf.buf[i].flags == FLAGS_ACKED) {
                                    sndBuf.base++;
                                    sndBuf.buf[i].timer.cancel();
                                    sndBuf.semEmpty.release(); // Notify that an empty slot is available, so putNext() can take more data
                                }
                                else {
                                    break;
                                }
                            }
                        }

                        sndBuf.semMutex.release(); // Release lock
                    }
                    catch (InterruptedException e) {
                        System.out.println(e);
                    }
                }

                if (seg.containsData()) {
                    System.out.println(System.currentTimeMillis() + ":" + ANSI_YELLOW + " RECEIVED SEGMENT: " + ANSI_RESET +
                            "SegNum=" + seg.seqNum);

                    // Prepare ack segment
                    RDTSegment ack_seg = new RDTSegment();
                    ack_seg.ackNum = seg.seqNum;
                    ack_seg.flags = FLAGS_ACK;

                    // Send ack
                    Utility.udp_send(ack_seg, socket, dst_ip, dst_port, false);

                    // If the received sequence number is within the window, put it in the correct slot in the receive buffer
                    if (seg.seqNum >= rcvBuf.base && seg.seqNum < (rcvBuf.base + rcvBuf.size)) {
                        rcvBuf.putSeqNum(seg);

                        // If sequence number = base, prepare to deliver it (and any contiguous packets) to the upper layer
                        if (seg.seqNum == rcvBuf.base) {
                            int numToDeliver = 1;

                            try {
                                rcvBuf.semMutex.acquire();

                                rcvBuf.nextSeqNum = seg.seqNum;
                                int next = rcvBuf.nextSeqNum;

                                while (rcvBuf.contains(++next)) {
                                    numToDeliver++;
                                }

                                // Notify that N slots have been filled, so that getNext() knows to deliver them to the upper layer
                                rcvBuf.semFull.release(numToDeliver);

                                rcvBuf.semMutex.release();

                                rcvBuf.base += numToDeliver; // Move base up by N slots
                            }
                            catch (InterruptedException e) {
                                System.out.println(e);
                            }
                        }
                    }
                }
            }
        }
    }

    /** Populates the fields of an RDTSegment with the values found in the network datagram packet
     *
     * @param seg RDT segment to be populated with data
     * @param payload data from received packet to populate the segment
     */
    void makeSegment(RDTSegment seg, byte[] payload) {
        seg.seqNum = Utility.byteToInt(payload, RDTSegment.SEQ_NUM_OFFSET);
        seg.ackNum = Utility.byteToInt(payload, RDTSegment.ACK_NUM_OFFSET);
        seg.flags  = Utility.byteToInt(payload, RDTSegment.FLAGS_OFFSET);
        seg.checksum = Utility.byteToInt(payload, RDTSegment.CHECKSUM_OFFSET);
        seg.rcvWin = Utility.byteToInt(payload, RDTSegment.RCV_WIN_OFFSET);
        seg.length = Utility.byteToInt(payload, RDTSegment.LENGTH_OFFSET);

        byte segData[] = new byte[seg.length];

        for (int i = 0; i < seg.length; i++) {
            segData[i] = payload[i + RDTSegment.HDR_SIZE];
        }

        seg.setData(segData);
    }

} // end ReceiverThread class
