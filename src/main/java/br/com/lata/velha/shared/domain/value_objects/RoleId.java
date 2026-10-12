package br.com.lata.velha.shared.domain.value_objects;

import java.util.Objects;
import java.util.UUID;

/**
 * Value Object que encapsula o identificador de uma Role.
 */
public final class RoleId {
    private final UUID id;

    private RoleId(UUID id) {
        this.id = id;
    }

    public static RoleId create(UUID id) {
        return new RoleId(id);
    }

    public static RoleId random() {
        var id = UUID.randomUUID();
        return RoleId.create(id);
    }

    public UUID getValue() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof RoleId roleId)) return false;
        return Objects.equals(id, roleId.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return id.toString();
    }
}
