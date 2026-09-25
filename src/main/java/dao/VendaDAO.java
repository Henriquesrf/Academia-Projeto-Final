package dao;

import model.Venda;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class VendaDAO {
    public void inserir(Venda obj) throws SQLException {
        String sql = "INSERT INTO venda (id_cliente, id_plano, id_funcionario, data_hora, valor_contratado) VALUES (?, ?, ?, ?, ?)";
        try (Connection c = Conexao.conectar(); PreparedStatement p = c.prepareStatement(sql)) {
            preencher(p, obj);
            p.executeUpdate();
            try (Statement st = c.createStatement(); ResultSet r = st.executeQuery("SELECT last_insert_rowid()")) {
                if (r.next()) obj.setId(r.getInt(1));
            }
        }
    }

    public void atualizar(Venda obj) throws SQLException {
        String sql = "UPDATE venda SET id_cliente = ?, id_plano = ?, id_funcionario = ?, data_hora = ?, valor_contratado = ? WHERE id = ?";
        try (Connection c = Conexao.conectar(); PreparedStatement p = c.prepareStatement(sql)) {
            preencher(p, obj);
            p.setInt(6, obj.getId());
            if (p.executeUpdate() == 0) throw new SQLException("Registro não encontrado. Atualize a lista.");
        }
    }

    public void excluir(int id) throws SQLException {
        try (Connection c = Conexao.conectar(); PreparedStatement p = c.prepareStatement("DELETE FROM venda WHERE id = ?")) {
            p.setInt(1, id);
            if (p.executeUpdate() == 0) throw new SQLException("Registro não encontrado. Atualize a lista.");
        }
    }

    public List<Venda> listar() throws SQLException {
        List<Venda> lista = new ArrayList<>();
        try (Connection c = Conexao.conectar(); PreparedStatement p = c.prepareStatement("SELECT * FROM venda ORDER BY id"); ResultSet r = p.executeQuery()) {
            while (r.next()) lista.add(ler(r));
        }
        return lista;
    }

    public Venda buscarPorId(int id) throws SQLException {
        try (Connection c = Conexao.conectar(); PreparedStatement p = c.prepareStatement("SELECT * FROM venda WHERE id = ?")) {
            p.setInt(1, id);
            try (ResultSet r = p.executeQuery()) { return r.next() ? ler(r) : null; }
        }
    }

    private Venda ler(ResultSet r) throws SQLException {
        Venda obj = new Venda();
        obj.setId(r.getInt("id"));
        obj.setIdCliente(r.getInt("id_cliente"));
        obj.setIdPlano(r.getInt("id_plano"));
        obj.setIdFuncionario(r.getInt("id_funcionario"));
        obj.setDataHora(r.getString("data_hora"));
        obj.setValorContratado(r.getDouble("valor_contratado"));
        return obj;
    }

    private void preencher(PreparedStatement p, Venda obj) throws SQLException {
        p.setInt(1, obj.getIdCliente());
        p.setInt(2, obj.getIdPlano());
        p.setInt(3, obj.getIdFuncionario());
        p.setString(4, obj.getDataHora());
        p.setDouble(5, obj.getValorContratado());
    }
}
