package org.hyperledger.fabric.samples.privatedata;

import java.util.HashMap;
import java.util.Map;

public final class Session {
    private String name;
    private int type;
    private String pcc_rule;
    private Map<String, Object> ambr = new HashMap<>();
    private Qos qos;

    public Session() {

    }

    public Session(final String name, final int type, final String pcc_rule, final Qos qos) {
        this.name = name;
        this.type = type;
        this.pcc_rule = pcc_rule;
        this.qos = qos;
    }

    public void setAmbr(final Map<String, Object> amb) {
        this.ambr = amb;
    }

    public Map<String, Object> getAmbr() {
        return ambr;
    }

    public String getName() {
        return name;
    }

    public void setName(final String nam) {
        this.name = nam;
    }

    public int getType() {
        return type;
    }

    public void setType(final int typ) {
        this.type = typ;
    }

    public String getPcc_rule() {
        return pcc_rule;
    }

    public void setPcc_rule(final String pcc_rul) {
        this.pcc_rule = pcc_rul;
    }

    public Qos getQos() {
        return qos;
    }

    public void setQos(final Qos qo) {
        this.qos = qo;
    }

}
