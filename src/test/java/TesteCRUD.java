import dao.*;
import model.*;
import java.sql.*;

/** Executar em pasta temporária, sem banco.db. Não usa nem modifica o banco do projeto. */
public class TesteCRUD {
    private static void conferir(boolean valor, String mensagem) {
        if (!valor) throw new AssertionError(mensagem);
    }
    public static void main(String[] args) throws Exception {
        if (new java.io.File("banco.db").exists()) throw new IllegalStateException("Use uma pasta temporária sem banco.db.");
        Conexao.inicializarBanco();
        ClienteDAO cd = new ClienteDAO(); PlanoDAO pd = new PlanoDAO();
        FuncionarioDAO fd = new FuncionarioDAO(); EquipamentoDAO ed = new EquipamentoDAO(); VendaDAO vd = new VendaDAO();
        Cliente c = new Cliente(); c.setNome("Cliente d'Ávila"); c.setCpf("123"); c.setEmail("a@b.com"); c.setTelefone("51999999999"); c.setAtivo(1);
        cd.inserir(c); conferir(c.getId() > 0 && cd.listar().size() == 1, "Inserção de cliente");
        c.setNome("Cliente alterado"); cd.atualizar(c); conferir(cd.buscarPorId(c.getId()).getNome().equals(c.getNome()), "Alteração de cliente");
        Planos p = new Planos(); p.setNome("Mensal"); p.setDuracao(30); p.setPreco(100.50); p.setAtivo(1);
        pd.inserir(p); p.setPreco(120.75); pd.atualizar(p); conferir(pd.buscarPorId(p.getId()).getPreco() == 120.75 && pd.listar().size() == 1, "CRUD plano");
        Funcionario f = new Funcionario(); f.setNome("Gerente"); f.setCpf("456"); f.setCargo("GERENTE"); f.setLogin("gerente"); f.setSenhaHash(FuncionarioDAO.gerarHash("senhaTeste")); f.setAtivo(1);
        fd.inserir(f); f.setNome("Gerente alterado"); fd.atualizar(f); conferir(fd.buscarPorId(f.getId()).getNome().equals(f.getNome()), "Alteração funcionário");
        conferir(fd.autenticar("gerente", "senhaTeste").isGerente(), "Login correto");
        conferir(fd.autenticar("gerente", "errada") == null, "Senha errada");
        conferir(fd.autenticar("' OR 1=1 --", "senhaTeste") == null, "Parâmetros SQL");
        f.setAtivo(0); fd.atualizar(f); conferir(fd.autenticar("gerente", "senhaTeste") == null, "Login inativo"); f.setAtivo(1); fd.atualizar(f);
        try { fd.inserir(f); throw new AssertionError("Login duplicado permitido"); } catch (SQLException esperado) { }
        Equipamento e = new Equipamento(); e.setNome("Esteira"); e.setDataAquisicao("24/09/2026"); e.setEstadoConservacao("Novo");
        ed.inserir(e); e.setEstadoConservacao("Usado"); ed.atualizar(e); conferir(ed.buscarPorId(e.getId()).getEstadoConservacao().equals("Usado") && ed.listar().size() == 1, "CRUD equipamento");
        Venda v = new Venda(); v.setIdCliente(c.getId()); v.setIdPlano(p.getId()); v.setIdFuncionario(f.getId()); v.setDataHora("24/09/2026 12:00:00"); v.setValorContratado(120.75);
        vd.inserir(v); v.setValorContratado(110); vd.atualizar(v); conferir(vd.buscarPorId(v.getId()).getValorContratado() == 110 && vd.listar().size() == 1, "CRUD venda");
        conferir(new GerenciaDAO().listarVendas("alterado").size() == 1, "Busca de vendas por cliente");
        conferir(new GerenciaDAO().listarVendas("inexistente").isEmpty(), "Filtro sem resultados");
        javax.swing.SwingUtilities.invokeAndWait(() -> {
            conferir(contarLinhas(new view.TelaFuncionario(f)) == 1, "Tabela do operador");
            conferir(contarLinhas(new view.TelaGerencia(f)) == 1, "Tabela da gerência");
        });
        v.setIdCliente(9999);
        try { vd.inserir(v); throw new AssertionError("FK inválida permitida"); } catch (SQLException esperado) { }
        vd.excluir(v.getId()); conferir(vd.listar().isEmpty(), "Excluir venda");
        v.setIdCliente(c.getId()); vd.inserir(v); cd.excluir(c.getId()); conferir(vd.listar().isEmpty() && cd.listar().isEmpty(), "Exclusão em cascata do banco");
        ed.excluir(e.getId()); pd.excluir(p.getId()); fd.excluir(f.getId());
        conferir(ed.listar().isEmpty() && pd.listar().isEmpty() && fd.listar().isEmpty(), "Exclusão dos demais cadastros");
        try (Connection con = Conexao.conectar(); Statement st = con.createStatement(); ResultSet r = st.executeQuery("PRAGMA integrity_check")) {
            conferir(r.next() && "ok".equals(r.getString(1)), "Integridade SQLite");
        }
        System.out.println("OK: CRUD das cinco tabelas, autenticação, filtros, painéis Swing, restrições e integridade SQLite.");
    }
    private static int contarLinhas(java.awt.Container c) {
        for (java.awt.Component filho : c.getComponents()) {
            if (filho instanceof javax.swing.JTable) return ((javax.swing.JTable)filho).getRowCount();
            if (filho instanceof java.awt.Container) { int n = contarLinhas((java.awt.Container)filho); if (n >= 0) return n; }
        }
        return -1;
    }
}
