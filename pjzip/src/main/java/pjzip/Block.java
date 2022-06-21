package pjzip.src.main.java.pjzip;

/**
 * Block
 */
public class Block {

    private byte[] data;
    private byte[] compressed;
    private int sequence_number;
    private int size;
    private int compressed_size;


    public int getCompressed_size() {
        return compressed_size;
    }


    public void setCompressed_size(int compressed_size) {
        this.compressed_size = compressed_size;
    }


    public int getSize() {
        return size;
    }


    public void setSize(int size) {
        this.size = size;
    }


    public byte[] getData() {
        return data;
    }


    public void setData(byte[] data) {
        this.data = data;
    }


    public byte[] getCompressed() {
        return compressed;
    }


    public void setCompressed(byte[] compressed) {
        this.compressed = compressed;
    }


    public int getSequence_number() {
        return sequence_number;
    }


    public void setSequence_number(int sequence_number) {
        this.sequence_number = sequence_number;
    }


    public Block(byte[] _data, int _size, int _sequence_number) {
        data = new byte[_size];
        System.arraycopy(_data, 0, data, 0, _size);
        sequence_number = _sequence_number;
        size = _size;
        try {
            compressed = new byte[PJzip.BLOCK_SIZE * 2];
        } catch (java.lang.OutOfMemoryError e) {
            //TODO: handle exception
            System.err.println("Out of Memory :/");
        }
    }


}