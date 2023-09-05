package io.horizen.data_types;

public class MainchainTip {
    private final int blockHeight;
    private final String blockHash;
    public MainchainTip(int height, String hash) {
        this.blockHeight = height;
        this.blockHash = hash;
    }
    public int getBlockHeight() {
        return blockHeight;
    }
    public String getBlockHash() {
        return blockHash;
    }

    @Override
    public String toString() {
        return "MainchainTip{" +
                "blockHeight=" + blockHeight +
                ", blockHash='" + blockHash + '\'' +
                '}';
    }
}
