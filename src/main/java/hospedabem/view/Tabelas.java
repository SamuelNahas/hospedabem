package hospedabem.view;

import hospedabem.model.Consumo;
import hospedabem.model.ResumoHospedagem;
import javafx.scene.control.TableView;

import static hospedabem.view.Campos.DATA_HORA;
import static hospedabem.view.Campos.coluna;
import static hospedabem.view.Campos.moeda;
import static hospedabem.view.Campos.tabela;
import static hospedabem.view.Campos.texto;

/** Tabelas compartilhadas pelo lançamento de consumo (UC11) e pelo check-out (UC12). */
final class Tabelas {
    private Tabelas() {
    }

    static TableView<ResumoHospedagem> hospedagensAtivas() {
        return tabela(
                coluna("Hospedagem", ResumoHospedagem::codigo),
                coluna("Quarto", ResumoHospedagem::numeroQuarto),
                coluna("Hóspedes", ResumoHospedagem::hospedes),
                coluna("Entrada", r -> DATA_HORA.format(r.entrada())),
                coluna("Saída prevista", r -> texto(r.saidaPrevista())));
    }

    static TableView<Consumo> consumos() {
        return tabela(
                coluna("Data/hora", c -> DATA_HORA.format(c.dataHora())),
                coluna("Item", c -> c.codigoItem() + " - " + c.descricaoItem()),
                coluna("Qtd.", Consumo::quantidade),
                coluna("Unitário", c -> moeda(c.precoUnitario())),
                coluna("Total", c -> moeda(c.valorTotal())));
    }
}
