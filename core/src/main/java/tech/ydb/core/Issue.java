package tech.ydb.core;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.Immutable;

import tech.ydb.YdbIssueMessage.IssueMessage;


/**
 * @author Sergey Polovko
 */
@Immutable
@ParametersAreNonnullByDefault
public class Issue implements Serializable {
    private static final long serialVersionUID = 902460195087723460L;

    public enum Severity {
        FATAL(0),
        ERROR(1),
        WARNING(2),
        INFO(3);

        private final int code;

        Severity(int code) {
            this.code = code;
        }

        public int getCode() {
            return this.code;
        }

        public static Severity of(int code) {
            for (Severity s: Severity.values()) {
                if (s.code == code) {
                    return s;
                }
            }
            return FATAL;
        }
    }

    public static final Issue[] EMPTY_ARRAY = new Issue[0];

    private final Position position;
    private final Position endPosition;
    private final int code;
    private final String message;
    private final Severity severity;
    private final Issue[] issues;

    private Issue(Position position, Position endPosition, int code, String message,
            Severity severity, Issue... issues) {
        this.position = Objects.requireNonNull(position, "position");
        this.endPosition = Objects.requireNonNull(endPosition, "endPosition");
        this.code = code;
        this.message = Objects.requireNonNull(message, "message");
        this.severity = Objects.requireNonNull(severity, "severity");
        this.issues = Objects.requireNonNull(issues, "issues");
    }

    public static Issue of(Position position, Position endPosition, int code,
            String message, Severity severity, Issue... issues) {
        return new Issue(position, endPosition, code, message, severity, issues);
    }

    public static Issue of(Position position, Position endPosition, int code,
            String message, Severity severity) {
        return new Issue(position, endPosition, code, message, severity, EMPTY_ARRAY);
    }

    public static Issue of(Position position, int code, String message, Severity severity) {
        return new Issue(position, Position.EMPTY, code, message, severity, EMPTY_ARRAY);
    }

    public static Issue of(int code, String message, Severity severity) {
        return new Issue(Position.EMPTY, Position.EMPTY, code, message, severity, EMPTY_ARRAY);
    }

    public static Issue of(String message, Severity severity) {
        return new Issue(Position.EMPTY, Position.EMPTY, 0, message, severity, EMPTY_ARRAY);
    }

    public static Issue fromPb(IssueMessage m) {
        return Issue.of(
            m.hasPosition() ? Position.fromPb(m.getPosition()) : Issue.Position.EMPTY,
            m.hasEndPosition() ? Position.fromPb(m.getEndPosition()) : Issue.Position.EMPTY,
            m.getIssueCode(),
            m.getMessage(),
            Severity.of(m.getSeverity()),
            fromPb(m.getIssuesList()));
    }

    public static Issue[] fromPb(List<IssueMessage> issues) {
        if (issues.isEmpty()) {
            return EMPTY_ARRAY;
        }
        Issue[] arr = new Issue[issues.size()];
        for (int i = 0; i < issues.size(); i++) {
            arr[i] = fromPb(issues.get(i));
        }
        return arr;
    }

    public Position getPosition() {
        return position;
    }

    public Position getEndPosition() {
        return endPosition;
    }

    public String getMessage() {
        return message;
    }

    public int getCode() {
        return code;
    }

    public Severity getSeverity() {
        return severity;
    }

    public Issue[] getIssues() {
        return issues;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Issue issue = (Issue) o;

        return code == issue.code
                && Objects.equals(position, issue.position)
                && Objects.equals(endPosition, issue.endPosition)
                && Objects.equals(message, issue.message)
                && severity == issue.severity;
    }

    @Override
    public int hashCode() {
        int result = position.hashCode();
        result = 31 * result + endPosition.hashCode();
        result = 31 * result + code;
        result = 31 * result + message.hashCode();
        result = 31 * result + severity.hashCode();
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        toString(sb);
        return sb.toString();
    }

    public void toString(StringBuilder sb) {
        if (position != Position.EMPTY) {
            sb.append(position.toString());
            if (endPosition != Position.EMPTY) {
                sb.append(" - ");
                sb.append(endPosition.toString());
            }
            sb.append(": ");
        }
        if (code > 0) {
            sb.append('#').append(code).append(' ');
        }
        sb.append(message);
        sb.append(" (S_").append(severity).append(")\n");
        for (Issue issue : issues) {
            sb.append("  ").append(issue.toString()).append('\n');
        }
        sb.setLength(sb.length() - 1); // drop last \n
    }

    /**
     * Issue position.
     */
    @Immutable
    @ParametersAreNonnullByDefault
    public static class Position implements Serializable {
        private static final long serialVersionUID = 2809376012140299923L;

        public static final Position EMPTY = new Position(0, 0, "");

        private final int column;
        private final int row;
        private final String file;

        private Position(int column, int row, String file) {
            this.column = column;
            this.row = row;
            this.file = Objects.requireNonNull(file, "file");
        }

        public static Position of(int column, int row) {
            return of(column, row, "");
        }

        public static Position of(int column, int row, String file) {
            if (column == 0 && row == 0 && file.isEmpty()) {
                return EMPTY;
            }
            return new Position(column, row, file);
        }

        private static Position fromPb(IssueMessage.Position m) {
            return of(m.getColumn(), m.getRow(), m.getFile());
        }

        public int getColumn() {
            return column;
        }

        public int getRow() {
            return row;
        }

        public String getFile() {
            return file;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }

            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            Position position = (Position) o;

            return column == position.column
                    && row == position.row
                    && Objects.equals(file, position.file);
        }

        @Override
        public int hashCode() {
            int result = column;
            result = 31 * result + row;
            result = 31 * result + file.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return column + ":" + row + (file.isEmpty() ? "" : " at " + file);
        }
    }
}
