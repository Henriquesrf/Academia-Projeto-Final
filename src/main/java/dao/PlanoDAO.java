package dao;

import model.Planos;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PlanoDAO {
    public void inserir(Planos obj) throws SQLException {
        String sql = "INSERT INTO plano (nome, duracao, preco, ativo) VALUES (?, ?, ?, ?)";
        try (Connection c = Conexao.conectar(); PreparedStatement p = c.prepareStatement(sql)) {
            preencher(p, obj);
            p.executeUpdate();
            try (Statement st = c.createStatement(); ResultSet r = st.executeQuery("SELECT last_insert_rowid()")) {
                if (r.next()) obj.setId(r.getInt(1));
            }
        }
    }

    public void atualizar(Planos obj) throws SQLException {
        String sql = "UPDATE plano SET nome = ?, duracao = ?, preco = ?, ativo = ? WHERE id = ?";
        try (Connection c = Conexao.conectar(); PreparedStatement p = c.prepareStatement(sql)) {
            preencher(p, obj);
            p.setInt(5, obj.getId());
            if (p.executeUpdate() == 0) throw new SQLException("Registro não encontrado. Atualize a lista.");
        }
    }

    public void excluir(int id) throws SQLException {
        try (Connection c = Conexao.conectar(); PreparedStatement p = c.prepareStatement("DELETE FROM plano WHERE id = ?")) {
            p.setInt(1, id);
            if (p.executeUpdate() == 0) throw new SQLException("Registro não encontrado. Atualize a lista.");
        }
    }

    public List<Planos> listar() throws SQLException {
        List<Planos> lista = new ArrayList<>();
        try (Connection c = Conexao.conectar(); PreparedStatement p = c.prepareStatement("SELECT * FROM plano ORDER BY id"); ResultSet r = p.executeQuery()) {
            while (r.next()) lista.add(ler(r));
        }
        return lista;
    }

    public Planos buscarPorId(int id) throws SQLException {
        try (Connection c = Conexao.conectar(); PreparedStatement p = c.prepareStatement("SELECT * FROM plano WHERE id = ?")) {
            p.setInt(1, id);
            try (ResultSet r = p.executeQuery()) { return r.next() ? ler(r) : null; }
        }
    }

    private Planos ler(ResultSet r) throws SQLException {
        Planos obj = new Planos();
        obj.setId(r.getInt("id"));
        obj.setNome(r.getString("nome"));
        obj.setDuracao(r.getInt("duracao"));
        obj.setPreco(r.getDouble("preco"));
        obj.setAtivo(r.getInt("ativo"));
        return obj;
    }

    private void preencher(PreparedStatement p, Planos obj) throws SQLException {
        p.setString(1, obj.getNome());
        p.setInt(2, obj.getDuracao());
        p.setDouble(3, obj.getPreco());
        p.setInt(4, obj.getAtivo());
    }
}
