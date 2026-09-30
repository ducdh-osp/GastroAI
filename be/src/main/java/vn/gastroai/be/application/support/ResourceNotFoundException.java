package vn.gastroai.be.application.support;

/**
 * Raised when a requested resource does not exist or belongs to another patient.
 * Keeping those two cases together avoids leaking resource ownership information.
 */
public class ResourceNotFoundException extends IllegalArgumentException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
