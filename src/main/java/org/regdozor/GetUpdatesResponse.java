package org.regdozor;

import java.util.List;

public record GetUpdatesResponse(Boolean ok, List<Update> result) {
}
