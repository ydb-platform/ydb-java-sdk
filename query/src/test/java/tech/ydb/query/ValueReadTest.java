package tech.ydb.query;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

import java.time.chrono.IsoChronology;
import java.time.temporal.ChronoUnit;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;

import tech.ydb.common.transaction.TxMode;
import tech.ydb.core.Issue;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.query.tools.QueryReader;
import tech.ydb.query.tools.SessionRetryContext;
import tech.ydb.table.query.Params;
import tech.ydb.table.result.ResultSetReader;
import tech.ydb.table.result.ValueReader;
import tech.ydb.table.values.PrimitiveType;
import tech.ydb.table.values.PrimitiveValue;
import tech.ydb.table.values.Type;
import tech.ydb.test.junit4.GrpcTransportRule;

/**
 * @author Kirill Kurdyukov
 */
public class ValueReadTest {

    @ClassRule
    public final static GrpcTransportRule ydbTransport = new GrpcTransportRule();

    private final SessionRetryContext CTX = SessionRetryContext.create(QueryClient.newClient(ydbTransport).build()).build();

    @Test
    public void date32datetime64timestamp64interval64() {
        date32datetime64timestamp64interval64Assert(
                LocalDate.of(988, 2, 6),
                LocalDateTime.of(988, 2, 7, 12, 30, 0),
                Instant.parse("0998-06-02T12:30:00.678901Z"),
                Duration.parse("-PT2S")
        );

        date32datetime64timestamp64interval64Assert(
                LocalDate.now(),
                LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS),
                Instant.now().truncatedTo(ChronoUnit.MICROS),
                Duration.ZERO
        );

        QueryReader reader = CTX.supplyResult(
                s -> QueryReader.readFrom(s.createQuery("DECLARE $date32 AS Date32;\n" +
                        "DECLARE $datetime64 AS Datetime64;\n" +
                        "DECLARE $timestamp64 AS Timestamp64;\n" +
                        "DECLARE $interval64 AS Interval64;\n" +
                        "\n" +
                        "$date32=Date32('998-06-02'); \n" +
                        "$datetime64=Datetime64('0998-06-02T12:30:00Z');\n" +
                        "$timestamp64=Timestamp64('0998-06-02T12:30:00.678901Z');\n" +
                        "$interval64=Interval64('-PT2S');\n" +
                        "\n" +
                        "SELECT $date32, $datetime64, $timestamp64, $interval64;", TxMode.NONE))
        ).join().getValue();

        ResultSetReader resultSetReader = reader.getResultSet(0);

        Assert.assertTrue(resultSetReader.next());
        Assert.assertEquals(LocalDate.parse("0998-06-02"), resultSetReader.getColumn(0).getDate32());
        Assert.assertEquals(LocalDateTime.parse("0998-06-02T12:30:00"), resultSetReader.getColumn(1).getDatetime64());
        Assert.assertEquals(Instant.parse("0998-06-02T12:30:00.678901Z"), resultSetReader.getColumn(2).getTimestamp64());
        Assert.assertEquals(Duration.parse("-PT2S"), resultSetReader.getColumn(3).getInterval64());
    }

    @Test
    public void timestamp64ReadTest() {
        System.out.println(Instant.ofEpochSecond(-4611669897600L));
        QueryReader result = CTX.supplyResult(
                s -> QueryReader.readFrom(s.createQuery("SELECT "
                                + "Timestamp64('-144169-01-01T00:00:00Z') as t1,"
                                + "Timestamp64('148107-12-31T23:59:59.999999Z') as t2;",
                        TxMode.NONE
                ))
        ).join().getValue();

        Assert.assertEquals(1, result.getResultSetCount());

        ResultSetReader rs = result.getResultSet(0);
        Assert.assertTrue(rs.next());

        assertTimestamp64(rs.getColumn("t1"), false, Instant.ofEpochSecond(-4611669897600L));
        assertTimestamp64(rs.getColumn("t2"), false, Instant.ofEpochSecond(4611669811199L, 999999000));

        Status invalid = CTX.supplyResult(
                s -> s.createQuery("SELECT "
                                + "Timestamp64('-144170-01-01T00:00:00Z') as t1,"
                                + "Timestamp64('148108-01-01T00:00:00.000000Z') as t2;",
                        TxMode.NONE
                ).execute()
        ).join().getStatus();

        Assert.assertEquals(StatusCode.GENERIC_ERROR, invalid.getCode());
        Issue[] issues = invalid.getIssues();
        Assert.assertEquals(2, issues.length);
        Assert.assertEquals("Invalid value \"-144170-01-01T00:00:00Z\" for type Timestamp64", issues[0].getMessage());
        Assert.assertEquals("Invalid value \"148108-01-01T00:00:00.000000Z\" for type Timestamp64", issues[1].getMessage());
    }

    private void date32datetime64timestamp64interval64Assert(LocalDate date32, LocalDateTime datetime64,
                                                             Instant timestamp64, Duration interval64) {
        QueryReader reader = CTX.supplyResult(
                s -> QueryReader.readFrom(s.createQuery("" +
                                "DECLARE $date32 AS date32;\n" +
                                "DECLARE $datetime64 AS Datetime64;\n" +
                                "DECLARE $timestamp64 AS Timestamp64;\n" +
                                "DECLARE $interval64 AS Interval64;" +
                                "SELECT  $date32, $datetime64, $timestamp64, $interval64;",
                        TxMode.SERIALIZABLE_RW,
                        Params.of(
                                "$date32", PrimitiveValue.newDate32(date32),
                                "$datetime64", PrimitiveValue.newDatetime64(datetime64),
                                "$timestamp64", PrimitiveValue.newTimestamp64(timestamp64),
                                "$interval64", PrimitiveValue.newInterval64(interval64)
                        )
                ))
        ).join().getValue();

        ResultSetReader resultSetReader = reader.getResultSet(0);

        Assert.assertTrue(resultSetReader.next());
        Assert.assertEquals(date32, resultSetReader.getColumn(0).getDate32());
        Assert.assertEquals(datetime64, resultSetReader.getColumn(1).getDatetime64());
        Assert.assertEquals(timestamp64, resultSetReader.getColumn(2).getTimestamp64());
        Assert.assertEquals(interval64, resultSetReader.getColumn(3).getInterval64());
    }

    private void assertTimestamp64(ValueReader vr, boolean optional, Instant expected) {
        Assert.assertNotNull(vr);
        if (optional) {
            Assert.assertSame(Type.Kind.OPTIONAL, vr.getType().getKind());
            Assert.assertSame(PrimitiveType.Timestamp64, vr.getType().unwrapOptional());
        } else {
            Assert.assertSame(PrimitiveType.Timestamp64, vr.getType());
        }

        Assert.assertEquals(expected, vr.getTimestamp64());
    }
}
