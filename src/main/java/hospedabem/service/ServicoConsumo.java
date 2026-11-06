package hospedabem.service;

import hospedabem.dao.ConsumoDAO;
import hospedabem.dao.HospedagemDAO;
import hospedabem.dao.ItemConsumoDAO;
import hospedabem.model.Consumo;
import hospedabem.model.Hospedagem;
import hospedabem.model.ItemConsumo;
import hospedabem.model.NegocioException;
import hospedabem.model.ResumoHospedagem;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/** UC11 Lançar Consumo/Serviço. */
@Service
public class ServicoConsumo {
    private final HospedagemDAO hospedagemDAO;
    private final ConsumoDAO consumoDAO;
    private final ItemConsumoDAO itemDAO;
    private final Clock relogio;

    public ServicoConsumo(HospedagemDAO hospedagemDAO, ConsumoDAO consumoDAO, ItemConsumoDAO itemDAO, Clock relogio) {
        this.hospedagemDAO = hospedagemDAO;
        this.consumoDAO = consumoDAO;
        this.itemDAO = itemDAO;
        this.relogio = relogio;
    }

    @Transactional(readOnly = true)
    public List<ResumoHospedagem> listarHospedagensAtivas() {
        return hospedagemDAO.listarAtivas();
    }

    @Transactional(readOnly = true)
    public List<ItemConsumo> listarItens() {
        return itemDAO.listar();
    }

    /** Localiza a hospedagem e sua conta com os lançamentos já persistidos. */
    @Transactional(readOnly = true)
    public Hospedagem localizar(int codigoHospedagem) {
        Hospedagem h = exigirHospedagem(hospedagemDAO.buscarPorCodigo(codigoHospedagem), codigoHospedagem);
        h.exigirAtiva();
        h.getConta().exigirAberta();
        return h;
    }

    /**
     * Lança o item na conta. Trava hospedagem e conta (a mesma trava do check-out), relê o item no
     * catálogo para aplicar o preço vigente e grava tudo ou nada. A quantidade é validada pelo
     * {@link Consumo}.
     */
    @Transactional
    public Consumo lancar(int codigoHospedagem, String codigoItem, int quantidade, int funcionarioId) {
        Hospedagem h = exigirHospedagem(hospedagemDAO.buscarParaLancamento(codigoHospedagem), codigoHospedagem);
        ItemConsumo item = itemDAO.buscarPorCodigo(codigoItem)
                .orElseThrow(() -> new NegocioException("Item de consumo ou serviço não encontrado: " + codigoItem + "."));
        Consumo c = h.lancarConsumo(item, quantidade, LocalDateTime.now(relogio).truncatedTo(ChronoUnit.SECONDS));
        return c.comId(consumoDAO.inserir(h.getConta().getId(), item.id(), c, funcionarioId));
    }

    private static Hospedagem exigirHospedagem(java.util.Optional<Hospedagem> h, int codigo) {
        return h.orElseThrow(() -> new NegocioException("Hospedagem não localizada: " + codigo + "."));
    }
}
