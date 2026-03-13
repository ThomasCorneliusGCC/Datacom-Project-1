package protocols;


import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SelectiveAndRepeatARQ_Sender {

    private static final byte ACK = 0x06; // ACK
    private static final byte NAK = 0X21; // NAK
    private static final char MAX_SEQ_NUM = 255;
    private static final char TOTAL_SEQ_NUM = (MAX_SEQ_NUM+1);
    private final NetworkSender sender;
    private int winBase = 0;
    private int winSize = 0;

    public SelectiveAndRepeatARQ_Sender(NetworkSender sender, int winSize){
        this.sender = sender;
        // Sliding window
        this.winBase = 0;
        this.winSize = winSize;
    }

    public void transmit(List<BISYNCPacket> packets) throws IOException {

        // Handshake
        int N = packets.size();
        sender.sendHandshakeRequest(N, winSize);
        char[] response = sender.waitForResponse();
        if(response[0] != ACK) {
            System.out.println("Handshake failed, exit");
        }else{
            System.out.println("Handshake succeed, proceed!");
        }

        Boolean finished = false;

        // TODO: Task 3.a, Your code below
        int i = 0;
        int lastACK = 0;
        int ackCount = 0;
        System.out.println("N = " + N);
        while(!finished){
            System.out.println("here");
            try {
                // notice: use sender.sendPacketWithLost() to send out packet
                // but, to resend the lost packet after receiving NAK,
                // use sender.sendPacket(), otherwise, the receiver may not get the resent packet and get stuck
                // also, for the last packet, use sender.sendPacket(), otherwise, it will get stuck



                while (i < winSize + winBase && i<N){

                    if((i != N-1)) {
                        sender.sendPacketWithLost(packets.get(i), (char) i, false);
                    } else {
                        sender.sendPacket(packets.get(i).getPacket(),(char)i, true);
                    }
                    System.out.println("sending packet: " + i);

                    i++;
                }

                System.out.println("base: " + winBase);
                response = sender.waitForResponse();
                if (response[0] == ACK) {
                    if(lastACK > (int)response[1]){
                        ackCount += 256;
                    }
                    System.out.println("ACK: " + (int) response[1] + " + " + (ackCount/MAX_SEQ_NUM)*TOTAL_SEQ_NUM);

                    winBase = (int)response[1]+((ackCount/MAX_SEQ_NUM)*TOTAL_SEQ_NUM);
                    lastACK = (int)response[1];
                } else if (response[0] == NAK) {
                    System.out.println("NAK: " + (int) response[1]);
                    sender.sendPacket(packets.get(response[1]).getPacket(), response[1], false);
                }
                if(winBase >= N){
                    System.out.println(winBase);
                    finished = true;
                }


            }catch (IOException e){
                System.err.println("Error transmitting packet: " + e);
                return;
            }
        }
    }

}
