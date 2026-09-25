package dao;

import java.sql.*;
import java.util.*;

public class GerenciaDAO {
    public List<Object[]> listarVendas(String cliente) throws SQLException {
        List<Object[]> linhas = new ArrayList<>();
        String sql = "SELECT v.id, c.nome, p.nome, v.data_hora, v.valor_contratado, f.nome "
                + "FROM venda v JOIN clientes c ON c.id=v.id_cliente "
                + "JOIN plano p ON p.id=v.id_plano JOIN funcionario f ON f.id=v.id_funcionario "
                + "WHERE c.nome LIKE ? OR c.cpf LIKE ? ORDER BY v.id DESC";
        try (Connection c = Conexao.conectar(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, "%" + cliente + "%"); p.setString(2, "%" + cliente + "%");
            try (ResultSet r = p.executeQuery()) {
                while (r.next()) linhas.add(new Object[]{r.getInt(1),r.getString(2),r.getString(3),r.getString(4),r.getDouble(5),r.getString(6)});
            }
        }
        return linhas;
    }
}
