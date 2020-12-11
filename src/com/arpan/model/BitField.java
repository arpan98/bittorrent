package com.arpan.model;

import java.util.BitSet;

public class BitField {
    private BitSet bitField; // removed final as it needs to be set
    // final object methods can be called later
    private final int size;

    public BitField(boolean hasFile, int size) {
        this.size = size;
        bitField = new BitSet(size);

        if (hasFile) {
            bitField.set(0, size);
        }
        //System.out.println(hasFile +" " + bitField.toString());

    }

    public void setBits(byte[] bytes) {
        //System.out.println(bytes.toString());
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

    public boolean hasCompleteFile() {
        return size == bitField.cardinality();
    }

    public boolean getBitFieldBit(int index){
        return bitField.get(index);
    }

    public void setBitFieldBit(int index){
        bitField.set(index);
    }

    public int getCardinality(){
        return bitField.cardinality();
    }

    public byte[] toByteArray() {
        return bitField.toByteArray();
    }

    public BitSet getBitField() {
        return bitField;
    }
    public void setBitField(BitSet bitField){
        this.bitField = bitField;
    }

}
