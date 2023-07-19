package io.horizen;

public class ChainTip {
    private final int blockHeight;
    private final String blockHash;
    public ChainTip(int height, String hash) {
        this.blockHeight = height;
        this.blockHash = hash;
    }
    public int getBlockHeight() {
        return blockHeight;
    }
    public String getBlockHash() {
        return blockHash;
    }
}
