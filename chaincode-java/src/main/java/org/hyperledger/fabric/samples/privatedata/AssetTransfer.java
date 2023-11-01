package org.hyperledger.fabric.samples.privatedata;

import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.ContractInterface;
import org.hyperledger.fabric.contract.annotation.Contact;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.hyperledger.fabric.contract.annotation.Info;
import org.hyperledger.fabric.contract.annotation.Contract;
import org.hyperledger.fabric.contract.annotation.License;
import org.hyperledger.fabric.contract.annotation.Default;
import org.hyperledger.fabric.contract.annotation.Transaction;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.json.JSONObject;

import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;

@Contract(
        name = "private",
        info = @Info(
                title = "Asset Transfer Private Data",
                description = "The hyperlegendary asset transfer private data",
                version = "0.0.1-SNAPSHOT",
                license = @License(
                        name = "Apache 2.0 License",
                        url = "http://www.apache.org/licenses/LICENSE-2.0.html"),
                contact = @Contact(
                        email = "a.transfer@example.com",
                        name = "Private Transfer",
                        url = "https://hyperledger.example.com")))
@Default
public final class AssetTransfer implements ContractInterface {
    static final String ASSET_COLLECTION_NAME = "assetCollection";
    static final String AGREEMENT_KEYPREFIX = "transferAgreement";

    private enum AssetTransferErrors {
        INCOMPLETE_INPUT,
        INVALID_ACCESS,
        ASSET_NOT_FOUND,
        ASSET_ALREADY_EXISTS
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public Asset CreateAsset(final Context ctx) {
        ChaincodeStub stub = ctx.getStub();
        Map<String, byte[]> transientMap = ctx.getStub().getTransient();
        if (!transientMap.containsKey("asset_properties")) {
            String errorMessage = String.format("CreateAsset call must specify asset_properties in Transient map input");
            System.err.println(errorMessage);
            throw new ChaincodeException(errorMessage, AssetTransferErrors.INCOMPLETE_INPUT.toString());
        }

        byte[] transientAssetJSON = transientMap.get("asset_properties");
        //Asset
        final String imsi;
        final String operator;
        final Map<String, Object> homeNetwork;
        final Map<String, Object> backupNetwork;
        //AssetPrivate
        final Map<String, Object> slice;
        final Map<String, Object> ambr;
        final Map<String, Object> security;

        try {
            JSONObject json = new JSONObject(new String(transientAssetJSON, UTF_8));
            imsi = json.getString("imsi");
            operator = json.getString("operator");
            homeNetwork = json.getJSONObject("homeNetwork").toMap();
            backupNetwork = json.getJSONObject("backupNetwork").toMap();
            slice = json.getJSONObject("slice").toMap();
            ambr = json.getJSONObject("ambr").toMap();
            security = json.getJSONObject("security").toMap();
        } catch (Exception err) {
            String errorMessage = String.format("TransientMap deserialized error: %s ", err);
            System.err.println(errorMessage);
            throw new ChaincodeException(errorMessage, AssetTransferErrors.INCOMPLETE_INPUT.toString());
        }

        String errorMessage = null;
        if (imsi.equals("")) {
            errorMessage = String.format("Empty input in Transient map: imsi");
        }

        if (operator.equals("")) {
            errorMessage = String.format("Empty input in Transient map: operator");
        }

        if (homeNetwork.isEmpty()) {
            errorMessage = String.format("Empty input in Transient map: homeNetwork");
        }

        if (backupNetwork.isEmpty()) {
            errorMessage = String.format("Empty input in Transient map: backupNetwork");
        }

        if (slice.isEmpty()) {
            errorMessage = String.format("Empty input in Transient map: slice");
        }

        if (ambr.isEmpty()) {
            errorMessage = String.format("Empty input in Transient map: ambr");
        }

        if (security.isEmpty()) {
            errorMessage = String.format("Empty input in Transient map: security");
        }

        Asset asset = new Asset(imsi, operator, homeNetwork, backupNetwork);
        /*
        * check if the asset already exit
        * the parameter can be cllection name and a key
        * */
        byte[] assetJSON = ctx.getStub().getPrivateData(ASSET_COLLECTION_NAME, imsi);
        if (assetJSON != null && assetJSON.length > 0) {
            errorMessage = String.format("Asset %s already exists", imsi);
            System.err.println(errorMessage);
            throw new ChaincodeException(errorMessage, AssetTransferErrors.ASSET_ALREADY_EXISTS.toString());
        }


        /*
        * String clientID = ctx.getClientIdentity().getId();
        * clientID ?
        * */

        // Get collection name for this organization.
        String orgCollectionName = getCollectionName(ctx);

        verifyClientOrgMatchesPeerOrg(ctx);

        // Make submitting client the owner
        System.out.printf("CreateAsset Put: collection %s, ID %s\n", ASSET_COLLECTION_NAME, imsi);
        System.out.printf("Put: collection %s, ID %s\n", ASSET_COLLECTION_NAME, new String(asset.serialize()));
        stub.putPrivateData(ASSET_COLLECTION_NAME, imsi, asset.serialize());

        // Save AssetPrivateDetails to org collection
        AssetPrivateDetails assetPriv = new AssetPrivateDetails(imsi, slice, ambr, security);
        System.out.printf("Put AssetPrivateDetails: collection %s, ID %s\n", orgCollectionName, imsi);
        stub.putPrivateData(orgCollectionName, imsi, assetPriv.serialize());


        return asset;
    }


    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void DeleteAsset(final Context ctx) {
        ChaincodeStub stub = ctx.getStub();
        Map<String, byte[]> transientMap = ctx.getStub().getTransient();
        if (!transientMap.containsKey("asset_delete")) {
            String errorMessage = String.format("DeleteAsset call must specify 'asset_delete' in Transient map input");
            System.err.println(errorMessage);
            throw new ChaincodeException(errorMessage, AssetTransferErrors.INCOMPLETE_INPUT.toString());
        }

        byte[] transientAssetJSON = transientMap.get("asset_delete");
        final String imsi;

        try {
            JSONObject json = new JSONObject(new String(transientAssetJSON, UTF_8));
            imsi = json.getString("imsi");

        } catch (Exception err) {
            String errorMessage = String.format("TransientMap deserialized error: %s ", err);
            System.err.println(errorMessage);
            throw new ChaincodeException(errorMessage, AssetTransferErrors.INCOMPLETE_INPUT.toString());
        }

        System.out.printf("DeleteAsset: verify asset %s exists\n", imsi);
        byte[] assetJSON = stub.getPrivateData(ASSET_COLLECTION_NAME, imsi);

        if (assetJSON == null || assetJSON.length == 0) {
            String errorMessage = String.format("Asset %s does not exist", imsi);
            System.err.println(errorMessage);
            throw new ChaincodeException(errorMessage, AssetTransferErrors.ASSET_NOT_FOUND.toString());
        }

        String ownersCollectionName = getCollectionName(ctx);
        byte[] apdJSON = stub.getPrivateData(ownersCollectionName, imsi);

        if (apdJSON == null || apdJSON.length == 0) {
            String errorMessage = String.format("Failed to read asset from owner's Collection %s", ownersCollectionName);
            System.err.println(errorMessage);
            throw new ChaincodeException(errorMessage, AssetTransferErrors.ASSET_NOT_FOUND.toString());
        }
        verifyClientOrgMatchesPeerOrg(ctx);

        // delete the key from asset collection
        System.out.printf("DeleteAsset: collection %s, ID %s\n", ASSET_COLLECTION_NAME, imsi);
        stub.delPrivateData(ASSET_COLLECTION_NAME, imsi);

        // Finally, delete private details of asset
        stub.delPrivateData(ownersCollectionName, imsi);

    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public Asset ReadAsset(final Context ctx, final String imsi) {
        ChaincodeStub stub = ctx.getStub();
        System.out.printf("ReadAsset: collection %s, ID %s\n", ASSET_COLLECTION_NAME, imsi);
        byte[] assetJSON = stub.getPrivateData(ASSET_COLLECTION_NAME, imsi);

        if (assetJSON == null || assetJSON.length == 0) {
            System.out.printf("Asset not found: ID %s\n", imsi);
            return null;
        }

        Asset asset = Asset.deserialize(assetJSON);
        return asset;
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public AssetPrivateDetails ReadAssetPrivateDetails(final Context ctx, final String collection, final String imsi) {
        ChaincodeStub stub = ctx.getStub();
        System.out.printf("ReadAssetPrivateDetails: collection %s, ID %s\n", collection, imsi);
        byte[] assetPrvJSON = stub.getPrivateData(collection, imsi);

        if (assetPrvJSON == null || assetPrvJSON.length == 0) {
            String errorMessage = String.format("AssetPrivateDetails %s does not exist in collection %s", imsi, collection);
            System.out.println(errorMessage);
            return null;
        }

        AssetPrivateDetails assetpd = AssetPrivateDetails.deserialize(assetPrvJSON);
        return assetpd;
    }

    private void verifyClientOrgMatchesPeerOrg(final Context ctx) {
        String clientMSPID = ctx.getClientIdentity().getMSPID();
        String peerMSPID = ctx.getStub().getMspId();

        if (!peerMSPID.equals(clientMSPID)) {
            String errorMessage = String.format("Client from org %s is not authorized to read or write private data from an org %s peer", clientMSPID, peerMSPID);
            System.err.println(errorMessage);
            throw new ChaincodeException(errorMessage, AssetTransferErrors.INVALID_ACCESS.toString());
        }
    }

    private String getCollectionName(final Context ctx) {

        // Get the MSP ID of submitting client identity
        String clientMSPID = ctx.getClientIdentity().getMSPID();
        // Create the collection name
        return clientMSPID + "PrivateCollection";
    }
}
