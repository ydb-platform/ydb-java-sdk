package tech.ydb.query;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;

import tech.ydb.common.transaction.TxMode;
import tech.ydb.query.tools.QueryReader;
import tech.ydb.query.tools.SessionRetryContext;
import tech.ydb.table.query.Params;
import tech.ydb.table.result.ResultSetReader;
import tech.ydb.table.values.PrimitiveValue;
import tech.ydb.test.junit4.GrpcTransportRule;

/**
 * @author Kirill Kurdyukov
 */
public class ValueReadTest {

    @ClassRule
    public final static GrpcTransportRule ydbTransport = new GrpcTransportRule();

    private final SessionRetryContext CTX = SessionRetryContext.create(QueryClient.newClient(ydbTransport).build()).build();

//    @Test TODO
    public void date32datetime64timestamp64interval64() {
        date32datetime64timestamp64interval64Assert(
                LocalDate.of(988, 2, 6),
                LocalDateTime.of(988, 2, 7, 12, 30, 0),
                Instant.parse("0998-06-02T12:30:00.678901Z"),
                Duration.parse("-PT2S")
        );

//        date32datetime64timestamp64interval64Assert(
//                LocalDate.MIN,
//                LocalDateTime.MIN,
//                Instant.MIN,
//                Duration.ZERO
//        );


        QueryReader reader = CTX.supplyResult(
                s -> QueryReader.readFrom(s.createQuery("SELECT " +
                        "        Date32('998-06-02') AS c1,\n" +
                        "        Datetime64('0998-06-02T12:30:00Z') AS c2,\n" +
                        "        Timestamp64('0998-06-02T12:30:00.678901Z') AS c3,\n" +
                        "        Interval64('-PT2S') AS c4;", TxMode.NONE))
        ).join().getValue();

        ResultSetReader resultSetReader = reader.getResultSet(0);

        Assert.assertTrue(resultSetReader.next());
        Assert.assertEquals(LocalDate.parse("0998-06-02"), resultSetReader.getColumn(0).getDate32());
        Assert.assertEquals(LocalDateTime.parse("0998-06-02T12:30:00"), resultSetReader.getColumn(1).getDatetime64());
        Assert.assertEquals(Instant.parse("0998-06-02T12:30:00.678901Z"), resultSetReader.getColumn(2).getTimestamp64());
        Assert.assertEquals(Duration.parse("-PT2S"), resultSetReader.getColumn(3).getInterval64());
    }

    private void date32datetime64timestamp64interval64Assert(LocalDate date32, LocalDateTime datetime64,
                                                             Instant timestamp64, Duration interval64) {
        QueryReader reader = CTX.supplyResult(
                s -> QueryReader.readFrom(s.createQuery("" +
                                "DECLARE $date32 AS Date32;\n" +
                                "DECLARE $datetime64 AS Datetime64;\n" +
                                "DECLARE $timestamp64 AS Timestamp64;\n" +
                                "DECLARE $interval64 AS Instant64;\n" +
                                "SELECT $date32, $datetime64, $timestamp64, $interval64;",
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
}
