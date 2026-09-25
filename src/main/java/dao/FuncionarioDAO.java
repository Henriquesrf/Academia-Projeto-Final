package dao;

import model.Funcionario;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FuncionarioDAO {
    public void inserir(Funcionario obj) throws SQLException {
        String sql = "INSERT INTO funcionario (nome, cpf, cargo, login, senha_hash, ativo) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection c = Conexao.conectar(); PreparedStatement p = c.prepareStatement(sql)) {
            preencher(p, obj);
            p.executeUpdate();
            try (Statement st = c.createStatement(); ResultSet r = st.executeQuery("SELECT last_insert_rowid()")) {
                if (r.next()) obj.setId(r.getInt(1));
            }
        }
    }

    public void atualizar(Funcionario obj) throws SQLException {
        String sql = "UPDATE funcionario SET nome = ?, cpf = ?, cargo = ?, login = ?, senha_hash = ?, ativo = ? WHERE id = ?";
        try (Connection c = Conexao.conectar(); PreparedStatement p = c.prepareStatement(sql)) {
            preencher(p, obj);
            p.setInt(7, obj.getId());
            if (p.executeUpdate() == 0) throw new SQLException("Registro não encontrado. Atualize a lista.");
        }
    }

    public void excluir(int id) throws SQLException {
        try (Connection c = Conexao.conectar(); PreparedStatement p = c.prepareStatement("DELETE FROM funcionario WHERE id = ?")) {
            p.setInt(1, id);
            if (p.executeUpdate() == 0) throw new SQLException("Registro não encontrado. Atualize a lista.");
        }
    }

    public List<Funcionario> listar() throws SQLException {
        List<Funcionario> lista = new ArrayList<>();
        try (Connection c = Conexao.conectar(); PreparedStatement p = c.prepareStatement("SELECT * FROM funcionario ORDER BY id"); ResultSet r = p.executeQuery()) {
            while (r.next()) lista.add(ler(r));
        }
        return lista;
    }

    public Funcionario buscarPorId(int id) throws SQLException {
        try (Connection c = Conexao.conectar(); PreparedStatement p = c.prepareStatement("SELECT * FROM funcionario WHERE id = ?")) {
            p.setInt(1, id);
            try (ResultSet r = p.executeQuery()) { return r.next() ? ler(r) : null; }
        }
    }

    private Funcionario ler(ResultSet r) throws SQLException {
        Funcionario obj = new Funcionario();
        obj.setId(r.getInt("id"));
        obj.setNome(r.getString("nome"));
        obj.setCpf(r.getString("cpf"));
        obj.setCargo(r.getString("cargo"));
        obj.setLogin(r.getString("login"));
        obj.setSenhaHash(r.getString("senha_hash"));
        obj.setAtivo(r.getInt("ativo"));
        return obj;
    }

    private void preencher(PreparedStatement p, Funcionario obj) throws SQLException {
        p.setString(1, obj.getNome());
        p.setString(2, obj.getCpf());
        p.setString(3, obj.getCargo());
        p.setString(4, obj.getLogin());
        p.setString(5, obj.getSenhaHash());
        p.setInt(6, obj.getAtivo());
    }

    public Funcionario autenticar(String login, String senha) throws SQLException {
        try (Connection c = Conexao.conectar(); PreparedStatement p = c.prepareStatement(
                "SELECT * FROM funcionario WHERE login = ? AND ativo = 1")) {
            p.setString(1, login);
            try (ResultSet r = p.executeQuery()) {
                if (!r.next()) return null;
                Funcionario f = ler(r);
                return verificarSenha(senha, f.getSenhaHash()) ? f : null;
            }
        }
    }

    public static String gerarHash(String senha) {
        try {
            byte[] salt = new byte[16];
            new java.security.SecureRandom().nextBytes(salt);
            byte[] hash = derivar(senha, salt);
            return "pbkdf2$" + java.util.Base64.getEncoder().encodeToString(salt)
                    + "$" + java.util.Base64.getEncoder().encodeToString(hash);
        } catch (java.security.GeneralSecurityException e) {
            throw new IllegalStateException("Não foi possível proteger a senha.", e);
        }
    }

    private static byte[] derivar(String senha, byte[] salt) throws java.security.GeneralSecurityException {
        javax.crypto.spec.PBEKeySpec spec = new javax.crypto.spec.PBEKeySpec(senha.toCharArray(), salt, 120000, 256);
        try { return javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded(); }
        finally { spec.clearPassword(); }
    }

    private static boolean verificarSenha(String senha, String hash) {
        if (hash == null || !hash.startsWith("pbkdf2$")) return false;
        try {
            String[] partes = hash.split("\\$");
            return java.security.MessageDigest.isEqual(derivar(senha, java.util.Base64.getDecoder().decode(partes[1])),
                    java.util.Base64.getDecoder().decode(partes[2]));
        } catch (Exception e) { return false; }
    }
}
