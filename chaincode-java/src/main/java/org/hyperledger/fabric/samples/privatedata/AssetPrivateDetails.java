package org.hyperledger.fabric.samples.privatedata;

import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;

@DataType
public final class AssetPrivateDetails {
    @Property()
    private final String imsi;

    @Property()
    private final Map<String, Object> slice;

    @Property()
    private final Map<String, Object> ambr;

    @Property()
    private final Map<String, Object> security;

    public AssetPrivateDetails(final String imsi, final Map<String, Object> slice, final Map<String, Object> ambr, final Map<String, Object> security) {
        this.imsi = imsi;
        this.slice = slice;
        this.ambr = ambr;
        this.security = security;
    }

    public String getImsi() {
        return imsi;
    }

    public Map<String, Object> getSlice() {
        return slice;
    }

    public Map<String, Object> getAmbr() {
        return ambr;
    }

    public Map<String, Object> getSecurity() {
        return security;
    }

    //serialize
    public byte[] serialize() {
        String jsonStr = new JSONObject(this).toString();
        return jsonStr.getBytes(UTF_8);
    }

    public static AssetPrivateDetails deserialize(final byte[] assetJSON) {
        return deserialize(new String(assetJSON, UTF_8));
    }

    public static AssetPrivateDetails deserialize(final String assetJSON) {
        try {
            JSONObject json = new JSONObject(assetJSON);
            final String imsi = json.getString("imsi");

            Map<String, Integer> arp = new HashMap<>();
            arp.put("priority_level", json.getJSONObject("slice").getJSONObject("session").getJSONObject("qos").getJSONObject("arp").getInt("priority_level"));
            arp.put("pre_emption_capability", json.getJSONObject("slice").getJSONObject("session").getJSONObject("qos").getJSONObject("arp").getInt("pre_emption_capability"));
            arp.put("pre_emption_vulnerability", json.getJSONObject("slice").getJSONObject("session").getJSONObject("qos").getJSONObject("arp").getInt("pre_emption_vulnerability"));
            Qos qos = new Qos();
            qos.setArp(arp);
            qos.setIndex(json.getJSONObject("slice").getJSONObject("session").getJSONObject("qos").getInt("index"));
            Map<String, Object> ambr = new HashMap<>();
            ambr.put("uplink", json.getJSONObject("slice").getJSONObject("session").getJSONObject("ambr").getString("uplink"));
            ambr.put("downlink", json.getJSONObject("slice").getJSONObject("session").getJSONObject("ambr").getString("downlink"));

            Session session = new Session();
            session.setAmbr(ambr);
            session.setName(json.getJSONObject("slice").getJSONObject("session").getString("name"));
            session.setType(json.getJSONObject("slice").getJSONObject("session").getInt("type"));
            session.setPcc_rule(json.getJSONObject("slice").getJSONObject("session").getString("pcc_rule"));
            session.setQos(qos);

            Map<String, Object> slice = new HashMap<>();
            slice.put("sst", json.getJSONObject("slice").getInt("sst"));
            slice.put("default_indicator", json.getJSONObject("slice").getBoolean("default_indicator"));
            slice.put("session", session);

            Map<String, Object> security = new HashMap<>();
            security.put("k", json.getJSONObject("security").getString("k"));
            security.put("amf", json.getJSONObject("security").getString("amf"));
            security.put("opc", json.getJSONObject("security").getString("opc"));
            security.put("sqn", json.getJSONObject("security").getString("sqn"));

            return new AssetPrivateDetails(imsi, slice, ambr, security);
        } catch (Exception e) {
            throw new ChaincodeException("Deserialize error: " + e.getMessage(), "DATA_ERROR");
        }
    }
}
