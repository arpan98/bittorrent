package com.arpan;

import java.util.BitSet;

public class BitField {
    private BitSet bitField;

    public BitField(boolean hasFile, int size) {
        bitField = new BitSet(size);
        if (hasFile) {
            bitField.set(0, size);
        }
    }

    public byte[] toByteArray() {
        return bitField.toByteArray();
    }

}
