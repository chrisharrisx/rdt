/* NAME: Christopher Harris  LOGIN: charris */

CMPT 471 Project 2
File: readme.txt

BUGS/ISSUES:

There are no known bugs/issues in the code. All 7 test cases are included in TestClient.java and TestServer.java for reference,
and all have been run exhaustively without errors.

IMPLEMENTATION NOTES:

1. Command-line output of each event during execution prints the current time in milliseconds, and has been colour coded for ease of inspection
    - Green commands represent transmissions and retransmissions for both the client and server
    - Yellow commands represent packets received FROM the network, for both the client and TestServer
    - Red commands represent problems occurred during execution, such as lost packets/acks
    - Cyan and Magenta commands are used for other miscellaneous events, such as timer events on the client side, and delivery of packets to the 
      upper layer in selective repeat

  * Note: colourized output is known to work under Linux and OS/X. For best results, please run the program from the command-line in Linux,
    rather than Windows or within an IDE.

2. Currently the int value returned from rdt.receive on the server side is not used – the command-line output shown when packets are received is 
   generated from within the rdt.receive function. This choice was made to simplify printing output from the perspective of the server (i.e. avoiding the need to iterate over the test server's buffer). This !does not mean! that the buffers are not corrected filled and not able to be iterated over: I was purely aiming to provide consistent, readable output.

3. Corruption of data packets has not been implemented for the project, as it appeared to me to not have been part of the scope of the test cases.

4. Only the 5 argument constructor for RDT was used during testing. The 3 argument constructor did not seem to me to be useful given the test cases, so
   (if at all possible) please do not use it when checking my implementation.

5. The TimeoutHandler class was only used for Selective Repeat. I thought it more appropriate for Go Back N to use a single timer associated with the
   RDTBuffer class. This will not affect testing, but I thought I would mention it as a matter of course.

6. Code was compiled on the command-line using "javac *.java" from within the rdt directory
