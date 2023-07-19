import io.horizen.NscMethods;
import io.horizen.SnapshotMethods;
import org.junit.Test;

import java.util.List;
import java.util.Map;

public class MethodsTest {

    @Test
    public void testAbiParsing() {
        String abiReturnValue = "0000000000000000000000000000000000000000000000000000000000000020000000000000000000000000000000000000000000000000000000000000000e00000000000000000000000000c8f107a09cd4f463afc2f1e6e5bf6022ad46007a746800000000000000000000000000000000000000000000000000000000006d43715151726755564e714b4a4b78314b7178374b3168676a696e613242677200000000000000000000000000c8f107a09cd4f463afc2f1e6e5bf6022ad46007a7469000000000000000000000000000000000000000000000000000000000033536b73417977747754456938467938426a554278714e645667706178784e7700000000000000000000000000c8f107a09cd4f463afc2f1e6e5bf6022ad46007a74540000000000000000000000000000000000000000000000000000000000786f59534d3565734d665571326a716e6d4154666b423831324b68537566475800000000000000000000000000c8f107a09cd4f463afc2f1e6e5bf6022ad46007a7455000000000000000000000000000000000000000000000000000000000074484879317a6f546341797463343174423646346b4c6254457175595661665400000000000000000000000000c8f107a09cd4f463afc2f1e6e5bf6022ad46007a746d00000000000000000000000000000000000000000000000000000000006a6f4d79525778504d6e5766546441724b4c44766f646d62535054596970755400000000000000000000000000c8f107a09cd4f463afc2f1e6e5bf6022ad46007a74680000000000000000000000000000000000000000000000000000000000684a724752474d47704a534b664d364769333439326b37566f6442753932683600000000000000000000000000c8f107a09cd4f463afc2f1e6e5bf6022ad46007a745900000000000000000000000000000000000000000000000000000000004741687a76343279744a505462656b754b68726e486f6439425273656947723900000000000000000000000000c8f107a09cd4f463afc2f1e6e5bf6022ad46007a7462000000000000000000000000000000000000000000000000000000000057537650653967714b6e53415a354d56614a576763704179325943526451516200000000000000000000000000c8f107a09cd4f463afc2f1e6e5bf6022ad46007a745500000000000000000000000000000000000000000000000000000000003259313152333738423171623853475144424e356a586346575634646539375a00000000000000000000000000c8f107a09cd4f463afc2f1e6e5bf6022ad46007a74680000000000000000000000000000000000000000000000000000000000765a4257515a61625532364674364a73636d6e6232415054666e5235584c6d3800000000000000000000000000c8f107a09cd4f463afc2f1e6e5bf6022ad46007a746b0000000000000000000000000000000000000000000000000000000000715035764d4d784b7a465643464a743372797744587a4e61734d736a54773568000000000000000000000000ca12fcb886cbf73a39d87aac9610f8a3035366427a74540000000000000000000000000000000000000000000000000000000000476854744a4e434276665777734253665a617368724444586f47615471323272000000000000000000000000ca12fcb886cbf73a39d87aac9610f8a3035366427a74690000000000000000000000000000000000000000000000000000000000424d4544674431586a35625839686638615835357646544c4c34706262727150000000000000000000000000a0ccf49adbbdff7a814c07d1fcbc2b719d6749597a746400000000000000000000000000000000000000000000000000000000004b71317571564566566a684a31366b794b45414e6d654a65733178666d705031";
        NscMethods.getKeyOwnershipFromAbi(abiReturnValue);
    }


    @Test
    public void addOwnershipEntry() {
        String address = "ztWBHD2Eo6uRLN6xAYxj8mhmSPbUYrvMPwt";
        String owner = "0xA0CCf49aDBbdfF7A814C07D1FcBC2b719d674959";
        SnapshotMethods.addOwnershipEntry(address, owner);
    }

    @Test
    public void getOwnerships() throws Exception {
        String address = "0xA0CCf49aDBbdfF7A814C07D1FcBC2b719d674959";
        Map<String, List<String>> ret = SnapshotMethods.getMcAddressMap(address);
    }
}
