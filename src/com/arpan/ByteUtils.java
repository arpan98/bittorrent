package com.arpan;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

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
}
