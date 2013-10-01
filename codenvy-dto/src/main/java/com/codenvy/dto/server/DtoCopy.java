package com.codenvy.dto.server;

/**
 * Alternative for {@link Object#clone()} mechanism. If DTO interface extends this interface then {@link
 * com.codenvy.dto.generator.DtoGenerator} adds implementation for {@link #copy()} method.
 *
 * @author <a href="mailto:aparfonov@codenvy.com">Andrey Parfonov</a>
 */
public interface DtoCopy {
    Object copy();
}
