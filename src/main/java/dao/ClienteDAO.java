package dao;

import model.Cliente;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ClienteDAO {
    public void inserir(Cliente obj) throws SQLException {
        String sql = "INSERT INTO clientes (nome, cpf, email, telefone, ativo) VALUES (?, ?, ?, ?, ?)";
        try (Connection c = Conexao.conectar(); PreparedStatement p = c.prepareStatement(sql)) {
            preencher(p, obj);
            p.executeUpdate();
            try (Statement st = c.createStatement(); ResultSet r = st.executeQuery("SELECT last_insert_rowid()")) {
                if (r.next()) obj.setId(r.getInt(1));
            }
        }
    }

    public void atualizar(Cliente obj) throws SQLException {
        String sql = "UPDATE clientes SET nome = ?, cpf = ?, email = ?, telefone = ?, ativo = ? WHERE id = ?";
        try (Connection c = Conexao.conectar(); PreparedStatement p = c.prepareStatement(sql)) {
            preencher(p, obj);
            p.setInt(6, obj.getId());
            if (p.executeUpdate() == 0) throw new SQLException("Registro não encontrado. Atualize a lista.");
        }
    }

    public void excluir(int id) throws SQLException {
        try (Connection c = Conexao.conectar(); PreparedStatement p = c.prepareStatement("DELETE FROM clientes WHERE id = ?")) {
            p.setInt(1, id);
            if (p.executeUpdate() == 0) throw new SQLException("Registro não encontrado. Atualize a lista.");
        }
    }

    public List<Cliente> listar() throws SQLException {
        List<Cliente> lista = new ArrayList<>();
        try (Connection c = Conexao.conectar(); PreparedStatement p = c.prepareStatement("SELECT * FROM clientes ORDER BY id"); ResultSet r = p.executeQuery()) {
            while (r.next()) lista.add(ler(r));
        }
        return lista;
    }

    public Cliente buscarPorId(int id) throws SQLException {
        try (Connection c = Conexao.conectar(); PreparedStatement p = c.prepareStatement("SELECT * FROM clientes WHERE id = ?")) {
            p.setInt(1, id);
            try (ResultSet r = p.executeQuery()) { return r.next() ? ler(r) : null; }
        }
    }

    private Cliente ler(ResultSet r) throws SQLException {
        Cliente obj = new Cliente();
        obj.setId(r.getInt("id"));
        obj.setNome(r.getString("nome"));
        obj.setCpf(r.getString("cpf"));
        obj.setEmail(r.getString("email"));
        obj.setTelefone(r.getString("telefone"));
        obj.setAtivo(r.getInt("ativo"));
        return obj;
    }

    private void preencher(PreparedStatement p, Cliente obj) throws SQLException {
        p.setString(1, obj.getNome());
        p.setString(2, obj.getCpf());
        p.setString(3, obj.getEmail());
        p.setString(4, obj.getTelefone());
        p.setInt(5, obj.getAtivo());
    }
}
