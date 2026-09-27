package com.enterpriseflow.common;

import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Transient;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.proxy.HibernateProxy;
import org.springframework.data.domain.Persistable;

/**
 * Base class for entities whose ID is assigned by the application (UUIDv7) rather than the database.
 *
 * <p>Implementing {@link Persistable} tells Spring Data whether an entity is new. Without it, an entity
 * with a pre-assigned ID looks "existing", so {@code save()} would issue a needless SELECT before every
 * INSERT.
 */
@MappedSuperclass
public abstract class BaseEntity implements Persistable<UUID> {

    @Id
    private UUID id = UuidV7.generate();

    @Transient
    private boolean isNew = true;

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostPersist
    @PostLoad
    void markNotNew() {
        this.isNew = false;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        // Hibernate may hand out a lazy proxy (a generated subclass) instead of the entity itself.
        // Compare the real entity classes, and read the other ID through its getter; neither step
        // loads an uninitialised proxy from the database.
        if (other == null || entityClass(this) != entityClass(other)) {
            return false;
        }
        return Objects.equals(getId(), ((BaseEntity) other).getId());
    }

    @Override
    public int hashCode() {
        return entityClass(this).hashCode();
    }

    private static Class<?> entityClass(Object entity) {
        return entity instanceof HibernateProxy proxy
                ? proxy.getHibernateLazyInitializer().getPersistentClass()
                : entity.getClass();
    }
}
