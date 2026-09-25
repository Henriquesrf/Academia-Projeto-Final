package view;

import dao.*;
import model.*;
import java.awt.*;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

/** Janela simples de CRUD, reutilizada pelas cinco tabelas existentes. */
public class Cadastro extends JDialog {
    private final String tipo;
    private final Funcionario logado;
    private final boolean primeiro;
    private final JTable tabela = new JTable();
    private final Map<String, JComponent> campos = new LinkedHashMap<>();
    private int idSelecionado;
    private java.util.List<Object> registros = new ArrayList<>();
    private final JPanel formulario = new JPanel(new GridLayout(0, 2, 8, 8));

    public Cadastro(Window dono, String tipo, Funcionario logado, boolean primeiro) {
        super(dono, "Cadastro de " + tipo, ModalityType.APPLICATION_MODAL);
        this.tipo = tipo;
        this.logado = logado;
        this.primeiro = primeiro;
        if ((tipo.equals("Funcionários") || tipo.equals("Equipamentos"))
                && !primeiro && (logado == null || !logado.isGerente())) {
            throw new IllegalArgumentException("Acesso restrito à gerência.");
        }
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        try { criarCampos(); } catch (SQLException e) { throw new IllegalStateException(e); }
        JPanel botoes = new JPanel();
        botao(botoes, "Novo / Limpar", () -> limpar());
        botao(botoes, "Cadastrar", () -> salvar(false));
        botao(botoes, "Alterar", () -> salvar(true));
        botao(botoes, "Excluir", () -> excluir());
        botao(botoes, "Atualizar lista", () -> carregar());
        botao(botoes, "Fechar", () -> dispose());
        JPanel topo = new JPanel(new BorderLayout(8, 8));
        topo.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        topo.add(new JLabel("ID automático. Selecione uma linha para alterar ou excluir."), BorderLayout.NORTH);
        topo.add(formulario, BorderLayout.CENTER);
        topo.add(botoes, BorderLayout.SOUTH);
        add(topo, BorderLayout.NORTH);
        add(new JScrollPane(tabela), BorderLayout.CENTER);
        tabela.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabela.setAutoCreateRowSorter(true);
        tabela.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && tabela.getSelectedRow() >= 0) {
                preencher(registros.get(tabela.convertRowIndexToModel(tabela.getSelectedRow())));
            }
        });
        setSize(950, 620);
        setLocationRelativeTo(dono);
        limpar();
        carregar();
    }

    private void botao(JPanel painel, String texto, Runnable acao) {
        JButton b = new JButton(texto);
        b.addActionListener(e -> acao.run());
        painel.add(b);
    }

    private void campo(String nome, JComponent componente) {
        campos.put(nome, componente);
        formulario.add(new JLabel(nome)); formulario.add(componente);
    }
    private void texto(String nome) { campo(nome, new JTextField()); }
    private void ativo() { campo("Ativo", new JCheckBox("Sim", true)); }

    private void criarCampos() throws SQLException {
        switch (tipo) {
            case "Clientes":
                texto("Nome"); texto("CPF"); texto("E-mail"); texto("Telefone"); ativo(); break;
            case "Planos":
                texto("Nome"); texto("Duração (dias)"); texto("Preço"); ativo(); break;
            case "Equipamentos":
                texto("Nome"); texto("Data de aquisição (dd/MM/aaaa)"); texto("Estado de conservação"); break;
            case "Funcionários":
                texto("Nome"); texto("CPF");
                campo("Cargo", new JComboBox<>(new String[]{"FUNCIONARIO", "GERENTE"}));
                texto("Login"); campo("Senha (vazia mantém a atual)", new JPasswordField()); ativo();
                if (primeiro) { ((JComboBox<?>)campos.get("Cargo")).setSelectedItem("GERENTE"); campos.get("Cargo").setEnabled(false); }
                break;
            case "Vendas":
                campo("Cliente", new JComboBox<>(new ClienteDAO().listar().toArray()));
                campo("Plano", new JComboBox<>(new PlanoDAO().listar().toArray()));
                campo("Funcionário", new JComboBox<>(new FuncionarioDAO().listar().toArray()));
                texto("Data/hora (dd/MM/aaaa HH:mm:ss)"); texto("Valor contratado");
                ((JComboBox<?>)campos.get("Plano")).addActionListener(e -> {
                    Planos p = (Planos)((JComboBox<?>)campos.get("Plano")).getSelectedItem();
                    if (p != null) set("Valor contratado", p.getPreco());
                });
                campos.get("Funcionário").setEnabled(logado != null && logado.isGerente());
                break;
            default: throw new IllegalArgumentException("Cadastro desconhecido.");
        }
    }

    private String ler(String campo) {
        JComponent c = campos.get(campo);
        if (c instanceof JComboBox) return String.valueOf(((JComboBox<?>)c).getSelectedItem());
        if (c instanceof JPasswordField) return new String(((JPasswordField)c).getPassword());
        return ((JTextField)c).getText().trim();
    }
    private String obrigatorio(String campo) {
        String s = ler(campo);
        if (s.trim().isEmpty()) throw new IllegalArgumentException("Preencha: " + campo);
        return s;
    }
    private int estaAtivo() { return ((JCheckBox)campos.get("Ativo")).isSelected() ? 1 : 0; }
    private double numero(String campo) {
        try {
            double n = Double.parseDouble(obrigatorio(campo).replace(',', '.'));
            if (!Double.isFinite(n) || n < 0) throw new NumberFormatException();
            return n;
        } catch (NumberFormatException e) { throw new IllegalArgumentException(campo + ": informe um valor válido, maior ou igual a zero."); }
    }
    private int duracao() {
        try {
            int n = Integer.parseInt(obrigatorio("Duração (dias)"));
            if (n <= 0) throw new NumberFormatException();
            return n;
        } catch (NumberFormatException e) { throw new IllegalArgumentException("A duração deve ser um número inteiro maior que zero."); }
    }
    private String data(String campo, boolean hora) {
        String s = obrigatorio(campo);
        try {
            DateTimeFormatter f = DateTimeFormatter.ofPattern(hora ? "dd/MM/uuuu HH:mm:ss" : "dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);
            if (hora) LocalDateTime.parse(s, f); else LocalDate.parse(s, f);
            return s;
        } catch (java.time.DateTimeException e) { throw new IllegalArgumentException("Data inválida: " + campo); }
    }
    private Object escolhido(String campo) {
        Object obj = ((JComboBox<?>)campos.get(campo)).getSelectedItem();
        if (obj == null) throw new IllegalArgumentException("Cadastre e selecione: " + campo);
        return obj;
    }
    private void set(String nome, Object valor) {
        JComponent c = campos.get(nome);
        if (c instanceof JCheckBox) ((JCheckBox)c).setSelected(((Number)valor).intValue() == 1);
        else if (c instanceof JComboBox) ((JComboBox<?>)c).setSelectedItem(valor);
        else ((JTextField)c).setText(String.valueOf(valor));
    }
    private void selecionar(String nome, int id) {
        JComboBox<?> combo = (JComboBox<?>)campos.get(nome);
        for (int i = 0; i < combo.getItemCount(); i++) {
            Object o = combo.getItemAt(i);
            int atual = o instanceof Cliente ? ((Cliente)o).getId() : o instanceof Planos ? ((Planos)o).getId() : ((Funcionario)o).getId();
            if (atual == id) { combo.setSelectedIndex(i); return; }
        }
        combo.setSelectedIndex(-1);
    }
    private void limpar() {
        tabela.clearSelection(); idSelecionado = 0;
        for (JComponent c : campos.values()) {
            if (c instanceof JTextField) ((JTextField)c).setText("");
            else if (c instanceof JCheckBox) ((JCheckBox)c).setSelected(true);
        }
        if (tipo.equals("Vendas")) {
            set("Data/hora (dd/MM/aaaa HH:mm:ss)", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
            if (logado != null) selecionar("Funcionário", logado.getId());
            Planos p = (Planos)((JComboBox<?>)campos.get("Plano")).getSelectedItem();
            if (p != null) set("Valor contratado", p.getPreco());
        }
    }
    private void erro(Exception e) {
        JOptionPane.showMessageDialog(this, "Não foi possível concluir: " + e.getMessage(), "Atenção", JOptionPane.ERROR_MESSAGE);
    }

    private void salvar(boolean alteracao) {
        if (alteracao && idSelecionado == 0) { erro(new Exception("Selecione uma linha para alterar.")); return; }
        try {
            switch (tipo) {
                case "Clientes": {
                    Cliente o = new Cliente();
                    o.setId(alteracao ? idSelecionado : 0);
                    o.setNome(obrigatorio("Nome"));
                    o.setCpf(obrigatorio("CPF"));
                    o.setEmail(obrigatorio("E-mail"));
                    o.setTelefone(obrigatorio("Telefone"));
                    o.setAtivo(estaAtivo());
                    if (alteracao) new ClienteDAO().atualizar(o); else new ClienteDAO().inserir(o);
                    break;
                }
                case "Planos": {
                    Planos o = new Planos();
                    o.setId(alteracao ? idSelecionado : 0);
                    o.setNome(obrigatorio("Nome"));
                    o.setDuracao(duracao());
                    o.setPreco(numero("Preço"));
                    o.setAtivo(estaAtivo());
                    if (alteracao) new PlanoDAO().atualizar(o); else new PlanoDAO().inserir(o);
                    break;
                }
                case "Equipamentos": {
                    Equipamento o = new Equipamento();
                    o.setId(alteracao ? idSelecionado : 0);
                    o.setNome(obrigatorio("Nome"));
                    o.setDataAquisicao(data("Data de aquisição (dd/MM/aaaa)", false));
                    o.setEstadoConservacao(obrigatorio("Estado de conservação"));
                    if (alteracao) new EquipamentoDAO().atualizar(o); else new EquipamentoDAO().inserir(o);
                    break;
                }
                case "Funcionários": {
                    Funcionario o = new Funcionario();
                    o.setId(alteracao ? idSelecionado : 0);
                    o.setNome(obrigatorio("Nome"));
                    o.setCpf(obrigatorio("CPF"));
                    o.setCargo(obrigatorio("Cargo"));
                    o.setLogin(obrigatorio("Login"));
                    String senha = ler("Senha (vazia mantém a atual)");
                    Funcionario anterior = alteracao ? new FuncionarioDAO().buscarPorId(idSelecionado) : null;
                    if (senha.isEmpty() && anterior == null) throw new IllegalArgumentException("Informe a senha.");
                    o.setSenhaHash(senha.isEmpty() ? anterior.getSenhaHash() : FuncionarioDAO.gerarHash(senha));
                    o.setAtivo(estaAtivo());
                    if (primeiro) {
                        if (!new FuncionarioDAO().listar().isEmpty()) throw new IllegalArgumentException("Já existe funcionário cadastrado. Entre com seu login.");
                        o.setCargo("GERENTE"); o.setAtivo(1);
                    }
                    if (logado != null && o.getId() == logado.getId()
                            && (o.getAtivo() != 1 || !o.isGerente())) {
                        throw new IllegalArgumentException("Não desative nem remova seu próprio acesso à gerência nesta sessão.");
                    }
                    if (alteracao) new FuncionarioDAO().atualizar(o); else new FuncionarioDAO().inserir(o);
                    break;
                }
                case "Vendas": {
                    Venda o = new Venda();
                    o.setId(alteracao ? idSelecionado : 0);
                    o.setIdCliente(((Cliente)escolhido("Cliente")).getId());
                    o.setIdPlano(((Planos)escolhido("Plano")).getId());
                    o.setIdFuncionario(((Funcionario)escolhido("Funcionário")).getId());
                    o.setDataHora(data("Data/hora (dd/MM/aaaa HH:mm:ss)", true));
                    o.setValorContratado(numero("Valor contratado"));
                    if (logado != null && !logado.isGerente()) o.setIdFuncionario(logado.getId());
                    if (((Cliente)escolhido("Cliente")).getAtivo() != 1 || ((Planos)escolhido("Plano")).getAtivo() != 1
                            || ((Funcionario)escolhido("Funcionário")).getAtivo() != 1) {
                        throw new IllegalArgumentException("Selecione cliente, plano e funcionário ativos.");
                    }
                    if (alteracao) new VendaDAO().atualizar(o); else new VendaDAO().inserir(o);
                    break;
                }
            }
            limpar(); carregar();
            JOptionPane.showMessageDialog(this, "Dados salvos no banco.");
            if (primeiro) dispose();
        } catch (Exception e) { erro(e); }
    }

    private void excluir() {
        if (idSelecionado == 0) { erro(new Exception("Selecione uma linha para excluir.")); return; }
        if (tipo.equals("Funcionários") && logado != null && idSelecionado == logado.getId()) {
            erro(new Exception("Não é possível excluir o funcionário conectado.")); return;
        }
        String aviso = "Excluir definitivamente o registro " + idSelecionado + "?";
        if (tipo.equals("Clientes") || tipo.equals("Planos") || tipo.equals("Funcionários")) {
            aviso += "\nAs vendas vinculadas também serão excluídas, conforme o banco.";
        }
        if (JOptionPane.showConfirmDialog(this, aviso, "Confirmar exclusão", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        try {
            switch (tipo) {
                case "Clientes": new ClienteDAO().excluir(idSelecionado); break;
                case "Planos": new PlanoDAO().excluir(idSelecionado); break;
                case "Equipamentos": new EquipamentoDAO().excluir(idSelecionado); break;
                case "Funcionários": new FuncionarioDAO().excluir(idSelecionado); break;
                case "Vendas": new VendaDAO().excluir(idSelecionado); break;
            }
            limpar(); carregar();
        } catch (Exception e) { erro(e); }
    }

    private void carregar() {
        try {
            tabela.clearSelection();
            java.util.List<Object> novos = new ArrayList<>();
            String[] colunas = {};
            switch (tipo) {
                case "Clientes": novos.addAll(new ClienteDAO().listar()); colunas = new String[]{"ID","Nome","CPF","E-mail","Telefone","Ativo"}; break;
                case "Planos": novos.addAll(new PlanoDAO().listar()); colunas = new String[]{"ID","Nome","Duração (dias)","Preço","Ativo"}; break;
                case "Equipamentos": novos.addAll(new EquipamentoDAO().listar()); colunas = new String[]{"ID","Nome","Data de aquisição (dd/MM/aaaa)","Estado de conservação"}; break;
                case "Funcionários": novos.addAll(new FuncionarioDAO().listar()); colunas = new String[]{"ID","Nome","CPF","Cargo","Login","Ativo"}; break;
                case "Vendas": novos.addAll(new VendaDAO().listar()); colunas = new String[]{"ID","Cliente","Plano","Funcionário","Data/hora (dd/MM/aaaa HH:mm:ss)","Valor contratado"}; break;
            }
            DefaultTableModel m = new DefaultTableModel(colunas, 0) {
                @Override public boolean isCellEditable(int r, int c) { return false; }
            };
            for (Object registro : novos) {
                if (registro instanceof Cliente) {
                    Cliente o = (Cliente)registro;
                    m.addRow(new Object[]{o.getId(), o.getNome(), o.getCpf(), o.getEmail(), o.getTelefone(), o.getAtivo()});
                }
                if (registro instanceof Planos) {
                    Planos o = (Planos)registro;
                    m.addRow(new Object[]{o.getId(), o.getNome(), o.getDuracao(), o.getPreco(), o.getAtivo()});
                }
                if (registro instanceof Equipamento) {
                    Equipamento o = (Equipamento)registro;
                    m.addRow(new Object[]{o.getId(), o.getNome(), o.getDataAquisicao(), o.getEstadoConservacao()});
                }
                if (registro instanceof Funcionario) {
                    Funcionario o = (Funcionario)registro;
                    m.addRow(new Object[]{o.getId(), o.getNome(), o.getCpf(), o.getCargo(), o.getLogin(), o.getAtivo()});
                }
                if (registro instanceof Venda) {
                    Venda o = (Venda)registro;
                    m.addRow(new Object[]{o.getId(), o.getIdCliente(), o.getIdPlano(), o.getIdFuncionario(), o.getDataHora(), o.getValorContratado()});
                }
            }
            registros = novos;
            tabela.setModel(m);
            idSelecionado = 0;
        } catch (Exception e) { erro(e); }
    }

    private void preencher(Object registro) {
        if (registro instanceof Cliente) {
            Cliente o = (Cliente)registro;
            idSelecionado = o.getId();
            set("Nome", o.getNome());
            set("CPF", o.getCpf());
            set("E-mail", o.getEmail());
            set("Telefone", o.getTelefone());
            set("Ativo", o.getAtivo());
        }
        if (registro instanceof Planos) {
            Planos o = (Planos)registro;
            idSelecionado = o.getId();
            set("Nome", o.getNome());
            set("Duração (dias)", o.getDuracao());
            set("Preço", o.getPreco());
            set("Ativo", o.getAtivo());
        }
        if (registro instanceof Equipamento) {
            Equipamento o = (Equipamento)registro;
            idSelecionado = o.getId();
            set("Nome", o.getNome());
            set("Data de aquisição (dd/MM/aaaa)", o.getDataAquisicao());
            set("Estado de conservação", o.getEstadoConservacao());
        }
        if (registro instanceof Funcionario) {
            Funcionario o = (Funcionario)registro;
            idSelecionado = o.getId();
            set("Nome", o.getNome());
            set("CPF", o.getCpf());
            set("Cargo", o.getCargo());
            set("Login", o.getLogin());
            set("Senha (vazia mantém a atual)", "");
            set("Ativo", o.getAtivo());
        }
        if (registro instanceof Venda) {
            Venda o = (Venda)registro;
            idSelecionado = o.getId();
            selecionar("Cliente", o.getIdCliente());
            selecionar("Plano", o.getIdPlano());
            selecionar("Funcionário", o.getIdFuncionario());
            set("Data/hora (dd/MM/aaaa HH:mm:ss)", o.getDataHora());
            set("Valor contratado", o.getValorContratado());
        }
    }
}
