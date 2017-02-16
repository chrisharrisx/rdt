/* NAME: Christopher Harris  LOGIN: charris */

package rdt;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Semaphore;

import static rdt.RDT.*;
import static rdt.RDT.RTO;
import static rdt.RDT.timer;

/** Represents a buffer used for storing segments received from the network. Used by both
 * the Receiver Thread and the RDT class for sending, receiving and delivering packets to and from the
 * client/server applications
 *
 * @author Chris Harris
 */
class RDTBuffer {
    // Package-private access
    RDTSegment[] buf;
    int size;
    int base;
    int nextSeqNum;
    Semaphore semMutex; // For mutual exclusion
    Semaphore semFull;  // # of full slots
    Semaphore semEmpty; // # of Empty slots
    boolean receivedFirst;

    RDTBuffer (int bufSize) {
        buf = new RDTSegment[bufSize];

        for (int i = 0; i < bufSize; i++) {
            buf[i] = null;
        }

        size = bufSize;
        base = nextSeqNum = 0;
        semMutex = new Semaphore(1, true);
        semFull =  new Semaphore(0, true);
        semEmpty = new Semaphore(bufSize, true);
        receivedFirst = false;
    }

    /** Puts a segment in the next available slot in the buffer
     *
     * @param seg RDT segment to be put in the buffer
     */
    public void putNext(RDTSegment seg) {
        try {
            semEmpty.acquire(); // Wait for an empty slot
            semMutex.acquire(); // Acquire exclusive lock

            buf[nextSeqNum % size] = seg;

            semMutex.release(); // Release lock
            semFull.release(); // Notify that one buffer is full
        } catch(InterruptedException e) {
            System.out.println("Buffer putNext(): " + e);
        }
    }

    /** Puts a segment in the *right* slot based on seg.seqNum
     *
     * @param seg RDT segment to be put in the buffer
     */
    public void putSeqNum (RDTSegment seg) {
        this.buf[seg.seqNum % size] = seg;
    }

    // Return the next in-order segment
    public RDTSegment getNext() {
        RDTSegment seg = new RDTSegment();

        try {
            semFull.acquire();  // Wait for a slot to be filled
            semMutex.acquire(); // Acquire exclusive lock

            seg = buf[nextSeqNum++ % size];

            semMutex.release(); // Release lock
            semEmpty.release(); // Notify that an empty slot is available
        }
        catch (InterruptedException e) {
            System.out.println("Buffer getNext(): " + e);
        }

        if (seg == null) {
            return null;
        }

        return seg;
    }

    /** Checks if a target sequence number has already been placed in the buffer
     *
     * @param target value to be found
     * @return whether or not the value was found
     */
    public boolean contains(int target) {
        for (int i = 0; i < this.size; i++) {
            if (this.buf[i] != null && this.buf[i].seqNum == target) {
                return true;
            }
        }

        return false;
    }

    /** Handles timeouts for segments in Go Back N. In the event of a timeout, all unacknowledged packets
     * in the window are resent using udp_send()
     *
     * @param socket datagram socket used for transmission
     * @param dst_ip IP address of the recipient
     * @param dst_port port number of the recipient
     */
    public void runTimerTask(DatagramSocket socket, InetAddress dst_ip, int dst_port) {
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                System.out.println(System.currentTimeMillis() + ":" + ANSI_CYAN + " RESTART TIMER: " + ANSI_RESET + "Resending all un-acked packets starting from base=" + base);

                if (size > 1) {
                    for (int i = (base % size); i < (nextSeqNum % size); i++) {
                        Utility.udp_send(buf[i], socket, dst_ip, dst_port, true);
                    }
                }
                else {
                    Utility.udp_send(buf[0], socket, dst_ip, dst_port, true);
                }

            }
        };

        timer = new Timer();
        timer.schedule(task, RTO, RTO);
    }

    // For debugging
    public void dump() {
        System.out.println("Send Buffer: ");
        for (int i = 0; i < this.size; i++) {
            if (this.buf[i] != null) {
                System.out.print("{");
                this.buf[i].dump();
                System.out.println("}");
            }
            else {
                System.out.print("{} ");
            }
        }
        System.out.println();
    }
} // end RDTBuffer class
