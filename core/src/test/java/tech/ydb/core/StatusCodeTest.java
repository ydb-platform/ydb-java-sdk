package tech.ydb.core;

import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import tech.ydb.proto.StatusCodesProtos.StatusIds;


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
    public void statusTypes() {
        Assert.assertFalse(StatusCode.INTERNAL_ERROR.isTransportError());
        Assert.assertFalse(StatusCode.INTERNAL_ERROR.isRetryable(false));
        Assert.assertFalse(StatusCode.INTERNAL_ERROR.isRetryable(true));

        Assert.assertTrue(StatusCode.TRANSPORT_UNAVAILABLE.isTransportError());
        Assert.assertFalse(StatusCode.TRANSPORT_UNAVAILABLE.isRetryable(false));
        Assert.assertTrue(StatusCode.TRANSPORT_UNAVAILABLE.isRetryable(true));

        Assert.assertFalse(StatusCode.BAD_SESSION.isTransportError());
        Assert.assertTrue(StatusCode.BAD_SESSION.isRetryable(true));
        Assert.assertTrue(StatusCode.BAD_SESSION.isRetryable(true));
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
