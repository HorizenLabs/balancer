package io.horizen.data_types;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.annotations.SerializedName;
import io.horizen.helpers.MyGsonManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class VotingProposal {
    @SerializedName("ID")
    private final String id;
    @SerializedName("block_height")
    private final int mcBlockHeight;
    @SerializedName("block_hash")
    private final String mcBlockHash;
    @SerializedName("from")
    private final Date fromTime;
    @SerializedName("to")
    private final Date toTime;
    @SerializedName("Author")
    private final String author;

    @SerializedName("Snapshot")
    private final int snapshot;

    public VotingProposal(String id, int mcBlockHeight, String mcBlockHash, Date fromTime, Date toTime, String author, int snapshot) {
        this.id = id;
        this.mcBlockHeight = mcBlockHeight;
        this.mcBlockHash = mcBlockHash;
        this.fromTime = fromTime;
        this.toTime = toTime;
        this.author = author;
        this.snapshot = snapshot;
    }

    public String getId() {
        return id;
    }

    public int getMcBlockHeight() {
        return mcBlockHeight;
    }

    public String getMcBlockHash() {
        return mcBlockHash;
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
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yy HH:mm z");
        TimeZone utcTimeZone = TimeZone.getTimeZone("UTC");
        sdf.setTimeZone(utcTimeZone);

        JsonObject proposalObject = new JsonObject();
        proposalObject.add("ID", new JsonPrimitive(id));
        proposalObject.add("block_height", new JsonPrimitive(mcBlockHeight));
        proposalObject.add("block_hash", new JsonPrimitive(mcBlockHash));
        proposalObject.add("from", new JsonPrimitive(sdf.format(fromTime)));
        proposalObject.add("to", new JsonPrimitive(sdf.format(toTime)));
        proposalObject.add("Author", new JsonPrimitive(author));
        proposalObject.add("snapshot", new JsonPrimitive(snapshot));

        json.add("Proposal", proposalObject);
        return MyGsonManager.getGson().toJson(json);
    }

    public boolean isNull() {
        return id == null;
    }

    @Override
    public String toString() {
        return "VotingProposal{" +
                "id='" + id + '\'' +
                ", blockHeight=" + mcBlockHeight +
                ", blockHash='" + mcBlockHash + '\'' +
                ", fromTime=" + fromTime +
                ", toTime=" + toTime +
                ", author='" + author + '\'' +
                ", snapshot=" + snapshot +
                '}';
    }
}