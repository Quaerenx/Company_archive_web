package com.company.testsupport;

import static com.company.testsupport.ProxyDefaults.defaultValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class ProxyDefaultsTest {
    @Test
    void returnsJvmDefaultsWithTheExactPrimitiveWrapperType() {
        assertEquals(false, defaultValue(boolean.class));
        assertEquals('\0', defaultValue(char.class));
        assertEquals((byte) 0, defaultValue(byte.class));
        assertEquals((short) 0, defaultValue(short.class));
        assertEquals(0, defaultValue(int.class));
        assertEquals(0L, defaultValue(long.class));
        assertEquals(0F, defaultValue(float.class));
        assertEquals(0D, defaultValue(double.class));
    }

    @Test
    void returnsNullForReferenceVoidAndMissingTypes() {
        assertNull(defaultValue(String.class));
        assertNull(defaultValue(void.class));
        assertNull(defaultValue(null));
    }
}
