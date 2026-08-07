package com.company.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class PageResultTest {
    @Test
    void keepsAnImmutablePageAndCalculatesTotals() {
        List<String> source = new ArrayList<>(List.of("one", "two"));
        PageResult<String> result = new PageResult<>(source, 41, 3, 20);
        source.clear();

        assertEquals(List.of("one", "two"), result.items());
        assertEquals(3, result.totalPages());
        assertThrows(
                UnsupportedOperationException.class,
                () -> result.items().add("three"));
    }

    @Test
    void rejectsImpossiblePageMetadata() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new PageResult<>(List.of(), 0, 2, 20));
        assertThrows(
                IllegalArgumentException.class,
                () -> new PageResult<>(List.of(), 20, 2, 20));
    }
}
