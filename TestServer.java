/* NAME: Christopher Harris  LOGIN: charris */

package rdt;

import static rdt.RDT.SR;

/** Receives packets delivered over the network from the lower layers
 *
 * @author Chris Harris
 */
public class TestServer {

	public TestServer() {}

	/**
	 * @param args command-line arguments
	 */
	public static void main(String[] args) throws Exception {
	    if (args.length != 3) {
            System.out.println("Required arguments: dst_hostname dst_port local_port");
            return;
        }
        String hostname = args[0];
        int dst_port = Integer.parseInt(args[1]);
        int local_port = Integer.parseInt(args[2]);

        byte[] buf = new byte[500];
        System.out.println("Server is waiting to receive ... " );

        /*--------------------------------------

          TEST CASE 2:
          GBN with small message and 0 loss rate

         -------------------------------------*/
        // int bufsize = 10;
        // RDT rdt = new RDT(hostname, dst_port, local_port, bufsize, bufsize);
        // RDT.setLossRate(0);


        /*--------------------------------------

          TEST CASE 3:
          GBN with long message and 0 loss rate

         -------------------------------------*/
       // int bufsize = 3;
       // RDT rdt = new RDT(hostname, dst_port, local_port, bufsize, bufsize);
       // RDT.setMSS(10);
       // RDT.setLossRate(0);


        /*--------------------------------------

          TEST CASE 4:
          GBN with one-way losses

         -------------------------------------*/
       // int bufsize = 10;
       // RDT rdt = new RDT(hostname, dst_port, local_port, bufsize, bufsize);
       // RDT.setMSS(10);
       // RDT.setLossRate(0);


        /*--------------------------------------

          TEST CASE 5:
          GBN with two-way losses

         -------------------------------------*/
       // int bufsize = 1;
       // RDT rdt = new RDT(hostname, dst_port, local_port, bufsize, bufsize);
       // RDT.setMSS(10);
       // RDT.setLossRate(0.8);


        /*--------------------------------------

          TEST CASE 6:
          Selective Repeat with 0 loss rate

         -------------------------------------*/
       // int bufsize = 10;
       // RDT rdt = new RDT(hostname, dst_port, local_port, bufsize, bufsize);
       // RDT.setMSS(10);
       // RDT.setLossRate(0);
       // RDT.protocol = SR;


        /*--------------------------------------

          TEST CASE 7:
          Selective Repeat with two way losses

         -------------------------------------*/
       int bufsize = 6;
       RDT rdt = new RDT(hostname, dst_port, local_port, bufsize, bufsize);
       RDT.setMSS(45);
       RDT.setLossRate(0.4);
       RDT.protocol = SR;


        while (true) {
            int size = rdt.receive(buf, RDT.MSS);
            System.out.flush();
        }
	}
}

