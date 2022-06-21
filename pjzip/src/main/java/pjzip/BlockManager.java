package pjzip.src.main.java.pjzip;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * BlockManager
 */
public class BlockManager {

    private ConcurrentLinkedQueue<Block> available;
    private ConcurrentHashMap<Integer, Block> finished;
    private ConcurrentHashMap<Integer, Block> allBlocks;
    private boolean allBlocksRead;
    private int numBlocks;

    public int getNumBlocks() {
        return numBlocks;
    }

    public void setNumBlocks(int numBlocks) {
        this.numBlocks = numBlocks;
    }

    public BlockManager() {
        available = new ConcurrentLinkedQueue<Block>();
        finished = new ConcurrentHashMap<Integer, Block>();
        allBlocks = new ConcurrentHashMap<Integer, Block>();
        allBlocksRead = false;
    }

    public synchronized void finishedReading() {
        allBlocksRead = true;
        numBlocks = allBlocks.size();
        // System.err.println("All blocks read");
        notifyAll();
    }

    public synchronized void newFreeBlock(byte[] data, int size, int sequence_number) throws InterruptedException {
        Block b = new Block(data, size, sequence_number);
        available.add(b);
        allBlocks.put(sequence_number, b);
        notifyAll();
    }

    public byte[] getDict(Block b) {
        if(b.getSequence_number() == 0) {
            // the 0th block does not have a previous block to get data from
            return null;
        }
        byte[] dict = new byte[PJzip.DICT_SIZE];
        Block prev_block = allBlocks.get(b.getSequence_number() - 1);
        if (prev_block.getSize() < PJzip.DICT_SIZE) {
            return null;
        }
        System.arraycopy(prev_block.getData(), prev_block.getSize() - PJzip.DICT_SIZE, dict, 0, PJzip.DICT_SIZE);
        return dict;
    }

    public synchronized Block getNextFreeBlock() throws InterruptedException {
        while (available.isEmpty()) {
            if (!allBlocksRead) {
                // System.err.println("waiting for next free block");
                wait();
            } else {
                return null;
            }
        }
        return available.poll();
    }

    public synchronized void submitCompletedBlock(Block block) {
        finished.put(block.getSequence_number(), block);
        notifyAll();
    }

    public synchronized Block getCompletedBlock(int sequence_number) throws InterruptedException {
        while (!finished.containsKey(sequence_number)) {
            // System.err.println("waiting for block " + sequence_number + " to finish");
            wait();
        }
        return finished.get(sequence_number);
    }

}