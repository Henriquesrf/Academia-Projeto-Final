package dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Conexao {

    private static final String URL = "jdbc:sqlite:banco.db";

    public static Connection conectar() throws SQLException {
        Connection conexao = DriverManager.getConnection(URL);

        try (Statement comando = conexao.createStatement()) {
            comando.execute("PRAGMA foreign_keys = ON");
        } catch (SQLException e) {
            try {
                conexao.close();
            } catch (SQLException erroFechamento) {
                e.addSuppressed(erroFechamento);
            }
            throw e;
        }

        return conexao;
    }

    public static void inicializarBanco() {
        String[] tabelas = {
            "CREATE TABLE IF NOT EXISTS clientes ("
                + "id INTEGER PRIMARY KEY, "
                + "nome VARCHAR2(100) NOT NULL, "
                + "cpf VARCHAR2(14) NOT NULL, "
                + "email VARCHAR2(100) NOT NULL, "
                + "telefone VARCHAR2(100) NOT NULL, "
                + "ativo INTEGER"
                + ")",

            "CREATE TABLE IF NOT EXISTS equipamento ("
                + "id INTEGER PRIMARY KEY, "
                + "nome VARCHAR2(100) NOT NULL, "
                + "data_aquisicao VARCHAR(10) NOT NULL, "
                + "estado_conservacao VARCHAR(50) NOT NULL"
                + ")",

            "CREATE TABLE IF NOT EXISTS funcionario ("
                + "id INTEGER PRIMARY KEY, "
                + "nome VARCHAR2(100) NOT NULL, "
                + "cpf VARCHAR2(30) NOT NULL UNIQUE, "
                + "cargo VARCHAR2(30) NOT NULL, "
                + "login VARCHAR2(50) NOT NULL UNIQUE, "
                + "senha_hash VARCHAR2(255) NOT NULL, "
                + "ativo INTEGER"
                + ")",

            "CREATE TABLE IF NOT EXISTS plano ("
                + "id INTEGER PRIMARY KEY, "
                + "nome VARCHAR2(100) NOT NULL, "
                + "duracao INT NOT NULL, "
                + "preco FLOAT NOT NULL, "
                + "ativo INTEGER"
                + ")",

            "CREATE TABLE IF NOT EXISTS venda ("
                + "id INTEGER PRIMARY KEY, "
                + "id_cliente INTEGER NOT NULL, "
                + "id_plano INTEGER NOT NULL, "
                + "id_funcionario INTEGER NOT NULL, "
                + "data_hora VARCHAR2(19) NOT NULL, "
                + "valor_contratado FLOAT NOT NULL, "
                + "FOREIGN KEY (id_cliente) REFERENCES clientes(id)"
                + " ON DELETE CASCADE, "
                + "FOREIGN KEY (id_plano) REFERENCES plano(id)"
                + " ON DELETE CASCADE, "
                + "FOREIGN KEY (id_funcionario) REFERENCES funcionario(id)"
                + " ON DELETE CASCADE"
                + ")"
        };

        try (Connection conexao = conectar()) {
            conexao.setAutoCommit(false);

            try (Statement comando = conexao.createStatement()) {
                for (String sql : tabelas) {
                    comando.executeUpdate(sql);
                }

                conexao.commit();
                System.out.println("Banco conectado!");
                System.out.println("Tabelas da academia verificadas.");

            } catch (SQLException e) {
                try {
                    conexao.rollback();
                } catch (SQLException erroRollback) {
                    e.addSuppressed(erroRollback);
                }
                throw e;
            }

        } catch (SQLException e) {
            throw new IllegalStateException(
                "Erro ao inicializar o Banco: " + e.getMessage(), e
            );
        }
    }
}