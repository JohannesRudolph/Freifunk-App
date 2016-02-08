package de.inmotion_sst.freifunkfinder;

import android.os.Parcel;
import android.os.Parcelable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;

/**
 * Created by jr on 26/01/16.
 */
public class Node implements ClusterItem, Parcelable {
    private final String id;
    private final String name;
    private final String community;
    private final String status;
    private final double lon;
    private final double lat;
    private final double alt;
    private final int clients;
    private final LatLng latLng;

    @JsonCreator
    public Node(
            @JsonProperty("id")String id,
            @JsonProperty("name")String name,
            @JsonProperty("community")String community,
            @JsonProperty("lat")double lat,
            @JsonProperty("long")double lon,
            @JsonProperty("alt")double alt,
            @JsonProperty("status")String status,
            @JsonProperty(value = "clients", required = false) int clients
    ) {
        this.id = id;
        this.name = name;
        this.community = community;
        this.lon = lon;
        this.lat = lat;
        this.alt = alt;
        this.status = status;
        this.clients = clients;
        this.latLng = new LatLng(lat, lon);
    }

    protected Node(Parcel in) {
        id = in.readString();
        name = in.readString();
        community = in.readString();
        status = in.readString();
        lon = in.readDouble();
        lat = in.readDouble();
        alt = in.readDouble();
        clients = in.readInt();
        this.latLng = new LatLng(lat, lon);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(name);
        dest.writeString(community);
        dest.writeString(status);
        dest.writeDouble(lon);
        dest.writeDouble(lat);
        dest.writeDouble(alt);
        dest.writeInt(clients);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Node> CREATOR = new Creator<Node>() {
        @Override
        public Node createFromParcel(Parcel in) {
            return new Node(in);
        }

        @Override
        public Node[] newArray(int size) {
            return new Node[size];
        }
    };

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCommunity() {
        return community;
    }

    public double getLon() {
        return lon;
    }

    public double getLat() {
        return lat;
    }

    public double getAlt() {
        return alt;
    }

    public String getStatus() {
        return status;
    }

    public int getClients() {
        return clients;
    }

    @Override
    @JsonIgnore
    public LatLng getPosition() {
        return latLng;
    }
}
