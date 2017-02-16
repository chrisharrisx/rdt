/* NAME: Christopher Harris  LOGIN: charris */

package rdt;

import static rdt.RDT.SR;

/** Creates arrays of data, delivering them over the network by passing them to the lower layers for processing
 *
 * @author Chris Harris
 */
public class TestClient {

	/**
	 * 
	 */
	public TestClient() {}

	/**
	 * @param args command-line arguments
	 */
	public static void main(String[] args) {
	    if (args.length != 3) {
            System.out.println("Required arguments: dst_hostname dst_port local_port");
            return;
        }

        String hostname = args[0];
        int dst_port = Integer.parseInt(args[1]);
        int local_port = Integer.parseInt(args[2]);

        byte[] buf = new byte[RDT.MSS];

        /*--------------------------------------

          TEST CASE 2:
          GBN with small message and 0 loss rate

         -------------------------------------*/
        // int messageSize = 10;
        // int bufsize = 10;
        // byte[] data = new byte[messageSize];

        // RDT rdt = new RDT(hostname, dst_port, local_port, bufsize, bufsize);
        // RDT.setLossRate(0);

        // for (int i = 0; i < messageSize; i++) {
        //     data[i] = (byte) i;
        // }
        // rdt.send(data, messageSize);


        /*--------------------------------------

          TEST CASE 3:
          GBN with long message and 0 loss rate

         -------------------------------------*/
       // int messageSize = 45;
       // int bufsize = 3;
       // byte[] data = new byte[messageSize];

       // RDT rdt = new RDT(hostname, dst_port, local_port, bufsize, bufsize);
       // RDT.setMSS(10);
       // RDT.setLossRate(0);

       // int index = 1;

       // for (int i = 0; i < messageSize; i++) {
       //     if ((i % 10) == 0) {
       //         data[i] = (byte) index++;
       //     }
       //     else {
       //         data[i] = (byte) 0;
       //     }
       // }
       // rdt.send(data, messageSize);


        /*--------------------------------------

          TEST CASE 4:
          GBN with one-way losses

         -------------------------------------*/
       // int messageSize = 10;
       // int numMessages = 5;
       // int bufsize = 10;
       // byte[] data = new byte[messageSize];

       // RDT rdt = new RDT(hostname, dst_port, local_port, bufsize, bufsize);
       // RDT.setLossRate(0.4);

       // for (int i = 0; i < numMessages; i++) {
       //     for (int j = 0; j < messageSize; j++) {
       //         data[j] = (byte) i;
       //     }
       //     rdt.send(data, messageSize);
       // }


        /*--------------------------------------

          TEST CASE 5:
          GBN with two-way losses

         -------------------------------------*/
       // int messageSize = 10;
       // int numMessages = 5;
       // int bufsize = 1;
       // byte[] data = new byte[messageSize];

       // RDT rdt = new RDT(hostname, dst_port, local_port, bufsize, bufsize);
       // RDT.setLossRate(0.8);

       // for (int i = 0; i < numMessages; i++) {
       //     for (int j = 0; j < messageSize; j++) {
       //         data[j] = (byte) i;
       //     }
       //     rdt.send(data, messageSize);
       // }


        /*--------------------------------------

          TEST CASE 6:
          Selective Repeat with 0 loss rate

         -------------------------------------*/
       // int messageSize = 10;
       // int bufsize = 10;
       // int numMessages = 6;
       // byte[] data = new byte[messageSize];

       // RDT rdt = new RDT(hostname, dst_port, local_port, bufsize, bufsize);
       // RDT.setMSS(10);
       // RDT.setLossRate(0);
       // RDT.protocol = SR;

       // for (int i = 0; i < numMessages; i++) {
       //     for (int j = 0; j < messageSize; j++) {
       //         data[j] = (byte) i;
       //     }
       //     rdt.send(data, messageSize);
       // }


        /*--------------------------------------

          TEST CASE 7:
          Selective Repeat with two way losses

         -------------------------------------*/
       int messageSize = 45;
       int bufsize = 6;
       int numMessages = 10;
       byte[] data = new byte[messageSize];

       RDT rdt = new RDT(hostname, dst_port, local_port, bufsize, bufsize);
       RDT.setMSS(10);
       RDT.setLossRate(0.4);
       RDT.protocol = SR;

       for (int i = 0; i < numMessages; i++) {
           for (int j = 0; j < messageSize; j++) {
               data[j] = (byte) i;
           }
           rdt.send(data, messageSize);
       }



        /*--------------------------------------
          ALL CASES
         -------------------------------------*/
        System.out.println(System.currentTimeMillis() + ": Client has sent all data \n" );
        System.out.flush();
	     
        rdt.receive(buf, RDT.MSS);
        rdt.close();
	}
}
