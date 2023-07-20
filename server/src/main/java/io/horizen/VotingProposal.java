package io.horizen;

import com.google.gson.Gson;

import java.util.Date;

public class VotingProposal {
    private final String id;
    private final int blockHeight;
    private final String blockHash;
    private final Date fromTime;
    private final Date toTime;
    private final String author;

    public VotingProposal(String id, int blockHeight, String blockHash, Date fromTime, Date toTime, String author) {
        this.id = id;
        this.blockHeight = blockHeight;
        this.blockHash = blockHash;
        this.fromTime = fromTime;
        this.toTime = toTime;
        this.author = author;
    }

    public String getId() {
        return id;
    }

    public int getBlockHeight() {
        return blockHeight;
    }

    public String getBlockHash() {
        return blockHash;
    }

    public Date getFromTime() {
        return fromTime;
    }

    public Date getToTime() {
        return toTime;
    }

    public String getAuthor() {
        return author;
    }

    public String toJson() {
        Gson gson = MyGsonManager.getGson();
        return gson.toJson(this);
    }

    public boolean isNull() {
        return id == null;
    }
}