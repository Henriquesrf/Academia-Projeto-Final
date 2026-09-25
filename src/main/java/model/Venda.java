package model;

public class Venda {
    private int id;
    private int idCliente;
    private int idPlano;
    private int idFuncionario;
    private String dataHora;
    private double valorContratado;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getIdCliente() { return idCliente; }
    public void setIdCliente(int idCliente) { this.idCliente = idCliente; }

    public int getIdPlano() { return idPlano; }
    public void setIdPlano(int idPlano) { this.idPlano = idPlano; }

    public int getIdFuncionario() { return idFuncionario; }
    public void setIdFuncionario(int idFuncionario) { this.idFuncionario = idFuncionario; }

    public String getDataHora() { return dataHora; }
    public void setDataHora(String dataHora) { this.dataHora = dataHora; }

    public double getValorContratado() { return valorContratado; }
    public void setValorContratado(double valorContratado) { this.valorContratado = valorContratado; }
}
