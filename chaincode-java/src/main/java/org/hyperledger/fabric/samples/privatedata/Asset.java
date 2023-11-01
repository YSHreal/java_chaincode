/*
 * SPDX-License-Identifier: Apache-2.0
 */
package org.hyperledger.fabric.samples.privatedata;

import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static java.nio.charset.StandardCharsets.UTF_8;

@DataType
public final class Asset {

    @Property()
    private final String imsi;

    @Property()
    private final String operator;

    @Property()
    private final Map<String, Object> homeNetwork;

    @Property()
    private final Map<String, Object> backupNetwork;


    public Asset(final String imsi, final String operator, final Map<String, Object> homeNetwork, final Map<String, Object> backupNetwork) {
        this.imsi = imsi;
        this.operator = operator;
        this.homeNetwork = homeNetwork;
        this.backupNetwork = backupNetwork;
    }

    public String getImsi() {
        return imsi;
    }

    public String getOperator() {
        return operator;
    }

    public Map<String, Object> getHomeNetwork() {
        return homeNetwork;
    }

    public Map<String, Object> getBackupNetwork() {
        return backupNetwork;
    }

    public byte[] serialize() {
        String jsonStr = new JSONObject(this).toString();
        return jsonStr.getBytes(UTF_8);
    }

    public static Asset deserialize(final byte[] assetJSON) {
        return deserialize(new String(assetJSON, UTF_8));
    }

    public static Asset deserialize(final String assetJSON) {
        try {
            JSONObject json = new JSONObject(assetJSON);

            final String imsi = json.getString("imsi");
            final String operator = json.getString("operator");

            HashMap<String, Object> ipEndPoint = new HashMap<>();
            ipEndPoint.put("ipv4Addr", json.getJSONObject("homeNetwork").getJSONObject("ipEndPoint").getString("ipv4Addr"));
            ipEndPoint.put("port", json.getJSONObject("homeNetwork").getJSONObject("ipEndPoint").getInt("port"));
            Map<String, Object> homeNetwork = new HashMap<>();
            homeNetwork.put("ipEndPoint", ipEndPoint);
            homeNetwork.put("netID", json.getJSONObject("homeNetwork").getString("netID"));
            homeNetwork.put("netType", json.getJSONObject("homeNetwork").getString("netType"));
            homeNetwork.put("cert", json.getJSONObject("homeNetwork").getString("cert"));

            Map<String, Object> backupNetwork = new HashMap<>();
            backupNetwork.put("netID", json.getJSONObject("backupNetwork").getString("netID"));
            backupNetwork.put("netType", json.getJSONObject("backupNetwork").getString("netType"));
            //different ipEndPoint
            HashMap<String, Object> ipEndPoint2 = new HashMap<>();
            ipEndPoint2.put("ipv4Addr", json.getJSONObject("backupNetwork").getJSONObject("ipEndPoint").getString("ipv4Addr"));
            ipEndPoint2.put("port", json.getJSONObject("backupNetwork").getJSONObject("ipEndPoint").getInt("port"));
            backupNetwork.put("ipEndPoint", ipEndPoint2);

            return new Asset(imsi, operator, homeNetwork, backupNetwork);

        } catch (Exception e) {
            throw new ChaincodeException("Deserialize error: " + e.getMessage(), "DATA_ERROR");
        }
    }

    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if ((obj == null) || (getClass() != obj.getClass())) {
            return false;
        }

        Asset other = (Asset) obj;

        return Objects.deepEquals(
                new String[]{getImsi(), getOperator()},
                new String[]{other.getImsi(), other.getOperator()});
    }

    public int hashCode() {
        return Objects.hash(getImsi(), getOperator());
    }

    public String toString() {
        return this.getImsi() + "@" + this.getOperator();
    }
}
