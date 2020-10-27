package com.arpan;

import java.util.BitSet;

public class BitField {
    private final BitSet bitField;
    private final int size;

    public BitField(boolean hasFile, int size) {
        this.size = size;
        bitField = new BitSet(size);
        if (hasFile) {
            bitField.set(0, size);
        }
    }

    public void setBits(byte[] bytes) {
        for (int i = 0; i < size; i++) {
            if ((bytes[i/8] & (1<<(i%8))) != 0) {
                bitField.set(i);
            }
        }
    }

    public boolean hasExtraBits(BitField other) {
        for (int i = 0; i < this.size; i++) {
            if (this.bitField.get(i) && !other.bitField.get(i))
                return true;
        }
        return false;
    }

    public byte[] toByteArray() {
        return bitField.toByteArray();
    }

}
