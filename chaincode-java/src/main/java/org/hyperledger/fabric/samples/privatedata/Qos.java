package org.hyperledger.fabric.samples.privatedata;

import java.util.HashMap;
import java.util.Map;

public final class Qos {
    private int index;
    private Map<String, Integer> arp = new HashMap<>();

    public Qos() {
    }

    //to avoid hiding field
    public Qos(final int inde) {
        this.index = inde;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(final int inde) {
        this.index = inde;
    }

    public Map<String, Integer> getArp() {
        return arp;
    }

    public void setArp(final Map<String, Integer> ar) {
        this.arp = ar;
    }
}
