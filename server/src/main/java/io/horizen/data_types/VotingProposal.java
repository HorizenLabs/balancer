package io.horizen.data_types;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.annotations.SerializedName;
import io.horizen.helpers.MyGsonManager;

import java.text.SimpleDateFormat;
import java.util.Date;

public class VotingProposal {
    @SerializedName("ID")
    private final String id;
    @SerializedName("block_height")
    private final int blockHeight;
    @SerializedName("block_hash")
    private final String blockHash;
    @SerializedName("from")
    private final Date fromTime;
    @SerializedName("to")
    private final Date toTime;
    @SerializedName("Author")
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
        JsonObject json = new JsonObject();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        JsonObject proposalObject = new JsonObject();
        proposalObject.add("ID", new JsonPrimitive(id));
        proposalObject.add("block_height", new JsonPrimitive(blockHeight));
        proposalObject.add("block_hash", new JsonPrimitive(blockHash));
        proposalObject.add("from", new JsonPrimitive(sdf.format(fromTime)));
        proposalObject.add("to", new JsonPrimitive(sdf.format(toTime)));
        proposalObject.add("Author", new JsonPrimitive(author));

        json.add("Proposal", proposalObject);
        return MyGsonManager.getGson().toJson(json);
    }

    public boolean isNull() {
        return id == null;
    }
}