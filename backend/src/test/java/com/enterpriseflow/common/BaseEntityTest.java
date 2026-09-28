package com.enterpriseflow.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests for identity and equality rules shared by all entities (review finding R-002).
 *
 * <p>The Hibernate proxy case needs a real persistence context, so it is covered by
 * {@code DocumentRepositoryTest.anEntityEqualsALazyProxyOfItself} instead.
 */
class BaseEntityTest {

    static class Invoice extends BaseEntity {
    }

    static class Receipt extends BaseEntity {
    }

    @Test
    void aNewEntityHasATimeOrderedIdAndIsNew() {
        Invoice invoice = new Invoice();

        assertThat(invoice.getId()).isNotNull();
        assertThat(invoice.getId().version()).isEqualTo(7);
        assertThat(invoice.isNew()).isTrue();
    }

    @Test
    void isNoLongerNewOncePersistedOrLoaded() {
        Invoice invoice = new Invoice();

        invoice.markNotNew();

        assertThat(invoice.isNew()).isFalse();
    }

    @Test
    void entitiesWithTheSameIdAreEqual() {
        Invoice first = new Invoice();
        Invoice second = withId(new Invoice(), first.getId());

        assertThat(first).isEqualTo(second);
        assertThat(second).isEqualTo(first);
        assertThat(first.hashCode()).isEqualTo(second.hashCode());
    }

    @Test
    void entitiesWithDifferentIdsAreNotEqual() {
        assertThat(new Invoice()).isNotEqualTo(new Invoice());
    }

    @Test
    void differentEntityTypesAreNeverEqualEvenWithTheSameId() {
        Invoice invoice = new Invoice();
        Receipt receipt = withId(new Receipt(), invoice.getId());

        assertThat(invoice).isNotEqualTo(receipt);
    }

    @Test
    void isNotEqualToNullOrUnrelatedObjects() {
        Invoice invoice = new Invoice();

        assertThat(invoice).isNotEqualTo(null);
        assertThat(invoice).isNotEqualTo(invoice.getId());
    }

    @Test
    void hashCodeStaysStableSoEntitiesCanLiveInHashSets() {
        Invoice invoice = new Invoice();
        Set<Invoice> set = new HashSet<>(Set.of(invoice));

        invoice.markNotNew();

        assertThat(set).contains(invoice);
    }

    private static <T extends BaseEntity> T withId(T entity, UUID id) {
        ReflectionTestUtils.setField(entity, BaseEntity.class, "id", id, UUID.class);
        return entity;
    }
}
