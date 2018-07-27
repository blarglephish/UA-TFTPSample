//package ua.test;
//
//import java.io.BufferedOutputStream;
//import java.io.ByteArrayInputStream;
//import java.io.ByteArrayOutputStream;
//import java.io.DataOutputStream;
//import java.io.FileOutputStream;
//import java.io.IOException;
//import java.io.InputStreamReader;
//import java.io.OutputStream;
//import java.io.OutputStreamWriter;
//import java.io.Writer;
//import java.net.DatagramPacket;
//import java.net.DatagramSocket;
//import java.net.InetAddress;
//import java.net.SocketException;
//import java.nio.ByteBuffer;
//import java.nio.charset.Charset;
//import java.nio.charset.StandardCharsets;
//
//import sun.reflect.generics.reflectiveObjects.NotImplementedException;
//
///**
// * A server program that responds to TFTP WRQ requests
// * (RRQ is not implemented)
// * (Default port of 69)
// * 
// * @author adbetz
// *
// */
//public class TFTPServer
//{
//    private final static int TFTP_DEFAULT_PORT = 69;
//
//    // 516 bytes = 2 (opcode) + 2 (block number) + 512 (block)
//    private final static int MAX_PACKET_SIZE = 516;
//    private final static int MAX_BUFFER_SIZE = 1024;
//
//    private final static String MODE_NETASCII = "netascii";
//    private final static String MODE_OCTET = "octet";
//    private final static String MODE_MAIL = "mail";
//    
//    private final static byte ZERO_BYTE = 0x00;
//
//    private final static byte OPCODE_RRQ = 1;
//    private final static byte OPCODE_WRQ = 2;
//    private final static byte OPCODE_DATA = 3;
//    private final static byte OPCODE_ACK = 4;
//    private final static byte OPCODE_ERROR = 5;
//
//    // Members
//    private DatagramSocket datagramSocket;
//    private InetAddress clientInetAddress;
//    private int clientPort;
//    private Charset charSet;
//
//    /**
//     * Initialize to default port = 69
//     * @throws SocketException
//     */
//    public TFTPServer() throws SocketException
//    {
//        this.datagramSocket = new DatagramSocket(TFTP_DEFAULT_PORT);
//    }
//
//    /**
//     * TODO: Wondering of ways to turn this into a 
//     * multi-threaded application to handle multiple requests
//     * simultaneously ... probably need to transform this logic
//     * into a Thread class of some sort, and then let the main
//     * routine of the server class handle and start threads upon
//     * each new request.
//     * @param args
//     */
//    public static void main(String[] args)
//    {
//        try
//        {
//            TFTPServer server = new TFTPServer();
//            server.doService();
//        }
//        catch (SocketException e)
//        {
//            System.out.println("Socket error: " + e.getMessage());
//            e.printStackTrace();
//        }
//        catch (IOException e)
//        {
//            System.out.println("I/O Error: " + e.getMessage());
//            e.printStackTrace();
//        }
//    }
//
//    /**
//     * Just a simple message processing loop.
//     * TODO: If we turned this into a multi-threaded version,
//     * we probably wouldn't need the loop.
//     * @throws IOException
//     */
//    private void doService() throws IOException
//    {
//        byte[] readBuffer = new byte[MAX_BUFFER_SIZE];
//        while (true)
//        {
//            DatagramPacket request = new DatagramPacket(readBuffer, 0, readBuffer.length);
//            datagramSocket.receive(request);
//
//            clientInetAddress = request.getAddress();
//            clientPort = request.getPort();
//            byte[] requestOpCode = { readBuffer[0], readBuffer[1] };
//            switch (requestOpCode[1])
//            {
//            case OPCODE_RRQ:
//                doReadRequest(request);
//                break;
//
//            case OPCODE_WRQ:
//                doWriteRequest(request);
//                break;
//
//            default:
//                throw new IOException("Server cannot process message OpCode: " + requestOpCode[1]);
//            }
//        }
//    }
//
//    /**
//     * NOT IMPLEMENTED
//     * 
//     * @param request
//     */
//    private void doReadRequest(DatagramPacket request)
//    {
//        throw new NotImplementedException();
//    }
//
//    /**
//     * Helper method for handling WRQ messages from clients
//     * 
//     * @param request
//     * @throws IOException
//     */
//    private void doWriteRequest(DatagramPacket request) throws IOException
//    {
//        // Copy datagram data into byte[] for processing
//        byte[] requestData = new byte[request.getLength()];
//        int destinationOffset = 0;
//        System.arraycopy(request.getData(), request.getOffset(), requestData, destinationOffset, request.getLength());
//
//        // Get Filename
//        int messageIndex = 2;
//        String fileName = "";
//        byte currentByte;
//        while ((currentByte = requestData[messageIndex]) != ZERO_BYTE)
//        {
//            fileName += (char) currentByte;
//            messageIndex++;
//        }
//        messageIndex++;
//
//        // Get Mode
//        String mode = "";
//        while ((currentByte = requestData[messageIndex]) != ZERO_BYTE)
//        {
//            mode += (char) currentByte;
//            messageIndex++;
//        }
//        switch(mode.toLowerCase().trim())
//        {
//            case MODE_NETASCII:
//                charSet = StandardCharsets.US_ASCII;
//                break;
//            case MODE_OCTET:
//                charSet = StandardCharsets.UTF_8;
//                break;
//            default:
//                throw new IOException("Unsupported data mode");
//        }
//
//        // Send initial ACK response
//        int block = 0;
//        byte[] initialAckBlockArray = getBlockNumberAsByteArray(block);
//        sendAcknowledgement(initialAckBlockArray);
//        block++;
//
//        // Client should respond by sending data packets. Received packets
//        // will have data written to a ByteArrayOutputStream object. This object
//        // will remain stored in memory, so long as we are receiving packets
//        // from the client. When we are no longer receiving packets (final data
//        // packet will be > 512 bytes long), this outputstream array object will
//        // write to file.
//
//        ByteArrayOutputStream baoStream = new ByteArrayOutputStream();
//        DatagramPacket inboundDatagramPacket;
//
//        /**
//         * While we haven't received the last packet:
//         *  1. Get the next packet
//         *  2. Make sure its a data packet, report error if not
//         *  3. Get the bytes from the data packet, write them to the
//         *     byte array output stream
//         *  4. Send corresponding ACK response.
//         *  5. Increase block count
//         */
//        do
//        {
//            byte[] packetBuffer = new byte[MAX_PACKET_SIZE];
//            inboundDatagramPacket = new DatagramPacket(packetBuffer, packetBuffer.length, clientInetAddress,
//                    clientPort);
//            datagramSocket.receive(inboundDatagramPacket);
//
//            byte[] opCode = { packetBuffer[0], packetBuffer[1] };
//            if (opCode[1] == OPCODE_ERROR)
//            {
//                reportError(packetBuffer);
//            } else if (opCode[1] == OPCODE_DATA)
//            {
//                byte[] blockNumber = { packetBuffer[2], packetBuffer[3] };
//
//                DataOutputStream dos = new DataOutputStream(baoStream);
//                dos.write(inboundDatagramPacket.getData(), 4, inboundDatagramPacket.getLength() - 4);
//
//                sendAcknowledgement(blockNumber);
//                block++;
//            }
//        } 
//        while (!isLastDataPacket(inboundDatagramPacket));
//
//        // Send final acknowledgement
//        byte[] finalAckBlockArray = getBlockNumberAsByteArray(block);
//        sendAcknowledgement(finalAckBlockArray);
//
//        // Write received data to file
//        writeFile(baoStream, fileName, mode);
//    }
//
//    /**
//     * Tells us if the datagramPacket object (assumed to be a DATA opCode) is the
//     * final one or not
//     * 
//     * @param inboundDataPacket
//     * @return
//     */
//    private boolean isLastDataPacket(DatagramPacket inboundDataPacket)
//    {
//        return (inboundDataPacket.getLength() < 512) ? true : false;
//    }
//
//    /**
//     * ACK packet formation:
//     * 
//     * 2 bytes 2 bytes 
//     * --------------------- 
//     * | Opcode | Block # |
//     * ---------------------
//     * 
//     * @param blockNumber
//     */
//    private void sendAcknowledgement(byte[] blockNumber)
//    {
//        byte[] messageBuffer = { 0, OPCODE_ACK, blockNumber[0], blockNumber[1] };
//        DatagramPacket response = new DatagramPacket(messageBuffer, messageBuffer.length, clientInetAddress,
//                clientPort);
//        try
//        {
//            datagramSocket.send(response);
//        } 
//        catch (IOException e)
//        {
//            System.out.println("I/O error sending ACK request: " + e.getMessage());
//            e.printStackTrace();
//        }
//    }
//
////    private void sendError(byte[] errorCode, String errMsg)
////    {
////        byte[] messageBuffer = getErrorMessage(errorCode, errMsg);
////        DatagramPacket response = 
////                new DatagramPacket(messageBuffer, messageBuffer.length, clientInetAddress, clientPort);
////        try
////        {
////            datagramSocket.send(response);
////        } catch (IOException e)
////        {
////            System.out.println("I/O error sending ACK request: " + e.getMessage());
////            e.printStackTrace();
////        }
////    }
//
//    // For printing out errors received from the client
//    private void reportError(byte[] message)
//    {
//        String reportCode = new String(message, 3, 1);
//        String errorMessage = new String(message, 4, message.length - 4);
//        System.out.println("Client error: " + reportCode + " : " + errorMessage);
//    }
//
//    /**
//     * Error packet (OpCode 5):
//     * 
//     * 2 bytes 2 bytes string 1 byte 
//     *  --------------------------------
//     * |Opcode | ErrorCode | ErrMsg | 0 | 
//     *  --------------------------------
//     * 
//     * @param errorNumber
//     * @param errorMessage
//     */
//    private byte[] getErrorMessage(byte[] errorCode, String errMsg)
//    {
//        int messageByteLength = 2 + 2 + errMsg.length() + 1;
//        byte[] messageBuffer = new byte[messageByteLength];
//
//        int positionIndex = 0;
//        messageBuffer[positionIndex] = ZERO_BYTE;
//        positionIndex++;
//        messageBuffer[positionIndex] = OPCODE_ERROR;
//        positionIndex++;
//        messageBuffer[positionIndex] = errorCode[0];
//        positionIndex++;
//        messageBuffer[positionIndex] = errorCode[1];
//        positionIndex++;
//
//        for (int i = 0; i < errMsg.length(); i++)
//        {
//            messageBuffer[positionIndex] = (byte) errMsg.charAt(i);
//            positionIndex++;
//        }
//
//        messageBuffer[positionIndex] = ZERO_BYTE;
//        return messageBuffer;
//    }
//
//    /**
//     * Helper method for writing received bytes from data packets
//     * to file.
//     * TODO: unclear how the mode is used here. 
//     * @param baoStream
//     * @param fileName
//     * @param mode
//     * @throws IOException 
//     */
//    private void writeFile(ByteArrayOutputStream baoStream, String fileName, String mode) throws IOException
//    {
//        switch(mode.toLowerCase().trim())
//        {
//            case MODE_NETASCII:
//                try
//                {
//                    OutputStream outputStream = new FileOutputStream(fileName);
//                    baoStream.writeTo(outputStream);
//                    outputStream.close();
//                } 
//                catch (IOException e)
//                {
//                    System.out.println("I/O error writing data to file: " + e.getMessage());
//                    e.printStackTrace();
//                }
//                break;
//                
//            case MODE_OCTET:
//                try
//                {
//                    OutputStream outputStream = new FileOutputStream(fileName);
//                    baoStream.writeTo(outputStream);
//                    outputStream.close();
//                } 
//                catch (IOException e)
//                {
//                    System.out.println("I/O error writing data to file: " + e.getMessage());
//                    e.printStackTrace();
//                }
//                break;
//            default:
//                throw new IOException("Unsupported data mdoe");
//        }
//    }
//
//    /**
//     * Just a utility function for getting an int into a byte[] for easier message
//     * sending
//     * 
//     * @param blockNumber
//     * @return
//     */
//    private byte[] getBlockNumberAsByteArray(int blockNumber)
//    {
//        ByteBuffer buffer = ByteBuffer.allocate(4);
//        buffer.putInt(blockNumber);
//
//        return buffer.array();
//    }
//}
