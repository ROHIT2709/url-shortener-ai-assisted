package com.rohit.url_shortner.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class Base62Test {

    @Test
    void encodesZero() {
        assertEquals("0", Base62.encode(0));
    }

    @Test
    void encodesKnownValues() {
        assertEquals("1", Base62.encode(1));
        assertEquals("z", Base62.encode(35));
        assertEquals("Z", Base62.encode(61));
        assertEquals("10", Base62.encode(62));
    }

    @ParameterizedTest
    @ValueSource(longs = {0, 1, 61, 62, 12345, 999_999_999L, Long.MAX_VALUE})
    void roundTripsValues(long value) {
        assertEquals(value, Base62.decode(Base62.encode(value)));
    }

    @Test
    void rejectsNegativeValues() {
        assertThrows(IllegalArgumentException.class, () -> Base62.encode(-1));
    }

    @Test
    void rejectsInvalidCharactersOnDecode() {
        assertThrows(IllegalArgumentException.class, () -> Base62.decode("abc!"));
    }

    @Test
    void rejectsEmptyStringOnDecode() {
        assertThrows(IllegalArgumentException.class, () -> Base62.decode(""));
    }
}
