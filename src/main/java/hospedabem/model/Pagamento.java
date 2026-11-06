package hospedabem.model;

import java.time.LocalDateTime;

/** Pagamento confirmado no check-out (UC12): forma, extrato cobrado e momento da confirmação. */
public record Pagamento(FormaPagamento forma, Extrato extrato, LocalDateTime confirmadoEm) {
}
