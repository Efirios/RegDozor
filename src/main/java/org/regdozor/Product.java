package org.regdozor;

import java.util.List;

public record Product(String code, String officialName, List<String> productNames, String category,
                      Boolean markingRequired, String wave, List<Obligation> obligations, String codeSource,
                      String verifiedOn) {

}
