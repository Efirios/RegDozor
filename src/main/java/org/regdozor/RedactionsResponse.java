package org.regdozor;

import java.util.List;

public record RedactionsResponse(long docid, List<Redaction> redactions, String error) {
}
