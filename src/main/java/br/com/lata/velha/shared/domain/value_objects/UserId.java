package br.com.lata.velha.shared.domain.value_objects;

import java.util.Objects;
import java.util.UUID;

public final class UserId {
    private final UUID id;

    private UserId(UUID id) {
        this.id = id;
    }

    public static UserId create(UUID id) {
        return new UserId(id);
    }

    public static UserId random() {
        var id = UUID.randomUUID();
        return UserId.create(id);
    }

    public static UserId fromString(String userId) {
        var id = UUID.fromString(userId);
        return UserId.create(id);
    }

    public UUID getValue() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof UserId userId)) return false;
        return Objects.equals(id, userId.id);
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
