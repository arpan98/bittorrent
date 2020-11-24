package com.arpan.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public final class ByteUtils {

    public static byte[] concatByteArrays(byte[]... byteArrays) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        for (byte[] bytes : byteArrays) {
            try {
                outputStream.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return outputStream.toByteArray();
    }

    public static byte[] messageLengthToBytes(int messageLength) {
        return ByteBuffer.allocate(4).putInt(messageLength).array();
    }

    public static int readMessageLength(byte[] lengthBytes) {
        if (lengthBytes.length == 4)
            return ByteBuffer.wrap(lengthBytes).getInt();
        else
            return -1;
    }

    public static void printBits(byte[] bytes) {
        StringBuilder resultSb = new StringBuilder();
        for (byte b : bytes) {
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < 7; i++) {
                sb.append((b >> i) & 1);
            }
            sb.append("]\n");
            resultSb.append(sb);
        }
        System.out.println(resultSb.toString());
    }
}
