package org.hyperledger.fabric.samples.privatedata;

import org.hyperledger.fabric.contract.ClientIdentity;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.security.cert.CertificateException;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;
import static org.mockito.Mockito.mock;
import static org.hyperledger.fabric.samples.privatedata.AssetTransfer.ASSET_COLLECTION_NAME;
import static org.mockito.Mockito.when;

public final class AssetTransferTest {
    private static String testOrgOneMSP = "TestOrg1";
    private static String testOrg1Client = "testOrg1User";

    private static String testAsset1IMSI = "460110100010001";

    private static Asset testAsset1 = getAsset();

    private static Asset getAsset() {
        String imsi = "460110100010001";
        String operator = "China Telecom";

        Map<String, Object> homeNetwork = new HashMap<>();
        homeNetwork.put("netID", "46011010001");
        homeNetwork.put("netType", "Edge");
        Map<String, Object> ipEndPoint = new HashMap<>();
        ipEndPoint.put("ipv4Addr", "172.28.158.98");
        ipEndPoint.put("port", 7786);
        homeNetwork.put("ipEndPoint", ipEndPoint);
        homeNetwork.put("cert", "x.509");

        Map<String, Object> backupNetwork = new HashMap<>();
        backupNetwork.put("netID", "46011010000");
        backupNetwork.put("netType", "Center");
        Map<String, Object> ipEndPoint2 = new HashMap<>();
        ipEndPoint2.put("ipv4Addr", "172.28.158.99");
        ipEndPoint2.put("port", 7786);
        backupNetwork.put("ipEndPoint", ipEndPoint2);

        return new Asset(imsi, operator, homeNetwork, backupNetwork);
    }
    private static String dataAsset1String =
    "{\"backupNetwork\":{\"netID\":\"46011010000\","
            +
            "\"ipEndPoint\":{\"port\":7786,\"ipv4Addr\":\"172.28.158.99\"},\"netType\":\"Center\"},"
            +
            "\"imsi\":\"460110100010001\",\"homeNetwork\":{\"ipEndPoint\":{\"port\":7786,\"ipv4Addr\":\"172.28.158.98\"},\"cert\":\"x.509\","
            +
            "\"netID\":\"46011010001\",\"netType\":\"Edge\"},\"operator\":\"China Telecom\","
            +
            "\"ambr\":{\"uplink\":\"1073741824 bps\",\"downlink\":\"1073741824 bps\"},"
            +
            "\"security\":{\"sqn\":\"0000000000a1\",\"opc\":\"e8ed289deba952e4283b54e88e6183ca\",\"k\":\"465b5ce8b199b49faa5f0a2ee238a6bc\",\"amf\":\"8000\"},"
            +
            "\"slice\":{\"sst\":1,\"session\":{\"ambr\":{\"uplink\":\"1073741824 bps\",\"downlink\":\"1073741824 bps\"},\"qos\":{\"index\":9,"
            +
            "\"arp\":{\"priority_level\":8,\"pre_emption_capability\":1,\"pre_emption_vulnerability\":1}},\"pcc_rule\":\"\",\"name\":\"internet\",\"type\":3},"
            +
            "\"default_indicator\":true}}";
    private static byte[] dataAsset1Bytes = dataAsset1String.getBytes();

    @Nested
    class InvokeWriteTransaction {

        @Test
        public void createAssetWhenAssetExists() {
            AssetTransfer contract = new AssetTransfer();
            Context ctx = mock(Context.class);
            ChaincodeStub stub = mock(ChaincodeStub.class);
            //Calls the mock object, setting the value that should be returned when the corresponding method is called
            when(ctx.getStub()).thenReturn(stub);
            Map<String, byte[]> m = new HashMap<String, byte[]>();
            m.put("asset_properties", dataAsset1Bytes);
            when(ctx.getStub().getTransient()).thenReturn(m);
            when(stub.getPrivateData(ASSET_COLLECTION_NAME, testAsset1IMSI))
                    .thenReturn(dataAsset1Bytes);

            Throwable thrown = catchThrowable(() -> {
                contract.CreateAsset(ctx);
            });

            assertThat(thrown).isInstanceOf(ChaincodeException.class).hasNoCause()
                    .hasMessage("Asset %s already exists", testAsset1IMSI);

            assertThat(((ChaincodeException) thrown).getPayload()).isEqualTo("ASSET_ALREADY_EXISTS".getBytes());
        }

        @Test
        public void createAssetWhenNewAssetIsCreated() throws CertificateException, IOException {
            AssetTransfer contract = new AssetTransfer();
            Context ctx = mock(Context.class);
            ChaincodeStub stub = mock(ChaincodeStub.class);
            when(ctx.getStub()).thenReturn(stub);
            when(stub.getMspId()).thenReturn(testOrgOneMSP);
            ClientIdentity ci = mock(ClientIdentity.class);
            when(ci.getId()).thenReturn(testOrg1Client);
            when(ci.getMSPID()).thenReturn(testOrgOneMSP);
            when(ctx.getClientIdentity()).thenReturn(ci);

            Map<String, byte[]> m = new HashMap<String, byte[]>();
            m.put("asset_properties", dataAsset1Bytes);
            when(ctx.getStub().getTransient()).thenReturn(m);

            when(stub.getPrivateData(ASSET_COLLECTION_NAME, testAsset1IMSI))
                    .thenReturn(new byte[0]);

            Asset created = contract.CreateAsset(ctx);
            assertThat(created).isEqualTo(testAsset1);

            verify(stub).putPrivateData(ASSET_COLLECTION_NAME, testAsset1IMSI, created.serialize());
        }


    }

    @Nested
    class QueryReadAssetTransaction {

        @Test
        public void whenAssetExists() {
            AssetTransfer contract = new AssetTransfer();
            Context ctx = mock(Context.class);
            ChaincodeStub stub = mock(ChaincodeStub.class);
            when(ctx.getStub()).thenReturn(stub);
            when(stub.getPrivateData(ASSET_COLLECTION_NAME, testAsset1IMSI))
                    .thenReturn(dataAsset1Bytes);

            Asset asset = contract.ReadAsset(ctx, testAsset1IMSI);

            assertThat(asset).isEqualTo(testAsset1);
        }

        @Test
        public void whenAssetDoesNotExist() {
            AssetTransfer contract = new AssetTransfer();
            Context ctx = mock(Context.class);
            ChaincodeStub stub = mock(ChaincodeStub.class);
            when(ctx.getStub()).thenReturn(stub);
            when(stub.getStringState(testAsset1IMSI)).thenReturn(null);

            Asset asset = contract.ReadAsset(ctx, testAsset1IMSI);
            assertThat(asset).isNull();
        }
    }

}
