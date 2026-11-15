package br.com.lata.velha.shared.infrastructure.logging;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class PiiMaskerTest {

    @Test
    void mascaraCpfComPontuacao() {
        String masked = PiiMasker.mask("cliente 111.444.777-35 autenticado");
        assertFalse(masked.contains("111.444.777-35"));
        assertEquals("cliente ***.***.***-** autenticado", masked);
    }

    @Test
    void mascaraCpfSemPontuacao() {
        assertFalse(PiiMasker.mask("cpf=11144477735").contains("11144477735"));
    }

    @Test
    void mascaraEmail() {
        assertEquals("contato ***@***", PiiMasker.mask("contato cliente@example.com"));
    }

    @Test
    void mascaraJwt() {
        String jwt = "eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiIxMjMifQ.assinatura";
        assertEquals("token ***JWT***", PiiMasker.mask("token " + jwt));
    }

    @Test
    void mascaraParesSensibles() {
        assertEquals("password=*** authorization=***", PiiMasker.mask("password=Admin@123 authorization=Bearer"));
    }

    @Test
    void preservaNulosEVazios() {
        assertEquals(null, PiiMasker.mask(null));
        assertEquals("", PiiMasker.mask(""));
        assertEquals(null, PiiMasker.maskArg(null));
    }
}
