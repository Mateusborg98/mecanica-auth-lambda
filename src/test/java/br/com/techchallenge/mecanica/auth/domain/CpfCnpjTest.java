package br.com.techchallenge.mecanica.auth.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import br.com.techchallenge.mecanica.auth.domain.exception.InvalidCpfCnpjException;

class CpfCnpjTest {

    @Test
    void shouldAcceptValidCpfWithoutFormatting() {
        CpfCnpj document = new CpfCnpj("52998224725");

        assertEquals("52998224725", document.value());
        assertEquals(DocumentType.CPF, document.documentType());
        assertTrue(document.isCpf());
        assertFalse(document.isCnpj());
    }

    @Test
    void shouldNormalizeFormattedCpf() {
        CpfCnpj document = new CpfCnpj("529.982.247-25");

        assertEquals("52998224725", document.value());
        assertEquals(DocumentType.CPF, document.documentType());
    }

    @Test
    void shouldAcceptValidCnpjWithoutFormatting() {
        CpfCnpj document = new CpfCnpj("11222333000181");

        assertEquals("11222333000181", document.value());
        assertEquals(DocumentType.CNPJ, document.documentType());
        assertTrue(document.isCnpj());
        assertFalse(document.isCpf());
    }

    @Test
    void shouldNormalizeFormattedCnpj() {
        CpfCnpj document = new CpfCnpj("11.222.333/0001-81");

        assertEquals("11222333000181", document.value());
        assertEquals(DocumentType.CNPJ, document.documentType());
    }

    @Test
    void shouldIgnoreSurroundingWhitespace() {
        CpfCnpj document = new CpfCnpj(" 529.982.247-25 ");

        assertEquals("52998224725", document.value());
    }

    @Test
    void shouldMaskCpf() {
        CpfCnpj document = new CpfCnpj("52998224725");

        assertEquals("***.982.247-**", document.masked());
    }

    @Test
    void shouldMaskCnpj() {
        CpfCnpj document = new CpfCnpj("11222333000181");

        assertEquals("**.222.333/0001-**", document.masked());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            "   ",
            "123",
            "1234567890",
            "123456789012",
            "123456789012345",
            "11111111111",
            "00000000000",
            "11111111111111",
            "00000000000000",
            "52998224724",
            "11222333000180",
            "529.982.247-2A",
            "11.222.333/0001-8A",
            "CPF52998224725"
    })
    void shouldRejectInvalidDocument(String value) {
        assertThrows(
                InvalidCpfCnpjException.class,
                () -> new CpfCnpj(value));
    }
}