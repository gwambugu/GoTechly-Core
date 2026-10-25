package com.dotechly.gotechly.core.service.account;

/**
 * A linkage request Core rejects before (or instead of) calling the scheme.
 *
 * <p>Distinct from the SPI's {@code LinkedAccountException}, which a plugin throws about the
 * <em>scheme</em> leg. The {@link Kind} carries the HTTP status the resource should answer with, so
 * the mapping lives with the reason rather than being re-derived from a message.
 */
public class LinkedAccountRequestException extends RuntimeException {

    public enum Kind {
        /** Malformed or incomplete request — 400. */
        INVALID,
        /** No plugin registered for the payment type — 501: the deployment lacks the capability. */
        NO_HANDLER,
        /** Update/deregister against an account with no stored linkage — 404. */
        NOT_FOUND,
        /** Register against an account already linked — 409. */
        CONFLICT
    }

    private final Kind kind;

    public LinkedAccountRequestException(Kind kind, String message) {
        super(message);
        this.kind = kind;
    }

    public Kind getKind() {
        return kind;
    }
}
