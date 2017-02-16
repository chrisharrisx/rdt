/* NAME: Christopher Harris  LOGIN: charris */

package rdt;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.TimerTask;

import static rdt.RDT.ANSI_CYAN;
import static rdt.RDT.ANSI_RESET;

/** Handles retransmission of timed-out segments in Selective Repeat
 *
 * @author Chris Harris
 */
class TimeoutHandler extends TimerTask {
	RDTBuffer sndBuf;
	RDTSegment seg; 
	DatagramSocket socket;
	InetAddress dst_ip;
	int dst_port;
	
	TimeoutHandler (RDTBuffer sndBuf_, RDTSegment s, DatagramSocket sock, 
			InetAddress ip_addr, int p) {
		sndBuf = sndBuf_;
		seg = s;
		socket = sock;
		dst_ip = ip_addr;
		dst_port = p;
	}

    /** Prints notification to stdout and calls udp_send() with the given segment
     *
     */
	public void run() {
		System.out.println(System.currentTimeMillis()+ ":" + ANSI_CYAN + " TIMEOUT FOR SEGMENT: " + ANSI_RESET + "SegNum=" + seg.seqNum);
		System.out.flush();

        Utility.udp_send(seg, socket, dst_ip, dst_port, true);
	}
} // end TimeoutHandler class

