package tech.ydb.core;

import java.util.HashSet;
import java.util.Set;

import tech.ydb.StatusCodesProtos.StatusIds;

import org.junit.Assert;
import org.junit.Test;


/**
 * @author Sergey Polovko
 */
public class StatusCodeTest {

    @Test
    public void statusCodesAreUnique() {
        Set<Integer> codes = new HashSet<>();
        for (StatusCode code : StatusCode.values()) {
            Assert.assertTrue(codes.add(code.getCode()));
        }
    }

    @Test
    public void statusCodesMatchProtobufCodes() {
        for (StatusIds.StatusCode codePb : StatusIds.StatusCode.values()) {
            if (codePb == StatusIds.StatusCode.UNRECOGNIZED) {
                continue;
            }

            StatusCode code = StatusCode.fromProto(codePb);
            Assert.assertEquals(
                String.format("enums %s and %s has different codes", code, codePb),
                code.getCode(), codePb.getNumber());

            if (codePb == StatusIds.StatusCode.STATUS_CODE_UNSPECIFIED) {
                Assert.assertEquals(StatusCode.UNUSED_STATUS, code);
            } else {
                Assert.assertEquals(code.name(), codePb.name());
                Assert.assertFalse(code.isTransportError());
            }
        }
    }
}
