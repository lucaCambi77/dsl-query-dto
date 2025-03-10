package org.example.query;

public class QueryFilterException extends RuntimeException {
    public QueryFilterException(String exception) {
        super(exception);
    }

    public QueryFilterException(String exception, Throwable cause) {
        super(exception, cause);
    }
}
