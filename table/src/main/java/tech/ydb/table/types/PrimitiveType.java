package tech.ydb.table.types;


/**
 * @author Sergey Polovko
 */
public final class PrimitiveType implements Type {

    public enum Id {
        Bool(0x0006),
        Int8(0x0007),
        Uint8(0x0005),
        Int16(0x0008),
        Uint16(0x0009),
        Int32(0x0001),
        Uint32(0x0002),
        Int64(0x0003),
        Uint64(0x0004),
        Float(0x0021),
        Double(0x0020),
        String(0x1001),
        Utf8(0x1200),
        Yson(0x1201),
        Json(0x1202),
        Uuid(0x1203),
        Date(0x0030),
        Datetime(0x0031),
        Timestamp(0x0032),
        Interval(0x0033),
        TzDate(0x0034),
        TzDatetime(0x0035),
        TzTimestamp(0x0036),
        ;

        private final short numId;

        Id(int numId) {
            this.numId = (short) numId;
        }

        public short getNumId() {
            return numId;
        }
    }

    public static PrimitiveType of(Id id) {
        return BY_IDS[id.ordinal()];
    }

    public static PrimitiveType bool() { return PrimitiveType.BOOL; }
    public static PrimitiveType int8() { return PrimitiveType.INT_8; }
    public static PrimitiveType uint8() { return PrimitiveType.UINT_8; }
    public static PrimitiveType int16() { return PrimitiveType.INT_16; }
    public static PrimitiveType uint16() { return PrimitiveType.UINT_16; }
    public static PrimitiveType int32() { return PrimitiveType.INT_32; }
    public static PrimitiveType uint32() { return PrimitiveType.UINT_32; }
    public static PrimitiveType int64() { return PrimitiveType.INT_64; }
    public static PrimitiveType uint64() { return PrimitiveType.UINT_64; }
    public static PrimitiveType float32() { return PrimitiveType.FLOAT; }
    public static PrimitiveType float64() { return PrimitiveType.DOUBLE; }
    public static PrimitiveType string() { return PrimitiveType.STRING; }
    public static PrimitiveType utf8() { return PrimitiveType.UTF_8; }
    public static PrimitiveType yson() { return PrimitiveType.YSON; }
    public static PrimitiveType json() { return PrimitiveType.JSON; }
    public static PrimitiveType uuid() { return PrimitiveType.UUID; }
    public static PrimitiveType date() { return PrimitiveType.DATE; }
    public static PrimitiveType datetime() { return PrimitiveType.DATETIME; }
    public static PrimitiveType timestamp() { return PrimitiveType.TIMESTAMP; }
    public static PrimitiveType interval() { return PrimitiveType.INTERVAL; }
    public static PrimitiveType tzDate() { return PrimitiveType.TZ_DATE; }
    public static PrimitiveType tzDatetime() { return PrimitiveType.TZ_DATETIME; }
    public static PrimitiveType tzTimestamp() { return PrimitiveType.TZ_TIMESTAMP; }

    private static final PrimitiveType BOOL = new PrimitiveType(Id.Bool);
    private static final PrimitiveType INT_8 = new PrimitiveType(Id.Int8);
    private static final PrimitiveType UINT_8 = new PrimitiveType(Id.Uint8);
    private static final PrimitiveType INT_16 = new PrimitiveType(Id.Int16);
    private static final PrimitiveType UINT_16 = new PrimitiveType(Id.Uint16);
    private static final PrimitiveType INT_32 = new PrimitiveType(Id.Int32);
    private static final PrimitiveType UINT_32 = new PrimitiveType(Id.Uint32);
    private static final PrimitiveType INT_64 = new PrimitiveType(Id.Int64);
    private static final PrimitiveType UINT_64 = new PrimitiveType(Id.Uint64);
    private static final PrimitiveType FLOAT = new PrimitiveType(Id.Float);
    private static final PrimitiveType DOUBLE = new PrimitiveType(Id.Double);
    private static final PrimitiveType STRING = new PrimitiveType(Id.String);
    private static final PrimitiveType UTF_8 = new PrimitiveType(Id.Utf8);
    private static final PrimitiveType YSON = new PrimitiveType(Id.Yson);
    private static final PrimitiveType JSON = new PrimitiveType(Id.Json);
    private static final PrimitiveType UUID = new PrimitiveType(Id.Uuid);
    private static final PrimitiveType DATE = new PrimitiveType(Id.Date);
    private static final PrimitiveType DATETIME = new PrimitiveType(Id.Datetime);
    private static final PrimitiveType TIMESTAMP = new PrimitiveType(Id.Timestamp);
    private static final PrimitiveType INTERVAL = new PrimitiveType(Id.Interval);
    private static final PrimitiveType TZ_DATE = new PrimitiveType(Id.TzDate);
    private static final PrimitiveType TZ_DATETIME = new PrimitiveType(Id.TzDatetime);
    private static final PrimitiveType TZ_TIMESTAMP = new PrimitiveType(Id.TzTimestamp);

    private static final PrimitiveType[] BY_IDS;
    static {
        Id[] ids = Id.values();
        BY_IDS = new PrimitiveType[ids.length];
        for (Id id : ids) {
            BY_IDS[id.ordinal()] = byId(id);
        }
    }

    private final Id id;

    private PrimitiveType(Id id) {
        this.id = id;
    }

    @Override
    public Kind getKind() {
        return Kind.PRIMITIVE;
    }

    public Id getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        return this == o;
    }

    @Override
    public int hashCode() {
        return 31 * Kind.PRIMITIVE.hashCode() + id.hashCode();
    }

    @Override
    public java.lang.String toString() {
        return id.name();
    }

    private static PrimitiveType byId(Id id) {
        switch (id) {
            case Bool: return BOOL;
            case Int8: return INT_8;
            case Uint8: return UINT_8;
            case Int16: return INT_16;
            case Uint16: return UINT_16;
            case Int32: return INT_32;
            case Uint32: return UINT_32;
            case Int64: return INT_64;
            case Uint64: return UINT_64;
            case Float: return FLOAT;
            case Double: return DOUBLE;
            case String: return STRING;
            case Utf8: return UTF_8;
            case Yson: return YSON;
            case Json: return JSON;
            case Uuid: return UUID;
            case Date: return DATE;
            case Datetime: return DATETIME;
            case Timestamp: return TIMESTAMP;
            case Interval: return INTERVAL;
            case TzDate: return TZ_DATE;
            case TzDatetime: return TZ_DATETIME;
            case TzTimestamp: return TZ_TIMESTAMP;
        }
        throw new IllegalArgumentException("unknown PrimitiveType: " + id);
    }
}
