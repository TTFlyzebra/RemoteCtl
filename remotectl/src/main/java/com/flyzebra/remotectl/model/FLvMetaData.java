package com.flyzebra.remotectl.model;


import java.util.ArrayList;


/**
 * Created by tianyu on 15-12-29.
 * modified by lake on 16-4-8.
 * This class is able to generate a FLVTAG in accordance with Adobe Flash Video File Format
 * Specification v10.1 Annex E.5 with limited types available.
 */
public class FLvMetaData {
    private static final String Name = "onMetaData";
    private static final int ScriptData = 18;
    private static final byte[] TS_SID = {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
    private static final byte[] ObjEndMarker = {0x00, 0x00, 0x09};
    private static final int EmptySize = 21;
    private ArrayList<byte[]> MetaData;
    private int DataSize;
    private int pointer;
    private byte[] MetaDataFrame;

    public FLvMetaData(int a_datarate, int a_samplerate,int v_width, int v_height, int v_fps) {
        MetaData = new ArrayList<>();
        DataSize = 0;
        addProperty(toFlvString("audiocodecid"), (byte) 0, toFlvNum(10));
        addProperty(toFlvString("audiodatarate"), (byte) 0, toFlvNum(a_datarate/1024));
        addProperty(toFlvString("audiosamplerate"), (byte) 0, toFlvNum(a_samplerate));
        addProperty(toFlvString("videocodecid"), (byte) 0, toFlvNum(7));
        addProperty(toFlvString("VIDEO_WIDTH"), (byte) 0, toFlvNum(v_width));
        addProperty(toFlvString("VIDEO_HEIGHT"), (byte) 0, toFlvNum(v_height));
        addProperty(toFlvString("framerate"), (byte) 0, toFlvNum(v_fps));
    }


    private void addProperty(byte[] Key, byte datatype, byte[] data) {
        int Propertysize = Key.length + 1 + data.length;
        byte[] Property = new byte[Propertysize];
        System.arraycopy(Key, 0, Property, 0, Key.length);
        Property[Key.length] = datatype;
        System.arraycopy(data, 0, Property, Key.length + 1, data.length);
        MetaData.add(Property);
        DataSize += Propertysize;
    }

    public byte[] getMetaData() {
        MetaDataFrame = new byte[DataSize + EmptySize];
        pointer = 0;
        //SCRIPTDATA.name
        Addbyte(2);
        AddbyteArray(toFlvString(Name));
        //SCRIPTDATA.value ECMA array
        Addbyte(8);
        AddbyteArray(toUI(MetaData.size(), 4));
        for (byte[] Property : MetaData) {
            AddbyteArray(Property);
        }
        AddbyteArray(ObjEndMarker);
        return MetaDataFrame;
    }

    private void Addbyte(int value) {
        MetaDataFrame[pointer] = (byte) value;
        pointer++;
    }

    private void AddbyteArray(byte[] value) {
        System.arraycopy(value, 0, MetaDataFrame, pointer, value.length);
        pointer += value.length;
    }

    private byte[] toFlvString(String text) {
        byte[] FlvString = new byte[text.length() + 2];
        System.arraycopy(toUI(text.length(), 2), 0, FlvString, 0, 2);
        System.arraycopy(text.getBytes(), 0, FlvString, 2, text.length());
        return FlvString;
    }

    private byte[] toUI(long value, int bytes) {
        byte[] UI = new byte[bytes];
        for (int i = 0; i < bytes; i++) {
            UI[bytes - 1 - i] = (byte) (value >> (8 * i) & 0xff);
        }
        return UI;
    }

    private byte[] toFlvNum(double value) {
        long tmp = Double.doubleToLongBits(value);
        return toUI(tmp, 8);
    }
}
