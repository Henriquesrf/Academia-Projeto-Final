package dao;

import model.Equipamento;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EquipamentoDAO {
    public void inserir(Equipamento obj) throws SQLException {
        String sql = "INSERT INTO equipamento (nome, data_aquisicao, estado_conservacao) VALUES (?, ?, ?)";
        try (Connection c = Conexao.conectar(); PreparedStatement p = c.prepareStatement(sql)) {
            preencher(p, obj);
            p.executeUpdate();
            try (Statement st = c.createStatement(); ResultSet r = st.executeQuery("SELECT last_insert_rowid()")) {
                if (r.next()) obj.setId(r.getInt(1));
            }
        }
    }

    public void atualizar(Equipamento obj) throws SQLException {
        String sql = "UPDATE equipamento SET nome = ?, data_aquisicao = ?, estado_conservacao = ? WHERE id = ?";
        try (Connection c = Conexao.conectar(); PreparedStatement p = c.prepareStatement(sql)) {
            preencher(p, obj);
            p.setInt(4, obj.getId());
            if (p.executeUpdate() == 0) throw new SQLException("Registro não encontrado. Atualize a lista.");
        }
    }

    public void excluir(int id) throws SQLException {
        try (Connection c = Conexao.conectar(); PreparedStatement p = c.prepareStatement("DELETE FROM equipamento WHERE id = ?")) {
            p.setInt(1, id);
            if (p.executeUpdate() == 0) throw new SQLException("Registro não encontrado. Atualize a lista.");
        }
    }

    public List<Equipamento> listar() throws SQLException {
        List<Equipamento> lista = new ArrayList<>();
        try (Connection c = Conexao.conectar(); PreparedStatement p = c.prepareStatement("SELECT * FROM equipamento ORDER BY id"); ResultSet r = p.executeQuery()) {
            while (r.next()) lista.add(ler(r));
        }
        return lista;
    }

    public Equipamento buscarPorId(int id) throws SQLException {
        try (Connection c = Conexao.conectar(); PreparedStatement p = c.prepareStatement("SELECT * FROM equipamento WHERE id = ?")) {
            p.setInt(1, id);
            try (ResultSet r = p.executeQuery()) { return r.next() ? ler(r) : null; }
        }
    }

    private Equipamento ler(ResultSet r) throws SQLException {
        Equipamento obj = new Equipamento();
        obj.setId(r.getInt("id"));
        obj.setNome(r.getString("nome"));
        obj.setDataAquisicao(r.getString("data_aquisicao"));
        obj.setEstadoConservacao(r.getString("estado_conservacao"));
        return obj;
    }

    private void preencher(PreparedStatement p, Equipamento obj) throws SQLException {
        p.setString(1, obj.getNome());
        p.setString(2, obj.getDataAquisicao());
        p.setString(3, obj.getEstadoConservacao());
    }
}
