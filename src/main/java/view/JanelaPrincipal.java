package view;

import model.Funcionario;
import javax.swing.*;

/** Mantém os painéis originais e acrescenta acesso aos cadastros. */
public class JanelaPrincipal extends JFrame {
    public JanelaPrincipal(Funcionario funcionario) {
        super("Academia - " + funcionario.getNome());
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        JPanel painel = funcionario.isGerente() ? new TelaGerencia(funcionario) : new TelaFuncionario(funcionario);
        setContentPane(painel);
        JMenuBar barra = new JMenuBar();
        JMenu cadastros = new JMenu("Cadastros");
        barra.add(cadastros);
        String[] opcoes = funcionario.isGerente()
                ? new String[]{"Clientes", "Planos", "Vendas", "Funcionários", "Equipamentos"}
                : new String[]{"Clientes", "Planos", "Vendas"};
        for (String tipo : opcoes) {
            JMenuItem item = new JMenuItem(tipo);
            item.addActionListener(e -> {
                try {
                    new Cadastro(this, tipo, funcionario, false).setVisible(true);
                    if (painel instanceof TelaGerencia) ((TelaGerencia)painel).atualizarTabela();
                    else ((TelaFuncionario)painel).atualizarTabela();
                } catch (Exception ex) { JOptionPane.showMessageDialog(this, ex.getMessage()); }
            });
            cadastros.add(item);
        }
        setJMenuBar(barra);
        pack();
        setLocationRelativeTo(null);
    }
}
