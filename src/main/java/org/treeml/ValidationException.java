package org.treeml;

import java.util.Collections;
import java.util.List;

public class ValidationException extends RuntimeException {
    private final List<String> errors;

    ValidationException(String message, List<String> errors) {
        super(message);
        this.errors = Collections.unmodifiableList(errors);
    }

    public ValidationException(String message, Throwable cause, List<String> errors) {
        super(message, cause);
        this.errors = errors;
    }

    @SuppressWarnings("WeakerAccess")
    public List<String> getErrors() {
        return errors;
    }
}
