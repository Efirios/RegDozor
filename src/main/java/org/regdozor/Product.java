package org.regdozor;

import java.util.List;

public record Product(String code, String okpd2, String officialName, List<String> productNames, String category,
                      Boolean markingRequired, List<Obligation> obligations, String codeSource,
                      String verifiedOn) {
}
